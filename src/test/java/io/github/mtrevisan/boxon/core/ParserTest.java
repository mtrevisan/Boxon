/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mtrevisan.boxon.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.configurations.ConfigurationKey;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.internal.TimeWatch;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
class ParserTest{

	public static void main(String[] args) throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withDefaultConfigurations()
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		//~245-265 µs/msg = 4.1-3.8 kHz
		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		//warm-up
		for(int i = 0; i < 2_000; i ++)
			parser.parse(payload);
		TimeWatch watch = TimeWatch.start();
		for(int i = 0; i < 20_000; i ++)
			parser.parse(payload);
		watch.stop();
		System.out.println(watch.toStringMicros(20_000));
	}

	@Test
	void parseAndComposeSingleMessageHex() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.addToContext("deviceTypes", deviceTypes)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withContextFunction(ParserTest.class, "headerSize");

		//parse:
		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse composeResult = parser.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	private static int headerSize(){
		return 4;
	}

	@Test
	void parseMultipleMessagesHex() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withDefaultTemplates()
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseAndComposeSingleMessageASCII() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		//parse:
		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse parseResult = parser.parse(payload);

		Assertions.assertFalse(parseResult.hasErrors());
		Assertions.assertEquals(1, parseResult.getParsedMessageCount());

		//compose:
		ComposeResponse composeResult = parser.composeMessage(parseResult.getParsedMessageAt(0));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertArrayEquals(payload, composeResult.getComposedMessage());
	}

	@Test
	void parseMultipleMessagesASCII() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		byte[] payload = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseMultipleMessagesHexASCII() throws NoSuchMethodException, AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"));

		byte[] payload1 = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload1, payload2);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}

	@Test
	void parseMultipleMessagesASCIIHex() throws AnnotationException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		deviceTypes.add("QUECLINK_GV350M", (byte)0xCF);
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Parser parser = Parser.create()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplates(ACKMessageHex.class);

		byte[] payload1 = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = "+ACK:GTIOB,CF8002,359464038116666,GV350MG,2,0020,20170101123542,11F0$".getBytes(StandardCharsets.ISO_8859_1);
		byte[] payload = ArrayUtils.addAll(payload2, payload1);
		ParseResponse result = parser.parse(payload);

		Assertions.assertFalse(result.hasErrors());
		Assertions.assertEquals(2, result.getParsedMessageCount());
	}


	@Test
	void getConfigurations() throws AnnotationException, ConfigurationException, JsonProcessingException, CodecException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withConfigurations(REGConfigurationASCII.class);

		List<Map<String, Object>> configurations = parser.getConfigurations();

		Assertions.assertEquals(1, configurations.size());

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> configuration = configurations.get(0);
		String jsonHeader = mapper.writeValueAsString(configuration.get(ConfigurationKey.CONFIGURATION_HEADER.toString()));
		String jsonFields = mapper.writeValueAsString(configuration.get(ConfigurationKey.CONFIGURATION_FIELDS.toString()));
		String jsonProtocolVersionBoundaries = mapper.writeValueAsString(configuration.get(
			ConfigurationKey.CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES.toString()));

		Assertions.assertEquals("{\"longDescription\":\"The command AT+GTREG is used to do things.\",\"maxProtocol\":\"2.8\",\"shortDescription\":\"AT+GTREG\"}", jsonHeader);
		Assertions.assertEquals("{\"Operation mode report interval\":{\"minValue\":3600,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600,\"minProtocol\":\"1.21\",\"fieldType\":\"int\"},\"Maximum download retry count\":{\"alternatives\":[{\"maxProtocol\":\"1.20\",\"minValue\":0,\"fieldType\":\"int\",\"maxValue\":3,\"defaultValue\":0},{\"minValue\":0,\"fieldType\":\"int\",\"maxValue\":3,\"minProtocol\":\"1.21\",\"defaultValue\":1}]},\"Download timeout\":{\"alternatives\":[{\"maxProtocol\":\"1.18\",\"minValue\":5,\"unitOfMeasure\":\"min\",\"fieldType\":\"int\",\"maxValue\":30,\"defaultValue\":10},{\"minValue\":5,\"unitOfMeasure\":\"min\",\"fieldType\":\"int\",\"maxValue\":30,\"minProtocol\":\"1.19\",\"defaultValue\":20}]},\"Weekday\":{\"defaultValue\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"enumeration\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]},\"Update Over-The-Air\":{\"defaultValue\":\"FALSE\",\"mutuallyExclusive\":true,\"enumeration\":[\"TRUE\",\"FALSE\"]},\"Header\":{\"charset\":\"UTF-8\",\"defaultValue\":\"GTREG\",\"fieldType\":\"String\"},\"Download protocol\":{\"alternatives\":[{\"maxProtocol\":\"1.35\",\"enumeration\":[\"HTTP\",\"HTTPS\"],\"defaultValue\":\"HTTP\",\"mutuallyExclusive\":true},{\"enumeration\":[\"HTTP\",\"HTTPS\"],\"minProtocol\":\"1.36\",\"defaultValue\":\"HTTP\",\"mutuallyExclusive\":true}]},\"Download URL\":{\"pattern\":\".{0,100}\",\"charset\":\"UTF-8\",\"fields\":{\"URL\":{\"pattern\":\"https?://.{0,92}\",\"fieldType\":\"String\"},\"password\":{\"pattern\":\".{1,32}\",\"fieldType\":\"String\"},\"username\":{\"pattern\":\".{1,32}\",\"fieldType\":\"String\"}}},\"Update mode\":{\"minValue\":0,\"maxValue\":1,\"defaultValue\":1,\"fieldType\":\"int\"},\"Message counter\":{\"minValue\":0,\"maxValue\":65535,\"fieldType\":\"int\"},\"Operation mode\":{\"minValue\":0,\"maxValue\":3,\"defaultValue\":0,\"fieldType\":\"int\"},\"Motion report interval\":{\"maxProtocol\":\"1.20\",\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600,\"minProtocol\":\"1.19\",\"fieldType\":\"int\"},\"Password\":{\"charset\":\"UTF-8\",\"defaultValue\":\"gb200s\",\"pattern\":\"[0-9a-zA-Z]{4,20}\",\"fieldType\":\"String\"},\"Motionless report interval\":{\"maxProtocol\":\"1.20\",\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600,\"minProtocol\":\"1.19\",\"fieldType\":\"int\"}}", jsonFields);
		Assertions.assertEquals("[\"1.18\",\"1.19\",\"1.20\",\"1.21\",\"1.35\",\"1.36\",\"2.8\"]", jsonProtocolVersionBoundaries);
	}

	@Test
	void getProtocolVersionBoundaries() throws AnnotationException, ConfigurationException, JsonProcessingException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withConfigurations(REGConfigurationASCII.class);

		List<String> protocolVersionBoundaries = parser.getProtocolVersionBoundaries();

		ObjectMapper mapper = new ObjectMapper();
		String jsonProtocolVersionBoundaries = mapper.writeValueAsString(protocolVersionBoundaries);

		Assertions.assertEquals("[\"1.18\",\"1.19\",\"1.20\",\"1.21\",\"1.35\",\"1.36\",\"2.8\"]", jsonProtocolVersionBoundaries);
	}

	@Test
	void getConfigurationsByProtocol() throws AnnotationException, ConfigurationException, JsonProcessingException, CodecException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withConfigurations(REGConfigurationASCII.class);

		List<Map<String, Object>> configurations = parser.getConfigurations("1.19");

		Assertions.assertEquals(1, configurations.size());

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> configuration = configurations.get(0);
		String jsonHeader = mapper.writeValueAsString(configuration.get(ConfigurationKey.CONFIGURATION_HEADER.toString()));
		String jsonFields = mapper.writeValueAsString(configuration.get(ConfigurationKey.CONFIGURATION_FIELDS.toString()));

		Assertions.assertEquals("{\"longDescription\":\"The command AT+GTREG is used to do things.\",\"shortDescription\":\"AT+GTREG\"}", jsonHeader);
		Assertions.assertEquals("{\"Maximum download retry count\":{\"minValue\":0,\"fieldType\":\"int\",\"maxValue\":3,\"defaultValue\":0},\"Download timeout\":{\"minValue\":5,\"unitOfMeasure\":\"min\",\"fieldType\":\"int\",\"maxValue\":30,\"defaultValue\":20},\"Weekday\":{\"defaultValue\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"enumeration\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]},\"Update Over-The-Air\":{\"defaultValue\":\"FALSE\",\"mutuallyExclusive\":true,\"enumeration\":[\"TRUE\",\"FALSE\"]},\"Header\":{\"charset\":\"UTF-8\",\"defaultValue\":\"GTREG\",\"fieldType\":\"String\"},\"Download protocol\":{\"enumeration\":[\"HTTP\",\"HTTPS\"],\"defaultValue\":\"HTTP\",\"mutuallyExclusive\":true},\"Download URL\":{\"pattern\":\".{0,100}\",\"charset\":\"UTF-8\",\"fields\":{\"URL\":{\"pattern\":\"https?://.{0,92}\",\"fieldType\":\"String\"},\"password\":{\"pattern\":\".{1,32}\",\"fieldType\":\"String\"},\"username\":{\"pattern\":\".{1,32}\",\"fieldType\":\"String\"}}},\"Update mode\":{\"minValue\":0,\"maxValue\":1,\"defaultValue\":1,\"fieldType\":\"int\"},\"Message counter\":{\"minValue\":0,\"maxValue\":65535,\"fieldType\":\"int\"},\"Operation mode\":{\"minValue\":0,\"maxValue\":3,\"defaultValue\":0,\"fieldType\":\"int\"},\"Motion report interval\":{\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600,\"fieldType\":\"int\"},\"Password\":{\"charset\":\"UTF-8\",\"defaultValue\":\"gb200s\",\"pattern\":\"[0-9a-zA-Z]{4,20}\",\"fieldType\":\"String\"},\"Motionless report interval\":{\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600,\"fieldType\":\"int\"}}", jsonFields);
	}

	@Test
	void composeSingleConfigurationMessage() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withConfigurations(REGConfigurationASCII.class);

		//data:
		Map<String, Object> configurationData = new HashMap<>();
		configurationData.put("Weekday", "TUESDAY|WEDNESDAY");
		configurationData.put("Update Over-The-Air", "TRUE");
		configurationData.put("Header", "GTREG");
		configurationData.put("Download protocol", "HTTP");
		configurationData.put("Download URL", Map.of(
			"URL", "http://url.com",
			"username", "username",
			"password", "password"
		));
		configurationData.put("Update mode", 0);
		configurationData.put("Maximum download retry count", 2);
		configurationData.put("Message counter", 123);
		configurationData.put("Operation mode", 1);
		configurationData.put("Password", "pass");
		configurationData.put("Download timeout", 25);

		//compose:
		ComposeResponse composeResult = parser.composeConfiguration("1.20", Collections.singletonMap("AT+", configurationData));

		Assertions.assertFalse(composeResult.hasErrors());
		Assertions.assertEquals("AT+GTREG=pass,1,1,0,2,25,0,http://url.com@username@password,3600,3600,6,,7b$",
			new String(composeResult.getComposedMessage()));
	}


	@Test
	void getDescription() throws AnnotationException, ConfigurationException, JsonProcessingException, CodecException, TemplateException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		Parser parser = Parser.create()
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class);

		List<Map<String, Object>> descriptions = parser.describeTemplates();

		Assertions.assertEquals(1, descriptions.size());

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> description = descriptions.get(0);
		String jsonDescription = mapper.writeValueAsString(description);

		Assertions.assertEquals("{\"fields\":[{\"charset\":\"UTF-8\",\"size\":\"#headerSize()\",\"name\":\"messageHeader\",\"annotationType\":\"BindString\",\"fieldType\":\"String\",\"condition\":\"bla\",\"validator\":\"class\",\"converter\":\"class\",\"selectConverterFrom\":[{\"condition\":\"cond1\",\"converter\":\"conv1\"},{\"condition\":\"cond2\",\"converter\":\"conv2\"}]}, ..., \"header\":{\"charset\":\"UTF-8\",\"start\":[\"+ACK\"],\"end\":\"\\r\\n\"}]}", jsonDescription);
	}

}
