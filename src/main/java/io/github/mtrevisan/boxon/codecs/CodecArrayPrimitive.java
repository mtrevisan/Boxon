/**
 * Copyright (c) 2020 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArrayPrimitive implements CodecInterface<BindArrayPrimitive>{

	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		final BindArrayPrimitive binding = extractBinding(annotation);

		final Class<?> type = binding.type();
		final int size = Evaluator.evaluateSize(binding.size(), rootObject);

		final Object array = createArrayPrimitive(type, size);
		for(int i = 0; i < size; i ++){
			final Object value = reader.get(type, binding.byteOrder());
			Array.set(array, i, value);
		}

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
		final Object value = CodecHelper.converterDecode(chosenConverter, array);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	private static Object createArrayPrimitive(final Class<?> type, final int length){
		if(!ParserDataType.isPrimitive(type))
			throw new AnnotationException("Argument cannot be a non-primitive: {}", type);

		return Array.newInstance(type, length);
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindArrayPrimitive binding = extractBinding(annotation);

		CodecHelper.validateData(binding.validator(), value);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(), binding.converter(), rootObject);
		final Object array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = Evaluator.evaluateSize(binding.size(), rootObject);
		if(size != Array.getLength(array))
			throw new IllegalArgumentException("Size mismatch, expected " + size + ", got " + Array.getLength(value));

		for(int i = 0; i < size; i ++)
			writer.put(Array.get(array, i), binding.byteOrder());
	}

}
