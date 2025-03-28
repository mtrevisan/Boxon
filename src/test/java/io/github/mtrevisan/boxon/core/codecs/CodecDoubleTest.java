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
import io.github.mtrevisan.boxon.annotations.converters.LongToDoubleConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;


class CodecDoubleTest{

	@Test
	void doublePositiveLittleEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		double encodedValue = TestHelper.RANDOM.nextDouble();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", LongToDoubleConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		String expected = StringHelper.toHexString(Long.reverseBytes(Double.doubleToLongBits(encodedValue)), Long.BYTES);
		Assertions.assertEquals(expected, writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		double decoded = (double)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void doubleNegativeLittleEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		double encodedValue = -TestHelper.RANDOM.nextDouble();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
			"byteOrder", ByteOrder.LITTLE_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", LongToDoubleConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		String expected = StringHelper.toHexString(Long.reverseBytes(Double.doubleToLongBits(encodedValue)), Long.BYTES);
		Assertions.assertEquals(expected, writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		double decoded = (double)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void doublePositiveBigEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		double encodedValue = TestHelper.RANDOM.nextDouble();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", LongToDoubleConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		String expected = StringHelper.toHexString(Double.doubleToLongBits(encodedValue), Long.BYTES);
		Assertions.assertEquals(expected, writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		double decoded = (double)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void doubleNegativeBigEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		double encodedValue = -TestHelper.RANDOM.nextDouble();
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "64",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", NullValidator.class.getName(),
			"converter", LongToDoubleConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		String expected = StringHelper.toHexString(Double.doubleToLongBits(encodedValue), Long.BYTES);
		Assertions.assertEquals(expected, writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		double decoded = (double)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
