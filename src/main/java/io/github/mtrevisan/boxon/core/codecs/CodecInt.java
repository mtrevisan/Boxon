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

import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecInt implements CodecInterface<BindInt>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject){
		final BindInt binding = interpretBinding(annotation);

		final int value = reader.getInt(binding.byteOrder());

		return convertValue(binding, rootObject, value);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindInt binding = interpretBinding(annotation);

		CodecHelper.validate(value, binding.validator());

		final Class<? extends Converter<?, ?>> chosenConverter = getChosenConverter(binding, rootObject);
		final int v = CodecHelper.converterEncode(chosenConverter, value);

		writer.putInt(v, binding.byteOrder());
	}


	private <IN, OUT> OUT convertValue(final BindInt binding, final Object rootObject, final IN value){
		final Class<? extends Converter<?, ?>> converterType = getChosenConverter(binding, rootObject);
		final OUT convertedValue = CodecHelper.converterDecode(converterType, value);
		CodecHelper.validate(convertedValue, binding.validator());
		return convertedValue;
	}

	/**
	 * Get the first converter that matches the condition.
	 *
	 * @return	The converter class.
	 */
	private Class<? extends Converter<?, ?>> getChosenConverter(final BindInt binding, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = binding.selectConverterFrom().alternatives();
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return binding.converter();
	}

}
