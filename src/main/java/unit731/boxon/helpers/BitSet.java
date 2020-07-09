package unit731.boxon.helpers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;


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
 */
public class BitSet{

	/*
	 * BitSets are packed into arrays of "words".
	 * Currently a word is a long, which consists of 64 bits, requiring 6 address bits.
	 * The choice of word size is determined purely by performance concerns.
	 */
	private static final int ADDRESS_BITS_PER_WORD = 6;
	private static final int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;

	/* Used to shift left or right for a partial word mask */
	private static final long WORD_MASK = 0xFFFF_FFFF_FFFF_FFFFl;


	private long[] words;

	/** The number of words in the logical size of this BitSet. */
	private transient int wordsInUse;


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
			words[i] |= (bb.get() & 0xffL) << (8 * j);
		return new BitSet(words);
	}

	/**
	 * Creates a bit set using words as the internal representation.
	 * <p>The last word (if there is one) must be non-zero.</p>
	 */
	private BitSet(final long[] words){
		this.words = words;
		this.wordsInUse = words.length;
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
		words = new long[wordIndex(length - 1) + 1];
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
		final int n = wordsInUse;
		if(n == 0)
			return new byte[0];

		int len = 8 * (n - 1);
		for(long x = words[n - 1]; x != 0; x >>>= 8)
			len ++;
		final byte[] bytes = new byte[len];
		final ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		for(int i = 0; i < n - 1; i++)
			bb.putLong(words[i]);
		for(long x = words[n - 1]; x != 0; x >>>= 8)
			bb.put((byte) (x & 0xff));
		return bytes;
	}

	/**
	 * Sets the bit at the specified index to the complement of its current value.
	 *
	 * @param bitIndex	The index of the bit to flip
	 */
	public void flip(final int bitIndex){
		final int wordIndex = wordIndex(bitIndex);
		expandTo(wordIndex);

		words[wordIndex] ^= (1l << bitIndex);

		recalculateWordsInUse();
	}

	/**
	 * Sets the field wordsInUse to the logical size in words of the bit set.
	 * <p>WARNING: This method assumes that the number of words actually in use is
	 * less than or equal to the current value of wordsInUse!</p>
	 */
	private void recalculateWordsInUse(){
		//traverse the bitset until a used word is found
		int i;
		for(i = wordsInUse - 1; i >= 0; i --)
			if(words[i] != 0)
				break;

		//the new logical size
		wordsInUse = i + 1;
	}

	/**
	 * Sets the bit at the specified index to {@code true}.
	 *
	 * @param bitIndex	A bit index
	 */
	public void set(final int bitIndex){
		final int wordIndex = wordIndex(bitIndex);
		expandTo(wordIndex);

		words[wordIndex] |= (1l << bitIndex);
	}

	/**
	 * Ensures that the {@link BitSet} can accommodate a given {@code wordIndex}, temporarily violating the invariants.
	 * The caller must restore the invariants before returning to the user, possibly using {@link #recalculateWordsInUse()}.
	 *
	 * @param wordIndex	The index to be accommodated.
	 */
	private void expandTo(final int wordIndex){
		final int wordsRequired = wordIndex + 1;
		if(wordsInUse < wordsRequired){
			ensureCapacity(wordsRequired);
			wordsInUse = wordsRequired;
		}
	}

	/**
	 * Ensures that the BitSet can hold enough words.
	 *
	 * @param wordsRequired	The minimum acceptable number of words.
	 */
	private void ensureCapacity(final int wordsRequired){
		if(words.length < wordsRequired){
			//allocate larger of doubled size or required size
			final int request = Math.max(2 * words.length, wordsRequired);
			words = Arrays.copyOf(words, request);
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
		final int wordIndex = wordIndex(bitIndex);
		return (wordIndex < wordsInUse && ((words[wordIndex] & (1l << bitIndex)) != 0));
	}

	/**
	 * Returns the index of the first bit that is set to {@code true} that occurs on or after the specified starting index.
	 * <p>If no such bit exists then {@code -1} is returned.</p>
	 *
	 * @param fromIndex	The index to start checking from (inclusive)
	 * @return	The index of the next set bit, or {@code -1} if there is no such bit
	 */
	public int nextSetBit(final int fromIndex){
		int u = wordIndex(fromIndex);
		if(u >= wordsInUse)
			return -1;

		long word = words[u] & (WORD_MASK << fromIndex);

		while(true){
			if(word != 0)
				return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);

			if(++ u == wordsInUse)
				return -1;

			word = words[u];
		}
	}


	/**
	 * Returns a string representation of this bit set.
	 * <p>For every index for which this {@code BitSet} contains a bit in the set
	 * state, the decimal representation of that index is included in
	 * the result. Such indices are listed in order from lowest to
	 * highest, separated by ",&nbsp;" (a comma and a space) and
	 * surrounded by braces, resulting in the usual mathematical
	 * notation for a set of integers.</p>
	 *
	 * @return a string representation of this bit set
	 */
	@Override
	public String toString(){
		final int MAX_INITIAL_CAPACITY = Integer.MAX_VALUE - 8;
		final int numBits = (wordsInUse > 128)? cardinality(): wordsInUse * BITS_PER_WORD;
		//avoid overflow in the case of a humongous numBits
		final int initialCapacity = (numBits <= (MAX_INITIAL_CAPACITY - 2) / 6)? 6 * numBits + 2: MAX_INITIAL_CAPACITY;
		final StringBuilder sb = new StringBuilder(initialCapacity);
		sb.append('{');
		int i = nextSetBit(0);
		if(i != -1){
			sb.append(i);
			while(++ i >= 0 && (i = nextSetBit(i)) >= 0){
				int endOfRun = nextClearBit(i);
				do{
					sb.append(", ").append(i);
				}while(++ i != endOfRun);
			}
		}
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Returns the number of bits set to {@code true} in this {@code BitSet}.
	 *
	 * @return	The number of bits set to {@code true} in this {@code BitSet}
	 */
	private int cardinality(){
		int sum = 0;
		for(int i = 0; i < wordsInUse; i ++)
			sum += Long.bitCount(words[i]);
		return sum;
	}

	/**
	 * Returns the index of the first bit that is set to {@code false} that occurs on or after the specified starting index.
	 *
	 * @param fromIndex	The index to start checking from (inclusive)
	 * @return	The index of the next clear bit
	 */
	private int nextClearBit(final int fromIndex){
		int u = wordIndex(fromIndex);
		if(u >= wordsInUse)
			return fromIndex;

		long word = ~words[u] & (WORD_MASK << fromIndex);
		while(true){
			if(word != 0)
				return (u * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);

			if(++ u == wordsInUse)
				return wordsInUse * BITS_PER_WORD;

			word = ~words[u];
		}
	}

	/** Given a bit index, return word index containing it. */
	private static int wordIndex(final int bitIndex){
		return bitIndex >> ADDRESS_BITS_PER_WORD;
	}


	@Override
	public int hashCode(){
		long h = 1234;
		for(int i = wordsInUse; --i >= 0; )
			h ^= words[i] * (i + 1);
		return (int)((h >> 32) ^ h);
	}

	@Override
	public boolean equals(final Object obj){
		if(!(obj instanceof BitSet))
			return false;
		if(this == obj)
			return true;

		final BitSet rhs = (BitSet)obj;
		if(wordsInUse != rhs.wordsInUse)
			return false;

		//check words in use by both BitSets
		for(int i = 0; i < wordsInUse; i++)
			if(words[i] != rhs.words[i])
				return false;
		return true;
	}

}
