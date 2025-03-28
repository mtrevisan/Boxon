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

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;


class CodecDefaultTest{

	@Test
	void bitsLittleEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		//byte[] randomBytes = new byte[]{(byte)0xAB, (byte)0xCD};
		byte[] randomBytes = new byte[123];
		TestHelper.RANDOM.nextBytes(randomBytes);
		//prevent adding of zeroes while changing endianness
		randomBytes[0] = (byte)0xFF;
		//prevent adding of zeroes while changing endianness
		randomBytes[randomBytes.length - 1] = (byte)0xFF;
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindBitSet.class.getName(),
			"size", Integer.toString(randomBytes.length << 3),
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindBitSet annotation = AnnotationCreator.createAnnotation(BindBitSet.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		//NOTE: this is because, sometimes, the byte array ends with zero (and also because `BitSet` is little endian), and that's a problem
		// with `toByteArray`
		if(bb.length != randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(TestHelper.reverseBytes(bb, bb.length)), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bitsBigEndian() throws BoxonException{
		Codec codec = new CodecDefault();
		byte[] randomBytes = new byte[123];
		TestHelper.RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(TestHelper.reverseBytes(randomBytes, randomBytes.length));
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindBitSet.class.getName(),
			"size", Integer.toString(randomBytes.length << 3),
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindBitSet annotation = AnnotationCreator.createAnnotation(BindBitSet.class, annotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		//NOTE: this is because, sometimes, the byte array ends with zero (and also because `BitSet` is little endian), and that's a problem
		// with `toByteArray`
		if(bb.length != randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(TestHelper.reverseBytes(bb, randomBytes.length)), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
