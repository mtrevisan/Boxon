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

import io.github.mtrevisan.boxon.annotations.BindBits;
import io.github.mtrevisan.boxon.annotations.BindByte;
import io.github.mtrevisan.boxon.annotations.BindObject;
import io.github.mtrevisan.boxon.annotations.BindString;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.helpers.BitSet;
import io.github.mtrevisan.boxon.helpers.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;

import java.nio.charset.StandardCharsets;


class ProtocolMessageParserTest{

	@Test
	void parseSingleMessageHex(){
		byte[] payload = ByteHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<ACKMessageHex> protocolMessage = ProtocolMessage.createFrom(ACKMessageHex.class, protocolMessageParser.loader);

		if(!protocolMessage.canBeDecoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageHex message = protocolMessageParser.decode(protocolMessage, reader, null);
		Evaluator.removeFromContext("deviceTypes");

		BitWriter writer = new BitWriter();
		protocolMessageParser.encode(protocolMessage, writer, null, message);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertEquals(new String(payload), new String(reconstructedMessage));
	}

	@Test
	void parseSingleMessageASCII(){
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<ACKMessageASCII> protocolMessage = ProtocolMessage.createFrom(ACKMessageASCII.class, protocolMessageParser.loader);

		if(!protocolMessage.canBeDecoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageASCII message = protocolMessageParser.decode(protocolMessage, reader, null);
		Evaluator.removeFromContext("deviceTypes");

		BitWriter writer = new BitWriter();
		protocolMessageParser.encode(protocolMessage, writer, null, message);
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
	void parseWithConditionError(){
		byte[] payload = ByteHelper.toByteArray("746335011234");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestError1> protocolMessage = ProtocolMessage.createFrom(TestError1.class, protocolMessageParser.loader);

		SpelEvaluationException exc = Assertions.assertThrows(SpelEvaluationException.class, () -> protocolMessageParser.decode(protocolMessage, reader, null));
		Assertions.assertEquals("EL1008E: Property or field 'e' cannot be found on object of type 'io.github.mtrevisan.boxon.codecs.ProtocolMessageParserTest$TestError1' - maybe not public or not valid?", exc.getMessage());
	}


	@MessageHeader(start = "te2")
	static class TestError2{
		@BindString(size = "3")
		public String header;
		@BindByte(match = "as")
		public byte type;
	}

	@Test
	void parseWithMatchError1(){
		byte[] payload = ByteHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestError2> protocolMessage = ProtocolMessage.createFrom(TestError2.class, protocolMessageParser.loader);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> protocolMessageParser.decode(protocolMessage, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Value `1` does not match constraint `as` in field TestError2.type", exc.getMessage());
	}


	@MessageHeader(start = "te3")
	static class TestError3{
		class WrongConverter implements Converter<Byte, String>{

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
		@BindByte(converter = WrongConverter.class)
		public byte type;
	}

	@Test
	void parseWithConverterOutputError(){
		byte[] payload = ByteHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestError3> protocolMessage = ProtocolMessage.createFrom(TestError3.class, protocolMessageParser.loader);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> protocolMessageParser.decode(protocolMessage, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Can not set byte field io.github.mtrevisan.boxon.codecs.ProtocolMessageParserTest$TestError3.type to java.lang.String in field TestError3.type", exc.getMessage());
	}


	@MessageHeader(start = "te4")
	static class TestError4{
		class WrongConverter implements Converter<String, Byte>{

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
		@BindByte(converter = WrongConverter.class)
		public byte type;
	}

	@Test
	void parseWithConverterInputError(){
		byte[] payload = ByteHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestError4> protocolMessage = ProtocolMessage.createFrom(TestError4.class, protocolMessageParser.loader);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> protocolMessageParser.decode(protocolMessage, reader, null));
		Assertions.assertEquals("ClassCastException: class java.lang.Byte cannot be cast to class java.lang.String (java.lang.Byte and java.lang.String are in module java.base of loader 'bootstrap') in field TestError4.type", exc.getMessage());
	}


	@MessageHeader(start = "te5")
	static class TestError5{
		@BindString(size = "3")
		public String header;
		@BindBits(size = "8", match = "[1]")
		public BitSet type;
	}

	@Test
	void parseWithMatchError2(){
		byte[] payload = ByteHelper.toByteArray("74633501");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestError5> protocolMessage = ProtocolMessage.createFrom(TestError5.class, protocolMessageParser.loader);

		Exception exc = Assertions.assertThrows(RuntimeException.class, () -> protocolMessageParser.decode(protocolMessage, reader, null));
		Assertions.assertEquals("IllegalArgumentException: Value `[0]` does not match constraint `[1]` in field TestError5.type", exc.getMessage());
	}



	@MessageHeader(start = "tc")
	static class TestComposition{
		static class TestSubComposition{
			@BindByte
			public byte subtype;
			@BindString(condition = "type == 1", size = "1")
			public String field1;
			@BindString(condition = "#self.subtype == 1", size = "1")
			public String field2;
		}
		@BindString(size = "2")
		public String header;
		@BindByte
		public byte type;
		@BindByte(condition = "type == 1")
		public Byte subtype;
		@BindObject(type = TestSubComposition.class)
		public TestSubComposition sub;
	}

	@Test
	void parseCompositeMessage(){
		byte[] payload = ByteHelper.toByteArray("74630102016162");
		BitReader reader = BitReader.wrap(payload);

		ProtocolMessageParser protocolMessageParser = new ProtocolMessageParser();
		protocolMessageParser.loader.loadCodecs();
		ProtocolMessage<TestComposition> protocolMessage = ProtocolMessage.createFrom(TestComposition.class, protocolMessageParser.loader);

		TestComposition parsed = protocolMessageParser.decode(protocolMessage, reader, null);
		Assertions.assertNotNull(parsed);
		Assertions.assertEquals("tc", parsed.header);
		Assertions.assertEquals(1, parsed.type);
		Assertions.assertEquals(2, parsed.subtype.intValue());
		Assertions.assertEquals(1, parsed.sub.subtype);
		Assertions.assertEquals("a", parsed.sub.field1);
		Assertions.assertEquals("b", parsed.sub.field2);

		BitWriter writer = new BitWriter();
		protocolMessageParser.encode(protocolMessage, writer, null, parsed);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

}
