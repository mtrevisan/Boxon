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

import io.github.mtrevisan.boxon.annotations.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.boxon.annotations.BindArray;
import io.github.mtrevisan.boxon.annotations.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.BindByte;
import io.github.mtrevisan.boxon.annotations.BindString;
import io.github.mtrevisan.boxon.annotations.ByteOrder;
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.codecs.dtos.ComposeResponse;
import io.github.mtrevisan.boxon.codecs.dtos.ParseResponse;
import io.github.mtrevisan.boxon.helpers.ByteHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.util.List;


class CodecArrayTest{

	private class Version{
		@BindByte
		private byte major;
		@BindByte
		private byte minor;
		@BindByte
		private byte build;

		private Version(final byte major, final byte minor, final byte build){
			this.major = major;
			this.minor = minor;
			this.build = build;
		}
	}

	@MessageHeader(start = "tc4")
	static class TestChoice4{
		@BindString(size = "3")
		public String header;
		@BindArray(size = "3", type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(prefixSize = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = 1, type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = 2, type = CodecObjectTest.TestType2.class)
			}))
		public CodecObjectTest.TestType0[] value;
	}


	@Test
	void arrayPrimitive(){
		CodecInterface codec = new CodecArrayPrimitive();
		int[] encodedValue = new int[]{0x0000_0123, 0x0000_0456};
		BindArrayPrimitive annotation = new BindArrayPrimitive(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArrayPrimitive.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return int.class;
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length);
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return null;
			}
		};

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitReader reader = BitReader.wrap(writer);
		Object decoded = codec.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void arrayOfSameObject() throws NoSuchFieldException{
		CodecArray codec = new CodecArray();
		Version[] encodedValue = new Version[]{new Version((byte)0, (byte)1, (byte)12), new Version((byte)1, (byte)2, (byte)0)};
		BindArray annotation = new BindArray(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArray.class;
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
			public String size(){
				return Integer.toString(encodedValue.length);
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
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return null;
			}
		};

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ReflectionHelper.setFieldValue(codec, "protocolMessageParser", protocolMessageParser);
		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x01, 0x0C, 0x01, 0x02, 0x00}, writer.array());

		BitReader reader = BitReader.wrap(writer);
		Version[] decoded = (Version[])codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.length);
		Assertions.assertEquals(encodedValue[0].major, decoded[0].major);
		Assertions.assertEquals(encodedValue[0].minor, decoded[0].minor);
		Assertions.assertEquals(encodedValue[1].major, decoded[1].major);
		Assertions.assertEquals(encodedValue[1].minor, decoded[1].minor);
	}

	@Test
	void arrayOfDifferentObjects(){
		Loader loader = new Loader();
		loader.loadCodecs();
		ProtocolMessage<TestChoice4> protocolMessage = ProtocolMessage.createFrom(TestChoice4.class, loader);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withProtocolMessages(protocolMessage);

		byte[] payload = ByteHelper.toByteArray("7463340112340211223344010666");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice4.class, parsedMessages.get(0).getClass());
		TestChoice4 parsedMessage = (TestChoice4)parsedMessages.get(0);
		CodecObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CodecObjectTest.TestType1)values[0]).value);
		Assertions.assertEquals(CodecObjectTest.TestType2.class, values[1].getClass());
		Assertions.assertEquals(0x1122_3344, ((CodecObjectTest.TestType2)values[1]).value);
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[2].getClass());
		Assertions.assertEquals(0x0666, ((CodecObjectTest.TestType1)values[2]).value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

}
