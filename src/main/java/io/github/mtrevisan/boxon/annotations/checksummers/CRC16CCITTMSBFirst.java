/*
 * Copyright (c) 2020-2024 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.annotations.checksummers;


/**
 * Calculates a 16 bit Cyclic Redundancy Check of a sequence of bytes using the CRC-CCITT algorithms.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
 * @see <a href="https://www.source-code.biz/snippets/java/crc16/">Crc16 - Fast byte-wise 16-bit CRC calculation</a>
 */
abstract class CRC16CCITTMSBFirst implements Checksummer{

	/** CCITT polynomial: x^16 + x^12 + x^5 + 1 -> 1_0000_0010_0001 = 0x1021. */
	private static final int POLYNOMIAL = 0x0000_1021;


	/**
	 * The size in bits of the CRC read from the stream (NOT the real CRC size!).
	 *
	 * @return The size in bit of the CRC.
	 */
	public static int getCRCSize(){
		return 16;
	}

	abstract int initialValue();

	@Override
	public final Number calculateChecksum(final byte[] data, final int start, final int end){
		int checksum = initialValue();
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			checksum ^= datum << Byte.SIZE;
			for(int j = 0; j < Byte.SIZE; j ++){
				checksum <<= 1;
				if((checksum & 0x1_0000) != 0)
					checksum ^= POLYNOMIAL;
			}
		}
		return (short)checksum;
	}

}
