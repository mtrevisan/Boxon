package io.github.mtrevisan.boxon.helpers;

import java.util.BitSet;
import java.util.NavigableMap;
import java.util.TreeMap;


public class BitSetPool{

	private static final NavigableMap<Integer, BitSet> BIT_SETS = new TreeMap<>();


	private BitSetPool(){}


	public static synchronized BitSet require(final int requiredSize){
		final Integer key = BIT_SETS.ceilingKey(requiredSize);
		if(key == null)
			return createNewBitSet(requiredSize);

		final BitSet bitmap = BIT_SETS.get(key);
		BIT_SETS.remove(key);
		bitmap.clear();
		return bitmap;
	}

	private static BitSet createNewBitSet(final int size){
		return new BitSet(size);
	}


	public static synchronized void release(final BitSet bitSet){
		BIT_SETS.put(bitSet.size(), bitSet);
	}

}
