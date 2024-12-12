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
package io.github.mtrevisan.boxon.core.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.checksummers.BSD16;
import io.github.mtrevisan.boxon.annotations.validators.IMEIValidator;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@TemplateHeader(start = "-ACK", end = "\r\n")
public class ACKMessageHexByteChecksum{

	@BindString(size = "#headerLength()")
	private String messageHeader;
	@BindInteger(size = "8", converter = ACKMessageHex.MessageTypeConverter.class)
	private String messageType;
	@BindInteger(size = "8", converter = ACKMaskHex.ACKMaskConverter.class)
	private ACKMaskHex mask;
	@BindInteger(condition = "mask.hasLength()", size = "8")
	private byte messageLength;
	@BindInteger(condition = "mask.hasDeviceType()", size = "8")
	private byte deviceTypeCode;
	@BindInteger(condition = "mask.hasProtocolVersion()", size = "16", converter = QueclinkHelper.VersionConverter.class)
	private Version protocolVersion;
	@BindInteger(condition = "mask.hasFirmwareVersion()", size = "16", converter = QueclinkHelper.VersionConverter.class)
	private Version firmwareVersion;
	@BindInteger(condition = "mask.hasIMEI()", size = "8", converter = QueclinkHelper.IMEIConverter.class, validator = IMEIValidator.class)
	@BindAsArray(size = "8")
	private String imei;
	@BindString(condition = "!mask.hasIMEI()", size = "8")
	private String deviceName;
	@BindInteger(size = "8")
	private byte id;
	@BindInteger(size = "16")
	private short correlationId;
	@BindInteger(condition = "mask.hasEventTime()", size = "8", converter = QueclinkHelper.DateTimeYYYYMMDDHHMMSSConverter.class)
	@BindAsArray(size = "7")
	private LocalDateTime eventTime;
	@BindInteger(condition = "mask.hasMessageId()", size = "16")
	private short messageId;

	@Checksum(skipStart = 4, skipEnd = 4, algorithm = BSD16.class, crcSize = 16)
	private short checksum;

	@Evaluate("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
	private String deviceTypeName;
	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;
	@Evaluate("messageHeader.startsWith('+B')")
	private boolean buffered;

}
