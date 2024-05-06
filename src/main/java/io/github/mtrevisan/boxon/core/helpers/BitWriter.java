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
package io.github.mtrevisan.boxon.core.helpers;

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * A writer bit-by-bit to a byte array.
 *
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html">Bit Twiddling Hacks</a>
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
public final class BitWriter extends BitWriterData implements io.github.mtrevisan.boxon.io.BitWriter{

	/**
	 * Create an instance of this class.
	 *
	 * @return	An instance of this class.
	 */
	public static BitWriter create(){
		return new BitWriter();
	}


	private BitWriter(){}


	@Override
	public void put(final Object value, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType pdt = ParserDataType.fromType(value.getClass());
		if(pdt == null)
			throw AnnotationException.create("Cannot write type {}", value.getClass().getSimpleName());

		pdt.write(this, value, byteOrder);
	}

	@Override
	public void putByte(final byte value){
		putNumber(value);
	}

	@Override
	public void putBytes(final byte[] array){
		for(int i = 0, length = array.length; i < length; i ++)
			putByte(array[i]);
	}

	@Override
	public void putShort(final short value, final ByteOrder byteOrder){
		putNumber(byteOrder.correctEndianness(value));
	}

	@Override
	public void putInt(final int value, final ByteOrder byteOrder){
		putNumber(byteOrder.correctEndianness(value));
	}

	@Override
	public void putLong(final long value, final ByteOrder byteOrder){
		putNumber(byteOrder.correctEndianness(value));
	}

	@Override
	public void putText(final String text, final Charset charset){
		putBytes(text.getBytes(charset));
	}

	@Override
	public void putText(final String text){
		putText(text, StandardCharsets.UTF_8);
	}

}
