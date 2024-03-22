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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
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
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.io.CodecInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;


class CodecArrayTest{

	private record Version(@BindByte byte major, @BindByte byte minor, @BindByte byte build){ }

	@TemplateHeader(start = "tc4")
	static class TestChoice4{
		@BindString(size = "3")
		String header;
		@BindArray(size = "3", type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(prefixLength = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = 1, type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = 2, type = CodecObjectTest.TestType2.class)
			}))
		CodecObjectTest.TestType0[] value;
	}

	@TemplateHeader(start = "tc5")
	static class TestChoice5{
		@BindString(size = "3")
		String header;
		@BindByte
		byte type;
		@BindArray(size = "1", type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "type == 1", type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "type == 2", type = CodecObjectTest.TestType2.class)
			}))
		CodecObjectTest.TestType0[] value;
	}


	@Test
	void arrayPrimitive() throws FieldException{
		CodecInterface<BindArrayPrimitive> codec = new CodecArrayPrimitive();
		int[] encodedValue = {0x0000_0123, 0x0000_0456};
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
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Object decoded = codec.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void arrayOfSameObject() throws FieldException{
		CodecArray codec = new CodecArray();
		Version[] encodedValue = {new Version((byte) 0, (byte) 1, (byte) 12), new Version((byte) 1, (byte) 2, (byte) 0)};
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
					public int prefixLength(){
						return 0;
					}

					@Override
					public ByteOrder bitOrder(){
						return ByteOrder.BIG_ENDIAN;
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

		LoaderCodec loaderCodec = LoaderCodec.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.loadDefaultCodecs();
		ReflectionHelper.injectValue(codec, TemplateParserInterface.class, templateParser);
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x01, 0x0C, 0x01, 0x02, 0x00}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Version[] decoded = (Version[])codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.length);
		Assertions.assertEquals(encodedValue[0].major, decoded[0].major);
		Assertions.assertEquals(encodedValue[0].minor, decoded[0].minor);
		Assertions.assertEquals(encodedValue[1].major, decoded[1].major);
		Assertions.assertEquals(encodedValue[1].minor, decoded[1].minor);
	}

	@Test
	void arrayOfDifferentObjects() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withCodecsFrom(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplatesFrom(TestChoice4.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("7463340112340211223344010666");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertFalse(response.hasError());
		Assertions.assertEquals(TestChoice4.class, response.getMessage().getClass());
		TestChoice4 parsedMessage = (TestChoice4)response.getMessage();
		CodecObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CodecObjectTest.TestType1)values[0]).value);
		Assertions.assertEquals(CodecObjectTest.TestType2.class, values[1].getClass());
		Assertions.assertEquals(0x1122_3344, ((CodecObjectTest.TestType2)values[1]).value);
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[2].getClass());
		Assertions.assertEquals(0x0666, ((CodecObjectTest.TestType1)values[2]).value);
	}

	@Test
	void arrayOfDifferentObjectsWithNoPrefix() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestChoice5.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("746335011234");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		Assertions.assertFalse(response.hasError());
		Assertions.assertEquals(TestChoice5.class, response.getMessage().getClass());
		TestChoice5 parsedMessage = (TestChoice5)response.getMessage();
		CodecObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CodecObjectTest.TestType1)values[0]).value);
	}

}
