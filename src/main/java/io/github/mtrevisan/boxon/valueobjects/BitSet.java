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
package io.github.mtrevisan.boxon.valueobjects;

import java.util.Arrays;


/**
 * This class implements a vector of bits that grows as needed.
 * <p>Each set bit of the bit set is stored as an index.</p>
 * <p>By default, all bits in the set initially are zero.</p>
 * <p>The maximum value that can be stored is {@link java.lang.Integer#MAX_VALUE}</p>
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
public final class BitSet{

	/** The array containing the indexes. */
	private int[] indexes = new int[0];
	/** The number of indexes stored. */
	private int cardinality;


	/**
	 * Returns a new bit set containing all the bits in the given long array.
	 *
	 * @param array	A long array containing a little-endian representation of a sequence of bits to be used as the initial bits of the
	 * 	new bit set.
	 * @return	A {@code BitSet} containing all the bits in the long array.
	 */
	public static BitSet valueOf(final long[] array){
		return new BitSet(array);
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 *
	 * @param array	A byte array containing a little-endian representation of a sequence of bits to be used as the initial bits of the
	 * 	new bit set.
	 * @return	A {@code BitSet} containing all the bits in the byte array.
	 */
	public static BitSet valueOf(final byte[] array){
		return new BitSet(array);
	}


	private BitSet(final byte[] words){
		int length = 0;
		for(int i = 0; i < words.length; i ++)
			length += Integer.bitCount(words[i] & 0xFF);

		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(int i = 0; i < words.length; i ++){
			byte word = words[i];
			while(word != 0){
				final int skip = Integer.numberOfTrailingZeros(word);
				indexes[k ++] = skip + offset;
				word ^= 1 << skip;
			}
			offset += Byte.SIZE;
		}
		cardinality = length;
	}

	private BitSet(final long[] words){
		int length = 0;
		for(int i = 0; i < words.length; i ++)
			length += Long.bitCount(words[i]);

		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(int i = 0; i < words.length; i ++){
			long word = words[i];
			while(word != 0l){
				final int skip = Long.numberOfTrailingZeros(word);
				indexes[k ++] = skip + offset;
				word ^= 1l << skip;
			}
			offset += Long.SIZE;
		}
		cardinality = length;
	}

	public BitSet(){}


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
	public boolean testBit(final int bitIndex){
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
		reverseIndexes();
	}

	private void reverseIndexes(){
		for(int start = 0, end = indexes.length - 1; start < end; start ++, end --){
			//swap array[start] with array[end]
			indexes[start] ^= indexes[end];
			indexes[end] ^= indexes[start];
			indexes[start] ^= indexes[end];
		}
	}

	/**
	 * Returns a new byte array containing all the bits in this bit set.
	 * <p>More precisely, if
	 * {@code byte[] bytes = s.toByteArray();}<br>
	 * then {@code bytes.length == (s.length()+7)/8} and<br>
	 * {@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}<br>
	 * for all {@code n < 8 * bytes.length}.</p>
	 *
	 * @return	A byte array containing a little-endian representation of all the bits in this bit set.
	 */
	public byte[] toByteArray(){
		if(cardinality == 0)
			return new byte[]{0};

		final byte[] bytes = new byte[indexes[cardinality - 1] / Byte.SIZE + 1];
		for(int i = 0; i < indexes.length; i ++){
			final int index = indexes[i];
			bytes[index / Byte.SIZE] |= 1 << (index % Byte.SIZE);
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


	@Override
	public String toString(){
		return Arrays.toString(indexes);
	}

	@Override
	public int hashCode(){
		return Arrays.hashCode(indexes);
	}

	@Override
	public boolean equals(final Object obj){
		if(!(obj instanceof BitSet))
			return false;
		if(this == obj)
			return true;

		final BitSet rhs = (BitSet)obj;
		return (cardinality == rhs.cardinality
			&& Arrays.equals(indexes, 0, cardinality, rhs.indexes, 0, rhs.cardinality));
	}

}
