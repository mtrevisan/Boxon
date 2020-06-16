package unit731.boxon.annotations.checksummers;


/** Calculates a 16 bit Cyclic Redundancy Check of a set of bytes using the CRC-CCITT (0xFFFF) algorithm */
public class CRC16 implements Checksummer<Short>{

	private static final short START_VALUE_0xFFFF = (short)0xFFFF;

	/** CCITT polynomial: x^16 + x^12 + x^5 + 1 -> 1000000100001 = 0x1021 */
	private static final int POLYNOMIAL_CCITT = 0x1021;


	@Override
	public Short calculateCRC(final byte[] data, final int start, final int end){
		short value = START_VALUE_0xFFFF;
		for(int i = Math.max(start, 0); i < Math.min(end, data.length); i ++)
			for(int j = 0; j < Byte.SIZE; j ++){
				final boolean bit = (((data[i] >> (7 - j)) & 1) != 0);
				final boolean c15 = ((value & 0x8000) != 0);
				value <<= 1;
				if(c15 ^ bit){
					value ^= POLYNOMIAL_CCITT;
				}
			}
		return value;
	}

}
