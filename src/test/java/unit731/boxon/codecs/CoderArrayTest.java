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
import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.codecs.queclink.Version;
import unit731.boxon.helpers.ByteHelper;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;


class CoderArrayTest{

	@MessageHeader(start = "tc4")
	static class TestChoice4{
		@BindString(size = "3")
		public String header;
		@BindArray(size = "3", type = CoderObjectTest.TestType0.class, selectFrom = @Choices(prefixSize = 8, alternatives = {
			@Choices.Choice(condition = "#prefix == 1", prefix = 1, type = CoderObjectTest.TestType1.class),
			@Choices.Choice(condition = "#prefix == 2", prefix = 2, type = CoderObjectTest.TestType2.class)
		}))
		public CoderObjectTest.TestType0[] value;
	}


	@Test
	void arrayPrimitive(){
		Coder coder = Coder.ARRAY_PRIMITIVE;
		int[] encodedValue = new int[]{0x0000_0123, 0x0000_0456};
		BindArrayPrimitive annotation = new BindArrayPrimitive(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArrayPrimitive.class;
			}

			@Override
			public Class<?> type(){
				return int[].class;
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
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);

		Object decoded = coder.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void arrayOfSameObject(){
		Coder coder = Coder.ARRAY;
		Version[] encodedValue = new Version[]{new Version((byte)0, (byte)1, (byte)12), new Version((byte)1, (byte)2, (byte)0)};
		BindArray annotation = new BindArray(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArray.class;
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
			public Choices selectFrom(){
				return new Choices(){
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
					public Choice[] alternatives(){
						return new Choice[0];
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
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x01, 0x01, 0x02}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);

		Version[] decoded = (Version[])coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.length);
		Assertions.assertEquals(encodedValue[0].major, decoded[0].major);
		Assertions.assertEquals(encodedValue[0].minor, decoded[0].minor);
		Assertions.assertEquals(encodedValue[1].major, decoded[1].major);
		Assertions.assertEquals(encodedValue[1].minor, decoded[1].minor);
	}

	@Test
	void arrayOfDifferentObjects(){
		Codec<TestChoice4> codec = Codec.createFrom(TestChoice4.class);
		Parser parser = new Parser(null, Collections.singletonList(codec));

		byte[] payload = ByteHelper.hexStringToByteArray("7463340112340211223344010666");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		List<Object> parsedMessages = result.getParsedMessages();
		Assertions.assertEquals(1, parsedMessages.size());
		Assertions.assertEquals(TestChoice4.class, parsedMessages.get(0).getClass());
		TestChoice4 parsedMessage = (TestChoice4)parsedMessages.get(0);
		CoderObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CoderObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CoderObjectTest.TestType1)values[0]).value);
		Assertions.assertEquals(CoderObjectTest.TestType2.class, values[1].getClass());
		Assertions.assertEquals(0x1122_3344, ((CoderObjectTest.TestType2)values[1]).value);
		Assertions.assertEquals(CoderObjectTest.TestType1.class, values[2].getClass());
		Assertions.assertEquals(0x0666, ((CoderObjectTest.TestType1)values[2]).value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

}
