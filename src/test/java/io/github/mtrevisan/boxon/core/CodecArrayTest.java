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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;


@SuppressWarnings("ALL")
class CodecArrayTest{

	private static class Version{
		@BindByte
		private final byte major;
		@BindByte
		private final byte minor;
		@BindByte
		private final byte build;

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

	@MessageHeader(start = "tc5")
	static class TestChoice5{
		@BindString(size = "3")
		public String header;
		@BindByte
		public byte type;
		@BindArray(size = "1", type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "type == 1", type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "type == 2", type = CodecObjectTest.TestType2.class)
			}))
		public CodecObjectTest.TestType0[] value;
	}


	@Test
	void arrayPrimitive() throws FieldException{
		CodecInterface<BindArrayPrimitive> codec = new CodecArrayPrimitive();
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

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitReader reader = BitReader.wrap(writer);
		Object decoded = codec.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void arrayOfSameObject() throws FieldException{
		CodecArray codec = new CodecArray();
		Version[] encodedValue = new Version[]{new Version((byte) 0, (byte) 1, (byte) 12), new Version((byte) 1, (byte) 2, (byte) 0)};
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

		Loader loader = Loader.create();
		TemplateParser templateParser = TemplateParser.create(loader);
		loader.loadDefaultCodecs();
		ReflectionHelper.setFieldValue(codec, Loader.class, loader);
		ReflectionHelper.setFieldValue(codec, TemplateParser.class, templateParser);
		BitWriter writer = BitWriter.create();
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
	void arrayOfDifferentObjects() throws AnnotationException, TemplateException{
		Parser parser = Parser.create()
			.withCodecs(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplates(TestChoice4.class);

		byte[] payload = JavaHelper.toByteArray("7463340112340211223344010666");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice4.class, result.getParsedMessageAt(0).getClass());
		TestChoice4 parsedMessage = (TestChoice4)result.getParsedMessageAt(0);
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

	@Test
	void arrayOfDifferentObjectsWithNoPrefix() throws AnnotationException, TemplateException{
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withTemplates(TestChoice5.class);

		byte[] payload = JavaHelper.toByteArray("746335011234");
		ParseResponse result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(1, result.getParsedMessageCount());
		Assertions.assertEquals(TestChoice5.class, result.getParsedMessageAt(0).getClass());
		TestChoice5 parsedMessage = (TestChoice5)result.getParsedMessageAt(0);
		CodecObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CodecObjectTest.TestType1)values[0]).value);

		ComposeResponse response = parser.compose(parsedMessage);
		Assertions.assertNotNull(response);
		Assertions.assertFalse(response.hasErrors());
		Assertions.assertArrayEquals(payload, response.getComposedMessage());
	}

}
