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
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;


class CodecArrayTest{

	private record Version(@BindInteger(size = "8") byte major, @BindInteger(size = "8") byte minor, @BindInteger(size = "8") byte build){ }

	@TemplateHeader(start = "tc4")
	static class TestChoice4{
		@BindString(size = "3")
		String header;
		@BindObject(type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(prefixLength = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = "1", type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = "2", type = CodecObjectTest.TestType2.class)
			}))
		@BindAsArray(size = "3")
		CodecObjectTest.TestType0[] value;
	}

	@TemplateHeader(start = "tc5")
	static class TestChoice5{
		@BindString(size = "3")
		String header;
		@BindInteger(size = "8")
		byte type;
		@BindObject(type = CodecObjectTest.TestType0.class, selectFrom = @ObjectChoices(
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "type == 1", prefix = "", type = CodecObjectTest.TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "type == 2", prefix = "", type = CodecObjectTest.TestType2.class)
			}))
		@BindAsArray(size = "1")
		CodecObjectTest.TestType0[] value;
	}

	@Test
	void arrayPrimitive() throws BoxonException{
		Codec codec = new CodecDefault();
		int[] encodedValue = {0x0000_0123, 0x0000_0456};
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "32";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return AlwaysPassValidator.class;
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


			static class AlwaysPassValidator implements Validator<int[]>{
				@Override
				public boolean isValid(final int[] value){
					return true;
				}
			}
		};
		BindAsArray collectionAnnotation = new BindAsArray(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindAsArray.class;
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length);
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValues(codec, Evaluator.create());
		codec.encode(writer, annotation, collectionAnnotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Object decoded = codec.decode(reader, annotation, collectionAnnotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void arrayOfSameObject() throws BoxonException{
		CodecObject codec = new CodecObject();
		Version[] encodedValue = {new Version((byte) 0, (byte) 1, (byte) 12), new Version((byte) 1, (byte) 2, (byte) 0)};
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
						return ObjectChoices.class;
					}

					@Override
					public byte prefixLength(){
						return 0;
					}

					@Override
					public ByteOrder byteOrder(){
						return ByteOrder.BIG_ENDIAN;
					}

					@Override
					public ObjectChoice[] alternatives(){
						return new ObjectChoice[0];
					}
				};
			}

			@Override
			public ObjectChoicesList selectFromList(){
				return null;
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
		BindAsArray collectionAnnotation = new BindAsArray(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindAsArray.class;
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length);
			}
		};

		LoaderCodec loaderCodec = LoaderCodec.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.loadDefaultCodecs();
		loaderCodec.injectDependenciesIntoCodecs(templateParser, evaluator);
		FieldAccessor.injectValues(codec, templateParser, evaluator);
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, collectionAnnotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x01, 0x0C, 0x01, 0x02, 0x00}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Version[] decoded = (Version[])codec.decode(reader, annotation, collectionAnnotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.length);
		Assertions.assertEquals(encodedValue[0].major, decoded[0].major);
		Assertions.assertEquals(encodedValue[0].minor, decoded[0].minor);
		Assertions.assertEquals(encodedValue[1].major, decoded[1].major);
		Assertions.assertEquals(encodedValue[1].minor, decoded[1].minor);
	}

	@Test
	void arrayOfDifferentObjects() throws Exception{
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
		if(response.hasError())
			Assertions.fail(response.getError());
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
	void arrayOfDifferentObjectsWithNoPrefix() throws Exception{
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
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice5.class, response.getMessage().getClass());
		TestChoice5 parsedMessage = (TestChoice5)response.getMessage();
		CodecObjectTest.TestType0[] values = parsedMessage.value;
		Assertions.assertEquals(CodecObjectTest.TestType1.class, values[0].getClass());
		Assertions.assertEquals(0x1234, ((CodecObjectTest.TestType1)values[0]).value);
	}

}
