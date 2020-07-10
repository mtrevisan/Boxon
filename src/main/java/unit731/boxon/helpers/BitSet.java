package unit731.boxon.helpers;

import java.util.Arrays;


/**
 * This class implements a vector of bits that grows as needed.
 * <p>Each set bit of the bit set is stored as an index.</p>
 * <p>By default, all bits in the set initially are zero.</p>
 * <p>The maximum value that can be stored is {@link java.lang.Integer#MAX_VALUE}</p>
 * <p>Unless otherwise noted, passing a {@code null} parameter to any of the
 * methods in a {@code BitSet} will result in a {@code NullPointerException}.</p>
 *
 * <p>A {@code BitSet} is not safe for multi-threaded use without
 * external synchronization.</p>
 *
 * @see <a href="https://w6113.github.io/files/papers/sidm338-wangA.pdf">An Experimental Study of Bitmap Compression vs. Inverted List Compression</a>
 * @see <a href="https://onlinelibrary.wiley.com/doi/pdf/10.1002/spe.2203">Decoding billions of integers per second through vectorization</a>
 */
public class BitSet{

	private int[] indexes = new int[0];


	/**
	 * Returns a new bit set containing all the bits in the given long array.
	 * <p>More precisely,
	 * {@code BitSet.valueOf(longs).get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}<br>
	 * for all {@code n < 64 * longs.length}.</p>
	 *
	 * <p>This method is equivalent to {@code BitSet.valueOf(LongBuffer.wrap(longs))}.</p>
	 *
	 * @param array	A long array containing a little-endian representation of a sequence of bits to be used as the initial bits of the
	 * 	new bit set
	 * @return	A {@code BitSet} containing all the bits in the long array
	 */
	public static BitSet valueOf(final long[] array){
		return new BitSet(array);
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 * <p>More precisely,
	 * {@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}<br>
	 * for all {@code n <  8 * bytes.length}.</p>
	 *
	 * <p>This method is equivalent to
	 * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.</p>
	 *
	 * @param array	A byte array containing a little-endian representation of a sequence of bits to be used as the
	 * 	initial bits of the new bit set
	 * @return	A {@code BitSet} containing all the bits in the byte array
	 */
	public static BitSet valueOf(final byte[] array){
		return new BitSet(array);
	}

	private BitSet(final byte[] words){
		int length = 0;
		for(final byte word : words)
			length += Integer.bitCount(word & 0xFF);

		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(byte word : words){
			while(word != 0){
				final int skip = Integer.numberOfTrailingZeros(word);
				indexes[k ++] = skip + offset;
				word ^= 1 << skip;
			}
			offset += Byte.SIZE;
		}
	}

	private BitSet(final long[] words){
		int length = 0;
		for(final long word : words)
			length += Long.bitCount(word);

		indexes = new int[length];
		int k = 0;
		int offset = 0;
		for(long word : words){
			while(word != 0){
				final int skip = Long.numberOfTrailingZeros(word);
				indexes[k ++] = skip + offset;
				word ^= 1l << skip;
			}
			offset += Long.SIZE;
		}
	}

	public BitSet(){}

	/**
	 * Returns a new byte array containing all the bits in this bit set.
	 * <p>More precisely, if
	 * {@code byte[] bytes = s.toByteArray();}<br>
	 * then {@code bytes.length == (s.length()+7)/8} and<br>
	 * {@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}<br>
	 * for all {@code n < 8 * bytes.length}.</p>
	 *
	 * @return	A byte array containing a little-endian representation of all the bits in this bit set
	 */
	public byte[] toByteArray(){
		if(indexes.length == 0)
			return new byte[0];

		final byte[] bytes = new byte[indexes[indexes.length - 1] / Byte.SIZE + 1];
		for(final int index : indexes)
			bytes[index / Byte.SIZE] |= 1 << (index % Byte.SIZE);
		return bytes;
	}

	/**
	 * Returns a long of given length and starting at a given offset.
	 *
	 * @param offset	The bit offset to start the extraction
	 * @param size	The length in bits of the extraction (MUST BE less than {@link Long#SIZE}!)
	 * @return	A long starting at a given offset and of a given length
	 */
	public long toLong(final int offset, final int size){
		int index;
		long value = 0l;
		final int idx = (offset > 0? Arrays.binarySearch(indexes, offset): 0);
		int i = (idx >= 0? idx: -idx - 1);
		final int length = Math.min(size + i, indexes.length);
		while(i < length && 0 <= (index = indexes[i ++]) && index < size + offset)
			value |= 1l << (index - offset);
		return value;
	}

	/**
	 * Sets the bits of a number to the complement of its current value.
	 *
	 * @param size	The size of the number in bits.
	 */
	public void reverseBits(final int size){
		for(int i = 0; i < indexes.length; i ++)
			indexes[i] = size - indexes[i] - 1;
		//re-sort indexes
		for(int start = 0, end = indexes.length - 1; start < end; start ++, end --){
			indexes[start] ^= indexes[end];
			indexes[end] ^= indexes[start];
			indexes[start] ^= indexes[end];
		}
	}

	/**
	 * Adds a set bit at the specified index.
	 *
	 * @param bitIndex	A bit index (MUST BE greater than the previous index!)
	 */
	public void addNextSetBit(final int bitIndex){
		final int position = indexes.length;
		indexes = Arrays.copyOf(indexes, position + 1);
		indexes[position] = bitIndex;
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
		return Arrays.equals(indexes, rhs.indexes);
	}

}
