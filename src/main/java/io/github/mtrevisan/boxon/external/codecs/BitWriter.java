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
package io.github.mtrevisan.boxon.external.codecs;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html">Bit Twiddling Hacks</a>
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
@SuppressWarnings("WeakerAccess")
public final class BitWriter extends BitWriterData implements BitWriterInterface{

	/**
	 * Create an instance of this class.
	 *
	 * @return	An instance of this class.
	 */
	public static BitWriter create(){
		return new BitWriter();
	}

	private BitWriter(){}

	/**
	 * Writes the given value using the give byte order.
	 *
	 * @param value	The data to written. Here, the length of the types (in bits) are those defined by java (see {@link Byte#SIZE},
	 * 	{@link Short#SIZE}, {@link Integer#SIZE}, {@link Long#SIZE}, {@link Float#SIZE}, and {@link Double#SIZE}).
	 * @param byteOrder	The byte order used to write the value.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 */
	@Override
	public void put(final Object value, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType pdt = ParserDataType.fromType(value.getClass());
		if(pdt == null)
			throw AnnotationException.create("Cannot write type {}", value.getClass().getSimpleName());

		pdt.write(this, value, byteOrder);
	}

	/**
	 * Writes a value using {@link Byte#SIZE} bits.
	 *
	 * @param value	The {@code byte} to write.
	 */
	@Override
	public void putByte(final byte value){
		putValue(value, Byte.SIZE);
	}

	/**
	 * Writes an array of {@code byte}s using {@link Byte#SIZE} bits for each {@code byte}.
	 *
	 * @param array	The array of {@code byte}s to write.
	 */
	@Override
	public void putBytes(final byte[] array){
		for(int i = 0; i < array.length; i ++)
			putByte(array[i]);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Short#SIZE} bits.
	 *
	 * @param value	The {@code short} to write as an {@code int} for ease-of-use, but internally down-casted to a {@code short}.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	@Override
	public void putShort(final short value, final ByteOrder byteOrder){
		putValue(byteOrder == ByteOrder.BIG_ENDIAN? Short.reverseBytes(value): value, Short.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Integer#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	@Override
	public void putInt(final int value, final ByteOrder byteOrder){
		putValue((byteOrder == ByteOrder.BIG_ENDIAN? Integer.reverseBytes(value): value), Integer.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Long#SIZE} bits.
	 *
	 * @param value	The {@code long} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	@Override
	public void putLong(final long value, final ByteOrder byteOrder){
		putValue((byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value): value), Long.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Float#SIZE} bits.
	 *
	 * @param value	The {@code float} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	@Override
	public void putFloat(final float value, final ByteOrder byteOrder){
		putInt(Float.floatToRawIntBits(value), byteOrder);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Double#SIZE} bits.
	 *
	 * @param value	The {@code double} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	@Override
	public void putDouble(final double value, final ByteOrder byteOrder){
		putLong(Double.doubleToRawLongBits(value), byteOrder);
	}

	/**
	 * Write the text into with a given {@link Charset}.
	 * <p>Note that if a terminator is needed, it must be manually written.</p>
	 *
	 * @param text	The {@code String}s to be written.
	 * @param charset	The charset.
	 */
	@Override
	public void putText(final String text, final Charset charset){
		putBytes(text.getBytes(charset));
	}

	/**
	 * Write the text into with an {@link StandardCharsets#UTF_8 UTF-8} charset.
	 * <p>Note that if a terminator is needed, it must be manually written.</p>
	 *
	 * @param text	The {@code String}s to be written.
	 */
	@Override
	public void putText(final String text){
		putText(text, StandardCharsets.UTF_8);
	}

}
