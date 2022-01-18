/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.codecs.managers.Injected;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriterInterface;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArrayPrimitive implements CodecInterface<BindArrayPrimitive>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject) throws AnnotationException{
		final BindArrayPrimitive binding = extractBinding(annotation);

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);

		final int size = bindingData.evaluateSize();
		CodecHelper.assertSizePositive(size);

		final Class<?> type = binding.type();
		final ByteOrder byteOrder = binding.byteOrder();
		final Object array = Array.newInstance(type, size);
		for(int i = 0; i < size; i ++){
			final Object value = reader.get(type, byteOrder);
			Array.set(array, i, value);
		}

		return CodecHelper.convertValue(bindingData, array);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws AnnotationException{
		final BindArrayPrimitive binding = extractBinding(annotation);

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final Object array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = bindingData.evaluateSize();
		CodecHelper.assertSizePositive(size);
		CodecHelper.assertSizeEquals(size, Array.getLength(array));

		final ByteOrder byteOrder = binding.byteOrder();
		for(int i = 0; i < size; i ++){
			final Object val = Array.get(array, i);
			writer.put(val, byteOrder);
		}
	}

}
