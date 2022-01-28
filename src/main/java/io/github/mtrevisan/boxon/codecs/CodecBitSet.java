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

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.codecs.managers.Injected;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.io.BitReaderInterface;
import io.github.mtrevisan.boxon.external.io.BitWriterInterface;
import io.github.mtrevisan.boxon.external.io.BoxonBitSet;
import io.github.mtrevisan.boxon.external.io.CodecInterface;

import java.lang.annotation.Annotation;
import java.util.BitSet;


final class CodecBitSet implements CodecInterface<BindBitSet>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws AnnotationException{
		final BindBitSet binding = extractBinding(annotation);

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);
		final int size = bindingData.evaluateSize();
		CodecHelper.assertSizePositive(size);

		final BoxonBitSet bits = reader.getBits(size);
		bits.changeByteOrder(size, binding.byteOrder());
		final BitSet value = BitSet.valueOf(bits.toByteArray());

		return CodecHelper.convertValue(bindingData, value);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, Object value)
			throws AnnotationException{
		final BindBitSet binding = extractBinding(annotation);

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);
		final int size = bindingData.evaluateSize();
		bindingData.validate(value);
		CodecHelper.assertSizePositive(size);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final BitSet bits = CodecHelper.converterEncode(chosenConverter, value);
		value = BoxonBitSet.valueOf(bits.toByteArray());
		((BoxonBitSet)value).changeByteOrder(size, binding.byteOrder());

		writer.putBits((BoxonBitSet)value, size);
	}

}
