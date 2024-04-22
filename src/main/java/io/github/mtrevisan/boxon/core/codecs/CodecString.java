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

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.function.BiFunction;


final class CodecString implements CodecInterface<BindString>{

	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
		final Object rootObject) throws AnnotationException{
		final BindString binding = (BindString)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final Charset charset = CharsetHelper.lookup(binding.charset());

		Object instance = null;
		if(collectionBinding == null)
			instance = readValue(reader, size, charset);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			instance = readWithoutAlternatives(reader, arraySize, size, charset);
		}

		final Object convertedValue = convertValue(binding, instance, rootObject, CodecHelper::converterDecode);

		final Class<? extends Validator<?>> validator = binding.validator();
		CodecHelper.validate(convertedValue, validator);

		return convertedValue;
	}

	private static Object readWithoutAlternatives(final BitReaderInterface reader, final int arraySize, final int elementSize,
		final Charset charset){
		final Object array = CodecHelper.createArray(String.class, arraySize);
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readValue(reader, elementSize, charset);

			Array.set(array, i, element);
		}
		return array;
	}

	private static Object readValue(final BitReaderInterface reader, final int size, final Charset charset){
		return reader.getText(size, charset);
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
		final Object rootObject, final Object value) throws AnnotationException{
		final BindString binding = (BindString)annotation;

		final Class<? extends Validator<?>> validator = binding.validator();
		CodecHelper.validate(value, validator);

		final Object convertedValue = convertValue(binding, value, rootObject, CodecHelper::converterEncode);

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final Charset charset = CharsetHelper.lookup(binding.charset());

		if(collectionBinding == null)
			writeValue(writer, convertedValue, size, charset);
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			CodecHelper.assertSizeEquals(arraySize, Array.getLength(convertedValue));

			writeWithoutAlternatives(writer, convertedValue, size, charset);
		}
	}

	private static void writeWithoutAlternatives(final BitWriterInterface writer, final Object array, final int elementSize,
		final Charset charset){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeValue(writer, (String)element, elementSize, charset);
		}
	}

	private static void writeValue(final BitWriterInterface writer, final Object value, final int size, final Charset charset){
		String text = (String)value;
		text = text.substring(0, Math.min(text.length(), size));
		writer.putText(text, charset);
	}


	private Object convertValue(final BindString binding, final Object decodedValue, final Object rootObject,
		final BiFunction<Class<? extends Converter<?, ?>>, Object, Object> converter){
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		return converter.apply(chosenConverter, decodedValue);
	}

}
