package unit731.boxon.other.dtos;

import unit731.boxon.utils.ByteHelper;


public class Nibbles{

	private final byte high;
	private final byte low;


	public Nibbles(final byte value){
		high = ByteHelper.applyMaskAndShift(value, (byte)0xF0);
		low = ByteHelper.applyMaskAndShift(value, (byte)0x0F);
	}

	/**
	 * Take the lowest 0x0F bits of high and the lowest 0x0F bits of low.
	 *
	 * @param high	High nibble
	 * @param low	Low nibble
	 */
	public Nibbles(final byte high, final byte low){
		this.high = (byte)((high & 0x0F) << 4);
		this.low = (byte)(low & 0x0F);
	}

	public byte getHighNibble(){
		return high;
	}

	public byte getLowNibble(){
		return low;
	}

	public byte getByte(){
		return (byte)(high | low);
	}

	@Override
	public String toString(){
		return Byte.toString(getByte());
	}

}
