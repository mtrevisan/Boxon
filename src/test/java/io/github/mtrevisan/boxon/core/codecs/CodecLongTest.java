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
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;


class CodecLongTest{

	@Test
	void longLittleEndianNegative() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x8FFF_0000_FFFF_0000l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000FFFF0000FF8F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianSmall() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x0000_0000_7F00_FF00l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00FF007F00000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianPositive() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x7F00_FF00_0000_0000l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000000000FF007F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianRandom() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = TestHelper.RANDOM.nextLong();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		String expected = StringHelper.toHexString(Long.reverseBytes(encodedValue), Long.BYTES);
		Assertions.assertEquals(expected, writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianNegative() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x8FFF_0000_0000_0011l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("8FFF000000000011", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianSmall() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x0000_0000_7F00_FF00l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("000000007F00FF00", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianPositive() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = 0x7F00_FF00_0000_0000l;
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00FF0000000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianRandom() throws BoxonException{
		Codec codec = new CodecDefault();
		long encodedValue = TestHelper.RANDOM.nextLong();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
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
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringHelper.toHexString(encodedValue, Long.BYTES), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		long decoded = ((Number)codec.decode(reader, annotation, null, null))
			.longValue();

		Assertions.assertEquals(encodedValue, decoded);
	}

}
