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
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.helpers.CodecHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitSetHelper;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.BitSet;


public final class IntegerBehavior extends BitSetBehavior{

	private final ByteOrder byteOrder;


	IntegerBehavior(final int size, final ByteOrder byteOrder, final ConverterChoices converterChoices,
			final Class<? extends Converter<?, ?>> defaultConverter, final Class<? extends Validator<?>> validator){
		super(size, converterChoices, defaultConverter, validator);;

		this.byteOrder = byteOrder;
	}


	static Object convertValueType(final Annotation collectionBinding, final Class<? extends Converter<?, ?>> converterType,
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

				for(int i = 0; i < length; i++){
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
	public Object createArray(final int arraySize){
		return CodecHelper.createArray(BigInteger.class, arraySize);
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		return reader.getBigInteger(size, byteOrder);
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		final BigInteger v = ParserDataType.reinterpretToBigInteger((Number)value);
		final BitSet bitmap = BitSetHelper.createBitSet(size, v, byteOrder);

		writer.putBitSet(bitmap, size);
	}

}
