/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.codecs.ParserDataType;
import io.github.mtrevisan.boxon.internal.Evaluator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArrayPrimitive implements CodecInterface<BindArrayPrimitive>{

	/** Automatically injected. */
	@SuppressWarnings("unused")
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject) throws AnnotationException{
		final BindArrayPrimitive binding = extractBinding(annotation);

		final Class<?> type = binding.type();
		final int size = evaluator.evaluateSize(binding.size(), rootObject);
		CodecHelper.assertSizePositive(size);

		final ByteOrder byteOrder = binding.byteOrder();
		final Object array = createArrayPrimitive(type, size);
		for(int i = 0; i < size; i ++){
			final Object value = reader.get(type, byteOrder);
			Array.set(array, i, value);
		}

		final ConverterChoices selectConverterFrom = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(selectConverterFrom, defaultConverter,
			rootObject, evaluator);
		final Object value = CodecHelper.converterDecode(chosenConverter, array);

		CodecHelper.validateData(binding.validator(), value);

		return value;
	}

	private static Object createArrayPrimitive(final Class<?> type, final int length) throws AnnotationException{
		if(!ParserDataType.isPrimitive(type))
			throw AnnotationException.create("Argument cannot be a non-primitive: {}", type);

		return Array.newInstance(type, length);
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value)
			throws AnnotationException{
		final BindArrayPrimitive binding = extractBinding(annotation);

		CodecHelper.validateData(binding.validator(), value);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(),
			binding.converter(), rootObject, evaluator);
		final Object array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = evaluator.evaluateSize(binding.size(), rootObject);
		CodecHelper.assertSizePositive(size);
		CodecHelper.assertSizeEquals(size, Array.getLength(array));

		final ByteOrder byteOrder = binding.byteOrder();
		for(int i = 0; i < size; i ++){
			final Object val = Array.get(array, i);
			writer.put(val, byteOrder);
		}
	}

}
