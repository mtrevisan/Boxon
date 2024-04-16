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
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Descriptor;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


class CodecObjectTest{

	private record Version(@BindByte byte major, @BindByte byte minor){ }


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
		loaderCodec.loadDefaultCodecs();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		FieldAccessor.injectValue(codec, templateParser);
		FieldAccessor.injectValue(codec, Evaluator.create());
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x01, 0x02}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Version decoded = (Version)codec.decode(reader, annotation, null);

		Assertions.assertNotNull(decoded);
		Assertions.assertEquals(encodedValue.major, decoded.major);
		Assertions.assertEquals(encodedValue.minor, decoded.minor);
	}


	static class TestType0{}

	static class TestType1 extends TestType0{
		@BindShort
		short value;
	}

	static class TestType2 extends TestType0{
		@BindInt
		int value;
	}

	@TemplateHeader(start = "tc1")
	static class TestChoice1{
		@BindString(size = "3")
		String header;
		@BindObject(type = TestType0.class, selectFrom = @ObjectChoices(prefixLength = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = "1", type = TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = "2", type = TestType2.class)
			}))
		TestType0 value;
	}

	@TemplateHeader(start = "tc2")
	static class TestChoice2{
		@BindString(size = "3")
		String header;
		@BindArrayPrimitive(size = "2", type = byte.class)
		byte[] index;
		@BindObject(type = TestType0.class, selectFrom = @ObjectChoices(prefixLength = 8,
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "index[#prefix] == 5", prefix = "0", type = TestType1.class),
				@ObjectChoices.ObjectChoice(condition = "index[#prefix] == 6", prefix = "1", type = TestType2.class)
			}))
		TestType0 value;
	}

	@TemplateHeader(start = "tc3")
	static class TestChoice3{
		@BindString(size = "3")
		String header;
		@BindString(size = "2")
		String key;
		@BindObject(type = TestType0.class, selectFrom = @ObjectChoices(alternatives = {
			@ObjectChoices.ObjectChoice(condition = "key == 'aa'", prefix = "", type = TestType1.class),
			@ObjectChoices.ObjectChoice(condition = "key == 'bb'", prefix = "", type = TestType2.class)
		}))
		TestType0 value;
	}

	@Test
	void choice1() throws FieldException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestChoice1.class)
			.create();
		Parser parser = Parser.create(core);
		Descriptor descriptor = Descriptor.create(core);

		byte[] payload = StringHelper.hexToByteArray("746331011234");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice1.class, response.getMessage().getClass());
		TestChoice1 parsedMessage = (TestChoice1)response.getMessage();
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);


		payload = StringHelper.hexToByteArray("7463310211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice1.class, response.getMessage().getClass());
		parsedMessage = (TestChoice1)response.getMessage();
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);


		List<Map<String, Object>> descriptions = descriptor.describeTemplate();
		Assertions.assertEquals(10, descriptions.size());
		Map<String, Object> description = descriptions.get(1);
		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{context:{},template:io.github.mtrevisan.boxon.core.codecs.CodecArrayTest$TestChoice4,header:{start:[tc4],charset:UTF-8},fields:[{name:header,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,charset:UTF-8,size:3,fieldType:java.lang.String},{selectFrom:{prefixLength:8,byteOrder:BIG_ENDIAN,alternatives:[{condition:#prefix == 1,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType1,prefix:1,subtypes:[{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0},{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType1,fields:[{name:value,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindShort,fieldType:short,byteOrder:BIG_ENDIAN}]}]},{condition:#prefix == 2,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType2,prefix:2,subtypes:[{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0},{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType2,fields:[{name:value,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInt,fieldType:int,byteOrder:BIG_ENDIAN}]}]}]},size:3,selectDefault:void,name:value,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindArray,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0,fieldType:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0[]}]}", jsonDescription);
		Assertions.assertEquals(1371, jsonDescription.length());
	}

	@Test
	void choice2() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestChoice2.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("7463320506001234");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice2.class, response.getMessage().getClass());
		TestChoice2 parsedMessage = (TestChoice2)response.getMessage();
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);


		payload = StringHelper.hexToByteArray("74633205060111223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice2.class, response.getMessage().getClass());
		parsedMessage = (TestChoice2)response.getMessage();
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);
	}

	@Test
	void choice3() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(TestChoice3.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("74633361611234");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice3.class, response.getMessage().getClass());
		TestChoice3 parsedMessage = (TestChoice3)response.getMessage();
		TestType1 value1 = (TestType1)parsedMessage.value;
		Assertions.assertEquals(0x1234, value1.value);


		payload = StringHelper.hexToByteArray("746333626211223344");
		result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice3.class, response.getMessage().getClass());
		parsedMessage = (TestChoice3)response.getMessage();
		TestType2 value2 = (TestType2)parsedMessage.value;
		Assertions.assertEquals(0x1122_3344, value2.value);
	}

}
