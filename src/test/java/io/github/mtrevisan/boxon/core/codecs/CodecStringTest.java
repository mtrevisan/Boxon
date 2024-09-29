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

import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.core.Generator;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;


class CodecStringTest{

	@Test
	void stringUS_ASCII() throws BoxonException{
		Codec codec = new CodecDefault();
		String encodedValue = "123ABC";
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindString.class.getName(),
			"charset", StandardCharsets.US_ASCII.name(),
			"size", Integer.toString(TestHelper.toByteArray(encodedValue).length),
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindString annotation = Generator.createAnnotation(BindString.class, annotationData);

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));

		BitReaderInterface reader = BitReader.wrap(writer);
		String decoded = (String)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringUTF_8() throws BoxonException{
		Codec codec = new CodecDefault();
		String encodedValue = "123ABCíïóúüđɉƚñŧ";
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindString.class.getName(),
			"charset", StandardCharsets.UTF_8.name(),
			"size", Integer.toString(toByteArray(encodedValue).length),
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindString annotation = Generator.createAnnotation(BindString.class, annotationData);

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.UTF_8));

		BitReaderInterface reader = BitReader.wrap(writer);
		String decoded = (String)codec.decode(reader, annotation, null, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	private static byte[] toByteArray(final String payload){
		return payload.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	void stringTerminated() throws BoxonException{
		Codec codec = new CodecDefault();
		String encodedValue = "123ABC";
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindStringTerminated.class.getName(),
			"charset", StandardCharsets.US_ASCII.name(),
			"terminator", (byte)'C',
			"consumeTerminator", false,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindStringTerminated annotation = Generator.createAnnotation(BindStringTerminated.class, annotationData);

		BitReaderInterface reader = BitReader.wrap(TestHelper.toByteArray(encodedValue));
		Object decoded = codec.decode(reader, annotation, null, null);

		Assertions.assertEquals("123AB", decoded);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, decoded);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{49, 50, 51, 65, 66}, writer.array());
	}

	@Test
	void stringTerminatedButEndOfStream() throws BoxonException{
		Codec codec = new CodecDefault();
		String encodedValue = "123ABC";
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindStringTerminated.class.getName(),
			"charset", StandardCharsets.US_ASCII.name(),
			"terminator", (byte)'D',
			"consumeTerminator", false,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindStringTerminated annotation = Generator.createAnnotation(BindStringTerminated.class, annotationData);

		BitReaderInterface reader = BitReader.wrap(TestHelper.toByteArray(encodedValue));
		Object decoded = codec.decode(reader, annotation, null, null);

		Assertions.assertEquals("123ABC", decoded);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, decoded);
		writer.flush();

		//this seems strange, but it has to work like this
		Assertions.assertArrayEquals(new byte[]{49, 50, 51, 65, 66, 67}, writer.array());
	}

}
