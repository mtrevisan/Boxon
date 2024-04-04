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

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;
import java.util.BitSet;


final class CodecBitSet implements CodecInterface<BindBitSet>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws AnnotationException{
		final BindBitSet binding = interpretBinding(annotation);

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);

		final BitSet bitmap = reader.getBitSet(size);

		return convertValue(binding, rootObject, bitmap);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws AnnotationException{
		final BindBitSet binding = interpretBinding(annotation);

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		CodecHelper.validate(value, binding.validator());

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final BitSet bitmap = CodecHelper.converterEncode(chosenConverter, value);

		writer.putBitSet(bitmap, size);
	}


	private <IN, OUT> OUT convertValue(final BindBitSet binding, final Object rootObject, final IN value){
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final OUT convertedValue = CodecHelper.converterDecode(converterType, value);
		CodecHelper.validate(convertedValue, binding.validator());
		return convertedValue;
	}

}
