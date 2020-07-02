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
package unit731.boxon.coders;

import unit731.boxon.coders.queclink.ACKMessageHex;
import unit731.boxon.coders.queclink.ACKMessageASCII;
import unit731.boxon.coders.queclink.DeviceTypes;
import unit731.boxon.helpers.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


class MessageParserTest{

	@Test
	void parseSingleMessageHex(){
		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);

		MessageParser messageParser = new MessageParser();
		messageParser.getLoader().loadCoders();
		Codec<ACKMessageHex> codec = Codec.createFrom(ACKMessageHex.class);

		if(!codec.canBeDecoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageHex message = messageParser.decode(codec, reader);

		BitWriter writer = new BitWriter();
		messageParser.encode(codec, message, writer);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

	@Test
	void parseSingleMessageASCII(){
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		BitBuffer reader = BitBuffer.wrap(payload);

		MessageParser messageParser = new MessageParser();
		messageParser.getLoader().loadCoders();
		Codec<ACKMessageASCII> codec = Codec.createFrom(ACKMessageASCII.class);

		if(!codec.canBeDecoded())
			Assertions.fail("Cannot decode message");

		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Evaluator.addToContext("deviceTypes", deviceTypes);
		ACKMessageASCII message = messageParser.decode(codec, reader);

		BitWriter writer = new BitWriter();
		messageParser.encode(codec, message, writer);
		byte[] reconstructedMessage = writer.array();

		Assertions.assertArrayEquals(payload, reconstructedMessage);
	}

	@Test
	void evaluate(){
		Integer result = Evaluator.evaluate("8/4", Integer.class, null);

		Assertions.assertEquals(2, result);
	}

}
