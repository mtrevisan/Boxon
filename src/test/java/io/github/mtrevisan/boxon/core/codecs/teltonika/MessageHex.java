/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs.teltonika;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcessField;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16IBM;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@TemplateHeader(start = "\0\0\0\0")
public class MessageHex{

	@BindInt(validator = TeltonikaHelper.PreambleValidator.class)
	private int preamble;
	@BindInt
	private int messageLength;
	@BindByte
	private byte codecID;
	@BindByte
	private byte dataCount1;

	@BindLong(converter = TeltonikaHelper.UnixTimestampConverter.class)
	private LocalDateTime eventTime;
	@BindByte
	private byte priority;

	@BindInt(converter = TeltonikaHelper.CoordinateConverter.class)
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
	private BigDecimal longitude;
	@BindInt(converter = TeltonikaHelper.CoordinateConverter.class)
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
	private BigDecimal latitude;
	//[m]
	@BindShort
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
	private Short altitude;
	//Heading measured from magnetic north [°]
	@BindShort
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
	private Short heading;
	@BindByte
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Byte).valueOf(0)")
	private Byte satellitesCount;
	@BindShort
	@PostProcessField(condition = "invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
	private Short speed;
	@Evaluate("speed == 0")
	private boolean invalidPosition;

	//IO property that has changed, zero if it's not a record caused by an event
	@BindByte
	private int eventIOID;
	//propertiesCount = oneBytePropertiesCount + twoBytesPropertiesCount + fourBytesPropertiesCount + eightBytesPropertiesCount
	@BindByte
	private int propertiesCount;
	@BindByte
	private int oneBytePropertiesCount;
	@BindArray(size = "oneBytePropertiesCount", type = TeltonikaHelper.OneByteProperty.class)
	private TeltonikaHelper.OneByteProperty[] oneByteProperties;
	@BindByte
	private int twoBytesPropertiesCount;
	@BindArray(size = "twoBytesPropertiesCount", type = TeltonikaHelper.TwoBytesProperty.class)
	private TeltonikaHelper.TwoBytesProperty[] twoBytesProperties;
	@BindByte
	private int fourBytesPropertiesCount;
	@BindArray(size = "fourBytesPropertiesCount", type = TeltonikaHelper.FourBytesProperty.class)
	private TeltonikaHelper.FourBytesProperty[] fourBytesProperties;
	@BindByte
	private int eightBytesPropertiesCount;
	@BindArray(size = "eightBytesPropertiesCount", type = TeltonikaHelper.EightBytesProperty.class)
	private TeltonikaHelper.EightBytesProperty[] eightBytesProperties;

	@BindByte
	//should be same as `dataCount1`
	private byte dataCount2;

	@Skip(size = "16")
	@Checksum(skipStart = 8, skipEnd = 4, algorithm = CRC16IBM.class)
	private short checksum;

	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;

}
