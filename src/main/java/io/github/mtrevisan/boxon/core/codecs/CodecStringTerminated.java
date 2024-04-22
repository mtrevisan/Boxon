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
import io.github.mtrevisan.boxon.exceptions.FieldException;
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


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		final byte terminator = binding.terminator();
		final boolean consumeTerminator = binding.consumeTerminator();
		final Charset charset = CharsetHelper.lookup(binding.charset());

		final Object instance = decode(reader, collectionBinding, terminator, consumeTerminator, charset, rootObject);

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		return CodecHelper.decodeValue(converterType, validator, instance);
	}

	private Object decode(final BitReaderInterface reader, final Annotation collectionBinding, final byte terminator,
			final boolean consumeTerminator, final Charset charset, final Object rootObject) throws AnnotationException{
		Object instance = null;
		if(collectionBinding == null)
			instance = readText(reader, terminator, consumeTerminator, charset);
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			instance = decodeArray(reader, arraySize, terminator, consumeTerminator, charset);
		}
		return instance;
	}

	private static Object decodeArray(final BitReaderInterface reader, final int arraySize, final byte terminator,
			final boolean consumeTerminator, final Charset charset){
		final Object array = CodecHelper.createArray(String.class, arraySize);

		decodeWithoutAlternatives(reader, array, terminator, consumeTerminator, charset);

		return array;
	}

	private static void decodeWithoutAlternatives(final BitReaderInterface reader, final Object array, final byte terminator,
			final boolean consumeTerminator, final Charset charset){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readText(reader, terminator, consumeTerminator, charset);

			Array.set(array, i, element);
		}
	}

	private static String readText(final BitReaderInterface reader, final byte terminator, final boolean consumeTerminator,
			final Charset charset){
		final String text = reader.getTextUntilTerminator(terminator, charset);
		if(consumeTerminator){
			final int length = ParserDataType.getSize(terminator);
			reader.skip(length);
		}
		return text;
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws FieldException{
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		CodecHelper.validate(value, binding.validator());

		final byte terminator = binding.terminator();
		final boolean consumeTerminator = binding.consumeTerminator();
		final Charset charset = CharsetHelper.lookup(binding.charset());

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);

		if(collectionBinding == null){
			final String convertedText = CodecHelper.converterEncode(chosenConverter, value);

			writeText(writer, convertedText, terminator, consumeTerminator, charset);
		}
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			final Object array = CodecHelper.converterEncode(chosenConverter, value);

			CodecHelper.assertSizeEquals(arraySize, Array.getLength(array));

			encodeWithoutAlternatives(writer, array, terminator, consumeTerminator, charset);
		}
	}

	private static void encodeWithoutAlternatives(final BitWriterInterface writer, final Object array, final byte terminator,
			final boolean consumeTerminator, final Charset charset){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeText(writer, (String)element, terminator, consumeTerminator, charset);
		}
	}

	private static void writeText(final BitWriterInterface writer, final String text, final byte terminator, final boolean consumeTerminator,
			final Charset charset){
		writer.putText(text, charset);
		if(consumeTerminator)
			writer.putByte(terminator);
	}

}
