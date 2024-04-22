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
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
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
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.Charset;


final class CodecStringTerminated implements CodecInterface<BindStringTerminated>{

	@Injected
	private Evaluator evaluator;


	private record CodecBehavior(byte terminator, boolean consumeTerminator, Charset charset, ConverterChoices converterChoices,
			Class<? extends Converter<?, ?>> defaultConverter, Class<? extends Validator<?>> validator){
		public static CodecBehavior of(final Annotation annotation){
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			final byte terminator = binding.terminator();
			final boolean consumeTerminator = binding.consumeTerminator();
			final Charset charset = CharsetHelper.lookup(binding.charset());
			final ConverterChoices converterChoices = binding.selectConverterFrom();
			final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
			final Class<? extends Validator<?>> validator = binding.validator();
			return new CodecBehavior(terminator, consumeTerminator, charset, converterChoices, defaultConverter, validator);
		}

		private static Object readValue(final BitReaderInterface reader, final CodecBehavior behavior){
			final String text = reader.getTextUntilTerminator(behavior.terminator, behavior.charset);
			if(behavior.consumeTerminator){
				final int length = ParserDataType.getSize(behavior.terminator);
				reader.skip(length);
			}
			return text;
		}

		private static void writeValue(final BitWriterInterface writer, final Object value, final CodecBehavior behavior){
			writer.putText((String)value, behavior.charset);
			if(behavior.consumeTerminator)
				writer.putByte(behavior.terminator);
		}
	}


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final CodecBehavior behavior = CodecBehavior.of(annotation);

		final Object instance = decode(reader, collectionBinding, behavior, rootObject);

		final ConverterChoices converterChoices = behavior.converterChoices;
		final Class<? extends Converter<?, ?>> defaultConverter = behavior.defaultConverter;
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final Class<? extends Validator<?>> validator = behavior.validator;
		return CodecHelper.decodeValue(converterType, validator, instance);
	}

	private Object decode(final BitReaderInterface reader, final Annotation collectionBinding, final CodecBehavior behavior,
			final Object rootObject) throws AnnotationException{
		Object instance = null;
		if(collectionBinding == null)
			instance = CodecBehavior.readValue(reader, behavior);
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			instance = decodeArray(reader, arraySize, behavior);
		}
		return instance;
	}

	private static Object decodeArray(final BitReaderInterface reader, final int arraySize, final CodecBehavior behavior){
		final Object array = CodecHelper.createArray(String.class, arraySize);

		decodeWithoutAlternatives(reader, array, behavior);

		return array;
	}

	private static void decodeWithoutAlternatives(final BitReaderInterface reader, final Object array, final CodecBehavior behavior){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = CodecBehavior.readValue(reader, behavior);

			Array.set(array, i, element);
		}
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws AnnotationException{
		final CodecBehavior behavior = CodecBehavior.of(annotation);

		CodecHelper.validate(value, behavior.validator);

		final ConverterChoices converterChoices = behavior.converterChoices;
		final Class<? extends Converter<?, ?>> defaultConverter = behavior.defaultConverter;
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);

		if(collectionBinding == null){
			final Object convertedValue = CodecHelper.converterEncode(chosenConverter, value);

			CodecBehavior.writeValue(writer, convertedValue, behavior);
		}
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			final Object array = CodecHelper.converterEncode(chosenConverter, value);

			CodecHelper.assertSizeEquals(arraySize, Array.getLength(array));

			encodeWithoutAlternatives(writer, array, behavior);
		}
	}

	private static void encodeWithoutAlternatives(final BitWriterInterface writer, final Object array, final CodecBehavior behavior){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			CodecBehavior.writeValue(writer, element, behavior);
		}
	}

}
