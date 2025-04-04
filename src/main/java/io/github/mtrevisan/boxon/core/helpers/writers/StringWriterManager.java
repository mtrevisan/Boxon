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
package io.github.mtrevisan.boxon.core.helpers.writers;

import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * A manager for writing strings using a specific {@link BitWriterInterface} and {@link Charset}.
 * <p>
 * This class provides a way to configure the charset used for writing strings and delegates the actual writing to the underlying
 * {@link BitWriterInterface}.
 * </p>
 */
final class StringWriterManager implements WriterManager{

	private final BitWriterInterface writer;
	private Charset charset;


	static StringWriterManager create(final BitWriterInterface writer){
		return new StringWriterManager(writer);
	}


	private StringWriterManager(final BitWriterInterface writer){
		this.writer = writer;
		charset = StandardCharsets.UTF_8;
	}


	StringWriterManager withCharset(final Charset charset){
		this.charset = charset;

		return this;
	}

	@Override
	public void put(final Object value){
		writer.writeText((String)value, charset);
	}

}
