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

import java.math.BigInteger;
import java.util.Arrays;


/**
 * This class implements a vector of bits that grows as needed.
 * <p>Each set bit of the bit set is stored as an index.</p>
 * <p>By default, all bits in the set initially are zero.</p>
 * <p>The maximum value that can be stored is {@link java.lang.Integer#MAX_VALUE MAX_VALUE}</p>
 * <p>Unless otherwise noted, passing a {@code null} parameter to any of the
 * methods in a {@code BitSet} will result in a {@code NullPointerException}.</p>
 * <p>Moreover, passing a non-increasing value to {@link #addNextSetBit(int)} will
 * result in an unpredictable behavior.</p>
 *
 * <p>A {@code BitSet} is not safe for multi-threaded use without
 * external synchronization.</p>
 *
 * @see <a href="https://w6113.github.io/files/papers/sidm338-wangA.pdf">An Experimental Study of Bitmap Compression vs. Inverted List Compression</a>
 * @see <a href="https://onlinelibrary.wiley.com/doi/pdf/10.1002/spe.2203">Decoding billions of integers per second through vectorization</a>
 */
@SuppressWarnings("WeakerAccess")
public final class BitSet{

	/** The array containing the indexes. */
	private int[] indexes = new int[0];
	/** The number of indexes stored. */
	private int cardinality;


	/**
	 * Returns an empty new bit set.
	 *
	 * @return	A {@code BitSet} containing all the bits in the long array.
	 */
	public static BitSet empty(){
		return new BitSet();
	}

	/**
	 * Returns a new bit set containing all the bits in the given long array.
	 *
	 * @param array	A long array containing a little-endian representation of a sequence of bits to be used as
	 * 	the initial bits of the new bit set.
	 * @return	A {@code BitSet} containing all the bits in the long array.
	 */
	public static BitSet valueOf(final long[] array){
		return new BitSet(array);
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 *
	 * @param array	A byte array containing a little-endian representation of a sequence of bits to be used as
	 * 	the initial bits of the new bit set.
	 * @return	A {@code BitSet} containing all the bits in the byte array.
	 */
	public static BitSet valueOf(final byte[] array){
		return new BitSet(array);
	}

	/**
	 * Converts a BigInteger into a byte array ignoring the sign of the BigInteger, according to SRP specification.
	 *
	 * @param value	the value, must not be {@code null}.
	 * @param size	The size in bits of the value.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The bit set representing the given value.
	 */
	public static BitSet valueOf(final BigInteger value, final int size, final ByteOrder byteOrder){
		byte[] array = value.toByteArray();
		final int newSize = (size + Byte.SIZE - 1) >>> 3;
		if(newSize != array.length){
			final int offset = Math.max(array.length - newSize, 0);
			final byte[] newArray = new byte[newSize];
			final int newArrayOffset = Math.max(newArray.length - array.length, 0);
			System.arraycopy(array, offset, newArray, newArrayOffset, array.length - offset);
			array = newArray;
		}
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			reverse(array);
		return valueOf(array);
	}


	private BitSet(final byte[] words){
		final int length = bitCount(words);
		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(int i = 0; i < words.length; i ++, offset += Byte.SIZE)
			k = addWordToIndexes(words[i], k, offset);
		cardinality = length;
	}

	private static int bitCount(final byte[] words){
		int length = 0;
		for(int i = 0; i < words.length; i ++)
			length += Integer.bitCount(words[i] & 0xFF);
		return length;
	}

	private int addWordToIndexes(byte word, int k, final int offset){
		while(word != 0){
			final int skip = Integer.numberOfTrailingZeros(word);
			indexes[k ++] = skip + offset;
			word ^= 1 << skip;
		}
		return k;
	}

	private BitSet(final long[] words){
		final int length = bitCount(words);
		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(int i = 0; i < words.length; i ++, offset += Long.SIZE)
			k = addWordToIndexes(words[i], k, offset);
		cardinality = length;
	}

	private static int bitCount(final long[] words){
		int length = 0;
		for(int i = 0; i < words.length; i ++)
			length += Long.bitCount(words[i]);
		return length;
	}

	private int addWordToIndexes(long word, int k, final int offset){
		while(word != 0l){
			final int skip = Long.numberOfTrailingZeros(word);
			indexes[k ++] = skip + offset;
			word ^= 1l << skip;
		}
		return k;
	}

	private BitSet(){}


	/**
	 * Ensure the set can contain {@code size} more bits.
	 *
	 * @param size	The number of bits to be reserved.
	 */
	public void ensureAdditionalSpace(final int size){
		indexes = Arrays.copyOf(indexes, cardinality + size);
	}

	/**
	 * Adds a set bit at the specified index.
	 *
	 * @param bitIndex	A bit index (MUST BE greater than the previous index!).
	 */
	public void addNextSetBit(final int bitIndex){
		ensureAdditionalSpace(1);

		indexes[cardinality ++] = bitIndex;
	}

	/**
	 * Return the state of the bit at a specified index.
	 *
	 * @param bitIndex	A bit index (MUST BE greater than the previous index!).
	 * @return	The state of the bit at a specified index.
	 */
	public boolean isBitSet(final int bitIndex){
		return (Arrays.binarySearch(indexes, bitIndex) >= 0);
	}

	/**
	 * Sets the bits of a number to the complement of its current value.
	 *
	 * @param size	The size of the number in bits.
	 */
	public void reverseBits(final int size){
		for(int i = 0; i < cardinality; i ++)
			indexes[i] = size - indexes[i] - 1;

		//re-sort indexes
		for(int start = 0, end = cardinality - 1; start < end; start ++, end --)
			//swap array[start] with array[end]
			indexes[start] ^= indexes[end] ^ (indexes[end] = indexes[start]);
	}

	/**
	 * Returns a new byte array containing all the bits in this bit set.
	 * <p>More precisely, if
	 * {@code byte[] bytes = s.toByteArray();}<br>
	 * then {@code bytes.length == (s.length() + Byte.SIZE - 1) / Byte.SIZE} and<br>
	 * {@code s.get(n) == ((bytes[n / Byte.SIZE] & (1 << (n % Byte.SIZE))) != 0)}<br>
	 * for all {@code n < bytes.length * Byte.SIZE}.</p>
	 *
	 * @return	A byte array containing a little-endian representation of all the bits in this bit set.
	 */
	public byte[] toByteArray(){
		if(cardinality == 0)
			return new byte[]{0};

		final byte[] bytes = new byte[(indexes[cardinality - 1] >>> 3) + 1];
		for(int i = 0; i < indexes.length; i ++){
			final int index = indexes[i];
			bytes[index >>> 3] |= 1 << (index % Byte.SIZE);
		}
		return bytes;
	}

	/**
	 * Returns a long of given length and starting at a given offset.
	 *
	 * @param offset	The bit offset to start the extraction.
	 * @param size	The length in bits of the extraction (MUST BE less than {@link Long#SIZE}!).
	 * @return	A long starting at a given offset and of a given length.
	 */
	public long toLong(final int offset, final int size){
		long value = 0l;
		if(indexes.length > 0){
			int index;
			int i = getStartingIndex(offset);
			final int length = Math.min(size + i, cardinality);
			while(i < length && 0 <= (index = indexes[i ++]) && index - offset < size)
				value |= 1l << (index - offset);
		}
		return value;
	}

	private int getStartingIndex(final int offset){
		final int idx = (offset > 0? Arrays.binarySearch(indexes, offset): 0);
		return (idx >= 0? idx: -idx - 1);
	}

	/**
	 * Convert this bit set to {@link BigInteger}.
	 *
	 * @param size	The number of bits.
	 * @param byteOrder	The byte order.
	 * @return	The converted {@link BigInteger}.
	 */
	public BigInteger toInteger(final int size, final ByteOrder byteOrder){
		byte[] array = toByteArray();
		final int expectedLength = size >>> 3;
		if(array.length < expectedLength)
			array = Arrays.copyOf(array, expectedLength);
		if(byteOrder == ByteOrder.LITTLE_ENDIAN)
			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitMap is little-endian
			reverse(array);
		return extendSign(array);
	}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param array	Field value.
	 * @return	The 2-complement expressed as int.
	 */
	private static BigInteger extendSign(byte[] array){
		if((array[0] & 0x80) != 0x00){
			array = leftExtendArray(array);
			array[0] = -1;
		}
		return new BigInteger(array);
	}

	/**
	 * Extends an array leaving room for one more byte at the leftmost index.
	 *
	 * @param array	The array to extend.
	 * @return	The extended array.
	 */
	private static byte[] leftExtendArray(final byte[] array){
		final byte[] extendedArray = new byte[array.length + 1];
		System.arraycopy(array, 0, extendedArray, 1, array.length);
		return extendedArray;
	}

	/**
	 * Reverses the order of the given array.
	 *
	 * @param array	The array to reverse.
	 */
	private static void reverse(final byte[] array){
		for(int start = 0, end = array.length - 1; start < end; start ++, end --)
			//swap array[start] with array[end]
			array[start] ^= array[end] ^ (array[end] = array[start]);
	}


	@Override
	public String toString(){
		return Arrays.toString(indexes);
	}

	@Override
	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	public boolean equals(final Object obj){
		if(!BitSet.class.isInstance(obj))
			return false;
		if(this == obj)
			return true;

		final BitSet rhs = (BitSet)obj;
		return (cardinality == rhs.cardinality
			&& Arrays.equals(indexes, 0, cardinality, rhs.indexes, 0, rhs.cardinality));
	}

	@Override
	public int hashCode(){
		return Arrays.hashCode(indexes);
	}

}
