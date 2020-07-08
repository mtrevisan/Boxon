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
package unit731.boxon.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ByteHelperTest{

	@Test
	void hasBitWithByte(){
		byte mask = 0x27;

		byte output = 0x00;
		for(int i = 0; i < 8; i ++)
			if(ByteHelper.hasBit(mask, i)){
				output |= (1 << i);
			}

		Assertions.assertEquals(mask, output);
	}

	@Test
	void byteArrayToHexString(){
		byte[] array = new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06};

		String hex = ByteHelper.toHexString(array);

		Assertions.assertEquals("235E400351104206", hex);
	}

	@Test
	void hexStringToByteArray(){
		Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, ByteHelper.toByteArray("235e400351104206"));

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, ByteHelper.toByteArray("235e40035110420")));
		Assertions.assertEquals("Malformed input", exception.getMessage());
	}

	@Test
	void applyMaskAndShift(){
		byte value = (byte)ByteHelper.applyMaskAndShift(0x1B, 0x0E);

		Assertions.assertEquals(0b0101, value);


		value = (byte)ByteHelper.applyMaskAndShift(27, 0x18);

		Assertions.assertEquals(0b0011, value);


		value = (byte)ByteHelper.applyMaskAndShift(15, 0xFF);

		Assertions.assertEquals(15, value);
	}

	@Test
	void convert2ComplementWithPositiveNumber(){
		int value = ByteHelper.extendSign(0x6B, 8);

		Assertions.assertEquals(107, value);
	}

	@Test
	void convert2ComplementWithNegativeNumber(){
		int value = ByteHelper.extendSign(0xD7, 8);

		Assertions.assertEquals(-41, value);
	}

	@Test
	void reverseBits(){
		BitMap bits = BitMap.valueOf(new byte[]{0x10});
		ByteHelper.reverseBits(bits, Byte.SIZE);

		Assertions.assertEquals(BitMap.valueOf(new byte[]{0x08}), bits);


		bits = BitMap.valueOf(new byte[]{0x16});
		ByteHelper.reverseBits(bits, Byte.SIZE);

		Assertions.assertEquals(BitMap.valueOf(new byte[]{0x68}), bits);


		bits = BitMap.valueOf(new byte[]{(byte)0xE7});
		ByteHelper.reverseBits(bits, Byte.SIZE);

		Assertions.assertEquals(BitMap.valueOf(new byte[]{(byte)0xE7}), bits);
	}

}
