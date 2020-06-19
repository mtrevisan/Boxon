/**
 * Copyright (c) 2020 Mauro Trevisan
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
package unit731.boxon.utils;

import java.util.BitSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ByteHelperTest{

	@Test
	void hasBitWithByte(){
		byte mask = 0x27;

		byte output = 0x00;
		for(int i = 0; i < 8; i ++){
			boolean bitSet = ByteHelper.hasBit(mask, i);
			if(bitSet){
				output |= (1 << i);
			}
		}

		Assertions.assertEquals(mask, output);
	}

	@Test
	void indexOf1a(){
		byte[] source = new byte[]{0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x03};
		byte[] pattern = new byte[]{0x03, 0x07};

		int index = ByteHelper.indexOf(source, pattern, 0);

		Assertions.assertEquals(4, index);
	}

	@Test
	void indexOf1b(){
		byte[] source = new byte[]{0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x03};
		byte[] pattern = new byte[]{0x03, 0x07};

		int index = ByteHelper.indexOf(source, pattern, 1);

		Assertions.assertEquals(4, index);
	}

	@Test
	void indexOf1c(){
		byte[] source = new byte[]{0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x03};
		byte[] pattern = new byte[]{0x03, 0x07};

		int index = ByteHelper.indexOf(source, pattern, 5);

		Assertions.assertEquals(-1, index);
	}

	@Test
	void indexOf2(){
		byte[] source = new byte[]{0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x03};
		byte[] pattern = new byte[]{0x03, 0x03, 0x17};

		int index = ByteHelper.indexOf(source, pattern, 0);

		Assertions.assertEquals(-1, index);
	}

	@Test
	void indexOf3(){
		byte[] source = new byte[]{0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x03};
		byte[] pattern = new byte[]{0x03, 0x03, 0x07};

		int index = ByteHelper.indexOf(source, pattern, 0);

		Assertions.assertEquals(3, index);
	}

	@Test
	void byteArrayToHexString(){
		byte[] array = new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06};

		String hex = ByteHelper.byteArrayToHexString(array);

		Assertions.assertEquals("235E400351104206", hex);
	}

	@Test
	void hexStringToByteArray(){
		Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, ByteHelper.hexStringToByteArray("235e400351104206"));

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, ByteHelper.hexStringToByteArray("235e40035110420")));
		Assertions.assertEquals("Malformed input", exception.getMessage());
	}

	@Test
	void applyMaskAndShift(){
		byte value = ByteHelper.applyMaskAndShift((byte)0x1B, (byte)0x0E);

		Assertions.assertEquals(0b101, value);
	}

	@Test
	void compose(){
		byte highValue = 0x1B;
		byte highMask = 0x0F;
		byte lowValue = 0x03;
		byte lowMask = 0x0E;

		int value = ByteHelper.compose(highValue, highMask, lowValue, lowMask);

		Assertions.assertEquals(((highValue & highMask) << 3 | (lowValue & lowMask) >>> 1), value);
	}

	@Test
	void convert2ComplementWithPositiveNumber(){
		int value = ByteHelper.convert2Complement(0x6B, 8);

		Assertions.assertEquals(107, value);
	}

	@Test
	void convert2ComplementWithNegativeNumber(){
		int value = ByteHelper.convert2Complement(0xD7, 8);

		Assertions.assertEquals(-41, value);
	}

}
