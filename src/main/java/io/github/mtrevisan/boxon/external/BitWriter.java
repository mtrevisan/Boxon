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
import io.github.mtrevisan.boxon.internal.DataType;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;


/**
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html">Bit Twiddling Hacks</a>
 * @see <a href="https://git.irsamc.ups-tlse.fr/scemama/Bit-Twiddling-Hacks/">Bit Twiddling Hacks</a>
 */
public final class BitWriter{

	/** The backing {@link ByteArrayOutputStream}. */
	private final ByteArrayOutputStream os = new ByteArrayOutputStream(0);

	/** The <i>cache</i> used when writing bits. */
	private byte cache;
	/** The number of bits available (to write) within {@code cache}. */
	private int remaining;


	public void put(final Object value, final ByteOrder byteOrder){
		final DataType t = DataType.fromType(value.getClass());
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
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits.
	 *
	 * @param value	The value to write.
	 * @param length	The amount of bits to use when writing {@code value}.
	 */
	public void putBits(final BitSet value, final int length){
		//if the value that we're writing is too large to be placed entirely in the cache, then we need to place as
		//much as we can in the cache (the least significant bits), flush the cache to the backing ByteBuffer, and
		//place the rest in the cache
		int offset = 0;
		while(offset < length){
			//fill the cache one chunk of bits at a time
			final int size = Math.min(length - offset, Byte.SIZE - remaining);
			final byte nextCache = (byte)value.toLong(offset, size);
			cache = (byte)((cache << size) | nextCache);
			remaining += size;
			offset += size;

			//if cache is full, write it
			if(remaining == Byte.SIZE){
				os.write(cache);

				resetInnerVariables();
			}
		}
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits.
	 *
	 * @param value	The value to write.
	 * @param length	The amount of bits to use when writing {@code value} (MUST BE less than or equals to {@link Long#SIZE}).
	 */
	private void putValue(final long value, final int length){
		final BitSet bits = BitSet.valueOf(new long[]{value});
		putBits(bits, length);
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
		for(int i = 0; i < array.length; i ++)
			putByte(array[i]);
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
	public void putDecimal(final BigDecimal value, final Class<?> cls, final ByteOrder byteOrder){
		if(cls == float.class || cls == Float.class)
			putFloat(value.floatValue(), byteOrder);
		else if(cls == double.class || cls == Double.class)
			putDouble(value.doubleValue(), byteOrder);
		else
			throw new AnnotationException("Cannot write {} as a {}", BigDecimal.class.getSimpleName(), cls.getSimpleName());
	}

	/**
	 * Write the text into with a given {@link Charset}.
	 *
	 * @param text	The {@code String}s to be written.
	 * @param charset	The charset.
	 */
	public void putText(final String text, final Charset charset){
		putBytes(text.getBytes(charset));
	}

	/**
	 * Write the text into with a given {@link Charset} and terminator.
	 *
	 * @param text	The {@code String}s to be written.
	 * @param terminator	The terminator.
	 * @param consumeTerminator	Whether to consume the terminator.
	 * @param charset	The charset.
	 */
	public void putText(final String text, final byte terminator, final boolean consumeTerminator, final Charset charset){
		putBytes(text.getBytes(charset));
		if(consumeTerminator)
			putByte(terminator);
	}


	/** Flush an integral number of bytes to the output stream, padding any non-completed byte with zeros. */
	public void flush(){
		//put the cache into the buffer
		if(remaining > 0)
			os.write(cache);

		resetInnerVariables();
	}

	private void resetInnerVariables(){
		remaining = 0;
		cache = 0;
	}

	/**
	 * Returns a copy of the byte array that backs the buffer.
	 *
	 * @return	The copy of the array that backs this buffer.
	 * @throws ReadOnlyBufferException	If this buffer is backed by an array but is read-only.
	 * @throws UnsupportedOperationException	If this buffer is not backed by an accessible array.
	 */
	public byte[] array(){
		return os.toByteArray();
	}

	@Override
	public String toString(){
		return JavaHelper.toHexString(array());
	}

}