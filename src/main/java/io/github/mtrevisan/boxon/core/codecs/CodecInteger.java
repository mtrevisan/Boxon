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
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitSetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.BitSet;


final class CodecInteger implements CodecInterface<BindInteger>{

	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final BindInteger binding = (BindInteger)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ByteOrder byteOrder = binding.byteOrder();
		Object instance = decode(reader, collectionBinding, size, byteOrder, rootObject);

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);

		instance = convertValueType(collectionBinding, converterType, validator, instance);

		return CodecHelper.decodeValue(converterType, validator, instance);
	}

	private Object decode(final BitReaderInterface reader, final Annotation collectionBinding, final int size, final ByteOrder byteOrder,
			final Object rootObject) throws AnnotationException{
		Object instance = null;
		if(collectionBinding == null)
			instance = readBigInteger(reader, size, byteOrder);
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			instance = decodeArray(reader, arraySize, size, byteOrder);
		}
		return instance;
	}

	private Object decodeArray(final BitReaderInterface reader, final int arraySize, final int size, final ByteOrder byteOrder){
		final Object array = CodecHelper.createArray(BigInteger.class, arraySize);

		decodeWithoutAlternatives(reader, array, size, byteOrder);

		return array;
	}

	private static void decodeWithoutAlternatives(final BitReaderInterface reader, final Object array, final int size,
			final ByteOrder byteOrder){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readBigInteger(reader, size, byteOrder);

			Array.set(array, i, element);
		}
	}

	private static BigInteger readBigInteger(final BitReaderInterface reader, final int size, final ByteOrder byteOrder){
		return reader.getBigInteger(size, byteOrder);
	}

	private static Object convertValueType(final Annotation collectionBinding, final Class<? extends Converter<?, ?>> converterType,
			final Class<? extends Validator<?>> validator, Object instance){
		//convert value type into converter/validator input type
		Class<?> inputType = ParserDataType.resolveInputType(converterType, validator);
		if(collectionBinding == null)
			instance = ParserDataType.castValue((BigInteger)instance, inputType);
		else if(collectionBinding instanceof BindAsArray && inputType != null){
			inputType = inputType.getComponentType();
			if(inputType != instance.getClass().getComponentType()){
				final int length = Array.getLength(instance);
				final Object array = CodecHelper.createArray(inputType, length);

				for(int i = 0; i < length; i ++){
					Object element = Array.get(instance, i);
					element = ParserDataType.castValue((BigInteger)element, inputType);
					Array.set(array, i, element);
				}

				instance = array;
			}
		}
		return instance;
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws AnnotationException{
		final BindInteger binding = (BindInteger)annotation;

		CodecHelper.validate(value, binding.validator());

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		final ByteOrder byteOrder = binding.byteOrder();

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);

		if(collectionBinding == null){
			final Number convertedValue = CodecHelper.converterEncode(chosenConverter, value);

			writeBigInteger(writer, convertedValue, size, byteOrder);
		}
		else if(collectionBinding instanceof final BindAsArray ba){
			final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
			final Object array = CodecHelper.converterEncode(chosenConverter, value);

			CodecHelper.assertSizeEquals(arraySize, Array.getLength(array));

			encodeWithoutAlternatives(writer, array, size, byteOrder);
		}
	}

	private static void encodeWithoutAlternatives(final BitWriterInterface writer, final Object array, final int size,
			final ByteOrder byteOrder){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeBigInteger(writer, (Number)element, size, byteOrder);
		}
	}

	private static void writeBigInteger(final BitWriterInterface writer, final Number value, final int size, final ByteOrder byteOrder){
		final BigInteger v = ParserDataType.reinterpretToBigInteger(value);
		final BitSet bitmap = BitSetHelper.createBitSet(size, v, byteOrder);

		writer.putBitSet(bitmap, size);
	}

}
