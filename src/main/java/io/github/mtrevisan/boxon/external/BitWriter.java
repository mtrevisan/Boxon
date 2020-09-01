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
package io.github.mtrevisan.boxon.external;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.math.BigDecimal;
import java.nio.charset.Charset;


/**
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html">Bit Twiddling Hacks</a>
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
public final class BitWriter extends BitWriterData{

	/**
	 * Writes the given value using the give byte order.
	 *
	 * @param value	The data to written. Here, the length of the types (in bits) are those defined by java (see {@link Byte#SIZE}, {@link Short#SIZE}, {@link Integer#SIZE},
	 * 	{@link Long#SIZE}, {@link Float#SIZE}, and {@link Double#SIZE}).
	 * @param byteOrder	The byte order used to write the value.
	 */
	public void put(final Object value, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType t = ParserDataType.fromType(value.getClass());
		if(t == null)
			throw new AnnotationException("Cannot write type {}", value.getClass().getSimpleName());

		switch(t){
			case BYTE:
				putByte((Byte)value);
				break;

			case SHORT:
				putShort((Short)value, byteOrder);
				break;

			case INTEGER:
				putInt((Integer)value, byteOrder);
				break;

			case LONG:
				putLong((Long)value, byteOrder);
				break;

			case FLOAT:
				putFloat((Float)value, byteOrder);
				break;

			case DOUBLE:
				putDouble((Double)value, byteOrder);
		}
	}

	/**
	 * Writes a value to this {@link BitWriter} using {@link Byte#SIZE} bits.
	 *
	 * @param value	The {@code byte} to write.
	 */
	public void putByte(final byte value){
		putValue(value, Byte.SIZE);
	}

	/**
	 * Writes an array of {@code byte}s to this {@link BitWriter} using {@link Byte#SIZE} bits for each {@code byte}.
	 *
	 * @param array	The array of {@code byte}s to write.
	 */
	public void putBytes(final byte[] array){
		for(final byte elem : array)
			putByte(elem);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Short#SIZE} bits.
	 *
	 * @param value	The {@code short} to write as an {@code int} for ease-of-use, but internally down-casted to a {@code short}.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putShort(final short value, final ByteOrder byteOrder){
		putValue(byteOrder == ByteOrder.BIG_ENDIAN? Short.reverseBytes(value): value, Short.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Integer#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putInt(final int value, final ByteOrder byteOrder){
		putValue((byteOrder == ByteOrder.BIG_ENDIAN? Integer.reverseBytes(value): value), Integer.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Long#SIZE} bits.
	 *
	 * @param value	The {@code long} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putLong(final long value, final ByteOrder byteOrder){
		putValue((byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value): value), Long.SIZE);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Float#SIZE} bits.
	 *
	 * @param value	The {@code float} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putFloat(final float value, final ByteOrder byteOrder){
		putInt(Float.floatToRawIntBits(value), byteOrder);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Double#SIZE} bits.
	 *
	 * @param value	The {@code double} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putDouble(final double value, final ByteOrder byteOrder){
		putLong(Double.doubleToRawLongBits(value), byteOrder);
	}

	/**
	 * Writes a {@link BigDecimal} value to this {@link BitWriter}.
	 *
	 * @param value	The {@code BigDecimal} to write.
	 * @param cls	Either a {@code Float} or a {@link Double} class.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	public void putDecimal(final BigDecimal value, final Class<?> cls, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType dataType = ParserDataType.fromType(cls);
		if(dataType == ParserDataType.FLOAT)
			putFloat(value.floatValue(), byteOrder);
		else if(dataType == ParserDataType.DOUBLE)
			putDouble(value.doubleValue(), byteOrder);
		else
			throw new AnnotationException("Cannot write {} as a {}", BigDecimal.class.getSimpleName(), cls.getSimpleName());
	}

	/**
	 * Write the text into with a given {@link Charset}.
	 * <p>Note that if a terminator is needed, it must be manually written.</p>
	 *
	 * @param text	The {@code String}s to be written.
	 * @param charset	The charset.
	 */
	public void putText(final String text, final Charset charset){
		putBytes(text.getBytes(charset));
	}

}
