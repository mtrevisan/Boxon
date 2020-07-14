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
package unit731.boxon.coders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindInt;
import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.coders.dtos.ComposeResponse;
import unit731.boxon.coders.dtos.ParseResponse;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.util.List;


class CoderObjectTest{

	private static class Version{
		@BindByte
		private byte major;
		@BindByte
		private byte minor;

		private Version(final byte major, final byte minor){
			this.major = major;
			this.minor = minor;
		}
	}


	@Test
	void object() throws NoSuchFieldException{
		CoderObject coder = new CoderObject();
		Version encodedValue = new Version((byte)1, (byte)2);
		BindObject annotation = new BindObject(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindObject.class;
			}

			@Override
			public Class<?> type(){
				return Version.class;
			}

			@Override
			public Choices selectFrom(){
				return null;
			}

			@Override
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		messageParser.loader.loadCoders();
		ReflectionHelper.setFieldValue(coder, "messageParser", messageParser);
		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x01, 0x02}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);
		Version decoded = (Version)coder.decode(reader, annotation, null);

		Assertions.assertNotNull(decoded);
		Assertions.assertEquals(encodedValue.major, decoded.major);
		Assertions.assertEquals(encodedValue.minor, decoded.minor);
	}


	static class TestType0{}

	static class TestType1 extends TestType0{
		@BindShort
		public short value;
	}

	static class TestType2 extends TestType0{
		@BindInt
		public int value;
	}

	@MessageHeader(start = "tc1")
	static class TestChoice1{
		@BindString(size = "3")
		public String header;
		@BindObject(selectFrom = @Choices(prefixSize = 8, alternatives = {
			@Choices.Choice(condition = "#prefix == 1", prefix = 1, type = TestType1.class),
			@Choices.Choice(condition = "#prefix == 2", prefix = 2, type = TestType2.class)
		}))
		public TestType0 value;
	}

	@MessageHeader(start = "tc2")
	static class TestChoice2{
		@BindString(size = "3")
		public String header;
		@BindArrayPrimitive(size = "2", type = byte[].class)
		public byte[] index;
		@BindObject(selectFrom = @Choices(prefixSize = 8, alternatives = {
			@Choices.Choice(condition = "index[#prefix] == 5", prefix = 0, type = TestType1.class),
			@Choices.Choice(condition = "index[#prefix] == 6", prefix = 1, type = TestType2.class)
		}))
		public TestType0 value;
	}

	@MessageHeader(start = "tc3")
	static class TestChoice3{
		@BindString(size = "3")
		public String header;
		@BindString(size = "2")
		public String key;
		@BindObject(selectFrom = @Choices(prefixSize = 0, alternatives = {
			@Choices.Choice(condition = "key == 'aa'", type = TestType1.class),
			@Choices.Choice(condition = "key == 'bb'", type = TestType2.class)
		}))
		public TestType0 value;
	}

	@Test
	void choice1(){
		Loader loader = new Loader();
		loader.loadCoders();
		ProtocolMessage<TestChoice1> protocolMessage = ProtocolMessage.createFrom(TestChoice1.class, loader);
		Parser parser = Parser.create();
		parser.withDefaultCoders();
		parser.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("746331011234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice1.class, parsedMessages.get(0).getClass());
		TestChoice1 parsedMessage = (TestChoice1)parsedMessages.get(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = ByteHelper.toByteArray("7463310211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice1.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice1)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

	@Test
	void choice2(){
		Loader loader = new Loader();
		loader.loadCoders();
		ProtocolMessage<TestChoice2> protocolMessage = ProtocolMessage.createFrom(TestChoice2.class, loader);
		Parser parser = Parser.create();
		parser.withDefaultCoders();
		parser.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("7463320506001234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice2.class, parsedMessages.get(0).getClass());
		TestChoice2 parsedMessage = (TestChoice2)parsedMessages.get(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = ByteHelper.toByteArray("74633205060111223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice2.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice2)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

	@Test
	void choice3(){
		Loader loader = new Loader();
		loader.loadCoders();
		ProtocolMessage<TestChoice3> protocolMessage = ProtocolMessage.createFrom(TestChoice3.class, loader);
		Parser parser = Parser.create();
		parser.withDefaultCoders();
		parser.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("74633361611234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice3.class, parsedMessages.get(0).getClass());
		TestChoice3 parsedMessage = (TestChoice3)parsedMessages.get(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = ByteHelper.toByteArray("746333626211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice3.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice3)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

}
