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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;


class CodecIntegerTest{

	@Test
	void smallLittleEndianSmall1() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x00_1020l);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("201000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianSmall2() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x10_2000l);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("002010", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x7F_00FFl);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("FF007F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void smallBigEndianSmall1() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x00_1020l);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("001020", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianSmall2() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x10_2000l);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("102000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = BigInteger.valueOf(0x7F_00FFl);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "24",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00FF", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigLittleEndianSmall() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "128",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		BitSet bitmap = BitSetHelper.createBitSet(128, encodedValue, ByteOrder.LITTLE_ENDIAN);
		Assertions.assertEquals(rightPad(StringHelper.toHexString(TestHelper.reverseBytes(bitmap.toByteArray(), 128 / Byte.SIZE)),
			32, '0'), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	/**
	 * Right pad a string with a specified character.
	 *
	 * @param text	The string to pad out, must not be {@code null}.
	 * @param size	The size to pad to.
	 * @param padChar	The character to pad with.
	 * @return	Right padded string or original string if no padding is necessary.
	 */
	private static String rightPad(final String text, final int size, final char padChar){
		final int pads = size - text.length();
		if(pads <= 0)
			return text;

		return new StringBuilder(text)
			.repeat(padChar, pads)
			.toString();
	}

	@Test
	void bigLittleEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "128",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00000000000000000000FFFF0000FF7F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigBigEndianSmall() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "128",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00FF0000FFFF00000000000000000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "128",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7FFF0000FFFF00000000000000000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
