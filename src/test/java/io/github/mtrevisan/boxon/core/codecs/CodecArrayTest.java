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
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;


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
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindInteger.class.getName(),
			"size", "32",
			"byteOrder", ByteOrder.BIG_ENDIAN,
			"validator", AlwaysPassIntValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindInteger annotation = AnnotationCreator.createAnnotation(BindInteger.class, annotationData);
		Map<String, Object> collectionAnnotationData = Map.of(
			"annotationType", BindAsArray.class.getName(),
			"size", Integer.toString(encodedValue.length)
		);
		BindAsArray collectionAnnotation = AnnotationCreator.createAnnotation(BindAsArray.class, collectionAnnotationData);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, collectionAnnotation, null, encodedValue);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitReaderInterface reader = BitReader.wrap(writer);
		Object decoded = codec.decode(reader, annotation, collectionAnnotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	private static class AlwaysPassIntValidator implements Validator<int[]>{
		@Override
		public boolean isValid(final int[] value){
			return true;
		}
	}

	@Test
	void arrayOfSameObject() throws BoxonException{
		CodecObject codec = new CodecObject();
		Version[] encodedValue = {new Version((byte) 0, (byte) 1, (byte) 12), new Version((byte) 1, (byte) 2, (byte) 0)};
		Map<String, Object> annotationData = Map.of(
			"annotationType", BindObject.class.getName(),
			"type", Version.class.getName(),
			"selectFrom", Map.of(
				"annotationType", ObjectChoices.class.getName(),
				"prefixLength", 0,
				"byteOrder", ByteOrder.BIG_ENDIAN,
				"alternatives", Collections.emptyList()
			),
			"selectDefault", void.class,
			"validator", AlwaysPassVersionValidator.class.getName(),
			"converter", NullConverter.class.getName(),
			"selectConverterFrom", Map.of(
				"annotationType", ConverterChoices.class.getName(),
				"alternatives", Collections.emptyList()
			)
		);
		BindObject annotation = AnnotationCreator.createAnnotation(BindObject.class, annotationData);
		Map<String, Object> collectionAnnotationData = Map.of(
			"annotationType", BindAsArray.class.getName(),
			"size", Integer.toString(encodedValue.length)
		);
		BindAsArray collectionAnnotation = AnnotationCreator.createAnnotation(BindAsArray.class, collectionAnnotationData);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
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

	private static class AlwaysPassVersionValidator implements Validator<Version[]>{
		@Override
		public boolean isValid(final Version[] value){
			return true;
		}
	}

	@Test
	void arrayOfDifferentObjects() throws Exception{
		Core core = CoreBuilder.builder()
			.withCodecsFrom(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplate(TestChoice4.class)
			.build();
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
			.withTemplate(TestChoice5.class)
			.build();
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
