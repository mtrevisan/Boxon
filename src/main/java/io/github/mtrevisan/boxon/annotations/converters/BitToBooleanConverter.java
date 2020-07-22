package io.github.mtrevisan.boxon.annotations.converters;

import io.github.mtrevisan.boxon.helpers.BitSet;


public class BitToBooleanConverter implements Converter<BitSet, Boolean>{

	@Override
	public Boolean decode(final BitSet value){
		return value.testBit(0);
	}

	@Override
	public BitSet encode(final Boolean value){
		final BitSet bs = new BitSet();
		if(value)
			bs.addNextSetBit(0);
		return bs;
	}

}
