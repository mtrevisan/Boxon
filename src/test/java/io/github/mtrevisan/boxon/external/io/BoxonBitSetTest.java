/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
public class BoxonBitSetTest{

	@Test
	void reverseBits(){
		BoxonBitSet bits = BoxonBitSet.valueOf(new byte[]{0x10});
		bits.flipBits(Byte.SIZE);

		Assertions.assertEquals(BoxonBitSet.valueOf(new byte[]{0x08}), bits);


		bits = BoxonBitSet.valueOf(new byte[]{0x16});
		bits.flipBits(Byte.SIZE);

		Assertions.assertEquals(BoxonBitSet.valueOf(new byte[]{0x68}), bits);


		bits = BoxonBitSet.valueOf(new byte[]{(byte)0xE7});
		bits.flipBits(Byte.SIZE);

		Assertions.assertEquals(BoxonBitSet.valueOf(new byte[]{(byte)0xE7}), bits);
	}

}
