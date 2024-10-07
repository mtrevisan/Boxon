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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHexByteChecksum;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.nio.charset.StandardCharsets;


class TemplateParserTest{

	@Test
	void parseSingleMessageHex() throws Exception{
		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600abcd07e30405083639001256080d0a");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<ACKMessageHex> template = templateLoader.createTemplate(ACKMessageHex.class);
		postProcessCodecs(templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		evaluator.putToContext("deviceTypes", deviceTypes);
		evaluator.putToContext(TemplateParserTest.class.getDeclaredMethod("headerLength"));
		ACKMessageHex message = (ACKMessageHex)templateParser.decode(template, reader, null);
		evaluator.removeFromContext("deviceTypes");

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		final String expected = new String(payload, StandardCharsets.US_ASCII);
		final String actual = new String(reconstructedMessage, StandardCharsets.US_ASCII);
		Assertions.assertEquals(expected, actual);
	}

	@Test
	void parseSingleMessageHexByteChecksum() throws Exception{
		byte[] payload = StringHelper.hexToByteArray("2d41434b066f2446010a0311235e40035110420600ffff07e304050836390012ee7c0d0a");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<ACKMessageHexByteChecksum> template = templateLoader.createTemplate(ACKMessageHexByteChecksum.class);
		postProcessCodecs(templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		evaluator.putToContext("deviceTypes", deviceTypes);
		evaluator.putToContext(TemplateParserTest.class.getDeclaredMethod("headerLength"));
		ACKMessageHexByteChecksum message = (ACKMessageHexByteChecksum)templateParser.decode(template, reader, null);
		evaluator.removeFromContext("deviceTypes");

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		final String expected = new String(payload, StandardCharsets.US_ASCII);
		final String actual = new String(reconstructedMessage, StandardCharsets.US_ASCII);
		Assertions.assertEquals(expected, actual);
	}

	private static int headerLength(){
		return 4;
	}

	@Test
	void parseSingleMessageASCII() throws BoxonException{
		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<ACKMessageASCII> template = templateLoader.createTemplate(ACKMessageASCII.class);
		postProcessCodecs(templateParser, evaluator);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		evaluator.putToContext("deviceTypes", deviceTypes);
		ACKMessageASCII message = (ACKMessageASCII)templateParser.decode(template, reader, null);
		evaluator.removeFromContext("deviceTypes");

		BitWriter writer = BitWriter.create();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		final String expected = new String(payload, StandardCharsets.US_ASCII);
		final String actual = new String(reconstructedMessage, StandardCharsets.US_ASCII);
		Assertions.assertEquals(expected, actual);
	}

	@Test
	void evaluate(){
		Evaluator evaluator = Evaluator.create();
		Integer result = evaluator.evaluate("8/4", null, Integer.class);

		Assertions.assertEquals(2, result);
	}


	@TemplateHeader(start = "te1")
	private static class TestError1{
		@BindString(size = "3")
		String header;
		@BindInteger(condition = "e", size = "8")
		byte type;
	}

	@Test
	void parseWithConditionError() throws AnnotationException{
		byte[] payload = StringHelper.hexToByteArray("746335011234");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<TestError1> template = templateLoader.createTemplate(TestError1.class);
		postProcessCodecs(templateParser, evaluator);

		SpelEvaluationException exc = Assertions.assertThrows(SpelEvaluationException.class,
			() -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("EL1008E: Property or field 'e' cannot be found on object of type '"
			+ TemplateParserTest.TestError1.class.getName() + "' - maybe not public or not valid?", exc.getMessage());
	}


	@TemplateHeader(start = "te3")
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
		@BindInteger(size = "8", converter = WrongOutputConverter.class)
		byte type;
	}

	@Test
	void parseWithConverterOutputError(){
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();

		Exception exc = Assertions.assertThrows(AnnotationException.class, () -> templateLoader.createTemplate(TestError3.class));
		Assertions.assertEquals("Type mismatch between converter output (String) and field type (byte) in field "
			+ TemplateParserTest.TestError3.class.getName() + ".type", exc.getMessage());
	}


	@TemplateHeader(start = "te4")
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
		@BindInteger(size = "8", converter = WrongInputConverter.class)
		byte type;
	}

	@Test
	void parseWithConverterInputError(){
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Exception exc = Assertions.assertThrows(AnnotationException.class, () -> templateLoader.createTemplate(TestError4.class));
		Assertions.assertEquals("Type mismatch between annotation output (BigInteger) and converter input (String) in field "
			+ TemplateParserTest.TestError4.class.getName() + ".type", exc.getMessage());
	}


	@TemplateHeader(start = "tm1")
	static class TestComposition1{
		static class TestSubComposition{
			@BindInteger(size = "8")
			byte subsubtype;
			@BindString(condition = "type == 1", size = "1")
			String field1;
			@BindString(condition = "#self.subsubtype == 1", size = "1")
			String field2;
		}

		@BindString(size = "3")
		String header;
		@BindInteger(size = "8")
		byte type;
		@BindInteger(condition = "type == 1", size = "8")
		Byte subtype;
		@BindObject(type = TestSubComposition.class)
		TestSubComposition sub;
	}

	@Test
	void parseCompositeMessage1() throws BoxonException{
		byte[] payload = StringHelper.hexToByteArray("746D310102016162");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<TestComposition1> template = templateLoader.createTemplate(TestComposition1.class);
		postProcessCodecs(templateParser, evaluator);

		TestComposition1 parsed = (TestComposition1)templateParser.decode(template, reader, null);
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

	@TemplateHeader(start = "tm2")
	static class TestComposition2{
		static class TestSubCompositionBase{
			@BindInteger(size = "8")
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
			@BindInteger(condition = "#self.subsubtype == 2", size = "8")
			byte field2;
			@BindString(condition = "#self.field2 == 0x62", size = "1")
			String field3;
		}

		@BindString(size = "3")
		String header;
		@BindInteger(size = "8")
		byte type;
		@BindObject(type = TestSubCompositionBase.class, selectFrom = @ObjectChoices(
			alternatives = {
				@ObjectChoices.ObjectChoice(condition = "type == 1", prefix = "", type = TestSubComposition1.class),
				@ObjectChoices.ObjectChoice(condition = "type == 2", prefix = "", type = TestSubComposition2.class)
			}
		))
		private TestSubCompositionBase sub;
	}

	@Test
	void parseCompositeMessage21() throws BoxonException{
		byte[] payload = StringHelper.hexToByteArray("746D3201016162");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<TestComposition2> template = templateLoader.createTemplate(TestComposition2.class);
		postProcessCodecs(templateParser, evaluator);

		TestComposition2 parsed = (TestComposition2)templateParser.decode(template, reader, null);
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
	void parseCompositeMessage22() throws BoxonException{
		byte[] payload = StringHelper.hexToByteArray("7463320202616263");
		BitReaderInterface reader = BitReader.wrap(payload);

		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = io.github.mtrevisan.boxon.core.parsers.TemplateParser.create(evaluator);
		Template<TestComposition2> template = templateLoader.createTemplate(TestComposition2.class);
		postProcessCodecs(templateParser, evaluator);

		TestComposition2 parsed = (TestComposition2)templateParser.decode(template, reader, null);
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


	private static void postProcessCodecs(TemplateParserInterface templateParser, Evaluator evaluator){
		CodecLoader.injectDependenciesIntoCodecs(templateParser, evaluator);
	}

}
