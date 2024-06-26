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
package io.github.mtrevisan.boxon.io;

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.BitSet;


class BitWriterTest{

	private BitWriter writer;


	@BeforeEach
	void initialize(){
		writer = BitWriter.create();
	}

	@Test
	void bitSetBigEndian(){
		BitSet value = BitSet.valueOf(new long[]{0x1234_5678_1234_5678l, 0x6666_7777_8888_9999l});
		writer.writeBitSet(value, Long.SIZE << 1);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("66667777888899991234567812345678", reader.toString());
		Assertions.assertEquals(value, reader.readBitSet(Long.SIZE << 1));
	}

	@Test
	void bytePrimitive(){
		byte value = 0x16;
		writer.writeByte(value);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("16", reader.toString());
		Assertions.assertEquals(value, reader.readByte());
	}

	@Test
	void bytesPrimitive(){
		byte[] value = {(byte)0x16, (byte)0xFA};
		writer.writeBytes(value);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("16FA", reader.toString());
		Assertions.assertArrayEquals(value, reader.readBytes(value.length));
	}

	@Test
	void shortPrimitive(){
		short value = 2714;
		writer.writeShort(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("9A0A", reader.toString());
		Assertions.assertEquals(value, reader.readShort(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void shortPrimitiveBigEndian(){
		short value = 2714;
		writer.writeShort(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("0A9A", reader.toString());
		Assertions.assertEquals(value, reader.readShort(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void intPrimitive(){
		int value = 100_123;
		writer.writeInt(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("1B870100", reader.toString());
		Assertions.assertEquals(value, reader.readInt(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void intPrimitiveBigEndian(){
		int value = 100_123;
		writer.writeInt(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("0001871B", reader.toString());
		Assertions.assertEquals(value, reader.readInt(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void longPrimitive(){
		long value = 0x1234_5678_1234_4568l;
		writer.writeLong(value, ByteOrder.LITTLE_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("6845341278563412", reader.toString());
		Assertions.assertEquals(value, reader.readLong(ByteOrder.LITTLE_ENDIAN));
	}

	@Test
	void longPrimitiveBigEndian(){
		long value = 0x1234_5678_1234_4568l;
		writer.writeLong(value, ByteOrder.BIG_ENDIAN);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("1234567812344568", reader.toString());
		Assertions.assertEquals(value, reader.readLong(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void text(){
		String value = "test";
		writer.writeText(value);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("74657374", reader.toString());
		Assertions.assertEquals(value, reader.readText(4));
	}

	@Test
	void textWithTerminator(){
		String value = "test";
		writer.writeText(value);
		writer.writeByte((byte)'w');
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("7465737477", reader.toString());
		Assertions.assertEquals(value, reader.readTextUntilTerminator((byte)'w'));
		Assertions.assertEquals((byte)'w', reader.readByte());
	}

	@Test
	void textWithTerminatorConsumed(){
		String value = "test";
		writer.writeText(value);
		writer.writeByte((byte)'w');
		writer.writeByte((byte)'w');
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("746573747777", reader.toString());
		Assertions.assertEquals(value, reader.readTextUntilTerminator((byte)'w'));
		//consume terminator (`w`)
		reader.readByte();
		writer.writeByte((byte)'w');
	}

	@Test
	void skip(){
		writer.writeByte((byte)'w');
		writer.skipBits(3);
		BitReaderInterface reader = BitReader.wrap(writer);

		Assertions.assertEquals("7700", reader.toString());
	}

}
