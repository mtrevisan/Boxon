/*
 * Copyright (c) 2024 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Generator;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;


class CodecListTest{

	private record Version(@BindStringTerminated(terminator = ',') String type, @BindStringTerminated(terminator = ',') String major,
		@BindStringTerminated(terminator = ',') String minor, @BindStringTerminated(terminator = ',') String build){ }

	static class TestType3{}

	static class TestType4 extends TestType3{
		@BindStringTerminated(terminator = ',')
		String subtype;
		@BindStringTerminated(terminator = '.')
		String value;
	}

	static class TestType5 extends TestType3{
		@BindStringTerminated(terminator = ',')
		String subtype;
		@BindStringTerminated(terminator = '.')
		String value1;
		@BindStringTerminated(terminator = '.')
		String value2;
	}

	@TemplateHeader(start = "tc6")
	static class TestChoice6{
		@BindStringTerminated(terminator = ',')
		String type;
		@BindObject(type = TestType3.class, selectFromList = @ObjectChoicesList(terminator = ',',
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "#prefix == '1'", prefix = "1", type = TestType4.class),
				@ObjectChoices.ObjectChoice(condition = "#prefix == '2'", prefix = "2", type = TestType5.class)
			}))
		@BindAsList
		List<TestType3> value;
	}


	@Test
	void listOfSameObject() throws BoxonException{
		CodecObject codec = new CodecObject();
		List<Version> encodedValue = List.of(
			new Version("2", "0", "1", "12"),
			new Version("2", "1", "2", "0"));
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindObject.class.getName(),
			"type", CodecListTest.Version.class.getName(),
			"selectFromList", Map.of(
				"annotationType", ObjectChoicesList.class.getName(),
				"charset", StandardCharsets.US_ASCII.name(),
				"terminator", (byte)',',
				"alternatives", List.of(
					Map.of(
						"annotationType", ObjectChoices.ObjectChoice.class.getName(),
						"condition", "#prefix == '2'",
						"prefix", "2",
						"type", Version.class.getName()
					)
				)
			),
			"selectDefault", void.class.getName(),
			"validator", NullValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindObject annotation = Generator.createAnnotation(BindObject.class, annotationData);
		Map<String, Object> collectionAnnotationData = Map.of(
			"annotationType", BindAsList.class.getName()
		);
		BindAsList collectionAnnotation = Generator.createAnnotation(BindAsList.class, collectionAnnotationData);

		LoaderCodec loaderCodec = LoaderCodec.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.loadDefaultCodecs();
		FieldAccessor.injectValues(codec, templateParser, evaluator);
		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, collectionAnnotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("2,0,1,12,2,1,2,0,", new String(writer.array(), StandardCharsets.UTF_8));

		BitReaderInterface reader = BitReader.wrap(writer);
		List<Version> decoded = (List<Version>)codec.decode(reader, annotation, collectionAnnotation, null);

		Assertions.assertEquals(encodedValue.size(), decoded.size());
		Assertions.assertEquals(encodedValue.get(0).type, decoded.get(0).type);
		Assertions.assertEquals(encodedValue.get(0).major, decoded.get(0).major);
		Assertions.assertEquals(encodedValue.get(0).minor, decoded.get(0).minor);
		Assertions.assertEquals(encodedValue.get(1).type, decoded.get(1).type);
		Assertions.assertEquals(encodedValue.get(1).major, decoded.get(1).major);
		Assertions.assertEquals(encodedValue.get(1).minor, decoded.get(1).minor);
	}

	@Test
	void listOfDifferentObjects() throws Exception{
		Core core = CoreBuilder.builder()
			.withCodecsFrom(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplate(TestChoice6.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = TestHelper.toByteArray("tc6,1,1.2,v1.v2.1,2.");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		Assertions.assertEquals(TestChoice6.class, response.getMessage().getClass());
		TestChoice6 parsedMessage = (TestChoice6)response.getMessage();
		List<TestType3> values = parsedMessage.value;
		Assertions.assertEquals(TestType4.class, values.get(0).getClass());
		Assertions.assertEquals("1", ((TestType4)values.get(0)).value);
		Assertions.assertEquals(TestType5.class, values.get(1).getClass());
		Assertions.assertEquals("v1", ((TestType5)values.get(1)).value1);
		Assertions.assertEquals("v2", ((TestType5)values.get(1)).value2);
		Assertions.assertEquals(TestType4.class, values.get(2).getClass());
		Assertions.assertEquals("2", ((TestType4)values.get(2)).value);
	}

}
