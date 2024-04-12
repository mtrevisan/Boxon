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
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16IBM;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@TemplateHeader(start = "\0\0\0\0")
public class MessageHex{

	public static final class AVLData{
		@BindLong(converter = TeltonikaHelper.UnixTimestampConverter.class)
		private LocalDateTime eventTime;
		@BindByte
		private byte priority;
		@BindObject(type = GPSElement.class)
		private GPSElement gpsElement;
		@BindObject(type = IOElement.class)
		private IOElement ioElement;
	}

	public static final class GPSElement{
		@BindInt(converter = TeltonikaHelper.CoordinateConverter.class)
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal longitude;
		@BindInt(converter = TeltonikaHelper.CoordinateConverter.class)
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal latitude;
		//[m]
		@BindShort
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short altitude;
		//Heading measured from magnetic north [°]
		@BindShort
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short heading;
		@BindByte
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Byte).valueOf(0)")
		private Byte satellitesCount;
		@BindShort
		@PostProcessField(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short speed;

		@Evaluate("#self.speed == 0")
		private boolean invalidPosition;
	}

	public static final class IOElement{
		//IO property that has changed, zero if it's not a record caused by an event
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int eventIOID;
		@BindByte(condition = "codecID == 0x10")
		private int generationType;
		//propertiesCount = oneBytePropertiesCount + twoBytesPropertiesCount + fourBytesPropertiesCount + eightBytesPropertiesCount
		@BindInteger(size = "(codecID == -114? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int propertiesCount;
		@BindInteger(size = "(codecID == -114? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int oneBytePropertiesCount;
		@BindArray(size = "#self.oneBytePropertiesCount", type = TeltonikaHelper.OneByteProperty.class)
		private TeltonikaHelper.OneByteProperty[] oneByteProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int twoBytesPropertiesCount;
		@BindArray(size = "#self.twoBytesPropertiesCount", type = TeltonikaHelper.TwoBytesProperty.class)
		private TeltonikaHelper.TwoBytesProperty[] twoBytesProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int fourBytesPropertiesCount;
		@BindArray(size = "#self.fourBytesPropertiesCount", type = TeltonikaHelper.FourBytesProperty.class)
		private TeltonikaHelper.FourBytesProperty[] fourBytesProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)", converter = TeltonikaHelper.BigIntegerToIntConverter.class)
		private int eightBytesPropertiesCount;
		@BindArray(size = "#self.eightBytesPropertiesCount", type = TeltonikaHelper.EightBytesProperty.class)
		private TeltonikaHelper.EightBytesProperty[] eightBytesProperties;
		@BindShort(condition = "codecID == -114")
		private int variableBytesPropertiesCount;
		@BindArray(condition = "codecID == -114", size = "#self.variableBytesPropertiesCount",
			type = TeltonikaHelper.VariableBytesProperty.class)
		private TeltonikaHelper.VariableBytesProperty[] variableBytesProperties;
	}


	@BindInt(validator = TeltonikaHelper.PreambleValidator.class)
	private int preamble;
	@BindInt
	private int messageLength;
	@BindByte
	private byte codecID;
	@BindByte
	private byte dataCount1;
	@BindArray(size = "dataCount1", type = AVLData.class)
	private AVLData[] data;
	@BindByte
	//should be same as `dataCount1`
	private byte dataCount2;

	@SkipBits("16")
	@Checksum(skipStart = 8, skipEnd = 4, algorithm = CRC16IBM.class)
	private short checksum;

	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;

}
