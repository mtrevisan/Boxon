/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.ByteOrder;
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.exceptions.ProtocolMessageException;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.helpers.BitSet;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;

import java.util.Objects;
import java.util.regex.Pattern;


final class CodecHelper{

	public static final String CONTEXT_CHOICE_PREFIX = "prefix";
	public static final String CONTEXT_PREFIXED_CHOICE_PREFIX = "#" + CONTEXT_CHOICE_PREFIX;


	private CodecHelper(){}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(int i = 0; i < alternatives.length; i ++)
			if(alternatives[i].type() == type)
				return alternatives[i];
		return null;
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final BitReader reader, final int prefixSize, final ByteOrder prefixByteOrder,
			final ObjectChoices.ObjectChoice[] alternatives, final Object data){
		final Integer prefix = reader.getBigInteger(prefixSize, prefixByteOrder, true).intValue();

		final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives, prefix, data);
		if(chosenAlternative == null)
			throw new ProtocolMessageException("Cannot find a valid codec for prefix {}", prefix);

		return chosenAlternative;
	}

	static ObjectChoices.ObjectChoice chooseAlternativeNoPrefix(final ObjectChoices.ObjectChoice[] alternatives, final Object data){
		final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives, null, data);
		if(chosenAlternative == null)
			throw new ProtocolMessageException("Cannot find a valid codec");

		return chosenAlternative;
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Integer prefix, final Object data){
		ObjectChoices.ObjectChoice chosenAlternative = null;

		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		for(int i = 0; i < alternatives.length; i ++)
			if(Evaluator.evaluate(alternatives[i].condition(), boolean.class, data)){
				chosenAlternative = alternatives[i];
				break;
			}
		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, null);

		return chosenAlternative;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom, final Class<? extends Converter<?, ?>> baseConverter,
			final Object data){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0; i < alternatives.length; i ++)
			if(Evaluator.evaluate(alternatives[i].condition(), boolean.class, data))
				return alternatives[i].converter();
		return baseConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
		if(chosenAlternative.condition().contains(CONTEXT_PREFIXED_CHOICE_PREFIX)){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final long prefixValue = chosenAlternative.prefix();
			final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static void validateData(final String match, final Class<? extends Validator<?>> validatorType, final Object data){
		matchData(match, data);
		validateData(validatorType, data);
	}

	private static <T> void matchData(final String match, final T data){
		final Pattern pattern = extractPattern(match, data);
		if(pattern != null && !pattern.matcher(Objects.toString(data)).matches())
			throw new IllegalArgumentException("Value `" + Objects.toString(data) + "` does not match constraint `" + match + "`");
	}

	/** Extract pattern from a SpEL expression, or a string, or a regex pattern */
	private static <T> Pattern extractPattern(String match, final T data){
		Pattern p = null;
		if(isNotBlank(match)){
			//try SpEL expression
			match = extractSpELExpression(match, data);

			//try regex expression
			p = extractRegexExpression(match);

			//match exact
			if(p == null)
				p = extractRegexExpression("^" + Pattern.quote(match) + "$");
		}
		return p;
	}

	private static <T> String extractSpELExpression(String match, final T data){
		try{
			match = Evaluator.evaluate(match, String.class, data);
		}
		catch(final Exception ignored){}
		return match;
	}

	private static Pattern extractRegexExpression(final String match){
		Pattern p = null;
		try{
			p = Pattern.compile(match);
		}
		catch(final Exception ignored){}
		return p;
	}

	private static boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

	static <T> void validateData(final Class<? extends Validator<?>> validatorType, final T data){
		@SuppressWarnings("unchecked")
		final Validator<T> validator = (Validator<T>)ReflectionHelper.getCreator(validatorType)
			.get();
		if(!validator.validate(data))
			throw new IllegalArgumentException("Validation not passed (" + data + ")");
	}

	static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final IN data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.decode(data);
	}

	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final OUT data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode(data);
	}

}
