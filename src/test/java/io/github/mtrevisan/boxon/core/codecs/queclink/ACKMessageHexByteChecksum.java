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
package io.github.mtrevisan.boxon.core.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.checksummers.BSD8;
import io.github.mtrevisan.boxon.annotations.validators.IMEIValidator;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.time.ZonedDateTime;


@SuppressWarnings("ALL")
@MessageHeader(start = "-ACK", end = "\r\n")
public class ACKMessageHexByteChecksum{

	@BindString(size = "#headerSize()")
	private String messageHeader;
	@BindByte(converter = ACKMessageHex.MessageTypeConverter.class)
	private String messageType;
	@BindByte(converter = ACKMaskHex.ACKMaskConverter.class)
	private ACKMaskHex mask;
	@BindByte(condition = "mask.hasLength()")
	private byte messageLength;
	@BindByte(condition = "mask.hasDeviceType()")
	private byte deviceTypeCode;
	@BindArrayPrimitive(condition = "mask.hasProtocolVersion()", size = "2", type = byte.class,
		converter = QueclinkHelper.VersionConverter.class)
	private Version protocolVersion;
	@BindArrayPrimitive(condition = "mask.hasFirmwareVersion()", size = "2", type = byte.class,
		converter = QueclinkHelper.VersionConverter.class)
	private Version firmwareVersion;
	@BindArrayPrimitive(condition = "mask.hasIMEI()", size = "8", type = byte.class,
		converter = QueclinkHelper.IMEIConverter.class, validator = IMEIValidator.class)
	private String imei;
	@BindString(condition = "!mask.hasIMEI()", size = "8")
	private String deviceName;
	@BindByte
	private byte id;
	@BindShort
	private short correlationId;
	@BindArrayPrimitive(condition = "mask.hasEventTime()", size = "7", type = byte.class,
		converter = QueclinkHelper.DateTimeYYYYMMDDHHMMSSConverter.class)
	private ZonedDateTime eventTime;
	@BindShort(condition = "mask.hasMessageId()")
	private short messageId;

	@Checksum(type = byte.class, skipStart = 4, skipEnd = 3, algorithm = BSD8.class, startValue = BSD8.START_VALUE_0x00)
	private byte checksum;

	@Evaluate("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
	private String deviceTypeName;
	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;
	@Evaluate("messageHeader.startsWith('+B')")
	private boolean buffered;

}
