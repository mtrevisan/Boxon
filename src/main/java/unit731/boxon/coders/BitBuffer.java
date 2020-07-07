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

import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.helpers.BitMap;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;


/**
 * @see <a href="https://github.com/jhg023/BitBuffer/blob/master/src/main/java/bitbuffer/BitBuffer.java">BitBuffer</a>
 */
class BitBuffer{

	private static final class State{
		private int position;
		private int remaining;
		private byte cache;

		State(final int position, final int remaining, final byte cache){
			this.position = position;
			this.remaining = remaining;
			this.cache = cache;
		}
	}


	/** The backing {@link ByteBuffer} */
	private final ByteBuffer buffer;

	/** The <i>cache</i> used when writing and reading bits */
	private byte cache;
	/** The number of bits available (to read) within {@code cache} */
	private int remaining;

	private State fallbackPoint;


	/**
	 * Wraps a {@link java.io.File} containing a binary stream into a buffer.
	 *
	 * @param file	The file containing the binary stream
	 * @return	The new bit buffer
	 */
	static BitBuffer wrap(final File file) throws IOException{
		try(
				final FileInputStream fis = new FileInputStream(file);
				final FileChannel fc = fis.getChannel();
			){
			//map file into memory
			final ByteBuffer inputByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

			return wrap(inputByteBuffer);
		}
	}

	/**
	 * Wraps a {@link ByteBuffer} into a buffer.
	 * <p>The new buffer will be backed by the given byte buffer</p>.
	 *
	 * @param buffer	The buffer that will back this buffer
	 * @return	The new bit buffer
	 */
	static BitBuffer wrap(final ByteBuffer buffer){
		return new BitBuffer(buffer);
	}

	/**
	 * Wraps a byte array into a buffer.
	 * <p>The new buffer will be backed by the given byte array; that is, modifications to the buffer will cause the array
	 * to be modified and vice versa. The new buffer's capacity and limit will be {@code array.length}, its position will
	 * be zero, its mark will be undefined, and its byte `byteOrder` will be {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.</p>
	 *
	 * @param array	The array that will back this buffer
	 * @return	The new bit buffer
	 */
	static BitBuffer wrap(final byte[] array){
		return new BitBuffer(ByteBuffer.wrap(array));
	}

	/**
	 * Wraps a byte array into a buffer.
	 * <p>The new buffer will be backed by the given byte array containted into the {@link BitWriter}.</p>
	 *
	 * @param bitWriter	The {@link BitWriter}
	 * @return	The new bit buffer
	 */
	static BitBuffer wrap(final BitWriter bitWriter){
		bitWriter.flush();
		return wrap(bitWriter.array());
	}

	/**
	 * A private constructor.
	 *
	 * @param buffer the backing {@link ByteBuffer}.
	 */
	private BitBuffer(final ByteBuffer buffer){
		this.buffer = buffer;
	}

	void createFallbackPoint(){
		if(fallbackPoint != null){
			//overwrite current mark:
			fallbackPoint.position = buffer.position();
			fallbackPoint.remaining = remaining;
			fallbackPoint.cache = cache;
		}
		else
			//create new mark
			fallbackPoint = new State(buffer.position(), remaining, cache);
	}

	void restoreFallbackPoint() throws IOException{
		if(fallbackPoint == null)
			throw new IOException("No fallback point was marked before");

		buffer.position(fallbackPoint.position);
		remaining = fallbackPoint.remaining;
		cache = fallbackPoint.cache;

		fallbackPoint = null;
	}


	void skip(final int length){
		getBits(length);
	}

	void skipUntilTerminator(final byte terminator, final boolean consumeTerminator){
		getTextUntilTerminator(terminator, consumeTerminator, Charset.defaultCharset());
	}

	Object get(final Class<?> cls, final ByteOrder byteOrder){
		final Class<?> inputClass = ReflectionHelper.objectiveType(cls);
		if(inputClass == Byte.class)
			return getByte();
		if(inputClass == Short.class)
			return getShort(byteOrder);
		if(inputClass == Integer.class)
			return getInt(byteOrder);
		if(inputClass == Long.class)
			return getLong(byteOrder);
		if(inputClass == Float.class)
			return getFloat(byteOrder);
		if(inputClass == Double.class)
			return getDouble(byteOrder);

		throw new IllegalArgumentException("Cannot read type " + cls.getSimpleName());
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BitMap}.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BitMap} value at the {@link BitBuffer}'s current position.
	 */
	BitMap getBits(final int length){
		final BitMap value = new BitMap(length);
		int offset = 0;
		while(offset < length){
			//transfer the cache values
			final int size = Math.min(length, remaining);
			for(int i = offset; cache != 0 && i < offset + size; i ++, cache >>>= 1)
				if((cache & 0x01) != 0)
					value.set(i);
			remaining -= size;
			offset += size;

			//if cache is empty and there are more bits to be read, fill it
			if(length > offset){
				cache = buffer.get();

				remaining = Byte.SIZE;
			}
		}
		return value;
	}

	/**
	 * Reads the next {@code length} bits (should be less than 64!) and composes a <code>long</code>.
	 *
	 * @param length	The amount of bits to read (should be less than 64!).
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A <code>long</code> value at the {@link BitBuffer}'s current position.
	 */
	long getLong(final int length, final ByteOrder byteOrder){
		final BitMap bits = getBits(length);
		return (bits.length() > 0? ByteHelper.bitsToLong(bits, length, byteOrder): 0l);
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BigInteger}.
	 *
	 * @param length	The amount of bits to read.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@link BigInteger} value at the {@link BitBuffer}'s current position.
	 */
	BigInteger getBigInteger(final int length, final ByteOrder byteOrder){
		final BitMap bits = getBits(length);
		return ByteHelper.toInteger(bits, length, byteOrder);
	}

	/**
	 * Reads {@link Byte#SIZE} bits from this {@link BitBuffer} and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	byte getByte(){
		return (byte)getLong(Byte.SIZE, ByteOrder.LITTLE_ENDIAN);
	}

	/**
	 * Reads the specified amount of {@code byte}s from this {@link BitBuffer} into an array of {@code byte}s.
	 *
	 * @param length	The number of {@code byte}s to read.
	 * @return	An array of {@code byte}s of length {@code n} that contains {@code byte}s read from this {@link BitBuffer}.
	 */
	byte[] getBytes(final int length){
		final byte[] array = new byte[length];
		for(int i = 0; i < length; i ++)
			array[i] = (byte)getLong(Byte.SIZE, ByteOrder.LITTLE_ENDIAN);
		return array;
	}

	/**
	 * Reads {@link Short#SIZE} bits from this {@link BitBuffer} and composes a {@code short} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code short}.
	 */
	short getShort(final ByteOrder byteOrder){
		return (short)get(Short.SIZE, byteOrder);
	}

	/**
	 * Reads {@link Integer#SIZE} bits from this {@link BitBuffer} and composes an {@code int} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	An {@code int}.
	 */
	int getInt(final ByteOrder byteOrder){
		return (int)get(Integer.SIZE, byteOrder);
	}

	/**
	 * Reads {@link Long#SIZE} bits from this {@link BitBuffer} and composes a {@code long} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code long}.
	 */
	long getLong(final ByteOrder byteOrder){
		return get(Long.SIZE, byteOrder);
	}

	private long get(final int size, final ByteOrder byteOrder){
		final long value = getLong(size, ByteOrder.LITTLE_ENDIAN);
		return ByteHelper.reverseBytes(value, size, byteOrder);
	}

	/**
	 * Reads {@link Float#SIZE} bits from this {@link BitBuffer} and composes a {@code float} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code float}.
	 */
	float getFloat(final ByteOrder byteOrder){
		return Float.intBitsToFloat(getInt(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits from this {@link BitBuffer} and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code double}.
	 */
	double getDouble(final ByteOrder byteOrder){
		return Double.longBitsToDouble(getLong(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits from this {@link BitBuffer} and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param cls	Either a {@code Float} or a {@link Double} class.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@link BigDecimal}.
	 */
	BigDecimal getDecimal(final Class<?> cls, final ByteOrder byteOrder){
		if(cls == Float.class)
			return new BigDecimal(Float.toString(getFloat(byteOrder)));
		else if(cls == Double.class)
			return new BigDecimal(Double.toString(getDouble(byteOrder)));
		else
			throw new IllegalArgumentException("Cannot write " + BigDecimal.class.getSimpleName() + " as a " + cls.getSimpleName());
	}

	/**
	 * Reads the specified amount of {@code char}s from this {@link BitBuffer} into a {@link String} with a given {@link Charset}.
	 *
	 * @param length	The number of {@code char}s to read.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s
	 * 	read from this {@link BitBuffer}.
	 */
	String getText(final int length, final Charset charset){
		return new String(getBytes(length), charset);
	}

	/** Reads a string until a terminator is found */
	String getTextUntilTerminator(final byte terminator, final boolean consumeTerminator, final Charset charset){
		String text = null;
		try(
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final OutputStreamWriter osw = new OutputStreamWriter(baos, charset);
		){
			getTextUntilTerminator(osw, terminator, consumeTerminator);
			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

	private void getTextUntilTerminator(final OutputStreamWriter os, final byte terminator, final boolean consumeTerminator) throws IOException{
		if(!consumeTerminator)
			createFallbackPoint();

		byte byteRead;
		for(byteRead = getByte(); byteRead != terminator && (buffer.position() < buffer.limit() || buffer.remaining() > 0); ){
			os.write(byteRead);

			if(!consumeTerminator)
				createFallbackPoint();

			byteRead = getByte();
		}
		os.flush();

		if(!consumeTerminator)
			restoreFallbackPoint();
	}


	void clear(){
		//reset the buffer's position and limit
		buffer.clear();

		/** reset {@link #remaining} so that, on the next call to {@link #getBits(int)}, the cache will be reset */
		resetInnerVariables();
	}

	byte[] array(){
		return buffer.array();
	}

	/**
	 * Gets the position of the backing {@link ByteBuffer} in integral number of {@code byte}s (lower bound).
	 *
	 * @return	The position of the backing buffer in {@code byte}s.
	 */
	int position(){
		return buffer.position() - (remaining + Byte.SIZE - 1) / Byte.SIZE;
	}

	/**
	 * Sets the position of the backing {@link ByteBuffer} in {@code byte}s.
	 *
	 * @param newPosition	The position of the backing buffer in {@code byte}s.
	 */
	void position(final int newPosition){
		buffer.position(newPosition);

		resetInnerVariables();
	}

	private void resetInnerVariables(){
		remaining = 0;
		cache = 0;
	}

	/**
	 * Tells whether there are any elements between the current position and the limit of the underlying {@link ByteBuffer}.
	 *
	 * @return	Whether there is at least one element remaining in the underlying {@link ByteBuffer}
	 */
	boolean hasRemaining(){
		return buffer.hasRemaining();
	}

	@Override
	public String toString(){
		return ByteHelper.toHexString(array());
	}

}
