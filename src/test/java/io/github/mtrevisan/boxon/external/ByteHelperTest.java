/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external;

import io.github.mtrevisan.boxon.internal.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
public class ByteHelperTest{

	@Test
	void hasBitWithByte(){
		byte mask = 0x27;

		byte output = 0x00;
		for(int i = 0; i < 8; i ++)
			if(hasBit(mask, i))
				output |= (1 << i);

		Assertions.assertEquals(mask, output);
	}

	/**
	 * Checks whether the given `mask` has the bit at `index` set.
	 *
	 * @param mask	The value to check the bit into.
	 * @param index	The index of the bit (rightmost is zero). The value can range between {@code 0} and {@link Byte#SIZE}.
	 * @return	The state of the bit at a given index in the given byte.
	 */
	private static boolean hasBit(final byte mask, final int index){
		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask & bitMask) != 0);
	}

	@Test
	void byteArrayToHexString(){
		byte[] array = {0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06};

		String hex = StringHelper.toHexString(array);

		Assertions.assertEquals("235E400351104206", hex);
	}

	@Test
	void hexStringToByteArray(){
		Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, StringHelper.toByteArray("235e400351104206"));

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> Assertions.assertArrayEquals(new byte[]{0x23, 0x5e, 0x40, 0x03, 0x51, 0x10, 0x42, 0x06}, StringHelper.toByteArray("235e40035110420")));
		Assertions.assertEquals("Input should be of even length, was 15", exception.getMessage());
	}

	@Test
	void convert2ComplementWithPositiveNumber(){
		long value = extendSign(0x6B, 8);

		Assertions.assertEquals(107l, value);
	}

	@Test
	void convert2ComplementWithNegativeNumber(){
		long value = extendSign(0xD7, 8);

		Assertions.assertEquals(-41l, value);
	}

	/**
	 * Convert the value to signed primitive.
	 *
	 * @param value	Field value.
	 * @param size	Length in bits of the field.
	 * @return	The 2-complement expressed as int.
	 */
	@SuppressWarnings("ShiftOutOfRange")
	private static long extendSign(final long value, final int size){
		if(size <= 0)
			throw new IllegalArgumentException("Size must be a positive value, was " + size);

		final int shift = -size;
		return (value << shift) >> shift;
	}

	@Test
	void reverseBits(){
		BitSet bits = BitSet.valueOf(new byte[]{0x10});
		bits.reverseBits(Byte.SIZE);

		Assertions.assertEquals(BitSet.valueOf(new byte[]{0x08}), bits);


		bits = BitSet.valueOf(new byte[]{0x16});
		bits.reverseBits(Byte.SIZE);

		Assertions.assertEquals(BitSet.valueOf(new byte[]{0x68}), bits);


		bits = BitSet.valueOf(new byte[]{(byte)0xE7});
		bits.reverseBits(Byte.SIZE);

		Assertions.assertEquals(BitSet.valueOf(new byte[]{(byte)0xE7}), bits);
	}

}
