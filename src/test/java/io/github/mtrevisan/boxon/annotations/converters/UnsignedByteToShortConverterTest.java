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
package io.github.mtrevisan.boxon.annotations.converters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
class UnsignedByteToShortConverterTest{

	@Test
	void valid(){
		UnsignedByteToShortConverter converter = new UnsignedByteToShortConverter();
		Assertions.assertEquals(Short.valueOf((short)0x0023), converter.decode(Byte.valueOf((byte)0x23)));
		Assertions.assertEquals(Byte.valueOf((byte)0x23), converter.encode(Short.valueOf((short)0x0023)));

		Assertions.assertEquals(Short.valueOf((short)0x00CA), converter.decode(Byte.valueOf((byte)0xCA)));
		Assertions.assertEquals(Byte.valueOf((byte)0xCA), converter.encode(Short.valueOf((short)0x00CA)));
	}

}