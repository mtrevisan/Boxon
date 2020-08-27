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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindBits;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ReferenceException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.nio.charset.StandardCharsets;


class TemplateParserTest{

	@Test
	void parseSingleMessageHex() throws NoSuchMethodException, ReferenceException{
		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<ACKMessageHex> template = Template.createFrom(ACKMessageHex.class, templateParser.loader::hasCodec);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		Evaluator.addToContext(TemplateParserTest.class.getDeclaredMethod("headerSize"));
		ACKMessageHex message = templateParser.decode(template, reader, null);
		Evaluator.addToContext("deviceTypes", null);

		BitWriter writer = new BitWriter();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload), new String(reconstructedMessage));
	}

	private static int headerSize(){
		return 4;
	}

	@Test
	void parseSingleMessageASCII() throws ReferenceException{
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<ACKMessageASCII> template = Template.createFrom(ACKMessageASCII.class, templateParser.loader::hasCodec);

		if(!template.canBeCoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageASCII message = templateParser.decode(template, reader, null);
		Evaluator.addToContext("deviceTypes", null);

		BitWriter writer = new BitWriter();
		templateParser.encode(template, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload), new String(reconstructedMessage));
	}

	@Test
	void evaluate(){
		Integer result = Evaluator.evaluate("8/4", null, Integer.class);

		Assertions.assertEquals(2, result);
	}


	@MessageHeader(start = "te1")
	static class TestError1{
		@BindString(size = "3")
		public String header;
		@BindByte(condition = "e")
		public byte type;
	}

	@Test
	void parseWithConditionError() throws AnnotationException{
		byte[] payload = JavaHelper.toByteArray("746335011234");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestError1> template = Template.createFrom(TestError1.class, templateParser.loader::hasCodec);

		SpelEvaluationException exc = Assertions.assertThrows(SpelEvaluationException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("EL1008E: Property or field 'e' cannot be found on object of type 'io.github.mtrevisan.boxon.codecs.TemplateParserTest$TestError1' - maybe not public or not valid?", exc.getMessage());
	}


	@MessageHeader(start = "te2")
	static class TestError2{
		@BindString(size = "3")
		public String header;
		@BindByte(match = "as")
		public byte type;
	}

	@Test
	void parseWithMatchError1() throws AnnotationException{
		byte[] payload = JavaHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestError2> template = Template.createFrom(TestError2.class, templateParser.loader::hasCodec);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Value `1` does not match constraint `as` in field TestError2.type", exc.getMessage());
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
		public String header;
		@BindByte(converter = WrongOutputConverter.class)
		public byte type;
	}

	@Test
	void parseWithConverterOutputError() throws AnnotationException{
		byte[] payload = JavaHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestError3> template = Template.createFrom(TestError3.class, templateParser.loader::hasCodec);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Can not set byte field to String in field TestError3.type", exc.getMessage());
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
		public String header;
		@BindByte(converter = WrongInputConverter.class)
		public byte type;
	}

	@Test
	void parseWithConverterInputError() throws AnnotationException{
		byte[] payload = JavaHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestError4> template = Template.createFrom(TestError4.class, templateParser.loader::hasCodec);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Can not input Byte to decode method of converter WrongInputConverter in field TestError4.type", exc.getMessage());
	}


	@MessageHeader(start = "te5")
	static class TestError5{
		@BindString(size = "3")
		public String header;
		@BindBits(size = "8", match = "[1]")
		public BitSet type;
	}

	@Test
	void parseWithMatchError2() throws AnnotationException{
		byte[] payload = JavaHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestError5> template = Template.createFrom(TestError5.class, templateParser.loader::hasCodec);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> templateParser.decode(template, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Value `[0]` does not match constraint `[1]` in field TestError5.type", exc.getMessage());
	}



	@MessageHeader(start = "tc")
	static class TestComposition{
		static class TestSubComposition{
			@BindByte
			byte subtype;
			@BindString(condition = "type == 1", size = "1")
			String field1;
			@BindString(condition = "#self.subtype == 1", size = "1")
			String field2;
		}
		@BindString(size = "2")
		String header;
		@BindByte
		public byte type;
		@BindByte(condition = "type == 1")
		Byte subtype;
		@BindObject(type = TestSubComposition.class)
		TestSubComposition sub;
	}

	@Test
	void parseCompositeMessage() throws ReferenceException{
		byte[] payload = JavaHelper.toByteArray("74630102016162");
		BitReader reader = BitReader.wrap(payload);

		TemplateParser templateParser = new TemplateParser();
		templateParser.loader.loadDefaultCodecs();
		Template<TestComposition> template = Template.createFrom(TestComposition.class, templateParser.loader::hasCodec);

		TestComposition parsed = templateParser.decode(template, reader, null);
		Assertions.assertNotNull(parsed);
		Assertions.assertEquals("tc", parsed.header);
		Assertions.assertEquals(1, parsed.type);
		Assertions.assertEquals(2, parsed.subtype.intValue());
		Assertions.assertEquals(1, parsed.sub.subtype);
		Assertions.assertEquals("a", parsed.sub.field1);
		Assertions.assertEquals("b", parsed.sub.field2);

		BitWriter writer = new BitWriter();
		templateParser.encode(template, writer, null, parsed);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

}
