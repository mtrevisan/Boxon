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
	 * Sets the bit at the specified index to the complement of its current value.
	 *
	 * @param bitIndex	The index of the bit to flip
	 */
	public void flip(final int bitIndex){
		final int idx = Arrays.binarySearch(indexes, bitIndex);
		if(idx < 0)
			addSetBit(-idx - 1, bitIndex);
		else
			removeSetBit(idx);
	}

	public void reverseBits(final int size){
		for(int start = 0, end = size - 1; start < end; start ++, end --)
			if(get(start) != get(end)){
				//FIXME one instruction, move to BitSet
				flip(start);
				flip(end);
			}
	}

	/**
	 * Sets the bit at the specified index to {@code true}.
	 *
	 * @param bitIndex	A bit index
	 */
	public void set(final int bitIndex){
		final int idx = Arrays.binarySearch(indexes, bitIndex);
		if(idx < 0)
			addSetBit(-idx - 1, bitIndex);
	}

	private void addSetBit(final int position, final int bitIndex){
		//add index
		final int[] tmp = new int[indexes.length + 1];
		System.arraycopy(indexes, 0, tmp, 0, position);
		tmp[position] = bitIndex;
		System.arraycopy(indexes, position, tmp, position + 1, indexes.length - position);
		indexes = tmp;
	}

	private void removeSetBit(final int idx){
		final int[] tmp = new int[indexes.length - 1];
		System.arraycopy(indexes, 0, tmp, 0, idx);
		System.arraycopy(indexes, idx + 1, tmp, idx, indexes.length - idx - 1);
		indexes = tmp;
	}

	/**
	 * Returns the value of the bit with the specified index.
	 * <p>The value is {@code true} if and only if the bit with the index {@code bitIndex} is currently set in this {@code BitSet}.</p>
	 *
	 * @param bitIndex	The bit index
	 * @return	The value of the bit with the specified index
	 */
	public boolean get(final int bitIndex){
		return (Arrays.binarySearch(indexes, bitIndex) >= 0);
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
