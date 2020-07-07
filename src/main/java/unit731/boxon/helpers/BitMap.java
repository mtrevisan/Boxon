package unit731.boxon.helpers;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;


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
		return BitMap.valueOf(ByteBuffer.wrap(array));
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
		int n;
		for(n = array.length; n > 0 && array[n - 1] == 0; n --)
			;
		return new BitMap(Arrays.copyOf(array, n));
	}

	public static BitMap valueOf(final ByteBuffer bb){
		//TODO
		final BitMap bm = new BitMap(0);
		bm.bset = BitSet.valueOf(bb);
		return bm;
	}

	public BitMap(final int length){
		//TODO
		bset = new BitSet(length);
	}

	/**
	 * Creates a bit set using words as the internal representation.
	 * The last word (if there is one) must be non-zero.
	 */
	private BitMap(final long[] array){
		bset = BitSet.valueOf(array);
	}

	public boolean get(final int bitIndex){
		//TODO
		return bset.get(bitIndex);
	}

	public void set(final int bitIndex){
		//TODO
		bset.set(bitIndex);
	}

	public void flip(final int bitIndex){
		//TODO
		bset.flip(bitIndex);
	}

	public int nextSetBit(final int fromIndex){
		//TODO
		return bset.nextSetBit(fromIndex);
	}

	public int length(){
		//TODO
		return bset.length();
	}

	public byte[] toByteArray(){
		//TODO
		return bset.toByteArray();
	}

	public long[] toLongArray(){
		//TODO
		return bset.toLongArray();
	}


	public boolean equals(final Object obj){
		if(!(obj instanceof BitMap))
			return false;
		if(this == obj)
			return true;

		return bset.equals(((BitMap)obj).bset);
	}

	public int hashCode(){
		return bset.hashCode();
	}

}
