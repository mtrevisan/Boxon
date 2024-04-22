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

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class DescriberTest{

	@Test
	void describeParsing() throws FieldException, NoSuchMethodException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageASCII.class)
			.create();
		Describer describer = Describer.create(core);

		List<Map<String, Object>> descriptions = describer.describeParsing();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = descriptions.getFirst();

		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{template:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII,context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S (0x46)]},header:{start:[+ACK:, +BCK:],charset:UTF-8,end:$},evaluatedFields:[{name:deviceTypeName,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,fieldType:java.lang.String,value:#deviceTypes.getDeviceTypeName(deviceTypeCode)},{name:receptionTime,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,fieldType:java.time.ZonedDateTime,value:T(java.time.ZonedDateTime).now()},{name:buffered,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,fieldType:boolean,value:messageHeader.startsWith('+B')}],fields:[{name:messageHeader,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,terminator:58,consumeTerminator:true,fieldType:java.lang.String},{name:messageType,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,terminator:44,consumeTerminator:true,fieldType:java.lang.String},{name:deviceTypeCode,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,charset:UTF-8,size:2,fieldType:byte,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToByteConverter},{charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringVersionConverter,name:protocolVersion,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:44,consumeTerminator:true,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version},{charset:UTF-8,name:imei,validator:io.github.mtrevisan.boxon.annotations.validators.IMEIValidator,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:44,consumeTerminator:true,fieldType:java.lang.String},{charset:UTF-8,converter:io.github.mtrevisan.boxon.annotations.converters.StringToBigDecimalConverter,name:latitude,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:44,consumeTerminator:true,fieldType:java.math.BigDecimal},{name:id,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,terminator:44,consumeTerminator:true,fieldType:java.lang.String},{charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToShortConverter,name:correlationId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:44,consumeTerminator:true,fieldType:short},{annotationType:io.github.mtrevisan.boxon.annotations.SkipUntilTerminator,terminator:44,consumeTerminator:true},{annotationType:io.github.mtrevisan.boxon.annotations.SkipUntilTerminator,terminator:44,consumeTerminator:true},{charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$StringDateTimeYYYYMMDDHHMMSSConverter,name:eventTime,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:44,consumeTerminator:true,fieldType:java.time.LocalDateTime},{charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToShortConverter,name:messageId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,terminator:36,consumeTerminator:false,fieldType:short}],postProcessedFields:[{name:messageHeader,valueEncode:'+BCK',annotationType:io.github.mtrevisan.boxon.annotations.PostProcess,condition:buffered,valueDecode:'+ACK',fieldType:java.lang.String}]}", jsonDescription);
		Assertions.assertEquals(3578, jsonDescription.length());
	}

	@Test
	void describeTemplates() throws FieldException, NoSuchMethodException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.create();
		Describer describer = Describer.create(core);

		List<Map<String, Object>> descriptions = describer.describeTemplate();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = descriptions.getFirst();

		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S (0x46)]},template:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex,header:{start:[+ACK],charset:UTF-8,end:\n" + "},fields:[{name:messageHeader,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,charset:UTF-8,size:#headerLength(),fieldType:java.lang.String},{name:messageType,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,fieldType:java.lang.String,byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex$MessageTypeConverter},{name:mask,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex,byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex$ACKMaskConverter},{name:messageLength,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,condition:mask.hasLength(),size:8,fieldType:byte,byteOrder:BIG_ENDIAN},{name:deviceTypeCode,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,condition:mask.hasDeviceType(),size:8,fieldType:byte,byteOrder:BIG_ENDIAN},{condition:mask.hasProtocolVersion(),size:16,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter,name:protocolVersion,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,byteOrder:BIG_ENDIAN},{condition:mask.hasFirmwareVersion(),size:16,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter,name:firmwareVersion,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,byteOrder:BIG_ENDIAN},{collectionType:io.github.mtrevisan.boxon.annotations.bindings.BindAsArray,condition:mask.hasIMEI(),size:8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$IMEIConverter,name:imei,validator:io.github.mtrevisan.boxon.annotations.validators.IMEIValidator,collectionArraySize:8,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,fieldType:java.lang.String,byteOrder:BIG_ENDIAN},{name:deviceName,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,charset:UTF-8,condition:!mask.hasIMEI(),size:8,fieldType:java.lang.String},{name:id,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,fieldType:byte,byteOrder:BIG_ENDIAN},{name:correlationId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:16,fieldType:short,byteOrder:BIG_ENDIAN},{collectionType:io.github.mtrevisan.boxon.annotations.bindings.BindAsArray,condition:mask.hasEventTime(),size:8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$DateTimeYYYYMMDDHHMMSSConverter,name:eventTime,collectionArraySize:7,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,fieldType:java.time.LocalDateTime,byteOrder:BIG_ENDIAN},{name:messageId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,condition:mask.hasMessageId(),size:16,fieldType:short,byteOrder:BIG_ENDIAN},{skipEnd:4,skipStart:4,name:checksum,annotationType:io.github.mtrevisan.boxon.annotations.Checksum,fieldType:short,byteOrder:BIG_ENDIAN,algorithm:io.github.mtrevisan.boxon.annotations.checksummers.CRC16CCITT_FALSE}]}", jsonDescription);
		Assertions.assertEquals(3502, jsonDescription.length());
	}

	@Test
	void describeConfigurations() throws FieldException, NoSuchMethodException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();
		Describer describer = Describer.create(core);

		List<Map<String, Object>> descriptions = describer.describeConfiguration();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = descriptions.getFirst();

		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S (0x46)]},header:{start:AT+,maxProtocol:2.8,longDescription:The command AT+GTREG is used to do things.,charset:UTF-8,end:$,shortDescription:AT+GTREG},fields:[{radix:10,charset:UTF-8,defaultValue:GTREG,name:messageHeader,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:=,shortDescription:Header,fieldType:java.lang.String},{radix:10,charset:UTF-8,defaultValue:gb200s,name:password,pattern:[0-9a-zA-Z]{4,20},annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Password,fieldType:java.lang.String},{radix:10,charset:UTF-8,minValue:0,defaultValue:0,maxValue:3,name:operationMode,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Operation mode,fieldType:java.lang.Integer},{radix:10,charset:UTF-8,defaultValue:27,name:randomField,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Random field,fieldType:java.math.BigInteger},{radix:10,charset:UTF-8,defaultValue:3.1415928f,name:decimalField,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Decimal field,fieldType:float},{radix:10,charset:UTF-8,defaultValue:FALSE,name:updateOverTheAir,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Update Over-The-Air,enumeration:io.github.mtrevisan.boxon.annotations.configurations.BooleanType,fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},{radix:10,charset:UTF-8,minValue:0,defaultValue:1,maxValue:1,name:updateMode,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Update mode,fieldType:int},{name:maxDownloadRetryCount,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Maximum download retry count,fieldType:int,value:[{radix:10,maxProtocol:1.20,charset:UTF-8,minValue:0,defaultValue:0,maxValue:3},{radix:10,charset:UTF-8,minValue:0,defaultValue:1,maxValue:3,minProtocol:1.21}]},{unitOfMeasure:min,name:downloadTimeout,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Download timeout,fieldType:int,value:[{radix:10,maxProtocol:1.18,charset:UTF-8,minValue:5,defaultValue:10,maxValue:30},{radix:10,charset:UTF-8,minValue:5,defaultValue:20,maxValue:30,minProtocol:1.19}]},{name:downloadProtocol,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Download protocol,enumeration:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,value:[{radix:10,maxProtocol:1.35,charset:UTF-8,defaultValue:HTTP},{radix:10,charset:UTF-8,defaultValue:HTTPS,minProtocol:1.36}]},{charset:UTF-8,composition:${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>,name:downloadURL,pattern:.{0,100},annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField,terminator:,,shortDescription:Download URL,fieldType:java.lang.String,value:[{pattern:https?://.{0,92},shortDescription:URL},{pattern:.{1,32},shortDescription:username},{pattern:.{1,32},shortDescription:password}]},{charset:UTF-8,unitOfMeasure:s,defaultValue:3600,maxValue:86400,minProtocol:1.19,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Motion report interval,radix:10,maxProtocol:1.20,minValue:90,name:motionReportInterval,terminator:,,fieldType:int},{charset:UTF-8,unitOfMeasure:s,defaultValue:3600,maxValue:86400,minProtocol:1.19,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Motionless report interval,radix:10,maxProtocol:1.20,minValue:90,name:motionlessReportInterval,terminator:,,fieldType:int},{radix:10,charset:UTF-8,minValue:3600,unitOfMeasure:s,defaultValue:3600,maxValue:86400,minProtocol:1.21,name:operationModeReportInterval,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Operation mode report interval,fieldType:int},{radix:16,charset:UTF-8,defaultValue:MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY,name:weekday,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Weekday,enumeration:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[]},{radix:10,charset:UTF-8,name:messageCounter,pattern:[0-9A-F]{4},annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Message counter,fieldType:java.lang.String}],configuration:io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII}", jsonDescription);
		Assertions.assertEquals(5202, jsonDescription.length());
	}

}
