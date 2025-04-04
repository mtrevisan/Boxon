/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataTypeCaster;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;


/**
 * Abstract class that defines common behavior for reading and writing arrays from and to a {@link BitReaderInterface} and
 * {@link BitWriterInterface}, as well as converting values using converters and validators.
 */
public abstract class CommonBehavior{

	private final ConverterChoices converterChoices;
	private final Class<? extends Converter<?, ?>> defaultConverter;
	private final Class<? extends Validator<?>> validator;


	CommonBehavior(final ConverterChoices converterChoices, final Class<? extends Converter<?, ?>> defaultConverter,
			final Class<? extends Validator<?>> validator){
		this.converterChoices = converterChoices;
		this.defaultConverter = defaultConverter;
		this.validator = validator;
	}


	/**
	 * Reads an array of objects from a {@link BitReaderInterface} without alternatives.
	 *
	 * @param reader	The {@link BitReaderInterface} used to read the array.
	 * @param arraySize	The size of the array to be read.
	 * @return	The array of objects read from the {@link BitReaderInterface}.
	 */
	public final Object readArrayWithoutAlternatives(final BitReaderInterface reader, final int arraySize){
		final Object array = createArray(arraySize);
		for(int i = 0; i < arraySize; i ++){
			final Object element = readValue(reader);

			Array.set(array, i, element);
		}
		return array;
	}

	/**
	 * Writes an array of objects to a {@link BitWriterInterface} without alternatives.
	 *
	 * @param writer	The {@link BitWriterInterface} used to write the array.
	 * @param array	The array of objects to be written.
	 */
	public final void writeArrayWithoutAlternatives(final BitWriterInterface writer, final Object array){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeValue(writer, element);
		}
	}

	/**
	 * Converts the value to the input type of the chosen converter based on the collection binding.
	 *
	 * @param value	The value to be converted.
	 * @param chosenConverter	The chosen converter to be used for the conversion.
	 * @param collectionBinding	The collection binding annotation.
	 * @return	The converted value.
	 */
	public final Object convertValueType(final Object value, final Class<? extends Converter<?, ?>> chosenConverter,
			final Annotation collectionBinding){
		return convertValueType(collectionBinding, chosenConverter, validator, value);
	}

	/**
	 * Retrieves the chosen converter for the given evaluator and root object.
	 *
	 * @param rootObject	The root object being converted.
	 * @return	The chosen converter for the given evaluator and root object.
	 */
	public final Class<? extends Converter<?, ?>> getChosenConverter(final Object rootObject){
		return CodecHelper.getChosenConverter(converterChoices, defaultConverter, rootObject);
	}

	//convert value type into converter/validator input type
	private static Object convertValueType(final Annotation collectionBinding, final Class<? extends Converter<?, ?>> converterType,
			final Class<? extends Validator<?>> validator, Object instance){
		Class<?> inputType = DataTypeCaster.resolveInputType(converterType, validator);
		if(collectionBinding == null)
			instance = DataTypeCaster.cast((BigInteger)instance, inputType);
		else if(collectionBinding instanceof BindAsArray && inputType != null){
			inputType = inputType.getComponentType();
			if(inputType != instance.getClass().getComponentType())
				instance = DataTypeCaster.cast(instance, inputType);
		}
		return instance;
	}


	/**
	 * Creates an array of objects.
	 *
	 * @param arraySize	The size of the array to be created.
	 * @return	An array of objects with the specified size.
	 */
	abstract Object createArray(int arraySize);

	/**
	 * Reads the specified number of bits from the given {@link BitReaderInterface} and composes a value.
	 *
	 * @param reader	The {@link BitReaderInterface} used to read the bits.
	 * @return	A value at the current position of the {@link BitReaderInterface}.
	 */
	public abstract Object readValue(BitReaderInterface reader);

	/**
	 * Writes a value to the given {@link BitWriterInterface} using the specified length.
	 *
	 * @param writer	The {@link BitWriterInterface} used to write the value.
	 * @param value	The value to be written.
	 */
	public abstract void writeValue(BitWriterInterface writer, Object value);

	/**
	 * Retrieves the validator to be applied to converted values read from a bind annotation.
	 *
	 * @return	The validator to be applied.
	 */
	public final Class<? extends Validator<?>> validator(){
		return validator;
	}

}
