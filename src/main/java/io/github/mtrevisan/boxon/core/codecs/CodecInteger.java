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


	private record CodecBehavior(int size, ByteOrder byteOrder, ConverterChoices converterChoices,
			Class<? extends Converter<?, ?>> defaultConverter, Class<? extends Validator<?>> validator){
		public static CodecBehavior of(final Annotation annotation, final Evaluator evaluator, final Object rootObject)
				throws AnnotationException{
			final BindInteger binding = (BindInteger)annotation;

			final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
			final ByteOrder byteOrder = binding.byteOrder();
			final ConverterChoices converterChoices = binding.selectConverterFrom();
			final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
			final Class<? extends Validator<?>> validator = binding.validator();
			return new CodecBehavior(size, byteOrder, converterChoices, defaultConverter, validator);
		}

		private static BigInteger readValue(final BitReaderInterface reader, final CodecBehavior behavior){
			return reader.getBigInteger(behavior.size, behavior.byteOrder);
		}

		private static void writeValue(final BitWriterInterface writer, final Object value, final CodecBehavior behavior){
			final BigInteger v = ParserDataType.reinterpretToBigInteger((Number)value);
			final BitSet bitmap = BitSetHelper.createBitSet(behavior.size, v, behavior.byteOrder);

			writer.putBitSet(bitmap, behavior.size);
		}
	}


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws AnnotationException{
		final CodecBehavior behavior = CodecBehavior.of(annotation, evaluator, rootObject);

		Object instance = decode(reader, collectionBinding, behavior, rootObject);

		final ConverterChoices converterChoices = behavior.converterChoices;
		final Class<? extends Converter<?, ?>> defaultConverter = behavior.defaultConverter;
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final Class<? extends Validator<?>> validator = behavior.validator;

		instance = convertValueType(collectionBinding, converterType, validator, instance);

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
		final Object array = CodecHelper.createArray(BigInteger.class, arraySize);

		decodeWithoutAlternatives(reader, array, behavior);

		return array;
	}

	private static void decodeWithoutAlternatives(final BitReaderInterface reader, final Object array, final CodecBehavior behavior){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = CodecBehavior.readValue(reader, behavior);

			Array.set(array, i, element);
		}
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
		final CodecBehavior behavior = CodecBehavior.of(annotation, evaluator, rootObject);

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
