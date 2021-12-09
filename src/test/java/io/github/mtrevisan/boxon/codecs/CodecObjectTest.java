/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.codecs.managers.ReflectionHelper;
import io.github.mtrevisan.boxon.core.ComposeResponse;
import io.github.mtrevisan.boxon.core.ParseResponse;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;


@SuppressWarnings("ALL")
class CodecObjectTest{

	private static class Version{
		@BindByte
		private final byte major;
		@BindByte
		private final byte minor;

		private Version(final byte major, final byte minor){
			this.major = major;
			this.minor = minor;
		}

	}


	@Test
	void object() throws FieldException{
		CodecObject codec = new CodecObject();
		Version encodedValue = new Version((byte)1, (byte)2);
		BindObject annotation = new BindObject(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindObject.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return Version.class;
			}

			@Override
			public ObjectChoices selectFrom(){
				return new ObjectChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return null;
					}

					@Override
					public int prefixSize(){
						return 0;
					}

					@Override
					public ByteOrder byteOrder(){
						return null;
					}

					@Override
					public ObjectChoice[] alternatives(){
						return new ObjectChoice[0];
					}
				};
			}

			@Override
			public Class<?> selectDefault(){
				return void.class;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = LoaderCodec.create(eventListener);
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec, eventListener);
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec);
		ReflectionHelper.setFieldValue(codec, TemplateParserInterface.class, templateParser);
		BitWriter writer = BitWriter.create();
		Evaluator evaluator = Evaluator.create();
		codec.encode(writer, annotation, null, encodedValue, evaluator);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x01, 0x02}, writer.array());

		BitReader reader = BitReader.wrap(writer);
		Version decoded = (Version)codec.decode(reader, annotation, null, evaluator);

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
		@BindObject(type = TestType0.class, selectFrom = @ObjectChoices(prefixSize = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = 1, type = TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = 2, type = TestType2.class)
			}))
		public TestType0 value;
	}

	@MessageHeader(start = "tc2")
	static class TestChoice2{
		@BindString(size = "3")
		public String header;
		@BindArrayPrimitive(size = "2", type = byte.class)
		public byte[] index;
		@BindObject(selectFrom = @ObjectChoices(prefixSize = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "index[#prefix] == 5", prefix = 0, type = TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "index[#prefix] == 6", prefix = 1, type = TestType2.class)
			}))
		public TestType0 value;
	}

	@MessageHeader(start = "tc3")
	static class TestChoice3{
		@BindString(size = "3")
		public String header;
		@BindString(size = "2")
		public String key;
		@BindObject(selectFrom = @ObjectChoices(alternatives = {
			@ObjectChoices.ObjectChoice(condition = "key == 'aa'", type = TestType1.class),
			@ObjectChoices.ObjectChoice(condition = "key == 'bb'", type = TestType2.class)
		}))
		public TestType0 value;
	}

	@Test
	void choice1() throws AnnotationException, TemplateException{
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withTemplates(TestChoice1.class);

		byte[] payload = StringHelper.toByteArray("746331011234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice1.class, result.getParsedMessageAt(0).getClass());
		TestChoice1 parsedMessage = (TestChoice1)result.getParsedMessageAt(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = StringHelper.toByteArray("7463310211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice1.class, result.getParsedMessageAt(0).getClass());
		parsedMessage = (TestChoice1)result.getParsedMessageAt(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

	@Test
	void choice2() throws AnnotationException, TemplateException{
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withTemplates(TestChoice2.class);

		byte[] payload = StringHelper.toByteArray("7463320506001234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice2.class, result.getParsedMessageAt(0).getClass());
		TestChoice2 parsedMessage = (TestChoice2)result.getParsedMessageAt(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = StringHelper.toByteArray("74633205060111223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice2.class, result.getParsedMessageAt(0).getClass());
		parsedMessage = (TestChoice2)result.getParsedMessageAt(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

	@Test
	void choice3() throws AnnotationException, TemplateException{
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withTemplates(TestChoice3.class);

		byte[] payload = StringHelper.toByteArray("74633361611234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice3.class, result.getParsedMessageAt(0).getClass());
		TestChoice3 parsedMessage = (TestChoice3)result.getParsedMessageAt(0);
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);

		ComposeResponse response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());


		payload = StringHelper.toByteArray("746333626211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice3.class, result.getParsedMessageAt(0).getClass());
		parsedMessage = (TestChoice3)result.getParsedMessageAt(0);
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);

		response = parser.composeMessage(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

}
