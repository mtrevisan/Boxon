package unit731.boxon.codecs;

import unit731.boxon.codecs.queclink.ACKMessage;
import unit731.boxon.codecs.queclink.ASCII_ACKMessage;
import unit731.boxon.dto.DeviceTypes;
import unit731.boxon.utils.ByteHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


class MessageParserTest{

	@Test
	void parseSingleMessageHex(){
		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitBuffer reader = BitBuffer.wrap(payload);

		Codec<ACKMessage> codec = Codec.createFrom(ACKMessage.class);

		if(codec.canBeDecoded()){
			DeviceTypes deviceTypes = new DeviceTypes();
			deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
			Evaluator.addToContext("deviceTypes", deviceTypes);
			ACKMessage message = MessageParser.decode(codec, reader);

			BitWriter writer = new BitWriter();
			MessageParser.encode(codec, message, writer);
			byte[] reconstructedMessage = writer.array();

			Assertions.assertArrayEquals(payload, reconstructedMessage);
		}
		else
			Assertions.fail("Cannot decode message");
	}

	@Test
	void parseSingleMessageASCII(){
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		BitBuffer reader = BitBuffer.wrap(payload);

		Codec<ASCII_ACKMessage> codec = Codec.createFrom(ASCII_ACKMessage.class);

		if(codec.canBeDecoded()){
			DeviceTypes deviceTypes = new DeviceTypes();
			deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
			Evaluator.addToContext("deviceTypes", deviceTypes);
			ASCII_ACKMessage message = MessageParser.decode(codec, reader);

			BitWriter writer = new BitWriter();
			MessageParser.encode(codec, message, writer);
			byte[] reconstructedMessage = writer.array();

			Assertions.assertArrayEquals(payload, reconstructedMessage);
		}
		else
			Assertions.fail("Cannot decode message");
	}

	@Test
	void evaluate(){
		Integer result = Evaluator.evaluate("8/4", Integer.class, null);

		Assertions.assertEquals(2, result);
	}

}
