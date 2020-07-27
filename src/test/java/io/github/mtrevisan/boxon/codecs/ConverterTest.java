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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.boxon.annotations.BindByte;
import io.github.mtrevisan.boxon.annotations.BindString;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.codecs.exceptions.ParseException;
import io.github.mtrevisan.boxon.codecs.dtos.ParseResponse;
import io.github.mtrevisan.boxon.helpers.ByteHelper;

import java.util.List;


class ConverterTest{

	@MessageHeader(start = "wc1")
	static class TestConverter1{
		@BindString(size = "3")
		public String header;
		@BindByte(converter = WrongConverterInput.class)
		public String value;
	}

	public static class WrongConverterInput implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			return value[0] + "." + value[1];
		}

		@Override
		public byte[] encode(final String value){
			final String[] components = StringUtils.split(value, '.');
			return new byte[]{Byte.parseByte(components[0]), Byte.parseByte(components[1])};
		}
	}

	@MessageHeader(start = "wc2")
	static class TestConverter2{
		@BindString(size = "3")
		public String header;
		@BindByte(converter = WrongConverterOutput.class)
		public String value;
	}

	@MessageHeader(start = "wc3")
	static class TestConverter3{
		@BindString(size = "3")
		public String header;
		@BindByte(converter = WrongConverterOutput.class)
		public int value;
	}

	public static class WrongConverterOutput implements Converter<Byte, Byte>{
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
	void wrongInputOnConverter(){
		Loader loader = new Loader();
		loader.loadCodecs();
		ProtocolMessage<TestConverter1> protocolMessage = ProtocolMessage.createFrom(TestConverter1.class, loader);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("77633101");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertTrue(result.hasErrors());
		Assertions.assertEquals(1, result.getPayloads().size());
		Assertions.assertEquals(1, result.getErrorIndexes().size());
		Assertions.assertEquals(0, result.getErrorIndexes().get(0));
		Assertions.assertArrayEquals(payload, result.getPayloads().get(0));
		List<ParseException> errors = result.getErrors();
		Assertions.assertEquals(1, errors.size());
		ParseException error = errors.get(0);
		Assertions.assertEquals("Error decoding message: \r\n"
			+ "class java.lang.Byte cannot be cast to class [B (java.lang.Byte and [B are in module java.base of loader 'bootstrap'), field TestConverter1.value\r\n"
			+ "   at index 4", error.getMessage());
	}

	@Test
	void wrongOutputFromConverter(){
		Loader loader = new Loader();
		loader.loadCodecs();
		ProtocolMessage<TestConverter2> protocolMessage = ProtocolMessage.createFrom(TestConverter2.class, loader);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("77633201");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertTrue(result.hasErrors());
		Assertions.assertEquals(1, result.getPayloads().size());
		Assertions.assertEquals(1, result.getErrorIndexes().size());
		Assertions.assertEquals(0, result.getErrorIndexes().get(0));
		Assertions.assertArrayEquals(payload, result.getPayloads().get(0));
		List<ParseException> errors = result.getErrors();
		Assertions.assertEquals(1, errors.size());
		ParseException error = errors.get(0);
		Assertions.assertEquals("Error decoding message: \r\n"
			+ "Can not set java.lang.String field io.github.mtrevisan.boxon.codecs.ConverterTest$TestConverter2.value to java.lang.Byte, field TestConverter2.value\r\n"
			+ "   at index 4", error.getMessage());
	}

	@Test
	void allowedOutputFromConverter(){
		Loader loader = new Loader();
		loader.loadCodecs();
		ProtocolMessage<TestConverter3> protocolMessage = ProtocolMessage.createFrom(TestConverter3.class, loader);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("77633301");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
	}

}
