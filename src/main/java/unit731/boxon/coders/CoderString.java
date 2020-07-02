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
package unit731.boxon.coders;

import unit731.boxon.annotations.BindString;

import java.nio.charset.Charset;


class CoderString implements CoderInterface<BindString>{

	@Override
	public Object decode(final MessageParser messageParser, final BitBuffer reader, final BindString annotation, final Object data){
		final int size = Evaluator.evaluate(annotation.size(), int.class, data);
		final Charset charset = Charset.forName(annotation.charset());
		final String text = reader.getText(size, charset);

		final Object value = CoderHelper.converterDecode(annotation.converter(), text);

		CoderHelper.validateData(annotation.match(), annotation.validator(), value);

		return value;
	}

	@Override
	public void encode(final MessageParser messageParser, final BitWriter writer, final BindString annotation, final Object data,
			final Object value){
		CoderHelper.validateData(annotation.match(), annotation.validator(), value);

		final String text = CoderHelper.converterEncode(annotation.converter(), value);

		final int size = Evaluator.evaluate(annotation.size(), int.class, data);
		final Charset charset = Charset.forName(annotation.charset());
		writer.putText(text.substring(0, Math.min(text.length(), size)), charset);
	}

	@Override
	public Class<BindString> coderType(){
		return BindString.class;
	}

}
