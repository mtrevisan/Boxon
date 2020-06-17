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

import unit731.boxon.utils.ByteHelper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.util.BitSet;


@SuppressWarnings("unused")
class BitWriter{

	/** The backing {@link ByteArrayOutputStream} */
	private final ByteArrayOutputStream os = new ByteArrayOutputStream(0);
	/** The number of bits available within {@code cache} */
	private int remainingBits;
	/** The <i>cache</i> used when writing and reading bits */
	private long cache;


	public void put(final Object value, final ByteOrder byteOrder){
		final Class<?> cls = value.getClass();
		if(cls == Byte.class)
			putByte((Byte)value);
		else if(cls == Character.class)
			putCharacter((Character)value, byteOrder);
		else if(cls == Short.class)
			putShort((Short)value, byteOrder);
		else if(cls == Integer.class)
			putInteger((Integer)value, byteOrder);
		else if(cls == Long.class)
			putLong((Long)value, byteOrder);
		else if(cls == Float.class)
			putFloat((Float)value, byteOrder);
		else if(cls == Double.class)
			putDouble((Double)value, byteOrder);
		else
			throw new IllegalArgumentException("Cannot read type " + cls.getSimpleName());
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits.
	 *
	 * @param value	The value to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putBits(final BitSet value, final int length){
		//if the value that we're writing is too large to be placed entirely in the cache, then we need to place as
		//much as we can in the cache (the least significant bits), flush the cache to the backing ByteBuffer, and
		//place the rest in the cache
		if(Long.SIZE - remainingBits < length){
			//write an integer number of longs
			final int size = length / Long.SIZE;
			final long[] vv = value.toLongArray();
			for(int k = 0; k < size; k ++){
				final long v = vv[k];
				final int upperHalfBits = Math.min(length, Long.SIZE) - Long.SIZE + remainingBits;
				cache |= ((v & BitBuffer.MASKS[Long.SIZE - remainingBits]) << remainingBits);
				remainingBits = Long.SIZE;
				while(remainingBits > 0){
					os.write((byte)cache);

					cache >>>= Byte.SIZE;
					remainingBits -= Byte.SIZE;
				}
				cache = v & (BitBuffer.MASKS[upperHalfBits] << (Long.SIZE - remainingBits));
				remainingBits = upperHalfBits;
			}

			//write remainder bits
			final int sizeAsBits = size * Long.SIZE;
			if(sizeAsBits < length)
				putBits(value.get(sizeAsBits, length), length);
		}
		else{
			cache |= ((value.toLongArray()[0] & BitBuffer.MASKS[length]) << remainingBits);
			remainingBits += length;
		}
		return this;
	}

	/**
	 * Writes {@code value} to this {@link BitWriter} using {@code length} bits.
	 *
	 * @param value	The value to write.
	 * @param length	The amount of bits to use when writing {@code value}.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	@SuppressWarnings("ShiftOutOfRange")
	private BitWriter putValue(final long value, final int length){
		//if the value that we're writing is too large to be placed entirely in the cache, then we need to place as
		//much as we can in the cache (the least significant bits), flush the cache to the backing ByteBuffer, and
		//place the rest in the cache
		if(Long.SIZE - remainingBits < length){
			final int upperHalfBits = length - Long.SIZE + remainingBits;
			cache |= ((value & BitBuffer.MASKS[Long.SIZE - remainingBits]) << remainingBits);
			while(remainingBits > 0){
				os.write((byte)cache);

				cache >>>= Byte.SIZE;
				remainingBits -= Byte.SIZE;
			}
			cache = value & (BitBuffer.MASKS[upperHalfBits] << (Long.SIZE - remainingBits));
			remainingBits = upperHalfBits;
		}
		else{
			cache |= ((value & BitBuffer.MASKS[length]) << remainingBits);
			remainingBits += length;
		}
		return this;
	}

	/**
	 * Writes a value to this {@link BitWriter} using {@link Byte#SIZE} bits.
	 *
	 * @param value	The {@code byte} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putByte(final byte value){
		return putValue(value, Byte.SIZE);
	}

	/**
	 * Writes an array of {@code byte}s to this {@link BitWriter} using {@link Byte#SIZE} bits for each {@code byte}.
	 *
	 * @param array	The array of {@code byte}s to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putBytes(final byte[] array){
		for(final byte value : array)
			putByte(value);
		return this;
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Character#SIZE} bits.
	 *
	 * @param chr	The {@code char} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putCharacter(char, ByteOrder)
	 */
	public BitWriter putCharacter(final char chr){
		return putCharacter(chr, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Character#SIZE} bits.
	 *
	 * @param chr	The {@code char} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putCharacter(final char chr, final ByteOrder byteOrder){
		return putValue((byteOrder == ByteOrder.BIG_ENDIAN? Character.reverseBytes(chr): chr), Character.SIZE);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Short#SIZE} bits.
	 *
	 * @param value	The {@code short} to write as an {@code int} for ease-of-use, but internally down-casted to a {@code short}.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putShort(final short value){
		return putShort(value, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Short#SIZE} bits.
	 *
	 * @param value	The {@code short} to write as an {@code int} for ease-of-use, but internally down-casted to a {@code short}.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putShort(final short value, final ByteOrder byteOrder){
		return putValue(byteOrder == ByteOrder.BIG_ENDIAN? Short.reverseBytes(value): value, Short.SIZE);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Integer#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putInteger(int, ByteOrder)
	 */
	public BitWriter putInteger(final int value){
		return putInteger(value, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Integer#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putInteger(final int value, final ByteOrder byteOrder){
		return putValue((byteOrder == ByteOrder.BIG_ENDIAN? Integer.reverseBytes(value): value), Integer.SIZE);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Long#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putLong(long, ByteOrder)
	 */
	public BitWriter putLong(final long value){
		return putLong(value, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Long#SIZE} bits.
	 *
	 * @param value	The {@code long} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putLong(final long value, final ByteOrder byteOrder){
		return putValue((byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value): value), Long.SIZE);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Float#SIZE} bits.
	 *
	 * @param value	The {@code float} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putFloat(float, ByteOrder)
	 */
	public BitWriter putFloat(final float value){
		return putFloat(value, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Float#SIZE} bits.
	 *
	 * @param value	The {@code float} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putInteger(int, ByteOrder)
	 */
	public BitWriter putFloat(final float value, final ByteOrder byteOrder){
		return putInteger(Float.floatToRawIntBits(value), byteOrder);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Double#SIZE} bits.
	 *
	 * @param value	The {@code double} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putDouble(double, ByteOrder)
	 */
	public BitWriter putDouble(final double value){
		return putDouble(value, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a value with the specified {@link ByteOrder} to this {@link BitWriter} using {@link Double#SIZE} bits.
	 *
	 * @param value	The {@code double} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putLong(long, ByteOrder)
	 */
	public BitWriter putDouble(final double value, final ByteOrder byteOrder){
		return putLong(Double.doubleToRawLongBits(value), byteOrder);
	}

	/**
	 * Writes a value with {@link ByteOrder#LITTLE_ENDIAN} byteOrder to this {@link BitWriter} using {@link Double#SIZE} bits.
	 *
	 * @param value	The {@code double} to write.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 * @see	#putDouble(double, ByteOrder)
	 */
	public BitWriter putNumber(final BigDecimal value, final Class<?> cls){
		return putNumber(value, cls, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Writes a {@link BigDecimal} value to this {@link BitWriter}.
	 *
	 * @param value	The {@code BigDecimal} to write.
	 * @param cls	Either a {@code Float} or a {@link Double} class.
	 * @return	The {@link BitWriter} to allow for the convenience of method-chaining.
	 */
	public BitWriter putNumber(final BigDecimal value, final Class<?> cls, final ByteOrder byteOrder){
		if(cls == Float.class)
			return putFloat(value.floatValue(), byteOrder);
		else if(cls == Double.class)
			return putDouble(value.doubleValue(), byteOrder);
		else
			throw new IllegalArgumentException("Cannot write " + BigDecimal.class.getSimpleName() + " as a " + cls.getSimpleName());
	}

	/**
	 * Write the text into with a given {@link Charset}.
	 *
	 * @param text	The {@code String}s to be written.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s
	 * 	read from this {@link BitBuffer}.
	 */
	public BitWriter putText(final String text, final Charset charset){
		return putBytes(text.getBytes(charset));
	}

	/**
	 * Write the text into with a given {@link Charset} and terminator.
	 *
	 * @param text	The {@code String}s to be written.
	 * @param terminator	The terminator.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s
	 * 	read from this {@link BitBuffer}.
	 */
	public BitWriter putText(final String text, final byte terminator, final boolean consumeTerminator, final Charset charset){
		putBytes(text.getBytes(charset));
		if(consumeTerminator)
			putByte(terminator);
		return this;
	}


	public void flush(){
		//put the cache into the buffer, if needed
		while(remainingBits > 0){
			os.write((byte)cache);

			cache >>>= Byte.SIZE;
			remainingBits -= Byte.SIZE;
		}
	}

	/**
	 * Returns a copy of the byte array that backs the buffer.
	 *
	 * @return	The copy of the array that backs this buffer
	 * @throws ReadOnlyBufferException	If this buffer is backed by an array but is read-only
	 * @throws UnsupportedOperationException	If this buffer is not backed by an accessible array
	 */
	public byte[] array(){
		return os.toByteArray();
	}

	@Override
	public String toString(){
		return ByteHelper.byteArrayToHexString(array());
	}

}
