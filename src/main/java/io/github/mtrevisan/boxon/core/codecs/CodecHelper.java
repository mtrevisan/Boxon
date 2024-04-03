/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.reflect.Array;
import java.util.BitSet;


/**
 * A collection of convenience methods for working with codecs.
 */
final class CodecHelper{

	private CodecHelper(){}


	static void assertSizePositive(final int size) throws AnnotationException{
		if(size <= 0)
			throw AnnotationException.create("Size must be a positive integer, was {}", size);
	}

	static void assertSizeNonNegative(final int size) throws AnnotationException{
		if(size < 0)
			throw AnnotationException.create("Size must be a non-negative integer, was {}", size);
	}

	static void assertSizeEquals(final int expectedSize, final int size){
		if(expectedSize != size)
			throw DataException.create("Size mismatch, expected {}, got {}", expectedSize, size);
	}

	static ObjectChoices.ObjectChoice chooseAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type)
			throws CodecException{
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			if(alternative.type().isAssignableFrom(type))
				return alternative;
		}

		throw CodecException.create("Cannot find a valid codec for type {}", type.getSimpleName());
	}

	static void writeHeader(final BitWriterInterface writer, final ObjectChoices.ObjectChoice chosenAlternative,
			final ObjectChoices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @ObjectChoice.prefix()
		if(ContextHelper.containsHeaderReference(chosenAlternative.condition())){
			final int prefixSize = selectFrom.prefixLength();

			final BitSet bitmap = BitSetHelper.createBitSet(prefixSize, chosenAlternative.prefix());

			writer.putBitSet(bitmap, prefixSize);
		}
	}

	static <IN, OUT> OUT convertValue(final BindingData bindingData, final IN value){
		final Class<? extends Converter<?, ?>> converterType = bindingData.getChosenConverter();
		final OUT convertedValue = converterDecode(converterType, value);
		bindingData.validate(convertedValue);
		return convertedValue;
	}

	@SuppressWarnings("unchecked")
	private static <IN, OUT> OUT converterDecode(final Class<? extends Converter<?, ?>> converterType, final IN data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getEmptyCreator(converterType)
				.get();

			return converter.decode(data);
		}
		catch(final Exception e){
			throw DataException.create("Can not input {} ({}) to decode method of converter {}",
				data.getClass().getSimpleName(), data, converterType.getSimpleName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	static <IN, OUT> IN converterEncode(final Class<? extends Converter<?, ?>> converterType, final OUT data){
		try{
			final Converter<IN, OUT> converter = (Converter<IN, OUT>)ConstructorHelper.getEmptyCreator(converterType)
				.get();
			return converter.encode(data);
		}
		catch(final Exception e){
			throw DataException.create("Can not input {} ({}) to encode method of converter {}",
				data.getClass().getSimpleName(), data, converterType.getSimpleName(), e);
		}
	}

	static Object interpretValue(final Class<?> fieldType, Object value) throws CodecException{
		value = ParserDataType.getValueOrSelf(fieldType, value);
		if(value != null){
			if(value instanceof ConfigurationEnum v)
				value = v.getCode();
			else if(value.getClass().isArray())
				value = calculateCompositeValue(value);
		}
		return value;
	}

	private static int calculateCompositeValue(final Object value){
		int compositeEnumValue = 0;
		for(int i = 0, length = Array.getLength(value); i < length; i ++)
			compositeEnumValue |= ((ConfigurationEnum)Array.get(value, i)).getCode();
		return compositeEnumValue;
	}

}
