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

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.function.BiFunction;


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


	public ConverterChoices converterChoices(){
		return converterChoices;
	}

	public Class<? extends Converter<?, ?>> defaultConverter(){
		return defaultConverter;
	}


	public Object readArrayWithoutAlternatives(final BitReaderInterface reader, final int arraySize){
		final Object array = createArray(arraySize);
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readValue(reader);

			Array.set(array, i, element);
		}
		return array;
	}

	public void writeArrayWithoutAlternatives(final BitWriterInterface writer, final Object array){
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeValue(writer, element);
		}
	}

	public Object convertValue(final Object value, final Evaluator evaluator, final Object rootObject,
			final BiFunction<Class<? extends Converter<?, ?>>, Object, Object> converter){
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		return converter.apply(chosenConverter, value);
	}

	public Object convertValue(Object value, final Evaluator evaluator, final Object rootObject,
			final BiFunction<Class<? extends Converter<?, ?>>, Object, Object> converter, final Annotation collectionBinding){
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);

		value = adaptValue(validator, value, chosenConverter, collectionBinding);

		return converter.apply(chosenConverter, value);
	}

	private static Object adaptValue(final Class<? extends Validator<?>> validator, final Object value,
		final Class<? extends Converter<?, ?>> chosenConverter, final Annotation collectionBinding){
		return IntegerBehavior.convertValueType(collectionBinding, chosenConverter, validator, value);
	}


	abstract Object createArray(int arraySize);

	public abstract Object readValue(BitReaderInterface reader);

	public abstract void writeValue(BitWriterInterface writer, Object value);

	public Class<? extends Validator<?>> validator(){
		return validator;
	}

}
