/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
class DescriptorTest{

	@Test
	void description() throws AnnotationException, ConfigurationException, JsonProcessingException, CodecException, TemplateException,
			NoSuchMethodException{
		DeviceTypes deviceTypes = new DeviceTypes();
		deviceTypes.add("QUECLINK_GB200S", (byte)0x46);
		ParserCore core = ParserCore.create()
			.addToContext("deviceTypes", deviceTypes)
			.withContextFunction(ParserTest.class.getDeclaredMethod("headerSize"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class);
		Descriptor descriptor = Descriptor.create(core);

		List<Map<String, Object>> descriptions = descriptor.describeTemplates();

		Assertions.assertEquals(1, descriptions.size());

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> description = descriptions.get(0);
		String jsonDescription = mapper.writeValueAsString(description);

		Assertions.assertEquals("{\"fields\":[{\"charset\":\"UTF-8\",\"size\":\"#headerSize()\",\"name\":\"messageHeader\",\"annotationType\":\"BindString\",\"fieldType\":\"String\"},{\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.ACKMessageHex$MessageTypeConverter\",\"name\":\"messageType\",\"annotationType\":\"BindByte\",\"fieldType\":\"String\"},{\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.ACKMaskHex$ACKMaskConverter\",\"name\":\"mask\",\"annotationType\":\"BindByte\",\"fieldType\":\"ACKMaskHex\"},{\"condition\":\"mask.hasLength()\",\"name\":\"messageLength\",\"annotationType\":\"BindByte\",\"fieldType\":\"byte\"},{\"condition\":\"mask.hasDeviceType()\",\"name\":\"deviceTypeCode\",\"annotationType\":\"BindByte\",\"fieldType\":\"byte\"},{\"condition\":\"mask.hasProtocolVersion()\",\"size\":\"2\",\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.QueclinkHelper$VersionConverter\",\"name\":\"protocolVersion\",\"annotationType\":\"BindArrayPrimitive\",\"type\":\"byte\",\"fieldType\":\"String\",\"byteOrder\":\"BIG_ENDIAN\"},{\"condition\":\"mask.hasFirmwareVersion()\",\"size\":\"2\",\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.QueclinkHelper$VersionConverter\",\"name\":\"firmwareVersion\",\"annotationType\":\"BindArrayPrimitive\",\"type\":\"byte\",\"fieldType\":\"String\",\"byteOrder\":\"BIG_ENDIAN\"},{\"condition\":\"mask.hasIMEI()\",\"size\":\"8\",\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.QueclinkHelper$IMEIConverter\",\"name\":\"imei\",\"validator\":\"io.github.mtrevisan.boxon.annotations.validators.IMEIValidator\",\"annotationType\":\"BindArrayPrimitive\",\"type\":\"byte\",\"fieldType\":\"String\",\"byteOrder\":\"BIG_ENDIAN\"},{\"charset\":\"UTF-8\",\"condition\":\"!mask.hasIMEI()\",\"size\":\"8\",\"name\":\"deviceName\",\"annotationType\":\"BindString\",\"fieldType\":\"String\"},{\"name\":\"id\",\"annotationType\":\"BindByte\",\"fieldType\":\"byte\"},{\"name\":\"correlationId\",\"annotationType\":\"BindShort\",\"fieldType\":\"short\",\"byteOrder\":\"BIG_ENDIAN\"},{\"condition\":\"mask.hasEventTime()\",\"size\":\"7\",\"converter\":\"io.github.mtrevisan.boxon.codecs.queclink.QueclinkHelper$DateTimeYYYYMMDDHHMMSSConverter\",\"name\":\"eventTime\",\"annotationType\":\"BindArrayPrimitive\",\"type\":\"byte\",\"fieldType\":\"ZonedDateTime\",\"byteOrder\":\"BIG_ENDIAN\"},{\"condition\":\"mask.hasMessageId()\",\"name\":\"messageId\",\"annotationType\":\"BindShort\",\"fieldType\":\"short\",\"byteOrder\":\"BIG_ENDIAN\"},{\"skipEnd\":4,\"skipStart\":4,\"name\":\"checksum\",\"annotationType\":\"Checksum\",\"startValue\":-1,\"type\":\"short\",\"fieldType\":\"short\",\"byteOrder\":\"BIG_ENDIAN\",\"algorithm\":\"CRC16CCITT\"}],\"context\":{\"methods\":[\"private static int io.github.mtrevisan.boxon.core.ParserTest.headerSize()\"],\"deviceTypes\":\"[QUECLINK_GB200S (0x46)]\"},\"header\":{\"start\":[\"+ACK\"],\"charset\":\"UTF-8\"}}", jsonDescription);
	}

}
