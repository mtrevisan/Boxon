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
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.enums.ByteOrder;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.BitReader;
import io.github.mtrevisan.boxon.helpers.BitWriter;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.valueobjects.BitSet;

import java.util.Objects;
import java.util.regex.Pattern;


final class CodecHelper{

	/** The name of the current object being scanner (used for referencing variables from SpEL). */
	public static final String CONTEXT_SELF = "self";
	/** The name of the prefix for the alternative (used for referencing variables from SpEL). */
	public static final String CONTEXT_CHOICE_PREFIX = "prefix";

	static final Pattern CONTEXT_PREFIXED_CHOICE_PREFIX = Pattern.compile("#" + CONTEXT_CHOICE_PREFIX + "[^a-zA-Z]");


	private CodecHelper(){}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(alternative.type() == type)
				return alternative;
		}

		throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());
	}

	static ObjectChoices.ObjectChoice chooseAlternativeWithPrefix(final BitReader reader, final ObjectChoices selectFrom, final Object rootObject){
		final int prefixSize = selectFrom.prefixSize();
		final ByteOrder prefixByteOrder = selectFrom.byteOrder();
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();

		final int prefix = reader.getInteger(prefixSize, prefixByteOrder, true)
			.intValue();

		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives, rootObject);
		if(chosenAlternative == null)
			throw new CodecException("Cannot find a valid codec for prefix {} in object {}", prefix, rootObject.getClass().getSimpleName());

		return chosenAlternative;
	}

	static ObjectChoices.ObjectChoice chooseAlternativeWithoutPrefix(final ObjectChoices selectFrom, final Object rootObject){
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();

		final ObjectChoices.ObjectChoice chosenAlternative = chooseAlternative(alternatives, rootObject);
		if(chosenAlternative == null)
			throw new CodecException("Cannot find a valid codec in object {}", rootObject.getClass().getSimpleName());

		return chosenAlternative;
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Object rootObject){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative;
		}
		return null;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom, final Class<? extends Converter<?, ?>> baseConverter,
			final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0; i < alternatives.length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative.converter();
		}
		return baseConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(CONTEXT_PREFIXED_CHOICE_PREFIX.matcher(chosenAlternative.condition()).find()){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final long prefixValue = chosenAlternative.prefix();
			final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static void validateData(final String match, final Class<? extends Validator<?>> validatorType, final Object currentObject){
		matchData(match, currentObject);
		validateData(validatorType, currentObject);
	}

	private static void matchData(final String match, final Object currentObject){
		final Pattern pattern = extractPattern(match);
		if(pattern != null && !pattern.matcher(Objects.toString(currentObject)).matches())
			throw new IllegalArgumentException("Value `" + currentObject + "` does not match constraint `" + match + "`");
	}

	/** Extract pattern from a SpEL expression, or a string, or a regex pattern. */
	private static Pattern extractPattern(String match){
		Pattern p = null;
		if(JavaHelper.isNotBlank(match)){
			//try SpEL expression
			match = extractSpELExpression(match);

			//try regex expression
			p = extractRegexExpression(match);

			//match exact
			if(p == null)
				p = extractRegexExpression("^" + Pattern.quote(match) + "$");
		}
		return p;
	}

	private static String extractSpELExpression(String match){
		try{
			match = Evaluator.evaluate(match, null, String.class);
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

	@SuppressWarnings("unchecked")
	static <T> void validateData(final Class<? extends Validator<?>> validatorType, final Object data){
		final Validator<T> validator = (Validator<T>)ReflectionHelper.getCreator(validatorType)
			.get();
		if(!validator.validate((T)data))
			throw new IllegalArgumentException("Validation with " + validatorType.getSimpleName() + " not passed (value is " + data + ")");
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
				.get();

			return converter.decode((IN)data);
		}
		catch(final ClassCastException ignored){
			throw new IllegalArgumentException("Can not input " + data.getClass().getSimpleName() + " to decode method of converter " + converterType.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode((OUT)data);
	}

}
