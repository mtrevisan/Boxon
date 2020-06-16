package unit731.boxon.codecs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import unit731.boxon.dto.DeviceTypes;
import unit731.boxon.dto.ParseResponse;
import unit731.boxon.utils.ByteHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;


class ParserTest{

	private static final ObjectMapper OM = new ObjectMapper();
	static{
		OM.registerModule(new JavaTimeModule());
		OM.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}


	@Test
	void parseMultipleMessagesHex() throws JsonProcessingException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = new Parser(context);

		byte[] payload = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse result = parser.parse(payload);

System.out.println(OM.writeValueAsString(result));
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseMultipleMessagesASCII() throws JsonProcessingException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = new Parser(context);

		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse result = parser.parse(payload);

System.out.println(OM.writeValueAsString(result));
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseMultipleMessagesHexASCII() throws JsonProcessingException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = new Parser(context);

		byte[] payload1 = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload1, payload2);
		ParseResponse result = parser.parse(payload);

System.out.println(OM.writeValueAsString(result));
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

	@Test
	void parseMultipleMessagesASCIIHex() throws JsonProcessingException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = new Parser(context);

		byte[] payload1 = ByteHelper.hexStringToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload2, payload1);
		ParseResponse result = parser.parse(payload);

System.out.println(OM.writeValueAsString(result));
		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessages().size());
	}

}
