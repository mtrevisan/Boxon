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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ConverterTest{

	@MessageHeader(start = "wc1")
	private static class TestConverter1{
		@BindString(size = "3")
		String header;
		@BindByte(converter = WrongConverterInput.class)
		String value;
	}

	static class WrongConverterInput implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			return value[0] + "." + value[1];
		}

		@Override
		public byte[] encode(final String value){
			final String[] components = StringHelper.split(value, '.');
			return new byte[]{Byte.parseByte(components[0]), Byte.parseByte(components[1])};
		}
	}

	@MessageHeader(start = "wc2")
	private static class TestConverter2{
		@BindString(size = "3")
		String header;
		@BindByte(converter = WrongConverterOutput.class)
		String value;
	}

	@MessageHeader(start = "wc3")
	private static class TestConverter3{
		@BindString(size = "3")
		String header;
		@BindByte(converter = WrongConverterOutput.class)
		int value;
	}

	static class WrongConverterOutput implements Converter<Byte, Byte>{
		@Override
		public Byte decode(final Byte value){
			return value;
		}

		@Override
		public Byte encode(final Byte value){
			return value;
		}
	}

	@Test
	void wrongInputOnConverter() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestConverter1.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("77633101");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(2, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertArrayEquals(payload, response.getSource());
		Assertions.assertTrue(response.hasError());
		Assertions.assertEquals("java.lang.IllegalArgumentException: Can not input Byte (1) to decode method of converter WrongConverterInput in field io.github.mtrevisan.boxon.core" +
			".ConverterTest$TestConverter1.value" + System.lineSeparator() + "   at index 4", response.getError().getMessage());
	}

	@Test
	void wrongOutputFromConverter() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestConverter2.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("77633201");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(2, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertArrayEquals(payload, response.getSource());
		Assertions.assertTrue(response.hasError());
		Assertions.assertEquals("java.lang.IllegalArgumentException: Can not set String field to Byte in field io.github.mtrevisan.boxon.core.ConverterTest$TestConverter2.value"
			+ System.lineSeparator() + "   at index 4", response.getError().getMessage());
	}

	@Test
	void allowedOutputFromConverter() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestConverter3.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("77633301");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertArrayEquals(payload, response.getSource());
		Assertions.assertFalse(response.hasError());
	}

}
