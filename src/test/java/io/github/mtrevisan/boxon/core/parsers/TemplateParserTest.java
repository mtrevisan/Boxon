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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHexByteChecksum;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.nio.charset.StandardCharsets;


class TemplateParserTest{

	@Test
	void parseSingleMessageHex() throws NoSuchMethodException, FieldException{
		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600abcd07e30405083639001256080d0a");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<ACKMessageHex> template = loaderTemplate.createTemplate(ACKMessageHex.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		evaluator.addToContext("deviceTypes", deviceTypes);
		evaluator.addToContext(TemplateParserTest.class.getDeclaredMethod("headerLength"));
		ACKMessageHex message = templateParser.decode(template, reader, null);
		evaluator.addToContext("deviceTypes", null);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload, StandardCharsets.US_ASCII), new String(reconstructedMessage, StandardCharsets.US_ASCII));
	}

	@Test
	void parseSingleMessageHexByteChecksum() throws NoSuchMethodException, FieldException{
		byte[] payload = StringHelper.hexToByteArray("2d41434b066f2446010a0311235e40035110420600ffff07e304050836390012b80d0a");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<ACKMessageHexByteChecksum> template = loaderTemplate.createTemplate(ACKMessageHexByteChecksum.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		evaluator.addToContext("deviceTypes", deviceTypes);
		evaluator.addToContext(TemplateParserTest.class.getDeclaredMethod("headerLength"));
		ACKMessageHexByteChecksum message = templateParser.decode(template, reader, null);
		evaluator.addToContext("deviceTypes", null);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload, StandardCharsets.US_ASCII), new String(reconstructedMessage, StandardCharsets.US_ASCII));
	}

	private static int headerLength(){
		return 4;
	}

	@Test
	void parseSingleMessageASCII() throws FieldException{
		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,20170101123542,11F0$");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<ACKMessageASCII> template = loaderTemplate.createTemplate(ACKMessageASCII.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageASCII message = templateParser.decode(template, reader, null);
		evaluator.addToContext("deviceTypes", null);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload, StandardCharsets.US_ASCII), new String(reconstructedMessage, StandardCharsets.US_ASCII));
	}

	@Test
	void evaluate(){
		Evaluator evaluator = Evaluator.create();
		Integer result = evaluator.evaluate("8/4", null, Integer.class);

		Assertions.assertEquals(2, result);
	}


	@MessageHeader(start = "te1")
	private static class TestError1{
		@BindString(size = "3")
		String header;
		@BindByte(condition = "e")
		byte type;
	}

	@Test
	void parseWithConditionError() throws AnnotationException{
		byte[] payload = StringHelper.hexToByteArray("746335011234");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestError1> template = loaderTemplate.createTemplate(TestError1.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		SpelEvaluationException exc = Assertions.assertThrows(SpelEvaluationException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("EL1008E: Property or field 'e' cannot be found on object of type '"
			+ TemplateParserTest.TestError1.class.getName() + "' - maybe not public or not valid?", exc.getMessage());
	}


	@MessageHeader(start = "te3")
	static class TestError3{
		static class WrongOutputConverter implements Converter<Byte, String>{

			@Override
			public String decode(final Byte value){
				return "";
			}

			@Override
			public Byte encode(final String value){
				return null;
			}

		}
		@BindString(size = "3")
		String header;
		@BindByte(converter = WrongOutputConverter.class)
		byte type;
	}

	@Test
	void parseWithConverterOutputError() throws AnnotationException{
		byte[] payload = StringHelper.hexToByteArray("74633501");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestError3> template = loaderTemplate.createTemplate(TestError3.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		Exception exc = Assertions.assertThrows(FieldException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("java.lang.IllegalArgumentException: Can not set byte field to String in field "
			+ TemplateParserTest.TestError3.class.getName() + ".type", exc.getMessage());
	}


	@MessageHeader(start = "te4")
	static class TestError4{
		static class WrongInputConverter implements Converter<String, Byte>{

			@Override
			public Byte decode(final String value){
				return null;
			}

			@Override
			public String encode(final Byte value){
				return "";
			}

		}
		@BindString(size = "3")
		String header;
		@BindByte(converter = WrongInputConverter.class)
		byte type;
	}

	@Test
	void parseWithConverterInputError() throws AnnotationException{
		byte[] payload = StringHelper.hexToByteArray("74633501");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestError4> template = loaderTemplate.createTemplate(TestError4.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		Exception exc = Assertions.assertThrows(FieldException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("java.lang.IllegalArgumentException: Can not input Byte (1) to decode method of converter WrongInputConverter in field "
			+ TemplateParserTest.TestError4.class.getName() + ".type", exc.getMessage());
	}


	@MessageHeader(start = "tm1")
	static class TestComposition1{
		static class TestSubComposition{
			@BindByte
			byte subsubtype;
			@BindString(condition = "type == 1", size = "1")
			String field1;
			@BindString(condition = "#self.subsubtype == 1", size = "1")
			String field2;
		}
		@BindString(size = "3")
		String header;
		@BindByte
		byte type;
		@BindByte(condition = "type == 1")
		Byte subtype;
		@BindObject(type = TestSubComposition.class)
		TestSubComposition sub;
	}

	@Test
	void parseCompositeMessage1() throws FieldException{
		byte[] payload = StringHelper.hexToByteArray("746D310102016162");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestComposition1> template = loaderTemplate.createTemplate(TestComposition1.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		TestComposition1 parsed = templateParser.decode(template, reader, null);
		Assertions.assertNotNull(parsed);
		Assertions.assertEquals("tm1", parsed.header);
		Assertions.assertEquals(1, parsed.type);
		Assertions.assertEquals(2, parsed.subtype.intValue());
		Assertions.assertEquals(1, parsed.sub.subsubtype);
		Assertions.assertEquals("a", parsed.sub.field1);
		Assertions.assertEquals("b", parsed.sub.field2);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, parsed);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

	@MessageHeader(start = "tm2")
	static class TestComposition2{
		static class TestSubCompositionBase{
			@BindByte
			byte subsubtype;

		}
		static class TestSubComposition1 extends TestSubCompositionBase{
			@BindString(condition = "type == 1", size = "1")
			String field1;
			@BindString(condition = "type == 1 && #self.subsubtype == 1", size = "1")
			String field2;
		}
		static class TestSubComposition2 extends TestSubCompositionBase{
			@BindString(condition = "type == 2", size = "1")
			String field1;
			@BindByte(condition = "#self.subsubtype == 2")
			byte field2;
			@BindString(condition = "#self.field2 == 0x62", size = "1")
			String field3;
		}
		@BindString(size = "3")
		String header;
		@BindByte
		byte type;
		@BindObject(type = TestSubCompositionBase.class, selectFrom = @ObjectChoices(
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "type == 1", type = TestSubComposition1.class),
				@ObjectChoices.ObjectChoice(condition = "type == 2", type = TestSubComposition2.class)
			}
		))
		private TestSubCompositionBase sub;
	}

	@Test
	void parseCompositeMessage21() throws FieldException{
		byte[] payload = StringHelper.hexToByteArray("746D3201016162");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestComposition2> template = loaderTemplate.createTemplate(TestComposition2.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		TestComposition2 parsed = templateParser.decode(template, reader, null);
		Assertions.assertNotNull(parsed);
		Assertions.assertEquals("tm2", parsed.header);
		Assertions.assertEquals(1, parsed.type);
		Assertions.assertEquals(1, parsed.sub.subsubtype);
		Assertions.assertEquals("a", ((TestComposition2.TestSubComposition1)parsed.sub).field1);
		Assertions.assertEquals("b", ((TestComposition2.TestSubComposition1)parsed.sub).field2);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, parsed);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

	@Test
	void parseCompositeMessage22() throws FieldException{
		byte[] payload = StringHelper.hexToByteArray("7463320202616263");
		BitReaderInterface reader = BitReader.wrap(payload);

		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		Template<TestComposition2> template = loaderTemplate.createTemplate(TestComposition2.class);
		postProcessCodecs(loaderCodec, templateParser, evaluator);

		TestComposition2 parsed = templateParser.decode(template, reader, null);
		Assertions.assertNotNull(parsed);
		Assertions.assertEquals("tc2", parsed.header);
		Assertions.assertEquals(2, parsed.type);
		Assertions.assertEquals(2, parsed.sub.subsubtype);
		Assertions.assertEquals("a", ((TestComposition2.TestSubComposition2)parsed.sub).field1);
		Assertions.assertEquals(0x62, ((TestComposition2.TestSubComposition2)parsed.sub).field2);
		Assertions.assertEquals("c", ((TestComposition2.TestSubComposition2)parsed.sub).field3);

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, parsed);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}


	private static void postProcessCodecs(LoaderCodec loaderCodec, TemplateParserInterface templateParser, Evaluator evaluator){
		loaderCodec.injectFieldInCodecs(TemplateParserInterface.class, templateParser);
		loaderCodec.injectFieldInCodecs(Evaluator.class, evaluator);
	}

}
