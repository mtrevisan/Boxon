/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.behaviors;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.nio.charset.Charset;


/**
 * This class extends {@link StringCommonBehavior} and represents the behavior of handling strings terminated by a specific byte.
 * <p>
 * It provides functionality for reading and writing strings that are terminated by a specified byte.
 * </p>
 */
public final class StringTerminatedBehavior extends StringCommonBehavior{

	private final byte terminator;
	private final boolean consumeTerminator;


	StringTerminatedBehavior(final byte terminator, final boolean consumeTerminator, final Charset charset,
			final ConverterChoices converterChoices, final Class<? extends Converter<?, ?>> defaultConverter,
			final Class<? extends Validator<?>> validator){
		super(charset, converterChoices, defaultConverter, validator);

		this.terminator = terminator;
		this.consumeTerminator = consumeTerminator;
	}

	@Override
	public Object readValue(final BitReaderInterface reader){
		final String text = reader.readTextUntilTerminator(terminator, charset);
		if(consumeTerminator)
			//`terminator` is a byte
			reader.skip(Byte.SIZE);
		return text;
	}

	@Override
	public void writeValue(final BitWriterInterface writer, final Object value){
		writer.writeText((String)value, charset);
		if(consumeTerminator)
			writer.writeByte(terminator);
	}

}
