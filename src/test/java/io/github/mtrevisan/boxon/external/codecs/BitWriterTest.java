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
package io.github.mtrevisan.boxon.external.codecs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
class BitWriterTest{

	private BitWriter writer;


	@BeforeEach
	void initialize(){
		writer = BitWriter.create();
	}

	@Test
	void bits(){
		BoxonBitSet value = BoxonBitSet.valueOf(new long[]{0x1234_5678_1234_5678l, 0x6666_7777_8888_9999l});
		writer.putBits(value, Long.SIZE << 1);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("78563412785634129999888877776666", reader.toString());
		Assertions.assertEquals(value, reader.getBits(Long.SIZE << 1));
	}

	@Test
	void bytePrimitive(){
		byte value = 0x16;
		writer.putByte(value);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("16", reader.toString());
		Assertions.assertEquals(value, reader.getByte());
	}

	@Test
	void bytesPrimitive(){
		byte[] value = {(byte)0x16, (byte)0xFA};
		writer.putBytes(value);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("16FA", reader.toString());
		Assertions.assertArrayEquals(value, reader.getBytes(value.length));
	}

	@Test
	void shortPrimitive(){
		short value = 2714;
		writer.putShort(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("9A0A", reader.toString());
		Assertions.assertEquals(value, reader.getShort(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void shortPrimitiveBigEndian(){
		short value = 2714;
		writer.putShort(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("0A9A", reader.toString());
		Assertions.assertEquals(value, reader.getShort(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void intPrimitive(){
		int value = 100_123;
		writer.putInt(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("1B870100", reader.toString());
		Assertions.assertEquals(value, reader.getInt(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void intPrimitiveBigEndian(){
		int value = 100_123;
		writer.putInt(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("0001871B", reader.toString());
		Assertions.assertEquals(value, reader.getInt(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void longPrimitive(){
		long value = 0x1234_5678_1234_4568l;
		writer.putLong(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("6845341278563412", reader.toString());
		Assertions.assertEquals(value, reader.getLong(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void longPrimitiveBigEndian(){
		long value = 0x1234_5678_1234_4568l;
		writer.putLong(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("1234567812344568", reader.toString());
		Assertions.assertEquals(value, reader.getLong(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void floatPrimitive(){
		float value = 1.23f;
		writer.putFloat(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("A4709D3F", reader.toString());
		Assertions.assertEquals(value, reader.getFloat(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void floatPrimitiveBigEndian(){
		float value = 1.23f;
		writer.putFloat(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("3F9D70A4", reader.toString());
		Assertions.assertEquals(value, reader.getFloat(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void doublePrimitive(){
		double value = 1.23;
		writer.putDouble(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("AE47E17A14AEF33F", reader.toString());
		Assertions.assertEquals(value, reader.getDouble(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void doublePrimitiveBigEndian(){
		double value = 1.23;
		writer.putDouble(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("3FF3AE147AE147AE", reader.toString());
		Assertions.assertEquals(value, reader.getDouble(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void text(){
		String value = "test";
		writer.putText(value
		);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("74657374", reader.toString());
		Assertions.assertEquals(value, reader.getText(4));
	}

	@Test
	void textWithTerminator(){
		String value = "test";
		writer.putText(value);
		writer.putByte((byte)'w');
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("7465737477", reader.toString());
		Assertions.assertEquals(value, reader.getTextUntilTerminator((byte)'w'));
		Assertions.assertEquals((byte)'w', reader.getByte());
	}

	@Test
	void textWithTerminatorConsumed(){
		String value = "test";
		writer.putText(value);
		writer.putByte((byte)'w');
		writer.putByte((byte)'w');
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("746573747777", reader.toString());
		Assertions.assertEquals(value, reader.getTextUntilTerminator((byte)'w'));
		//consume terminator (`w`)
		reader.getByte();
		writer.putByte((byte)'w');
	}

}
