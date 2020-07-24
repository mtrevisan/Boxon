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
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.helpers.BitSet;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


final class CodecHelper{

	public static final String CONTEXT_CHOICE_PREFIX = "prefix";


	private CodecHelper(){}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Integer prefix, final Object data){
		ObjectChoices.ObjectChoice chosenAlternative = null;

		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), boolean.class, data)){
				chosenAlternative = alternative;
				break;
			}
		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, null);

		return chosenAlternative;
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(alternative.type() == type)
				return alternative;
		return null;
	}

	@SuppressWarnings("rawtypes")
	static Class<? extends Converter> chooseConverter(final ConverterChoices selectConverterFrom, final Class<? extends Converter> baseConverter,
			final Object data){
		final ConverterChoices.ConverterChoice[] alternatives = (selectConverterFrom != null? selectConverterFrom.alternatives(): null);
		if(alternatives != null)
			for(final ConverterChoices.ConverterChoice alternative : alternatives)
				if(Evaluator.evaluate(alternative.condition(), boolean.class, data))
					return alternative.converter();
		return baseConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
		if(chosenAlternative.condition().contains("#" + CONTEXT_CHOICE_PREFIX)){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final long prefixValue = chosenAlternative.prefix();
			final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static <T> void validateData(final String match, @SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType, final T data){
		matchData(match, data);
		validateData(validatorType, data);
	}

	private static <T> void matchData(final String match, final T data){
		final Pattern pattern = extractPattern(match, data);
		if(pattern != null && !pattern.matcher(Objects.toString(data)).matches())
			throw new IllegalArgumentException("Parameter does not match constraint `" + match + "`");
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
		catch(final PatternSyntaxException ignored){}
		return p;
	}

	private static boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

	static <T> void validateData(@SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType, final T data){
		@SuppressWarnings("unchecked")
		final Validator<T> validator = ReflectionHelper.getCreator(validatorType)
			.get();
		if(!validator.validate(data))
			throw new IllegalArgumentException("Validation not passed (" + data + ")");
	}

	static <OUT, IN> OUT converterDecode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType, final IN data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.getCreator(converterType)
			.get();
		return converter.decode(data);
	}

	static <OUT, IN> IN converterEncode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType, final OUT data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode(data);
	}

}
