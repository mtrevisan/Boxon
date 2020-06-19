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
package unit731.boxon.codecs.queclink;

import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.utils.ByteHelper;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Locale;


public class QueclinkHelper{

	public static class VersionConverter implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			return value[0] + "." + value[1];
		}

		@Override
		public byte[] encode(final String value){
			final String[] components = StringUtils.split(value, '.');
			return new byte[]{Byte.parseByte(components[0]), Byte.parseByte(components[1])};
		}
	}
	public static class IMEIConverter implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			final StringBuffer sb = new StringBuffer();
			for(int i = 0; i < 7; i ++)
				sb.append(String.format("%02d", value[i] & 255));
			sb.append(ByteHelper.applyMaskAndShift(value[7], (byte)0x0F));
			return sb.toString();
		}

		@Override
		public byte[] encode(final String value){
			final byte[] imei = new byte[8];
			final String[] components = value.split("(?<=\\G\\d{2})", 8);
			for(int i = 0; i < 8; i ++)
				imei[i] = Integer.valueOf(components[i]).byteValue();
			return imei;
		}
	}
	public static class ShortFromDecConverter implements Converter<Short, Integer>{
		@Override
		public Integer decode(final Short value){
			return Integer.parseInt(Integer.toHexString(value));
		}

		@Override
		public Short encode(final Integer value){
			return Short.valueOf(Integer.toString(value), 16);
		}
	}
	public static class Int3BConverter implements Converter<byte[], Integer>{
		@Override
		public Integer decode(final byte[] value){
			return ((value[0] << 16) | (value[1] << 8) | value[2]);
		}

		@Override
		public byte[] encode(final Integer value){
			return new byte[]{
				(byte)ByteHelper.applyMaskAndShift(value, 0x00FF_0000),
				(byte)ByteHelper.applyMaskAndShift(value, 0x0000_FF00),
				(byte)ByteHelper.applyMaskAndShift(value, 0x0000_00FF)};
		}
	}
	public static class Double1BConverter implements Converter<byte[], BigDecimal>{
		@Override
		public BigDecimal decode(final byte[] value){
			final BigDecimal integerPart = new BigDecimal(value[0]);
			final BigDecimal decimalPart = new BigDecimal(value[1]);
			return integerPart.add(decimalPart.divide(BigDecimal.TEN, MathContext.DECIMAL128));
		}

		@Override
		public byte[] encode(final BigDecimal value){
			final byte integerPart = value.byteValue();
			final byte decimalPart = extractDecimalPart(value);
			return new byte[]{integerPart, decimalPart};
		}

		private byte extractDecimalPart(final BigDecimal value){
			return value.remainder(BigDecimal.ONE)
				.multiply(BigDecimal.TEN, MathContext.DECIMAL128)
				.byteValue();
		}
	}
	public static class Double2BConverter implements Converter<byte[], BigDecimal>{
		@Override
		public BigDecimal decode(final byte[] value){
			final BigDecimal integerPart = new BigDecimal((value[0] << Byte.SIZE) | value[1]);
			final BigDecimal decimalPart = new BigDecimal(value[2]);
			return integerPart.add(decimalPart.divide(BigDecimal.TEN, MathContext.DECIMAL128));
		}

		@Override
		public byte[] encode(final BigDecimal value){
			final short integerPart = value.shortValue();
			final byte decimalPart = extractDecimalPart(value);
			return new byte[]{(byte)(integerPart >>> Byte.SIZE), (byte)(integerPart & 0xFF), decimalPart};
		}

		private byte extractDecimalPart(final BigDecimal value){
			return value.remainder(BigDecimal.ONE)
				.multiply(BigDecimal.TEN, MathContext.DECIMAL128)
				.byteValue();
		}
	}
	public static class Double4BConverter implements Converter<byte[], BigDecimal>{
		@Override
		public BigDecimal decode(final byte[] value){
			final BigDecimal integerPart = new BigDecimal((value[0] << (Short.SIZE + Byte.SIZE)) | (value[1] << Short.SIZE) | (value[2] << Byte.SIZE) | value[3]);
			final BigDecimal decimalPart = new BigDecimal(value[2]);
			return integerPart.add(decimalPart.divide(BigDecimal.TEN, MathContext.DECIMAL128));
		}

		@Override
		public byte[] encode(final BigDecimal value){
			final int integerPart = value.intValue();
			final byte decimalPart = extractDecimalPart(value);
			return new byte[]{(byte)(integerPart >>> (Short.SIZE + Byte.SIZE)), (byte)(integerPart >>> Short.SIZE), (byte)(integerPart >>> Byte.SIZE), (byte)(integerPart & 0xFF), decimalPart};
		}

		private byte extractDecimalPart(final BigDecimal value){
			return value.remainder(BigDecimal.ONE)
				.multiply(BigDecimal.TEN, MathContext.DECIMAL128)
				.byteValue();
		}
	}
	/** Ritorna un double contenente la coordinata (interpreta 4 byte considerando una parte decimale di 6 cifre) */
	public static class CoordinateConverter implements Converter<Integer, BigDecimal>{

		private final BigDecimal COORDINATE_FACTOR = new BigDecimal(1_000_000);

		@Override
		public BigDecimal decode(final Integer value){
			return new BigDecimal(value)
				.divide(COORDINATE_FACTOR, MathContext.DECIMAL128);
		}

		@Override
		public Integer encode(final BigDecimal value){
			return value.multiply(COORDINATE_FACTOR).intValue();
		}
	}
	/** Ritorna un delta per una coordinata (interpreta 2 byte considerando una parte decimale di 6 cifre) */
	public static class DeltaCoordinateConverter implements Converter<Short, BigDecimal>{

		private final BigDecimal COORDINATE_FACTOR = new BigDecimal(1_000_000);

		@Override
		public BigDecimal decode(final Short value){
			return new BigDecimal(value)
				.divide(COORDINATE_FACTOR, MathContext.DECIMAL128);
		}

		@Override
		public Short encode(final BigDecimal value){
			return value.multiply(COORDINATE_FACTOR).shortValue();
		}
	}
	/**
	 * 0:		< -133 dBm
	 * 1:		-111 dBm
	 * 2-30:	-109 - -53 dBm
	 * 31:		> -51 dBm
	 * 99:		unknown
	 */
	public static class RSSIConverter implements Converter<Byte, Short>{

		public static final int RSSI_UNKNOWN = 0;

		@Override
		public Short decode(final Byte value){
			if(value == 0)
				//< -133 dBm
				return (byte)-133;
			if(value == 99)
				return RSSI_UNKNOWN;
			//31 is > -51 dBm
			return (short)(value * 2 - 113);
		}

		@Override
		public Byte encode(final Short value){
			if(value == -133)
				return 0;
			if(value == RSSI_UNKNOWN)
				return 99;
			return (byte)((value + 133) / 2);
		}
	}
	public static class DateTimeUnixConverter implements Converter<Integer, ZonedDateTime>{
		@Override
		public ZonedDateTime decode(final Integer value){
			return DateTimeUtils.createFrom(value);
		}

		@Override
		public Integer encode(final ZonedDateTime value){
			return (int)value.toEpochSecond();
		}
	}
	public static class DateTimeYYYYMMDDHHMMSSConverter implements Converter<byte[], ZonedDateTime>{
		@Override
		public ZonedDateTime decode(final byte[] value){
			final ByteBuffer bb = ByteBuffer.wrap(value);
			final int year = bb.getShort();
			final int month = bb.get();
			final int dayOfMonth = bb.get();
			final int hour = bb.get();
			final int minute = bb.get();
			final int second = bb.get();
			return DateTimeUtils.createFrom(year, month, dayOfMonth, hour, minute, second);
		}

		@Override
		public byte[] encode(final ZonedDateTime value){
			return ByteBuffer.allocate(7)
				.putShort((short)value.getYear())
				.put((byte)value.getMonthValue())
				.put((byte)value.getDayOfMonth())
				.put((byte)value.getHour())
				.put((byte)value.getMinute())
				.put((byte)value.getSecond())
				.array();
		}
	}
	/** Ritorna un intervallo temporale in minuti (interpretando 2 byte come HH MM) */
	public static class TimeHHMMConverter implements Converter<byte[], Integer>{
		@Override
		public Integer decode(final byte[] value){
			final ByteBuffer bb = ByteBuffer.wrap(value);
			final int hour = bb.get();
			final int minute = bb.get();
			return (hour * 60 + minute);
		}

		@Override
		public byte[] encode(final Integer value){
			final byte hour = (byte)(value / 60);
			final byte minute = (byte)(value - hour * 60);
			return ByteBuffer.allocate(2)
				.put(hour)
				.put(minute)
				.array();
		}
	}
	/** Ritorna un intervallo temporale in secondi (interpretando 3 byte come HH MM SS) */
	public static class TimeHHMMSSConverter implements Converter<byte[], Integer>{
		@Override
		public Integer decode(final byte[] value){
			final ByteBuffer bb = ByteBuffer.wrap(value);
			final int hour = bb.get();
			final int minute = bb.get();
			final int second = bb.get();
			return ((hour * 60 + minute) * 60 + second);
		}

		@Override
		public byte[] encode(final Integer value){
			final byte hour = (byte)(value / 60);
			final int tmp = value - hour * 60;
			final byte minute = (byte)(tmp / 60);
			final byte second = (byte)(tmp - minute * 60);
			return ByteBuffer.allocate(3)
				.put(hour)
				.put(minute)
				.put(second)
				.array();
		}
	}
	/** Ritorna un intervallo temporale in secondi (interpretando 6 byte come HH HH HH HH MM SS) */
	public static class TimeHHHHHHHHMMSSConverter implements Converter<byte[], Integer>{
		@Override
		public Integer decode(final byte[] value){
			final ByteBuffer bb = ByteBuffer.wrap(value);
			final int hour = bb.getInt();
			final int minute = bb.get();
			final int second = bb.get();
			return ((hour * 60 + minute) * 60 + second);
		}

		@Override
		public byte[] encode(final Integer value){
			final int hour = value / 60;
			final int tmp = value - hour * 60;
			final byte minute = (byte)(tmp / 60);
			final byte second = (byte)(tmp - minute * 60);
			return ByteBuffer.allocate(6)
				.putInt(hour)
				.put(minute)
				.put(second)
				.array();
		}
	}
	/** Reads a string considering each nibble as a decimal number (thus in the range 0x0-0x9) */
	public static class TextOfDecDigitsFromNibblesConverter implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			final byte[] result = new byte[value.length];
			int i = 0;
			for(byte chr : value){
				final byte highNibble = (byte)(chr >>> 4);
				final byte lowNibble = (byte)(chr & 0x0F);
				final int b = ByteHelper.compose(highNibble, (byte)0x0F, lowNibble, (byte)0x0F);
				result[i ++] = (byte)b;
			}
			return new String(result);
		}

		@Override
		public byte[] encode(final String value){
			ByteBuffer bb = ByteBuffer.allocate(value.length() << 1);
			for(int i = 0; i < value.length(); i ++){
				final char chr = value.charAt(i);
				final byte highNibble = (byte)(chr >>> 4);
				final byte lowNibble = (byte)(chr & 0x0F);
				bb.put(highNibble);
				bb.put(lowNibble);
			}
			return bb.array();
		}
	}
	/** Reads a string considering each nibble as an hexadecimal number (thus in the range 0x0-0xF) */
	public static class TextOfHexDigitsFromNibblesConverter implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			final byte[] result = new byte[value.length];
			int i = 0;
			for(byte chr : value){
				final byte highNibble = Integer.valueOf(Integer.toHexString((byte)(chr >>> 4))).byteValue();
				final byte lowNibble = Integer.valueOf(Integer.toHexString((byte)(chr & 0x0F))).byteValue();
				final int b = ByteHelper.compose(highNibble, (byte)0x0F, lowNibble, (byte)0x0F);
				result[i ++] = (byte)b;
			}
			return new String(result);
		}

		@Override
		public byte[] encode(final String value){
			ByteBuffer bb = ByteBuffer.allocate(value.length() << 1);
			for(int i = 0; i < value.length(); i ++){
				final char chr = value.charAt(i);
				final byte highNibble = (byte)(chr >>> 4);
				final byte lowNibble = (byte)(chr & 0x0F);
				bb.put(highNibble);
				bb.put(lowNibble);
			}
			return bb.array();
		}
	}
	/**
	 * Put a phone number as a string, using the high nibble of the first byte as the length of the number (plus the length itself)
	 * and the low nibble as a indicator of the presence of the '+' character, each nibble of the following byte are the number
	 */
	public static class PhoneNumberConverter implements Converter<byte[], String>{
		@Override
		public String decode(final byte[] value){
			final StringBuilder phoneNumber = new StringBuilder();
			//length of the phone number (plus the byte of the length)
			final int length = (value[0] >>> 4) - 1;
			if(length > 0){
				if((value[0] & 0x0F) != 0)
					//add the '+' because it's missing
					phoneNumber.append('+');
				for(int i = 0; i < length; i ++){
					phoneNumber.append(value[i + 1] >>> 4);

					final int lowNibble = value[i + 1] & 0x0F;
					if(lowNibble != 0x0F)
						phoneNumber.append(lowNibble);
				}
			}
			return phoneNumber.toString();
		}

		@Override
		public byte[] encode(final String value){
			final ByteBuffer bb = ByteBuffer.allocate(value.length() << 1);
			//length of the phone number (plus the byte of the length)
			final int numberLength = value.length() + 1;
			if(numberLength > 1){
				//add the '+' if needed
				final int numberType = (value.charAt(0) == '+'? 1: 0);
				bb.put((byte)ByteHelper.compose((byte)numberLength, (byte)0x0F, (byte)numberType, (byte)0x0F));

				final long evenNumberLength = ByteHelper.clearBit(numberLength, 0);
				for(int i = numberType; i < evenNumberLength; i ++)
					bb.put((byte)ByteHelper.compose((byte)value.charAt(i), (byte)0x0F, (byte)value.charAt(i + 1), (byte)0x0F));
				if(numberLength > evenNumberLength)
					bb.put((byte)ByteHelper.compose((byte)value.charAt(numberLength - 1), (byte)0x0F, (byte)0x0F, (byte)0x0F));
			}
			return bb.array();
		}
	}

	public static class StringDateTimeYYYYMMDDHHMMSSConverter implements Converter<String, ZonedDateTime>{
		@Override
		public ZonedDateTime decode(final String value){
			final int year = Integer.parseInt(value.substring(0, 4));
			final int month = Integer.parseInt(value.substring(4, 6));
			final int dayOfMonth = Integer.parseInt(value.substring(6, 8));
			final int hour = Integer.parseInt(value.substring(8, 10));
			final int minute = Integer.parseInt(value.substring(10, 12));
			final int second = Integer.parseInt(value.substring(12, 14));
			return DateTimeUtils.createFrom(year, month, dayOfMonth, hour, minute, second);
		}

		@Override
		public String encode(final ZonedDateTime value){
			StringBuilder sb = new StringBuilder();
			sb.append(StringUtils.leftPad(Integer.toString(value.getYear()), 4, '0'));
			sb.append(StringUtils.leftPad(Integer.toString(value.getMonthValue()), 2, '0'));
			sb.append(StringUtils.leftPad(Integer.toString(value.getDayOfMonth()), 2, '0'));
			sb.append(StringUtils.leftPad(Integer.toString(value.getHour()), 2, '0'));
			sb.append(StringUtils.leftPad(Integer.toString(value.getMinute()), 2, '0'));
			sb.append(StringUtils.leftPad(Integer.toString(value.getSecond()), 2, '0'));
			return sb.toString();
		}
	}
	public static class HexStringToIntConverter implements Converter<String, Integer>{
		@Override
		public Integer decode(final String value){
			return Integer.parseInt(value, 16);
		}

		@Override
		public String encode(final Integer value){
			return Integer.toString(value, 16)
				.toUpperCase(Locale.ROOT);
		}
	}

}
