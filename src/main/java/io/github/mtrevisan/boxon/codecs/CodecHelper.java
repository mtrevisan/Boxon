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

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


final class CodecHelper{

	/** The name of the current object being scanner (used for referencing variables from SpEL). */
	static final String CONTEXT_SELF = "self";
	/** The name of the prefix for the alternative (used for referencing variables from SpEL). */
	private static final String CONTEXT_CHOICE_PREFIX = "prefix";

	private static final Matcher CONTEXT_PREFIXED_CHOICE_PREFIX = Pattern.compile("#" + CONTEXT_CHOICE_PREFIX + "[^a-zA-Z]").matcher("");


	private CodecHelper(){}

	static void assertSizePositive(final int size) throws AnnotationException{
		if(size <= 0)
			throw new AnnotationException("Size must be a positive integer, was {}", size);
	}

	static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw new IllegalArgumentException("Size mismatch, expected " + expectedSize + ", got " + size);
	}

	static void assertCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw new AnnotationException("Invalid charset: '{}'", charsetName);
		}
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(alternative.type() == type)
				return alternative;

		throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final BitReader reader, final ObjectChoices selectFrom, final Object rootObject){
		if(selectFrom.prefixSize() > 0){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();
			final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
				.intValue();

			Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		}

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		return chooseAlternative(alternatives, rootObject);
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives,
			final Object rootObject){
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative;
		return null;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> baseConverter, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(final ConverterChoices.ConverterChoice alternative : alternatives){
			if(Evaluator.evaluate(alternative.condition(), rootObject, boolean.class))
				return alternative.converter();
		}
		return baseConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(containsPrefixReference(chosenAlternative.condition())){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final BitSet bits = BitSet.valueOf(new long[]{chosenAlternative.prefix()});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static boolean containsPrefixReference(final CharSequence condition){
		return CONTEXT_PREFIXED_CHOICE_PREFIX.reset(condition).find();
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
		if(!validator.isValid((T)data))
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
			throw new IllegalArgumentException("Can not input " + data.getClass().getSimpleName() + " to decode method of converter "
				+ converterType.getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ReflectionHelper.getCreator(converterType)
			.get();
		return converter.encode((OUT)data);
	}

}
