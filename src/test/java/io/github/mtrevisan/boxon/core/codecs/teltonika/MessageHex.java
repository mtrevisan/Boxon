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
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16IBM;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@TemplateHeader(start = "\0\0\0\0")
public class MessageHex{

	private static final class AVLData{
		@BindInteger(size = "64", converter = TeltonikaHelper.UnixTimestampConverter.class)
		private LocalDateTime eventTime;
		@BindInteger(size = "8")
		private byte priority;
		@BindObject(type = GPSElement.class)
		private GPSElement gpsElement;
		@BindObject(type = IOElement.class)
		private IOElement ioElement;
	}

	private static final class GPSElement{
		//[°]
		@BindInteger(size = "32", converter = TeltonikaHelper.CoordinateConverter.class)
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal longitude;
		//[°]
		@BindInteger(size = "32", converter = TeltonikaHelper.CoordinateConverter.class)
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal latitude;
		//[m]
		@BindInteger(size = "16")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short altitude;
		//Heading measured from magnetic north [°]
		@BindInteger(size = "16")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short heading;
		@BindInteger(size = "8")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Byte).valueOf(0)")
		private Byte satellitesCount;
		//[km/h]
		@BindInteger(size = "16")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short speed;

		@Evaluate("#self.satellitesCount == 0")
		private boolean invalidPosition;
	}

	private static final class IOElement{
		//IO property that has changed, zero if it's not a record caused by an event
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int eventIOID;
		@BindInteger(condition = "codecID == 0x10", size = "8")
		private int generationType;
		//propertiesCount = oneBytePropertiesCount + twoBytesPropertiesCount + fourBytesPropertiesCount + eightBytesPropertiesCount
		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int propertiesCount;
		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int oneBytePropertiesCount;
		@BindObject(type = TeltonikaHelper.OneByteProperty.class)
		@BindAsArray(size = "#self.oneBytePropertiesCount")
		private TeltonikaHelper.OneByteProperty[] oneByteProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int twoBytesPropertiesCount;
		@BindObject(type = TeltonikaHelper.TwoBytesProperty.class)
		@BindAsArray(size = "#self.twoBytesPropertiesCount")
		private TeltonikaHelper.TwoBytesProperty[] twoBytesProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int fourBytesPropertiesCount;
		@BindObject(type = TeltonikaHelper.FourBytesProperty.class)
		@BindAsArray(size = "#self.fourBytesPropertiesCount")
		private TeltonikaHelper.FourBytesProperty[] fourBytesProperties;
		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int eightBytesPropertiesCount;
		@BindObject(type = TeltonikaHelper.EightBytesProperty.class)
		@BindAsArray(size = "#self.eightBytesPropertiesCount")
		private TeltonikaHelper.EightBytesProperty[] eightBytesProperties;
		@BindInteger(condition = "codecID == -114", size = "16")
		private int variableBytesPropertiesCount;
		@BindObject(condition = "codecID == -114", type = TeltonikaHelper.VariableBytesProperty.class)
		@BindAsArray(size = "#self.variableBytesPropertiesCount")
		private TeltonikaHelper.VariableBytesProperty[] variableBytesProperties;
	}


	@BindInteger(size = "32", validator = TeltonikaHelper.PreambleValidator.class)
	private int preamble;
	@BindInteger(size = "32")
	private int messageLength;
	@BindInteger(size = "8")
	private byte codecID;
	@BindInteger(size = "8")
	private byte dataCount;
	@BindObject(type = AVLData.class)
	@BindAsArray(size = "dataCount")
	private AVLData[] data;
	@SkipBits("24")
	@Checksum(skipStart = 8, skipEnd = 4, algorithm = CRC16IBM.class)
	private short checksum;

	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;

}
