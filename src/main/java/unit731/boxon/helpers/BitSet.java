package unit731.boxon.helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * This class implements a vector of bits that grows as needed.
 * <p>Each component of the bit set has a {@code boolean} value. The
 * bits of a {@code BitSet} are indexed by non-negative integers.
 * Individual indexed bits can be examined, set, or cleared. One
 * {@code BitSet} may be used to modify the contents of another
 * {@code BitSet} through logical AND, logical inclusive OR, and
 * logical exclusive OR operations.</p>
 *
 * <p>By default, all bits in the set initially have the value
 * {@code false}.</p>
 *
 * <p>Every bit set has a current size, which is the number of bits
 * of space currently in use by the bit set. Note that the size is
 * related to the implementation of a bit set, so it may change with
 * implementation. The length of a bit set relates to logical length
 * of a bit set and is defined independently of implementation.</p>
 *
 * <p>Unless otherwise noted, passing a null parameter to any of the
 * methods in a {@code BitSet} will result in a {@code NullPointerException}.</p>
 *
 * <p>A {@code BitSet} is not safe for multi-threaded use without
 * external synchronization.</p>
 *
 * @author Arthur van Hoff
 * @author Michael McCloskey
 * @author Martin Buchholz
 *
 * @see <a href="https://w6113.github.io/files/papers/sidm338-wangA.pdf">An Experimental Study of Bitmap Compression vs. Inverted List Compression</a>
 * @see <a href="https://onlinelibrary.wiley.com/doi/pdf/10.1002/spe.2203">Decoding billions of integers per second through vectorization</a>
 */
public class BitSet{

	private int[] indexes;


	/**
	 * Returns a new bit set containing all the bits in the given long array.
	 * <p>More precisely,
	 * <br>{@code BitSet.valueOf(longs).get(n) == ((longs[n/64] & (1L<<(n%64))) != 0)}
	 * <br>for all {@code n < 64 * longs.length}.</p>
	 *
	 * <p>This method is equivalent to {@code BitSet.valueOf(LongBuffer.wrap(longs))}.</p>
	 *
	 * @param longs	A long array containing a little-endian representation of a sequence of bits to be used as the initial bits of the
	 * 	new bit set
	 * @return	A {@code BitSet} containing all the bits in the long array
	 */
	public static BitSet valueOf(final long[] longs){
		int n = longs.length;
		while(n > 0 && longs[n - 1] == 0)
			n --;
		return new BitSet(Arrays.copyOf(longs, n));
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 * <p>More precisely,
	 * <br>{@code BitSet.valueOf(bytes).get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
	 * <br>for all {@code n <  8 * bytes.length}.</p>
	 *
	 * <p>This method is equivalent to
	 * {@code BitSet.valueOf(ByteBuffer.wrap(bytes))}.</p>
	 *
	 * @param bytes	A byte array containing a little-endian representation of a sequence of bits to be used as the
	 * 	initial bits of the new bit set
	 * @return	A {@code BitSet} containing all the bits in the byte array
	 */
	public static BitSet valueOf(final byte[] bytes){
		return BitSet.valueOf(ByteBuffer.wrap(bytes));
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte buffer between its position and limit.
	 * <p>More precisely,
	 * <br>{@code BitSet.valueOf(bb).get(n) == ((bb.get(bb.position()+n/8) & (1<<(n%8))) != 0)}
	 * <br>for all {@code n < 8 * bb.remaining()}.</p>
	 *
	 * <p>The byte buffer is not modified by this method, and no
	 * reference to the buffer is retained by the bit set.</p>
	 *
	 * @param bb	A byte buffer containing a little-endian representation of a sequence of bits between its position and limit, to be
	 * 	used as the initial bits of the new bit set
	 * @return	A {@code BitSet} containing all the bits in the buffer in the specified range
	 */
	public static BitSet valueOf(ByteBuffer bb){
		bb = bb.slice().order(ByteOrder.LITTLE_ENDIAN);
		int n = bb.remaining();
		while(n > 0 && bb.get(n - 1) == 0)
			n --;
		final long[] words = new long[(n + 7) / 8];
		bb.limit(n);
		int i = 0;
		while(bb.remaining() >= 8)
			words[i ++] = bb.getLong();
		for(int remaining = bb.remaining(), j = 0; j < remaining; j ++)
			words[i] |= (bb.get() & 0xFFl) << (8 * j);
		return new BitSet(words);
	}

	/**
	 * Creates a bit set using words as the internal representation.
	 * <p>The last word (if there is one) must be non-zero.</p>
	 */
	private BitSet(final long[] words){
		final List<Integer> list = new ArrayList<>();
		int offset = 0;
		for(final long word : words){
			for(int i = 0; i < Long.SIZE; i ++)
				if((word & (1l << i)) != 0)
					list.add(i + offset);
			offset += Long.SIZE;
		}
		indexes = list.stream()
			.mapToInt(i -> i)
			.toArray();
	}

	/**
	 * Creates a bit set whose initial size is large enough to explicitly
	 * represent bits with indices in the range {@code 0} through
	 * {@code length-1}.
	 * <p>All bits are initially {@code false}.</p>
	 *
	 * @param length	The initial size of the bit set
	 */
	public BitSet(final int length){
		indexes = new int[0];
	}

	/**
	 * Returns a new byte array containing all the bits in this bit set.
	 * <p>More precisely, if
	 * <br>{@code byte[] bytes = s.toByteArray();}
	 * <br>then {@code bytes.length == (s.length()+7)/8} and
	 * <br>{@code s.get(n) == ((bytes[n/8] & (1<<(n%8))) != 0)}
	 * <br>for all {@code n < 8 * bytes.length}.</p>
	 *
	 * @return	A byte array containing a little-endian representation of all the bits in this bit set
	 */
	public byte[] toByteArray(){
		if(indexes.length == 0)
			return new byte[0];

		final byte[] bytes = new byte[(indexes[indexes.length - 1] + Byte.SIZE - 1) / Byte.SIZE];
		for(final int index : indexes)
			bytes[index / Byte.SIZE] |= 1 << (index % Byte.SIZE);
		return bytes;
	}

	/**
	 * Sets the bit at the specified index to the complement of its current value.
	 *
	 * @param bitIndex	The index of the bit to flip
	 */
	public void flip(final int bitIndex){
		int idx = Arrays.binarySearch(indexes, bitIndex);
		if(idx >= 0){
			//remove index
			final int[] tmp = new int[indexes.length - 1];
			System.arraycopy(indexes, 0, tmp, 0, idx);
			System.arraycopy(indexes, idx + 1, tmp, idx, indexes.length - idx - 1);
			indexes = tmp;
		}
		else{
			idx = -idx - 1;

			//add index
			final int[] tmp = new int[indexes.length + 1];
			System.arraycopy(indexes, 0, tmp, 0, idx);
			tmp[idx] = bitIndex;
			System.arraycopy(indexes, idx, tmp, idx + 1, indexes.length - idx);
			indexes = tmp;
		}
	}

	/**
	 * Sets the bit at the specified index to {@code true}.
	 *
	 * @param bitIndex	A bit index
	 */
	public void set(final int bitIndex){
		int idx = Arrays.binarySearch(indexes, bitIndex);
		if(idx <= 0){
			idx = -idx - 1;

			//add index
			final int[] tmp = new int[indexes.length + 1];
			System.arraycopy(indexes, 0, tmp, 0, idx);
			tmp[idx] = bitIndex;
			System.arraycopy(indexes, idx, tmp, idx + 1, indexes.length - idx);
			indexes = tmp;
		}
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

	/**
	 * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index.
	 * <p>If no such bit exists then {@code -1} is returned.</p>
	 *
	 * @param fromIndex	The index to start checking from (inclusive)
	 * @return	The index of the next set bit, or {@code -1} if there is no such bit
	 */
	public int nextSetBit(final int fromIndex){
		int idx = Arrays.binarySearch(indexes, fromIndex);
		if(idx < 0)
			idx = -idx - 1;
		return (idx < indexes.length? indexes[idx]: -1);
	}


	@Override
	public int hashCode(){
		long h = 1234;
		for(int i = indexes.length; -- i >= 0; )
			h ^= indexes[i] * (i + 1);
		return (int)((h >> 32) ^ h);
	}

	@Override
	public boolean equals(final Object obj){
		if(!(obj instanceof BitSet))
			return false;
		if(this == obj)
			return true;

		final BitSet rhs = (BitSet)obj;
		if(indexes.length != rhs.indexes.length)
			return false;

		//check words in use by both BitSets
		return Arrays.equals(indexes, rhs.indexes);
	}

}
