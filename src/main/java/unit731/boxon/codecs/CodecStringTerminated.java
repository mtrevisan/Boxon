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
package unit731.boxon.codecs;

import unit731.boxon.annotations.BindStringTerminated;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;


@SuppressWarnings("unused")
final class CodecStringTerminated implements CodecInterface<BindStringTerminated>{

	@Override
	public final Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		final Charset charset = Charset.forName(binding.charset());

		final String text = reader.getTextUntilTerminator(binding.terminator(), binding.consumeTerminator(), charset);

		final Object value = CodecHelper.converterDecode(binding.converter(), text);

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		return value;
	}

	@Override
	public final void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
		final BindStringTerminated binding = (BindStringTerminated)annotation;

		CodecHelper.validateData(binding.match(), binding.validator(), value);

		final Charset charset = Charset.forName(binding.charset());

		final String text = CodecHelper.converterEncode(binding.converter(), value);

		writer.putText(text, binding.terminator(), binding.consumeTerminator(), charset);
	}

	@Override
	public final Class<BindStringTerminated> codecType(){
		return BindStringTerminated.class;
	}

}
