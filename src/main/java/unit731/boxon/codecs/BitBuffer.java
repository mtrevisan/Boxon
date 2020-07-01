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

import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;


/**
 * @see <a href="https://github.com/jhg023/BitBuffer/blob/master/src/main/java/bitbuffer/BitBuffer.java">BitBuffer</a>
 * @see <a href="https://graphics.stanford.edu/~seander/bithacks.html#ConditionalSetOrClearBitsWithoutBranching">Bit Twiddling Hacks</a>
 */
class BitBuffer{

	/** The mask used when writing/reading bits */
	static final byte[] MASKS = new byte[Byte.SIZE + 1];
	static{
		byte tmp = 1;
		for(int i = 0; i < MASKS.length; i ++){
			MASKS[i] = (byte)(tmp - 1);
			tmp <<= 1;
		}
	}

	private static final class State{
		private final int position;
		private final int remainingBits;
		private final byte cache;

		State(final int position, final int remainingBits, final byte cache){
			this.position = position;
			this.remainingBits = remainingBits;
			this.cache = cache;
		}
	}


	/** The backing {@link ByteBuffer} */
	private final ByteBuffer buffer;
	/** The number of bits available (to read) within {@code cache} */
	private int remaining;
	/** The <i>cache</i> used when writing and reading bits */
	private byte cache;

	private State fallbackPoint;


	/**
	 * Wraps a byte array into a buffer.
	 *
	 * <p> The new buffer will be backed by the given byte array; that is, modifications to the buffer will cause the array
	 * to be modified and vice versa. The new buffer's capacity and limit will be {@code array.length}, its position will
	 * be zero, its mark will be undefined, and its byte byteOrder will be {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
	 *
	 * @param array	The array that will back this buffer
	 * @return	The new bit buffer
	 */
	public static BitBuffer wrap(final byte[] array){
		return new BitBuffer(ByteBuffer.wrap(array));
	}

	/**
	 * Wraps a byte array into a buffer.
	 *
	 * <p> The new buffer will be backed by the given byte array; that is, modifications to the buffer will cause the array to be modified
	 * and vice versa. The new buffer's capacity and limit will be {@code array.length}, its position will be zero, its mark will be
	 * undefined, and its byte byteOrder will be {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
	 *
	 * @param bitWriter	The {@link BitWriter}
	 * @return	The new bit buffer
	 */
	public static BitBuffer wrap(final BitWriter bitWriter){
		bitWriter.flush();
		return wrap(bitWriter.array());
	}

	/**
	 * A private constructor.
	 *
	 * @param buffer the backing {@link ByteBuffer}.
	 */
	private BitBuffer(final ByteBuffer buffer){
		this.buffer = buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
	}

	public void createFallbackPoint(){
		fallbackPoint = new State(buffer.position(), remaining, cache);
	}

	public void restoreFallbackPoint(){
		if(fallbackPoint != null){
			buffer.position(fallbackPoint.position);
			remaining = fallbackPoint.remainingBits;
			cache = fallbackPoint.cache;

			fallbackPoint = null;
		}
	}


	public void skip(final int length){
		getBits(length);
	}

	public void skipUntilTerminator(final byte terminator, final boolean consumeTerminator){
		getTextUntilTerminator(terminator, consumeTerminator, Charset.defaultCharset());
	}

	public Object get(final Class<?> cls, final ByteOrder byteOrder){
		final Class<?> inputClass = ReflectionHelper.objectiveType(cls);
		if(inputClass == Byte.class)
			return getByte();
		if(inputClass == Short.class)
			return getShort(byteOrder);
		if(inputClass == Integer.class)
			return getInteger(byteOrder);
		if(inputClass == Long.class)
			return getLong(byteOrder);
		if(inputClass == Float.class)
			return getFloat(byteOrder);
		if(inputClass == Double.class)
			return getDouble(byteOrder);

		throw new IllegalArgumentException("Cannot read type " + cls.getSimpleName());
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet}.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link BitBuffer}'s current position.
	 */
	public BitSet getBits(final int length){
		final BitSet value = new BitSet(length);
		int offset = 0;
		while(offset < length){
			//transfer the cache values
			final int size = Math.min(length, remaining);
			int i = offset;
			while(cache != 0 && i < offset + size){
				value.set(i, ((cache & MASKS[1]) != 0));

				cache >>>= 1;
				i ++;
			}
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
	 * Reads the next {@code length} bits and composes a <code>long</code>.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A <code>long</code> value at the {@link BitBuffer}'s current position.
	 */
	public long getLong(final int length, final ByteOrder byteOrder, final boolean unsigned){
		final BitSet bits = getBits(length);
		return ByteHelper.bitsToLong(bits, length, byteOrder, unsigned);
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BigInteger}.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BigInteger} value at the {@link BitBuffer}'s current position.
	 */
	public BigInteger getBigInteger(final int length, final ByteOrder byteOrder){
		final BitSet bits = getBits(length);
		return ByteHelper.bitsToBigInteger(bits, length, byteOrder);
	}

	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet}.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link BitBuffer}'s current position.
	 */
	private long getValue(final int length){
		final BitSet bits = getBits(length);
		return (bits.length() > 0? bits.toLongArray()[0]: 0l);
	}

	/**
	 * Reads {@link Byte#SIZE} bits from this {@link BitBuffer} and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	public byte getByte(){
		return (byte)getValue(Byte.SIZE);
	}

	/**
	 * Reads {@link Byte#SIZE} bits from this {@link BitBuffer} and composes a {@code byte} without advancing the position.
	 *
	 * @return	A {@code byte}.
	 */
	private byte peekByte(){
		long value;
		long temporaryCache = cache;
		if(remaining < Byte.SIZE){
			value = temporaryCache & MASKS[remaining];
			final int remaining = Math.min(buffer.remaining(), Byte.SIZE);
			if(remaining == 0)
				throw new BufferUnderflowException();

			final int position = buffer.position();
			for(int i = 0; i < remaining; i ++)
				//read next byte from the byte buffer
				temporaryCache |= ((long)buffer.array()[position + i] & 0x0000_0000_0000_00FFl) << (i * Byte.SIZE);
			final int difference = Byte.SIZE - this.remaining;
			value |= (temporaryCache & MASKS[difference]) << this.remaining;
		}
		else
			value = temporaryCache & MASKS[Byte.SIZE];
		return (byte)value;
	}

	/**
	 * Reads {@link Byte#SIZE} bits from this {@link BitBuffer} and composes a {@code short}.
	 *
	 * @return    A {@code short}.
	 */
	public short getByteUnsigned(){
		return (short)(getValue(Byte.SIZE) & 0x0000_FFFF);
	}

	/**
	 * Reads the specified amount of {@code byte}s from this {@link BitBuffer} into an array of {@code byte}s.
	 *
	 * @param length	The number of {@code byte}s to read.
	 * @return	An array of {@code byte}s of length {@code n} that contains {@code byte}s read from this {@link BitBuffer}.
	 */
	public byte[] getBytes(final int length){
		final byte[] array = new byte[length];
		for(int i = 0; i < length; i ++)
			array[i] = (byte)getValue(Byte.SIZE);
		return array;
	}

	/**
	 * Reads {@link Short#SIZE} bits from this {@link BitBuffer} and composes a {@code short} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return	A {@code short}.
	 */
	public short getShort(final ByteOrder byteOrder){
		final short value = (short)getValue(Short.SIZE);
		return (byteOrder == ByteOrder.BIG_ENDIAN? Short.reverseBytes(value): value);
	}

	/**
	 * Reads {@link Short#SIZE} bits from this {@link BitBuffer} and composes a {@code short} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return    A {@code short}.
	 */
	public int getShortUnsigned(final ByteOrder byteOrder){
		final short value = (short)getValue(Short.SIZE);
		return ((int)(byteOrder == ByteOrder.BIG_ENDIAN? Short.reverseBytes(value): value) & 0x0000_FFFF);
	}

	/**
	 * Reads {@link Integer#SIZE} bits from this {@link BitBuffer} and composes an {@code int} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return	An {@code int}.
	 */
	public int getInteger(final ByteOrder byteOrder){
		final int value = (int)getValue(Integer.SIZE);
		return (byteOrder == ByteOrder.BIG_ENDIAN? Integer.reverseBytes(value): value);
	}

	/**
	 * Reads {@link Integer#SIZE} bits from this {@link BitBuffer} and composes an {@code int} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return    An {@code int}.
	 */
	public long getIntegerUnsigned(final ByteOrder byteOrder){
		final int value = (int)getValue(Integer.SIZE);
		return ((long)(byteOrder == ByteOrder.BIG_ENDIAN? Integer.reverseBytes(value): value) & 0x0000_0000_FFFF_FFFFl);
	}

	/**
	 * Reads {@link Long#SIZE} bits from this {@link BitBuffer} and composes a {@code long} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return	A {@code long}.
	 */
	public long getLong(final ByteOrder byteOrder){
		final long value = getValue(Long.SIZE);
		return (byteOrder == ByteOrder.BIG_ENDIAN? Long.reverseBytes(value): value);
	}

	/**
	 * Reads {@link Float#SIZE} bits from this {@link BitBuffer} and composes a {@code float} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return	A {@code float}.
	 * @see	#getFloat(ByteOrder)
	 */
	public float getFloat(final ByteOrder byteOrder){
		return Float.intBitsToFloat(getInteger(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits from this {@link BitBuffer} and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @return	A {@code double}.
	 * @see	#getLong(ByteOrder)
	 */
	public double getDouble(final ByteOrder byteOrder){
		return Double.longBitsToDouble(getLong(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits from this {@link BitBuffer} and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param cls	Either a {@code Float} or a {@link Double} class.
	 * @return	A {@code double}.
	 * @see	#getLong(ByteOrder)
	 */
	public BigDecimal getDecimal(final Class<?> cls, final ByteOrder byteOrder){
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
	public String getText(final int length, final Charset charset){
		return new String(getBytes(length), charset);
	}

	/** Reads a string until a terminator is found */
	public String getTextUntilTerminator(final byte terminator, final boolean consumeTerminator, final Charset charset){
		String text = null;
		try(
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(baos, charset);
			){
			readToWriter(terminator, consumeTerminator, osw);
			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

	private void readToWriter(final byte terminator, final boolean consumeTerminator, final OutputStreamWriter os) throws IOException{
		while(buffer.position() < buffer.limit() || remaining > 0){
			final byte byteRead = (consumeTerminator? getByte(): peekByte());
			if(byteRead == terminator)
				break;
			if(!consumeTerminator)
				getByte();

			os.write(byteRead);
		}
		os.flush();
	}


	public void clear(){
		//reset the buffer's position and limit
		buffer.clear();

		//set remainingBits to 0 so that, on the next call to getBits, the cache will be reset
		resetInnerVariables();
	}

	public void reset(){
		buffer.reset();
	}

	public byte[] array(){
		return buffer.array();
	}

	/**
	 * Gets the position of the backing {@link ByteBuffer} in {@code byte}s.
	 *
	 * @return	The position of the backing buffer in {@code byte}s.
	 */
	public int position(){
		return buffer.position();
	}

	/**
	 * Sets the position of the backing {@link ByteBuffer} in {@code byte}s.
	 *
	 * @param newPosition	The position of the backing buffer in {@code byte}s.
	 */
	public void position(final int newPosition){
		buffer.position(newPosition);

		resetInnerVariables();
	}

	private void resetInnerVariables(){
		remaining = 0;
		cache = 0;
	}

	/**
	 * Gets the position of the backing {@link ByteBuffer} in {@code bit}s.
	 *
	 * @return	The position of the backing buffer in {@code bit}s.
	 */
	public int positionAsBits(){
		return buffer.position() * Byte.SIZE - remaining;
	}

	/**
	 * Tells whether there are any elements between the current position and the limit of the underlying {@link ByteBuffer}.
	 *
	 * @return	Whether there is at least one element remaining in the underlying {@link ByteBuffer}
	 */
	public boolean hasRemaining(){
		return buffer.hasRemaining();
	}

	/**
	 * Gets the capacity of the backing {@link ByteBuffer}.
	 *
	 * @return	The capacity of the backing buffer in {@code byte}s.
	 */
	public int capacity(){
		return buffer.capacity();
	}

	/**
	 * Returns the underlying {@link ByteBuffer}'s limit.
	 *
	 * @return  The limit of the underlying {@link ByteBuffer}
	 */
	public int limit(){
		return buffer.limit();
	}

	/** Compacts the backing {@link ByteBuffer} */
	public void compact(){
		buffer.compact();
	}

	@Override
	public String toString(){
		return ByteHelper.byteArrayToHexString(buffer.array());
	}

}
