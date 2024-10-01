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
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class DescriberTest{

	@Test
	void describeContext() throws Exception{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContext("deviceTypes", deviceTypes)
			.withContext("string", "text")
			.withContext("number", 666)
			.withContext("array", new int[]{0, 1, 2, 3})
			.withContext(ParserTest.class.getDeclaredMethod("headerLength2", int.class))
			.withDefaultCodecs()
			.withTemplate(ACKMessageASCII.class)
			.create();
		Describer describer = Describer.create(core);

		List<Map<String, Object>> descriptions = describer.describeParsing();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = (Map<String, Object>)descriptions.getFirst()
			.get(DescriberKey.CONTEXT.toString());

		String jsonDescription = PrettyPrintMap.toString(description);
//		Assertions.assertEquals("{number:666,deviceTypes:[QUECLINK_GB200S(0x46)],string:text,array:[0, 1, 2, 3],headerLength2:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength2(int)}", jsonDescription);
		Assertions.assertEquals(173, jsonDescription.length());
	}

	@Test
	void describeParsing() throws Exception{
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
//		Assertions.assertEquals("{context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S(0x46)]},header:{end:$,charset:UTF-8,start:[+ACK:, +BCK:]},template:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII,fields:[{name:messageHeader,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,consumeTerminator:true,terminator:58},{name:messageType,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,consumeTerminator:true,terminator:44},{name:deviceTypeCode,fieldType:byte,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,size:2,charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToByteConverter},{name:protocolVersion,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringVersionConverter,consumeTerminator:true,terminator:44},{name:imei,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,consumeTerminator:true,terminator:44,validator:io.github.mtrevisan.boxon.annotations.validators.IMEIValidator},{name:latitude,fieldType:java.math.BigDecimal,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,converter:io.github.mtrevisan.boxon.annotations.converters.StringToBigDecimalConverter,consumeTerminator:true,terminator:44},{name:id,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,consumeTerminator:true,terminator:44},{name:correlationId,fieldType:short,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToShortConverter,consumeTerminator:true,terminator:44},{value:44,annotationType:io.github.mtrevisan.boxon.annotations.SkipUntilTerminator,consumeTerminator:true},{value:44,annotationType:io.github.mtrevisan.boxon.annotations.SkipUntilTerminator,consumeTerminator:true},{name:eventTime,fieldType:java.time.LocalDateTime,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$StringDateTimeYYYYMMDDHHMMSSConverter,consumeTerminator:true,terminator:44},{name:messageId,fieldType:short,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated,charset:UTF-8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$HexStringToShortConverter,consumeTerminator:false,terminator:36}],evaluatedFields:[{name:deviceTypeName,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,value:#deviceTypes.getDeviceTypeName(deviceTypeCode)},{name:receptionTime,fieldType:java.time.ZonedDateTime,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,value:T(java.time.ZonedDateTime).now()},{name:buffered,fieldType:boolean,annotationType:io.github.mtrevisan.boxon.annotations.Evaluate,value:messageHeader.startsWith('+B')}],postProcessedFields:[{name:messageHeader,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.PostProcess,condition:buffered,valueEncode:'+BCK',valueDecode:'+ACK'}]}", jsonDescription);
		Assertions.assertEquals(3567, jsonDescription.length());
	}

	@Test
	void describeTemplates() throws Exception{
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
//		Assertions.assertEquals("{context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S(0x46)]},header:{end:\n" + ",charset:UTF-8,start:[+ACK]},template:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex,fields:[{name:messageHeader,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,size:#headerLength(),charset:UTF-8},{name:messageType,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex$MessageTypeConverter},{name:mask,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex$ACKMaskConverter},{name:messageLength,fieldType:byte,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,condition:mask.hasLength(),byteOrder:BIG_ENDIAN},{name:deviceTypeCode,fieldType:byte,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,condition:mask.hasDeviceType(),byteOrder:BIG_ENDIAN},{name:protocolVersion,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:16,condition:mask.hasProtocolVersion(),byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter},{name:firmwareVersion,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:16,condition:mask.hasFirmwareVersion(),byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter},{name:imei,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,condition:mask.hasIMEI(),byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$IMEIConverter,validator:io.github.mtrevisan.boxon.annotations.validators.IMEIValidator,collectionType:io.github.mtrevisan.boxon.annotations.bindings.BindAsArray,collectionArraySize:8},{name:deviceName,fieldType:java.lang.String,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,size:8,charset:UTF-8,condition:!mask.hasIMEI()},{name:id,fieldType:byte,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,byteOrder:BIG_ENDIAN},{name:correlationId,fieldType:short,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:16,byteOrder:BIG_ENDIAN},{name:eventTime,fieldType:java.time.LocalDateTime,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:8,condition:mask.hasEventTime(),byteOrder:BIG_ENDIAN,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$DateTimeYYYYMMDDHHMMSSConverter,collectionType:io.github.mtrevisan.boxon.annotations.bindings.BindAsArray,collectionArraySize:7},{name:messageId,fieldType:short,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindInteger,size:16,condition:mask.hasMessageId(),byteOrder:BIG_ENDIAN},{name:checksum,fieldType:short,annotationType:io.github.mtrevisan.boxon.annotations.Checksum,byteOrder:BIG_ENDIAN,algorithm:io.github.mtrevisan.boxon.annotations.checksummers.CRC16CCITT_FALSE,skipStart:4,skipEnd:4}]}", jsonDescription);
		Assertions.assertEquals(3501, jsonDescription.length());
	}

	@Test
	void describeConfigurations() throws Exception{
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
//		Assertions.assertEquals("{context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S(0x46)]},header:{end:$,charset:UTF-8,start:AT+,longDescription:The command AT+GTREG is used to do things.,shortDescription:AT+GTREG,maxProtocol:2.8},configuration:io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII,fields:[{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:messageHeader,fieldType:java.lang.String,charset:UTF-8,defaultValue:GTREG,radix:10,shortDescription:Header,terminator:=},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:password,fieldType:java.lang.String,charset:UTF-8,pattern:[0-9a-zA-Z]{4,20},defaultValue:gb200s,radix:10,shortDescription:Password,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:operationMode,fieldType:java.lang.Integer,charset:UTF-8,defaultValue:0,radix:10,maxValue:3,minValue:0,shortDescription:Operation mode,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:randomField,fieldType:java.math.BigInteger,charset:UTF-8,defaultValue:27,radix:10,shortDescription:Random field,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:decimalField,fieldType:float,charset:UTF-8,defaultValue:3.1415928f,radix:10,shortDescription:Decimal field,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:updateOverTheAir,fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType,charset:UTF-8,enumeration:io.github.mtrevisan.boxon.annotations.configurations.BooleanType,defaultValue:FALSE,radix:10,shortDescription:Update Over-The-Air,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:updateMode,fieldType:int,charset:UTF-8,defaultValue:1,radix:10,maxValue:1,minValue:0,shortDescription:Update mode,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,name:maxDownloadRetryCount,fieldType:int,value:[{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:0,radix:10,maxValue:3,minValue:0,maxProtocol:1.20},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:1,radix:10,maxValue:3,minValue:0,minProtocol:1.21}],shortDescription:Maximum download retry count,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,name:downloadTimeout,fieldType:int,value:[{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:10,radix:10,maxValue:30,minValue:5,maxProtocol:1.18},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:20,radix:10,maxValue:30,minValue:5,minProtocol:1.19}],shortDescription:Download timeout,unitOfMeasure:min,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,name:downloadProtocol,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,value:[{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:HTTP,radix:10,maxProtocol:1.35},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField,charset:UTF-8,defaultValue:HTTPS,radix:10,minProtocol:1.36}],enumeration:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,shortDescription:Download protocol,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField,name:downloadURL,fieldType:java.lang.String,value:[{annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField,pattern:https?://.{0,92},shortDescription:URL},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField,pattern:.{1,32},shortDescription:username},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField,pattern:.{1,32},shortDescription:password}],charset:UTF-8,pattern:.{0,100},composition:${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>,shortDescription:Download URL,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:motionReportInterval,fieldType:int,charset:UTF-8,defaultValue:3600,radix:10,maxValue:86400,minValue:90,minProtocol:1.19,shortDescription:Motion report interval,unitOfMeasure:s,maxProtocol:1.20,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:motionlessReportInterval,fieldType:int,charset:UTF-8,defaultValue:3600,radix:10,maxValue:86400,minValue:90,minProtocol:1.19,shortDescription:Motionless report interval,unitOfMeasure:s,maxProtocol:1.20,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:operationModeReportInterval,fieldType:int,charset:UTF-8,defaultValue:3600,radix:10,maxValue:86400,minValue:3600,minProtocol:1.21,shortDescription:Operation mode report interval,unitOfMeasure:s,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:weekday,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday[],charset:UTF-8,enumeration:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday,defaultValue:MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY,radix:16,shortDescription:Weekday,terminator:,},{annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,name:messageCounter,fieldType:java.lang.String,charset:UTF-8,pattern:[0-9A-F]{4},radix:10,shortDescription:Message counter}],enumerations:[{name:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,values:[HTTP(0), HTTPS(2)]},{name:io.github.mtrevisan.boxon.annotations.configurations.BooleanType,values:[TRUE(1), FALSE(0)]},{name:io.github.mtrevisan.boxon.core.codecs.queclink.Weekday,values:[MONDAY(1), TUESDAY(2), WEDNESDAY(4), THURSDAY(8), FRIDAY(16), SATURDAY(32), SUNDAY(64)]}]}", jsonDescription);
		Assertions.assertEquals(6357, jsonDescription.length());
	}

}
