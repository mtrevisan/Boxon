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
 * Calculates a 16 bit Cyclic Redundancy Check of a sequence of bytes using the CRC-CCITT Normal algorithm.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Cyclic_redundancy_check">Cyclic Redundancy Check</a>
 */
public final class CRC16CCITT implements Checksummer{

	/** Starting value 0x0000. */
	public static final short START_VALUE_0x0000 = 0x0000;
	/** Starting value 0x1D0F. */
	public static final short START_VALUE_0x1D0F = 0x1D0F;
	/** Starting value 0xFFFF. */
	public static final short START_VALUE_0xFFFF = (short)0xFFFF;

	/** CCITT polynomial: x^16 + x^12 + x^5 + 1 -> 1000000100001 = 0x1021. */
	private static final int POLYNOMIAL_CCITT = 0x1021;


	CRC16CCITT(){}


	@Override
	public short calculateChecksum(final byte[] data, final int start, final int end, final int startValue){
		short value = (short)startValue;
		for(int i = Math.max(start, 0), length = Math.min(end, data.length); i < length; i ++){
			final byte datum = data[i];

			for(int j = Byte.SIZE - 1; j >= 0; j --){
				final boolean bit = (((datum >> j) & 1) != 0);
				final boolean c15 = ((value & 0x8000) != 0);
				value <<= 1;
				if(c15 ^ bit)
					value ^= POLYNOMIAL_CCITT;
			}
		}
		return value;
	}

}
