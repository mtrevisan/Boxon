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
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Describer;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;


class CodecObjectTest{

	private record Version(@BindInteger(size = "8") byte major, @BindInteger(size = "8") byte minor){ }


	@Test
	void object() throws BoxonException{
		CodecObject codec = new CodecObject();
		Version encodedValue = new Version((byte)1, (byte)2);
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindObject.class.getName(),
			"type", Version.class.getName(),
			"selectFrom", Map.of(
				"annotationType", ObjectChoices.class.getName(),
				"prefixLength", 0,
				"byteOrder", ByteOrder.BIG_ENDIAN,
				"alternatives", Collections.emptyList()
			),
			"selectFromList", Map.of(
				"annotationType", ObjectChoicesList.class.getName(),
				"charset", StandardCharsets.UTF_8.name(),
				"terminator", (byte)0,
				"alternatives", Collections.emptyList()
			),
			"selectDefault", void.class,
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindObject annotation = AnnotationCreator.createAnnotation(BindObject.class, annotationData);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.injectDependenciesIntoCodecs(templateParser, evaluator);
		FieldAccessor.injectValues(codec, templateParser, evaluator);
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x01, 0x02}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Version decoded = (Version)codec.decode(reader, annotation, null, null);

		Assertions.assertNotNull(decoded);
		Assertions.assertEquals(encodedValue.major, decoded.major);
		Assertions.assertEquals(encodedValue.minor, decoded.minor);
	}


	static class TestType0{}

	static class TestType1 extends TestType0{
		@BindInteger(size = "16")
		short value;
	}

	static class TestType2 extends TestType0{
		@BindInteger(size = "32")
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
		@BindInteger(size = "8")
		@BindAsArray(size = "2")
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
	void choice1() throws BoxonException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(TestChoice1.class)
			.create();
		Parser parser = Parser.create(core);
		Describer describer = Describer.create(core);

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


		List<Map<String, Object>> descriptions = describer.describeTemplate();
		Assertions.assertEquals(1, descriptions.size());
		Map<String, Object> description = descriptions.getFirst();
		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{context:{},header:{charset:UTF-8,start:[tc1]},template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestChoice1,fields:[{annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,name:header,fieldType:java.lang.String,size:3,charset:UTF-8},{annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindObject,name:value,fieldType:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0,selectFromList:{charset:UTF-8,terminator:0},selectDefault:void,selectFrom:{byteOrder:BIG_ENDIAN,prefixLength:8,alternatives:[{annotationType:io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices$ObjectChoice,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType1,prefix:1,condition:#prefix == 1,subtypes:[{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0},{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType1,fields:[{annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,name:value,fieldType:short,size:16,byteOrder:BIG_ENDIAN}]}]},{annotationType:io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices$ObjectChoice,type:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType2,prefix:2,condition:#prefix == 2,subtypes:[{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType0},{template:io.github.mtrevisan.boxon.core.codecs.CodecObjectTest$TestType2,fields:[{annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,name:value,fieldType:int,size:32,byteOrder:BIG_ENDIAN}]}]}]}}]}", jsonDescription);
		Assertions.assertEquals(1608, jsonDescription.length());
	}

	@Test
	void choice2() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(TestChoice2.class)
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
	void choice3() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(TestChoice3.class)
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
