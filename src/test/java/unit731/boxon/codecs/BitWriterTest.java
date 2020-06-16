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
package unit731.boxon.codecs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;


class BitWriterTest{

	private BitWriter writer;


	@BeforeEach
	void initialize(){
		writer = new BitWriter();
	}

	@Test
	void bits(){
		BitSet value = BitSet.valueOf(new long[]{0x1234_5678_1234_5678l, 0x6666_7777_8888_9999l});
		writer.putBits(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("78563412785634129999888877776666", reader.toString());
		Assertions.assertEquals(value, reader.getBits(value.length()));
	}

	@Test
	void booleanPrimitiveFromBit(){
		writer.putBooleanFromBit(true);
		writer.putBooleanFromBit(false);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("01", reader.toString());
		Assertions.assertTrue(reader.getBooleanFromBit());
		Assertions.assertFalse(reader.getBooleanFromBit());
	}

	@Test
	void booleanPrimitiveFromByte(){
		writer.putBooleanFromByte(true);
		writer.putBooleanFromByte(false);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("0100", reader.toString());
		Assertions.assertTrue(reader.getBooleanFromByte());
		Assertions.assertFalse(reader.getBooleanFromByte());
	}

	@Test
	void bytePrimitive(){
		byte value = (byte)0x16;
		writer.putByte(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("16", reader.toString());
		Assertions.assertEquals(value, reader.getByte());
	}

	@Test
	void bytesPrimitive(){
		byte[] value = new byte[]{(byte)0x16, (byte)0xFA};
		writer.putBytes(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("16FA", reader.toString());
		Assertions.assertArrayEquals(value, reader.getBytes(value.length));
	}

	@Test
	void charPrimitive(){
		char value = '\u2714';
		writer.putCharacter(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("1427", reader.toString());
		Assertions.assertEquals(value, reader.getCharacter());
	}

	@Test
	void charPrimitiveBigEndian(){
		char value = '\u2714';
		writer.putCharacter(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("2714", reader.toString());
		Assertions.assertEquals(value, reader.getCharacter(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void shortPrimitive(){
		short value = 2714;
		writer.putShort(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("9A0A", reader.toString());
		Assertions.assertEquals(value, reader.getShort());
	}

	@Test
	void shortPrimitiveBigEndian(){
		short value = 2714;
		writer.putShort(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("0A9A", reader.toString());
		Assertions.assertEquals(value, reader.getShort(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void intPrimitive(){
		int value = 100_123;
		writer.putInteger(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("1B870100", reader.toString());
		Assertions.assertEquals(value, reader.getInteger());
	}

	@Test
	void intPrimitiveBigEndian(){
		int value = 100_123;
		writer.putInteger(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("0001871B", reader.toString());
		Assertions.assertEquals(value, reader.getInteger(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void longPrimitive(){
		long value = 0x1234_5678_1234_4568l;
		writer.putLong(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("6845341278563412", reader.toString());
		Assertions.assertEquals(value, reader.getLong());
	}

	@Test
	void longPrimitiveBigEndian(){
		long value = 0x1234_5678_1234_4568l;
		writer.putLong(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("1234567812344568", reader.toString());
		Assertions.assertEquals(value, reader.getLong(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void floatPrimitive(){
		float value = 1.23f;
		writer.putFloat(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("A4709D3F", reader.toString());
		Assertions.assertEquals(value, reader.getFloat());
	}

	@Test
	void floatPrimitiveBigEndian(){
		float value = 1.23f;
		writer.putFloat(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("3F9D70A4", reader.toString());
		Assertions.assertEquals(value, reader.getFloat(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void doublePrimitive(){
		double value = 1.23;
		writer.putDouble(value);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("AE47E17A14AEF33F", reader.toString());
		Assertions.assertEquals(value, reader.getDouble());
	}

	@Test
	void doublePrimitiveBigEndian(){
		double value = 1.23;
		writer.putDouble(value, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("3FF3AE147AE147AE", reader.toString());
		Assertions.assertEquals(value, reader.getDouble(ByteOrder.BIG_ENDIAN));
	}

	@Test
	void bigDecimalAsFloat(){
		BigDecimal value = new BigDecimal("1.23");
		writer.putNumber(value, Float.class);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("A4709D3F", reader.toString());
		Assertions.assertEquals(value, reader.getNumber(Float.class));
	}

	@Test
	void bigDecimalAsFloatBigEndian(){
		BigDecimal value = new BigDecimal("1.23");
		writer.putNumber(value, Float.class, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("3F9D70A4", reader.toString());
		Assertions.assertEquals(value, reader.getNumber(Float.class, ByteOrder.BIG_ENDIAN));
	}

	@Test
	void bigDecimalAsFDouble(){
		BigDecimal value = new BigDecimal("1.23");
		writer.putNumber(value, Double.class);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("AE47E17A14AEF33F", reader.toString());
		Assertions.assertEquals(value, reader.getNumber(Double.class));
	}

	@Test
	void bigDecimalAsFDoubleBigEndian(){
		BigDecimal value = new BigDecimal("1.23");
		writer.putNumber(value, Double.class, ByteOrder.BIG_ENDIAN);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("3FF3AE147AE147AE", reader.toString());
		Assertions.assertEquals(value, reader.getNumber(Double.class, ByteOrder.BIG_ENDIAN));
	}

	@Test
	void text(){
		String value = "test";
		writer.putText(value, StandardCharsets.UTF_8);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("74657374", reader.toString());
		Assertions.assertEquals(value, reader.getText(4, StandardCharsets.UTF_8));
	}

	@Test
	void textWithTerminatorConsumeTerminator(){
		String value = "test";
		writer.putText(value, (byte)'w', true, StandardCharsets.UTF_8);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("7465737477", reader.toString());
		Assertions.assertEquals(value, reader.getTextUntilTerminator((byte)'w', true, StandardCharsets.UTF_8));
	}

	@Test
	void textWithTerminator1(){
		String value = "test";
		writer.putText(value, (byte)'w', true, StandardCharsets.UTF_8);
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("7465737477", reader.toString());
		Assertions.assertEquals(value, reader.getTextUntilTerminator((byte)'w', false, StandardCharsets.UTF_8));
		Assertions.assertEquals((byte)'w', reader.getByte());
	}

	@Test
	void textWithTerminator2(){
		String value = "test";
		writer.putText(value, (byte)'w', false, StandardCharsets.UTF_8);
		writer.putByte((byte)'w');
		BitBuffer reader = BitBuffer.wrap(writer);

		Assertions.assertEquals("7465737477", reader.toString());
		Assertions.assertEquals(value, reader.getTextUntilTerminator((byte)'w', false, StandardCharsets.UTF_8));
		Assertions.assertEquals((byte)'w', reader.getByte());
	}

}
