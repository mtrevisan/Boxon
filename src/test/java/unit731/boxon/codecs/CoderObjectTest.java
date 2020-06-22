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
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.codecs.queclink.Version;
import unit731.boxon.utils.ByteHelper;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;


class CoderObjectTest{

	@Test
	void object(){
		Coder coder = Coder.OBJECT;
		Version encodedValue = new Version((byte)1, (byte)2, (byte)0);
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


	static class TestType1{
		@BindShort
		public short value;
	}

	static class TestType2{
		@BindInteger
		public int value;
	}

	@MessageHeader(start = "tc1")
	static class TestChoice1{
		@BindString(size = "3")
		public String header;
		@BindObject(selectFrom = @Choices(prefixSize = 8, alternatives = {
			@Choices.Choice(condition = "#prefix == 1", type = TestType1.class),
			@Choices.Choice(condition = "#prefix == 2", type = TestType2.class)
		}))
		public Object value;
	}

	@MessageHeader(start = "tc2")
	static class TestChoice2{
		@BindString(size = "3")
		public String header;
		@BindArrayPrimitive(size = "2", type = byte[].class)
		public byte[] index;
		@BindObject(selectFrom = @Choices(prefixSize = 8, alternatives = {
			@Choices.Choice(condition = "index[#prefix] == 5", type = TestType1.class),
			@Choices.Choice(condition = "index[#prefix] == 6", type = TestType2.class)
		}))
		public Object value;
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
		public Object value;
	}

	@Test
	void choice1(){
		Codec<TestChoice1> codec = Codec.createFrom(TestChoice1.class);
		Parser parser = new Parser(null, Collections.singletonList(codec));

		byte[] payload = ByteHelper.hexStringToByteArray("746331011234");
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


		payload = ByteHelper.hexStringToByteArray("7463310211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice1.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice1)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);
	}

	@Test
	void choice2(){
		Codec<TestChoice2> codec = Codec.createFrom(TestChoice2.class);
		Parser parser = new Parser(null, Collections.singletonList(codec));

		byte[] payload = ByteHelper.hexStringToByteArray("7463320506001234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice2.class, parsedMessages.get(0).getClass());
		TestChoice2 parsedMessage = (TestChoice2)parsedMessages.get(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);


		payload = ByteHelper.hexStringToByteArray("74633205060111223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice2.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice2)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);
	}

	@Test
	void choice3(){
		Codec<TestChoice3> codec = Codec.createFrom(TestChoice3.class);
		Parser parser = new Parser(null, Collections.singletonList(codec));

		byte[] payload = ByteHelper.hexStringToByteArray("74633361611234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice3.class, parsedMessages.get(0).getClass());
		TestChoice3 parsedMessage = (TestChoice3)parsedMessages.get(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);


		payload = ByteHelper.hexStringToByteArray("746333626211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice3.class, parsedMessages.get(0).getClass());
		parsedMessage = (TestChoice3)parsedMessages.get(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);
	}

}
