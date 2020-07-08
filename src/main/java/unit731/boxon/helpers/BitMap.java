package unit731.boxon.helpers;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;


/**
 * https://github.com/RoaringBitmap
 * COMPLICATED https://github.com/lemire/sparsebitmap/blob/master/src/main/java/sparsebitmap/SparseBitmap.java
 * https://github.com/brettwooldridge/SparseBitSet/blob/master/src/main/java/com/zaxxer/sparsebits/SparseBitSet.java
 * SAME, OR SLOWER! https://lucene.apache.org/core/3_0_3/api/core/org/apache/lucene/util/OpenBitSet.html
 * SLOWER! https://github.com/ashouldis/bitset
 */
public class BitMap{

	private BitSet bset;


	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 *
	 * <p>More precisely,<br>
	 * <br>{@code BitMap.valueOf(bytes).get(n) == ((bytes[n / 8] & (1 << (n % 8))) != 0)}
	 * <br>for all {@code n < 8 * bytes.length}.
	 * </p>
	 * <br>
	 * <p>This method is equivalent to<br>
	 * <br>{@code BitMap.valueOf(ByteBuffer.wrap(bytes))}.
	 * </p>
	 *
	 * @param array	A byte array containing a little-endian representation of a sequence of bits to be used as the
	 * 	initial bits of the new bit set
	 * @return	A {@code BitMap} containing all the bits in the byte array
	 */
	public static BitMap valueOf(final byte[] array){
		return new BitMap(ByteBuffer.wrap(array));
	}

	/**
	 * Returns a new bit set containing all the bits in the given byte array.
	 *
	 * <p>More precisely,<br>
	 * <br>{@code BitMap.valueOf(bytes).get(n) == ((bytes[n / 8] & (1 << (n % 8))) != 0)}
	 * <br>for all {@code n < 8 * bytes.length}.
	 * </p>
	 * <br>
	 * <p>This method is equivalent to<br>
	 * <br>{@code BitMap.valueOf(ByteBuffer.wrap(bytes))}.
	 * </p>
	 *
	 * @param array	A byte array containing a little-endian representation of a sequence of bits to be used as the
	 * 	initial bits of the new bit set
	 * @return	A {@code BitMap} containing all the bits in the byte array
	 */
	public static BitMap valueOf(final long[] array){
		int n = array.length;
		while(n > 0 && array[n - 1] == 0)
			n --;
		return new BitMap(Arrays.copyOf(array, n));
	}

	/**
	 * Creates a bit set whose initial size is large enough to explicitly
	 * represent bits with indices in the range {@code 0} through
	 * {@code length-1}. All bits are initially {@code false}.
	 *
	 * @param length	The initial size of the bit set
	 * @throws NegativeArraySizeException	If the specified initial size is negative
	 */
	public BitMap(final int length){
		bset = new BitSet(length);
	}

	private BitMap(final ByteBuffer buffer){
		bset = BitSet.valueOf(buffer);
	}

	/**
	 * Creates a bit set using words as the internal representation.
	 * <p>The last word (if there is one) must be non-zero.</p>
	 */
	private BitMap(final long[] array){
		bset = BitSet.valueOf(array);
	}

	/**
	 * Returns the value of the bit with the specified index.
	 *
	 * @param bitIndex	The bit index
	 * @return	The value of the bit with the specified index
	 * @throws IndexOutOfBoundsException	If the specified index is negative
	 */
	public boolean get(final int bitIndex){
		return bset.get(bitIndex);
	}

	/**
	 * Sets the bit at the specified index to {@code true}.
	 *
	 * @param bitIndex	A bit index
	 * @throws IndexOutOfBoundsException	If the specified index is negative
	 */
	public void set(final int bitIndex){
		bset.set(bitIndex);
	}

	/**
	 * Sets the bit at the specified index to the complement of its current value.
	 *
	 * @param bitIndex	The index of the bit to flip
	 * @throws IndexOutOfBoundsException	If the specified index is negative
	 */
	public void flip(final int bitIndex){
		bset.flip(bitIndex);
	}

	/**
	 * Returns the index of the first bit that is set to {@code true}
	 * that occurs on or after the specified starting index.
	 * <p>If no such bit exists then {@code -1} is returned.</p>
	 *
	 * @param fromIndex	The index to start checking from (inclusive)
	 * @return	The index of the next set bit, or {@code -1} if there is no such bit
	 * @throws IndexOutOfBoundsException	If the specified index is negative
	 */
	public int nextSetBit(final int fromIndex){
		return bset.nextSetBit(fromIndex);
	}

	/**
	 * Returns a new byte array containing all the bits in this bit set.
	 *
	 * <p>More precisely, if
	 * <br>{@code byte[] bytes = s.toByteArray();}
	 * <br>then {@code bytes.length == (s.length() + 7) / 8} and
	 * <br>{@code s.get(n) == ((bytes[n / 8] & (1 << (n % 8))) != 0)}
	 * <br>for all {@code n < 8 * bytes.length}.
	 *
	 * @return	A byte array containing a little-endian representation of all the bits in this bit set
	 */
	public byte[] toByteArray(){
		return bset.toByteArray();
	}


	@Override
	public boolean equals(final Object obj){
		if(!(obj instanceof BitMap))
			return false;
		if(this == obj)
			return true;

		return bset.equals(((BitMap)obj).bset);
	}

	@Override
	public int hashCode(){
		return bset.hashCode();
	}

}
