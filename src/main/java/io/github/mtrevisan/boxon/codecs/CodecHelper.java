/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
import io.github.mtrevisan.boxon.codecs.managers.ConstructorHelper;
import io.github.mtrevisan.boxon.codecs.managers.ContextHelper;
import io.github.mtrevisan.boxon.codecs.managers.encode.WriterManagerFactory;
import io.github.mtrevisan.boxon.codecs.managers.encode.WriterManagerInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.external.ConfigurationEnum;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.lang.reflect.Array;
import java.nio.charset.Charset;


public final class CodecHelper{

	private static final ObjectChoices.ObjectChoice EMPTY_CHOICE = new NullObjectChoice();


	private CodecHelper(){}

	static void assertSizePositive(final int size) throws AnnotationException{
		if(size <= 0)
			throw AnnotationException.create("Size must be a positive integer, was {}", size);
	}

	static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw new IllegalArgumentException("Size mismatch, expected " + expectedSize + ", got " + size);
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(alternative.type().isAssignableFrom(type))
				return alternative;
		}

		throw new IllegalArgumentException("Cannot find a valid codec for type " + type.getSimpleName());
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final BitReader reader, final ObjectChoices selectFrom, final Object rootObject){
		if(selectFrom.prefixSize() > 0){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();
			final int prefix = reader.getInteger(prefixSize, prefixByteOrder)
				.intValue();

			Evaluator.addToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		return chooseAlternative(alternatives, rootObject);
	}

	private static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives,
			final Object rootObject){
		for(int i = 0; i < alternatives.length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];
			if(Evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative;
		}
		return EMPTY_CHOICE;
	}

	static Class<? extends Converter<?, ?>> chooseConverter(final ConverterChoices selectConverterFrom,
			final Class<? extends Converter<?, ?>> defaultConverter, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = selectConverterFrom.alternatives();
		for(int i = 0; i < alternatives.length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];
			if(Evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return defaultConverter;
	}

	static void writePrefix(final BitWriter writer, final ObjectChoices.ObjectChoice chosenAlternative, final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(ContextHelper.containsPrefixReference(chosenAlternative.condition())){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final BitSet bits = BitSet.valueOf(new long[]{chosenAlternative.prefix()});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				bits.reverseBits(prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> void validateData(final Class<? extends Validator<?>> validatorType, final Object data){
		final Validator<T> validator = (Validator<T>)ConstructorHelper.getCreator(validatorType)
			.get();
		if(!validator.isValid((T)data))
			throw new IllegalArgumentException("Validation with " + validatorType.getSimpleName() + " not passed (value is " + data + ")");
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getCreator(converterType)
				.get();

			return converter.decode((IN)data);
		}
		catch(final Exception e){
			throw new IllegalArgumentException("Can not input " + data.getClass().getSimpleName() + " to decode method of converter "
				+ converterType.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final Object data){
		final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getCreator(converterType)
			.get();
		return converter.encode((OUT)data);
	}

	static void encode(final BitWriter writer, final Class<?> fieldType, Object value, final int radix, final String charsetName)
			throws CodecException{
		final Charset charset = Charset.forName(charsetName);

		value = interpretValue(fieldType, value);
		if(value != null){
			final WriterManagerInterface writerManager = WriterManagerFactory.buildManager(value, writer);
			if(writerManager == null)
				throw CodecException.create("Cannot handle this type of field: {}, please report to the developer",
					ParserDataType.toObjectiveTypeOrSelf(value.getClass()));

			writerManager.put(value, radix, charset);
		}
	}

	private static Object interpretValue(final Class<?> fieldType, Object value) throws CodecException{
		value = JavaHelper.getValueOrDefault(fieldType, value);
		if(value != null){
			if(value.getClass().isEnum())
				value = ((ConfigurationEnum)value).getCode();
			else if(value.getClass().isArray())
				value = calculateCompositeValue(value);
		}
		return value;
	}

	private static int calculateCompositeValue(final Object value){
		int compositeEnumValue = 0;
		for(int i = 0; i < Array.getLength(value); i ++)
			compositeEnumValue |= ((ConfigurationEnum)Array.get(value, i)).getCode();
		return compositeEnumValue;
	}

}
