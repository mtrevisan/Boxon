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

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class DescriptorTest{

	@Test
	void describeTemplates() throws AnnotationException, ConfigurationException, TemplateException, NoSuchMethodException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContextPair("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.create();
		Descriptor descriptor = Descriptor.create(core);

		List<Map<String, Object>> descriptions = descriptor.describeTemplate();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = descriptions.getFirst();

		String jsonDescription = PrettyPrintMap.toString(description);
		Assertions.assertEquals("{template:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex,context:{headerLength:private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength(),deviceTypes:[QUECLINK_GB200S (0x46)]},header:{start:[+ACK],charset:UTF-8},fields:[{charset:UTF-8,size:#headerLength(),name:messageHeader,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,fieldType:java.lang.String},{converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex$MessageTypeConverter,name:messageType,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindByte,fieldType:java.lang.String},{converter:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex$ACKMaskConverter,name:mask,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindByte,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.ACKMaskHex},{condition:mask.hasLength(),name:messageLength,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindByte,fieldType:byte},{condition:mask.hasDeviceType(),name:deviceTypeCode,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindByte,fieldType:byte},{condition:mask.hasProtocolVersion(),size:2,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter,name:protocolVersion,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive,type:byte,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,byteOrder:BIG_ENDIAN},{condition:mask.hasFirmwareVersion(),size:2,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter,name:firmwareVersion,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive,type:byte,fieldType:io.github.mtrevisan.boxon.semanticversioning.Version,byteOrder:BIG_ENDIAN},{condition:mask.hasIMEI(),size:8,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$IMEIConverter,name:imei,validator:io.github.mtrevisan.boxon.annotations.validators.IMEIValidator,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive,type:byte,fieldType:java.lang.String,byteOrder:BIG_ENDIAN},{charset:UTF-8,condition:!mask.hasIMEI(),size:8,name:deviceName,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindString,fieldType:java.lang.String},{name:id,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindByte,fieldType:byte},{name:correlationId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindShort,fieldType:short,byteOrder:BIG_ENDIAN},{condition:mask.hasEventTime(),size:7,converter:io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$DateTimeYYYYMMDDHHMMSSConverter,name:eventTime,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive,type:byte,fieldType:java.time.ZonedDateTime,byteOrder:BIG_ENDIAN},{condition:mask.hasMessageId(),name:messageId,annotationType:io.github.mtrevisan.boxon.annotations.bindings.BindShort,fieldType:short,byteOrder:BIG_ENDIAN},{skipEnd:4,skipStart:4,name:checksum,annotationType:io.github.mtrevisan.boxon.annotations.Checksum,startValue:-1,type:short,fieldType:short,byteOrder:BIG_ENDIAN,algorithm:io.github.mtrevisan.boxon.annotations.checksummers.CRC16CCITT}]}", jsonDescription);
	}

	@Test
	void describeConfigurations() throws AnnotationException, TemplateException, ConfigurationException, NoSuchMethodException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
			.withContextPair("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withConfigurationsFrom(REGConfigurationASCII.class)
			.create();
		Descriptor descriptor = Descriptor.create(core);

		List<Map<String, Object>> descriptions = descriptor.describeConfiguration();

		Assertions.assertEquals(1, descriptions.size());

		Map<String, Object> description = descriptions.getFirst();

		String jsonDescription = PrettyPrintMap.toString(description);
		Assertions.assertEquals("{fields:[{radix:10,charset:UTF-8,defaultValue:GTREG,name:messageHeader,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:=,shortDescription:Header,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:java.lang.String},{radix:10,charset:UTF-8,defaultValue:gb200s,name:password,pattern:[0-9a-zA-Z]{4,20},annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Password,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:java.lang.String},{radix:10,charset:UTF-8,minValue:0,maxValue:3,defaultValue:0,name:operationMode,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Operation mode,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:java.lang.Integer},{radix:10,charset:UTF-8,defaultValue:FALSE,name:updateOverTheAir,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Update Over-The-Air,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.BooleanType,fieldType:io.github.mtrevisan.boxon.annotations.configurations.BooleanType},{radix:10,charset:UTF-8,minValue:0,maxValue:1,defaultValue:1,name:updateMode,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Update mode,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int},{name:maxDownloadRetryCount,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Maximum download retry count,selectConverterFrom:[{maxProtocol:1.20,radix:10,charset:UTF-8,minValue:0,maxValue:3,defaultValue:0},{radix:10,charset:UTF-8,minValue:0,minProtocol:1.21,maxValue:3,defaultValue:1}],enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int,value:[@io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"1.20\", charset=\"UTF-8\", minValue=\"0\", unitOfMeasure=\"\", defaultValue=\"0\", maxValue=\"3\", minProtocol=\"\", pattern=\"\"), @io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"\", charset=\"UTF-8\", minValue=\"0\", unitOfMeasure=\"\", defaultValue=\"1\", maxValue=\"3\", minProtocol=\"1.21\", pattern=\"\")]},{unitOfMeasure:min,name:downloadTimeout,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Download timeout,selectConverterFrom:[{maxProtocol:1.18,radix:10,charset:UTF-8,minValue:5,maxValue:30,defaultValue:10},{radix:10,charset:UTF-8,minValue:5,minProtocol:1.19,maxValue:30,defaultValue:20}],enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int,value:[@io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"1.18\", charset=\"UTF-8\", minValue=\"5\", unitOfMeasure=\"\", defaultValue=\"10\", maxValue=\"30\", minProtocol=\"\", pattern=\"\"), @io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"\", charset=\"UTF-8\", minValue=\"5\", unitOfMeasure=\"\", defaultValue=\"20\", maxValue=\"30\", minProtocol=\"1.19\", pattern=\"\")]},{name:downloadProtocol,annotationType:io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField,terminator:,,shortDescription:Download protocol,selectConverterFrom:[{maxProtocol:1.35,radix:10,charset:UTF-8,defaultValue:HTTP},{radix:10,charset:UTF-8,minProtocol:1.36,defaultValue:HTTPS}],enumeration:class io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,fieldType:io.github.mtrevisan.boxon.core.codecs.queclink.DownloadProtocol,value:[@io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"1.35\", charset=\"UTF-8\", minValue=\"\", unitOfMeasure=\"\", defaultValue=\"HTTP\", maxValue=\"\", minProtocol=\"\", pattern=\"\"), @io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField(radix=10, longDescription=\"\", maxProtocol=\"\", charset=\"UTF-8\", minValue=\"\", unitOfMeasure=\"\", defaultValue=\"HTTPS\", maxValue=\"\", minProtocol=\"1.36\", pattern=\"\")]},{charset:UTF-8,composition:${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>,name:downloadURL,pattern:.{0,100},annotationType:io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField,terminator:,,shortDescription:Download URL,selectConverterFrom:[{pattern:https?://.{0,92},shortDescription:URL},{pattern:.{1,32},shortDescription:username},{pattern:.{1,32},shortDescription:password}],fieldType:java.lang.String},{maxProtocol:1.18,annotationType:ConfigurationSkip,terminator:,},{charset:UTF-8,unitOfMeasure:s,minProtocol:1.19,maxValue:86400,defaultValue:3600,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Motion report interval,maxProtocol:1.20,radix:10,minValue:90,name:motionReportInterval,terminator:,,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int},{annotationType:ConfigurationSkip,terminator:,,minProtocol:1.21},{maxProtocol:1.18,annotationType:ConfigurationSkip,terminator:,},{charset:UTF-8,unitOfMeasure:s,minProtocol:1.19,maxValue:86400,defaultValue:3600,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Motionless report interval,maxProtocol:1.20,radix:10,minValue:90,name:motionlessReportInterval,terminator:,,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int},{charset:UTF-8,unitOfMeasure:s,minProtocol:1.21,maxValue:86400,defaultValue:3600,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Operation mode report interval,radix:10,minValue:3600,name:operationModeReportInterval,terminator:,,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:int},{radix:16,charset:UTF-8,defaultValue:MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY,name:weekday,annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,terminator:,,shortDescription:Weekday,enumeration:class io.github.mtrevisan.boxon.core.codecs.queclink.Weekday,fieldType:[Lio.github.mtrevisan.boxon.core.codecs.queclink.Weekday;},{annotationType:ConfigurationSkip,terminator:,},{radix:10,charset:UTF-8,name:messageCounter,pattern:[0-9A-F]{4},annotationType:io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField,shortDescription:Message counter,enumeration:class io.github.mtrevisan.boxon.annotations.configurations.NullEnum,fieldType:java.lang.String}],configuration:io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII,header:{longDescription:The command AT+GTREG is used to do things.,maxProtocol:2.8,start:AT+,charset:UTF-8,end:$,shortDescription:AT+GTREG}}", jsonDescription);
	}

}
