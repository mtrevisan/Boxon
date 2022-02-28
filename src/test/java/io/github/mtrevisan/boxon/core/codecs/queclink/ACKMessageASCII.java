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

import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.IMEIValidator;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.time.ZonedDateTime;
import java.util.Map;


@SuppressWarnings("ALL")
@MessageHeader(start = "+ACK:", end = "$")
public class ACKMessageASCII{

	private static class MessageTypeConverter implements Converter<Byte, String>{
		@Override
		public String decode(final Byte value){
			return ACKMessageHex.MESSAGE_TYPE_MAP.get(value);
		}

		@Override
		public Byte encode(final String value){
			for(final Map.Entry<Byte, String> elem : ACKMessageHex.MESSAGE_TYPE_MAP.entrySet())
				if(elem.getValue().equals(value))
					return elem.getKey();
			return 0x00;
		}
	}


	@BindStringTerminated(terminator = ':')
	private String messageHeader;
	@BindStringTerminated(terminator = ',')
	private String messageType;
	@BindString(size = "2", converter = QueclinkHelper.HexStringToByteConverter.class)
	private byte deviceTypeCode;
	@BindStringTerminated(terminator = ',', converter = QueclinkHelper.StringVersionConverter.class)
	private Version protocolVersion;
	@BindStringTerminated(terminator = ',', validator = IMEIValidator.class)
	private String imei;
	@BindStringTerminated(terminator = ',')
	private String deviceName;
	@BindStringTerminated(terminator = ',')
	private String id;
	@BindStringTerminated(terminator = ',', converter = QueclinkHelper.HexStringToShortConverter.class)
	private short correlationId;
	@BindStringTerminated(terminator = ',', converter = QueclinkHelper.StringDateTimeYYYYMMDDHHMMSSConverter.class)
	private ZonedDateTime eventTime;
	@BindStringTerminated(terminator = '$', consumeTerminator = false, converter = QueclinkHelper.HexStringToShortConverter.class)
	private short messageId;

	@Evaluate("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
	private String deviceTypeName;
	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;
	@Evaluate("messageHeader.startsWith('+B')")
	private boolean buffered;

}
