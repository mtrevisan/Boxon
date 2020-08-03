/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.BindByte;
import io.github.mtrevisan.boxon.annotations.BindChecksum;
import io.github.mtrevisan.boxon.annotations.BindShort;
import io.github.mtrevisan.boxon.annotations.BindString;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.IMEIValidator;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


@MessageHeader(start = "+ACK", end = "\r\n")
public class ACKMessageHex{

	private static final Map<Byte, String> MESSAGE_TYPE_MAP = new HashMap<>();
	static{
		MESSAGE_TYPE_MAP.put((byte)0, "AT+GTBSI");
		MESSAGE_TYPE_MAP.put((byte)1, "AT+GTSRI");
		MESSAGE_TYPE_MAP.put((byte)2, "AT+GTQSS");
		MESSAGE_TYPE_MAP.put((byte)4, "AT+GTCFG");
		MESSAGE_TYPE_MAP.put((byte)5, "AT+GTTOW");
		MESSAGE_TYPE_MAP.put((byte)6, "AT+GTEPS");
		MESSAGE_TYPE_MAP.put((byte)7, "AT+GTDIS");
		MESSAGE_TYPE_MAP.put((byte)8, "AT+GTOUT");
		MESSAGE_TYPE_MAP.put((byte)9, "AT+GTIOB");
		MESSAGE_TYPE_MAP.put((byte)10, "AT+GTTMA");
		MESSAGE_TYPE_MAP.put((byte)11, "AT+GTFRI");
		MESSAGE_TYPE_MAP.put((byte)12, "AT+GTGEO");
		MESSAGE_TYPE_MAP.put((byte)13, "AT+GTSPD");
		MESSAGE_TYPE_MAP.put((byte)14, "AT+GTSOS");
		MESSAGE_TYPE_MAP.put((byte)15, "AT+GTMON");
		MESSAGE_TYPE_MAP.put((byte)16, "AT+GTRTO");
		MESSAGE_TYPE_MAP.put((byte)21, "AT+GTUPD");
		MESSAGE_TYPE_MAP.put((byte)22, "AT+GTPIN");
		MESSAGE_TYPE_MAP.put((byte)23, "AT+GTDAT");
		MESSAGE_TYPE_MAP.put((byte)24, "AT+GTOWH");
		MESSAGE_TYPE_MAP.put((byte)25, "AT+GTDOG");
		MESSAGE_TYPE_MAP.put((byte)26, "AT+GTAIS");
		MESSAGE_TYPE_MAP.put((byte)27, "AT+GTJDC");
		MESSAGE_TYPE_MAP.put((byte)28, "AT+GTIDL");
		MESSAGE_TYPE_MAP.put((byte)29, "AT+GTHBM");
		MESSAGE_TYPE_MAP.put((byte)30, "AT+GTHMC");
		MESSAGE_TYPE_MAP.put((byte)32, "AT+GTURT");
		MESSAGE_TYPE_MAP.put((byte)34, "AT+GTWLT");
		MESSAGE_TYPE_MAP.put((byte)35, "AT+GTHRM");
		MESSAGE_TYPE_MAP.put((byte)36, "AT+GTFFC");
		MESSAGE_TYPE_MAP.put((byte)37, "AT+GTJBS");
		MESSAGE_TYPE_MAP.put((byte)38, "AT+GTSSR");
		MESSAGE_TYPE_MAP.put((byte)41, "AT+GTEFS");
		MESSAGE_TYPE_MAP.put((byte)43, "AT+GTIDA");
		MESSAGE_TYPE_MAP.put((byte)44, "AT+GTACD");
		MESSAGE_TYPE_MAP.put((byte)45, "AT+GTPDS");
		MESSAGE_TYPE_MAP.put((byte)46, "AT+GTCRA");
		MESSAGE_TYPE_MAP.put((byte)47, "AT+GTBZA");
		MESSAGE_TYPE_MAP.put((byte)48, "AT+GTSPA");
		MESSAGE_TYPE_MAP.put((byte)53, "AT+GTRMD");
		MESSAGE_TYPE_MAP.put((byte)57, "AT+GTPGD");
		MESSAGE_TYPE_MAP.put((byte)62, "AT+GTSSI");
		MESSAGE_TYPE_MAP.put((byte)63, "AT+GTASC");
		MESSAGE_TYPE_MAP.put((byte)64, "AT+GTTRF");
	}

	public static class MessageTypeConverter implements Converter<Byte, String>{
		@Override
		public String decode(final Byte value){
			return MESSAGE_TYPE_MAP.get(value);
		}

		@Override
		public Byte encode(final String value){
			for(final Map.Entry<Byte, String> elem : MESSAGE_TYPE_MAP.entrySet()){
				if(elem.getValue().equals(value))
					return elem.getKey();
			}
			return 0x00;
		}
	}


	@BindString(size = "#headerSize()")
	private String messageHeader;
	@BindByte(converter = MessageTypeConverter.class)
	private String messageType;
	@BindByte(converter = ACKMaskHex.ACKMaskConverter.class)
	private ACKMaskHex mask;
	@BindByte(condition = "mask.hasLength()")
	private byte messageLength;
	@BindByte(condition = "mask.hasDeviceType()")
	private byte deviceTypeCode;
	@BindArrayPrimitive(condition = "mask.hasProtocolVersion()", size = "2", type = byte.class, converter = QueclinkHelper.VersionConverter.class)
	private String protocolVersion;
	@BindArrayPrimitive(condition = "mask.hasFirmwareVersion()", size = "2", type = byte.class, converter = QueclinkHelper.VersionConverter.class)
	private String firmwareVersion;
	@BindArrayPrimitive(condition = "mask.hasIMEI()", size = "8", type = byte.class, converter = QueclinkHelper.IMEIConverter.class, validator = IMEIValidator.class)
	private String imei;
	@BindString(condition = "!mask.hasIMEI()", size = "8")
	private String deviceName;
	@BindByte
	private byte id;
	@BindShort
	private short correlationId;
	@BindArrayPrimitive(condition = "mask.hasEventTime()", size = "7", type = byte.class, converter = QueclinkHelper.DateTimeYYYYMMDDHHMMSSConverter.class)
	private ZonedDateTime eventTime;
	@BindShort(condition = "mask.hasMessageId()")
	private short messageId;
	@BindChecksum(type = short.class, skipStart = 4, skipEnd = 4, algorithm = CRC16.class, startValue = CRC16.START_VALUE_0xFFFF)
	private short checksum;

	@Evaluate("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
	private String deviceTypeName;
	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;
	@Evaluate("messageHeader.startsWith('+B')")
	private boolean buffered;

}
