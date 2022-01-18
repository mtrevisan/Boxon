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

import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.codecs.managers.Injected;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriterInterface;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.codecs.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


final class CodecStringTerminated implements CodecInterface<BindStringTerminated>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;


	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		final BindStringTerminated binding = extractBinding(annotation);

		final Charset charset = Charset.forName(binding.charset());

		final byte terminator = binding.terminator();
		final String text = reader.getTextUntilTerminator(terminator, charset);
		if(binding.consumeTerminator()){
			final int length = ParserDataType.getSize(terminator);
			reader.getBits(length);
		}

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);
		return CodecHelper.convertValue(bindingData, text);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value){
		final BindStringTerminated binding = extractBinding(annotation);

		final BindingData bindingData = BindingData.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		final Charset charset = Charset.forName(binding.charset());

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final String text = CodecHelper.converterEncode(chosenConverter, value);

		writer.putText(text, charset);
		if(binding.consumeTerminator())
			writer.putByte(binding.terminator());
	}

}
