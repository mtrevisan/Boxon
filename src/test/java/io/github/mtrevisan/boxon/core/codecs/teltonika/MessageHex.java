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
import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.checksummers.CRC16;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


/**
 * Parser for Codec 8, Codec 8 Extended, and Codec 16.
 *
 * @see <a href="https://wiki.teltonika-gps.com/view/Codec">Codec</a>
 */
@TemplateHeader(start = "\0\0\0\0")
public class MessageHex{

	private static final class AVLData{
		@BindInteger(size = "64", converter = TeltonikaHelper.UnixTimestampConverter.class)
		private LocalDateTime eventTime;
		//0: low, 1: high, 2: panic
		@BindInteger(size = "8")
		private byte priority;
		@BindObject(type = GPSElement.class)
		private GPSElement gpsElement;
		@BindObject(type = IOElement.class)
		private IOElement ioElement;
	}

	private static final class GPSElement{
		@BindInteger(size = "32", converter = TeltonikaHelper.CoordinateConverter.class, unitOfMeasure = "°")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal longitude;
		@BindInteger(size = "32", converter = TeltonikaHelper.CoordinateConverter.class, unitOfMeasure = "°")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.BigDecimal).ZERO")
		private BigDecimal latitude;
		@BindInteger(size = "16", unitOfMeasure = "m")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short altitude;
		//Heading measured from magnetic north
		@BindInteger(size = "16", unitOfMeasure = "°")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short heading;
		@BindInteger(size = "8")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Byte).valueOf(0)")
		private Byte satellitesCount;
		@BindInteger(size = "16", unitOfMeasure = "km/h")
		@PostProcess(condition = "#self.invalidPosition", valueDecode = "null", valueEncode = "T(java.math.Short).valueOf(0)")
		private Short speed;

		@Evaluate("#self.satellitesCount == 0")
		private boolean invalidPosition;
	}

	private static final class IOElement{
		//IO property that has changed, zero if it's not a record caused by an event
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int eventIOID;
		//0: on exit, 1: on entrance, 2: on both exit and entrance, 4: hysteresis, 5: on change, 6: eventual, 7: periodical
		@BindInteger(condition = "(codecID == 0x10)", size = "8")
		private int generationType;

		//skip `propertiesCount` = `oneBytePropertiesCount` + `twoBytesPropertiesCount` + `fourBytesPropertiesCount`
		//	+ `eightBytesPropertiesCount`
		@SkipBits("(codecID == -114? 16: 8)")

		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int oneBytePropertiesCount;
		@BindObject(type = FixedSizeProperty.class)
		@BindAsArray(size = "#self.oneBytePropertiesCount")
		@ContextParameter(name = "valueSize", value = "8")
		private FixedSizeProperty[] oneByteProperties;

		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int twoBytesPropertiesCount;
		@BindObject(type = FixedSizeProperty.class)
		@BindAsArray(size = "#self.twoBytesPropertiesCount")
		@ContextParameter(name = "valueSize", value = "16")
		private FixedSizeProperty[] twoBytesProperties;

		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int fourBytesPropertiesCount;
		@BindObject(type = FixedSizeProperty.class)
		@BindAsArray(size = "#self.fourBytesPropertiesCount")
		@ContextParameter(name = "valueSize", value = "32")
		private FixedSizeProperty[] fourBytesProperties;

		@BindInteger(size = "(codecID == -114? 16: 8)")
		private int eightBytesPropertiesCount;
		@BindObject(type = FixedSizeProperty.class)
		@BindAsArray(size = "#self.eightBytesPropertiesCount")
		@ContextParameter(name = "valueSize", value = "64")
		private FixedSizeProperty[] eightBytesProperties;

		@BindInteger(condition = "(codecID == -114)", size = "16")
		private int variableSizePropertiesCount;
		@BindObject(condition = "(codecID == -114)", type = VariableSizeProperty.class)
		@BindAsArray(size = "#self.variableSizePropertiesCount")
		private VariableSizeProperty[] variableSizeProperties;
	}

	private static class FixedSizeProperty{
		@BindInteger(size = "(codecID == -114 || codecID == 0x10? 16: 8)")
		private int key;
		@BindInteger(size = "#valueSize")
		private BigInteger value;
	}

	private static class VariableSizeProperty{
		@BindInteger(size = "16")
		private int key;
		@BindInteger(size = "16")
		private int length;
		@BindInteger(size = "8 * #self.length")
		private BigInteger value;
	}

	//skip preamble of all zeros and message length
	@SkipBits("32+32")
	//0x08: Codec 8, 0x8E (-114): Codec 8 Extended, 0x10 Codec 16
	@BindInteger(size = "8")
	private byte codecID;
	@BindInteger(size = "8")
	private byte dataCount;
	@BindObject(type = AVLData.class)
	@BindAsArray(size = "dataCount")
	private AVLData[] data;
	//skip a copy of `dataCount` and other reserved data
	@SkipBits("8+16")
	@Checksum(skipStart = 8, skipEnd = 4, algorithm = CRC16.class, checksumSize = 16)
	private short checksum;

	@Evaluate("T(java.time.ZonedDateTime).now()")
	private ZonedDateTime receptionTime;

}
