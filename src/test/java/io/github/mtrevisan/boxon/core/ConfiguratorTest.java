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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.HeartbeatAcknowledgeASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ConfiguratorTest{

	@Test
	void getConfigurations() throws AnnotationException, ConfigurationException, CodecException, TemplateException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		List<Map<String, Object>> configurations = configurator.getConfigurations();

		Assertions.assertEquals(1, configurations.size());

		Map<String, Object> configuration = configurations.getFirst();
		String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.HEADER.toString()));
		String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.FIELDS.toString()));
		String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(configuration.get(
			ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES.toString()));

//		Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,shortDescription:AT+GTREG}", jsonHeader);
		Assertions.assertEquals(102, jsonHeader.length());
//		Assertions.assertEquals("{Operation mode report interval:{minValue:3600,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.21,fieldType:int},Maximum download retry count:{alternatives:[{maxProtocol:1.20,minValue:0,fieldType:int,maxValue:3,defaultValue:0},{minValue:0,fieldType:int,maxValue:3,minProtocol:1.21,defaultValue:1}]},Download timeout:{alternatives:[{maxProtocol:1.18,minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:10},{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,minProtocol:1.19,defaultValue:20}]},Random field:{defaultValue:27,fieldType:java.math.BigInteger},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE],fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:java.lang.String},Download protocol:{alternatives:[{maxProtocol:1.35,enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,defaultValue:HTTP,mutuallyExclusive:true},{enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,minProtocol:1.36,defaultValue:HTTPS,mutuallyExclusive:true}]},Download URL:{pattern:.{0,100},fields:{URL:{pattern:https?://.{0,92},fieldType:java.lang.String},password:{pattern:.{1,32},fieldType:java.lang.String},username:{pattern:.{1,32},fieldType:java.lang.String}},charset:UTF-8},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{charset:UTF-8,pattern:[0-9A-F]{4},fieldType:java.lang.String},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:java.lang.Integer},Motion report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:java.lang.String},Motionless report interval:{maxProtocol:1.20,minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,minProtocol:1.19,fieldType:int}}", jsonFields);
		Assertions.assertEquals(2229, jsonFields.length());
		Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
	}

	@Test
	void getProtocolVersionBoundaries() throws AnnotationException, ConfigurationException, TemplateException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfigurationsFrom(REGConfigurationASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		List<String> protocolVersionBoundaries = configurator.getProtocolVersionBoundaries();

		String jsonProtocolVersionBoundaries = PrettyPrintMap.toString(protocolVersionBoundaries);

		Assertions.assertEquals("[1.18,1.19,1.20,1.21,1.35,1.36,2.8]", jsonProtocolVersionBoundaries);
	}

	@Test
	void getConfigurationsByProtocol() throws AnnotationException, ConfigurationException, CodecException, TemplateException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		List<Map<String, Object>> configurations = configurator.getConfigurations("1.19");

		Assertions.assertEquals(1, configurations.size());

		Map<String, Object> configuration = configurations.getFirst();
		String jsonHeader = PrettyPrintMap.toString(configuration.get(ConfigurationKey.HEADER.toString()));
		String jsonFields = PrettyPrintMap.toString(configuration.get(ConfigurationKey.FIELDS.toString()));

//		Assertions.assertEquals("{longDescription:The command AT+GTREG is used to do things.,shortDescription:AT+GTREG}", jsonHeader);
		Assertions.assertEquals(86, jsonHeader.length());
//		Assertions.assertEquals("{Maximum download retry count:{minValue:0,fieldType:int,maxValue:3,defaultValue:0},Download timeout:{minValue:5,unitOfMeasure:min,fieldType:int,maxValue:30,defaultValue:20},Random field:{defaultValue:27,fieldType:java.math.BigInteger},Weekday:{defaultValue:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],enumeration:[MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},Update Over-The-Air:{defaultValue:FALSE,mutuallyExclusive:true,enumeration:[TRUE, FALSE],fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},Header:{charset:UTF-8,defaultValue:GTREG,fieldType:java.lang.String},Download protocol:{enumeration:[HTTP, HTTPS],fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,defaultValue:HTTP,mutuallyExclusive:true},Download URL:{pattern:.{0,100},fields:{URL:{pattern:https?://.{0,92},fieldType:java.lang.String},password:{pattern:.{1,32},fieldType:java.lang.String},username:{pattern:.{1,32},fieldType:java.lang.String}},charset:UTF-8},Update mode:{minValue:0,maxValue:1,defaultValue:1,fieldType:int},Message counter:{charset:UTF-8,pattern:[0-9A-F]{4},fieldType:java.lang.String},Operation mode:{minValue:0,maxValue:3,defaultValue:0,fieldType:java.lang.Integer},Motion report interval:{minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,fieldType:int},Password:{charset:UTF-8,defaultValue:gb200s,pattern:[0-9a-zA-Z]{4,20},fieldType:java.lang.String},Motionless report interval:{minValue:90,unitOfMeasure:s,maxValue:86400,defaultValue:3600,fieldType:int}}", jsonFields);
		Assertions.assertEquals(1611, jsonFields.length());
	}

	@Test
	void composeSingleConfigurationMessage() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withConfigurationsFrom(REGConfigurationASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		//data:
		Map<String, Object> configurationData = new HashMap<>(10);
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
		configurationData.put("Message counter", "007B");
		configurationData.put("Operation mode", 1);
		configurationData.put("Password", "pass");
		configurationData.put("Download timeout", 25);

		//compose:
		Response<String, byte[]> composeResult = configurator.composeConfiguration("1.20", "AT+GTREG",
			configurationData);

		if(composeResult.hasError())
			Assertions.fail(composeResult.getError());
		Assertions.assertEquals("AT+GTREG=pass,1,27,1,0,2,25,0,http://url.com@username@password,3600,3600,6,,007B$",
			new String(composeResult.getMessage(), StandardCharsets.US_ASCII));
	}

	@Test
	void composeSACK() throws TemplateException, ConfigurationException, AnnotationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withDefaultCodecs()
			.withConfiguration(HeartbeatAcknowledgeASCII.class)
			.create();
		Configurator configurator = Configurator.create(core);

		Map<String, Object> data = Map.of(
			"Device type code", (byte)0x87,
			"Protocol version", "0100",
			"Message counter", "007B"
		);
		Response<String, byte[]> composed = configurator.composeConfiguration("1.0", "Heartbeat acknowledge",
			data);

		if(composed.hasError()){
			Assertions.fail(composed.getError());
		}
		Assertions.assertEquals("+SACK:GTHBD,870100,007B$", StringHelper.toASCIIString(composed.getMessage()));
	}

}
