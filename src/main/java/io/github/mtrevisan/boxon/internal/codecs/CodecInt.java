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
package io.github.mtrevisan.boxon.internal.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.internal.managers.BindingData;
import io.github.mtrevisan.boxon.internal.managers.BindingDataBuilder;
import io.github.mtrevisan.boxon.internal.helpers.Evaluator;
import io.github.mtrevisan.boxon.internal.helpers.Injected;
import io.github.mtrevisan.boxon.external.io.BitReaderInterface;
import io.github.mtrevisan.boxon.external.io.BitWriterInterface;
import io.github.mtrevisan.boxon.external.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecInt implements CodecInterface<BindInt>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject){
		final BindInt binding = extractBinding(annotation);

		final int value = reader.getInt(binding.byteOrder());

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		return CodecHelper.convertValue(bindingData, value);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindInt binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final int v = CodecHelper.converterEncode(chosenConverter, value);

		writer.putInt(v, binding.byteOrder());
	}

}
