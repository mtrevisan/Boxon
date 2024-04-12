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

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.regex.Pattern;


public final class QueclinkHelper{

	private QueclinkHelper(){}


	public static class VersionConverter implements Converter<byte[], Version>{
		@Override
		public Version decode(final byte[] value){
			return Version.of(value[0], value[1]);
		}

		@Override
		public byte[] encode(final Version value){
			return new byte[]{value.getMajor().byteValue(), value.getMinor().byteValue()};
		}
	}

	static class HexStringVersionConverter implements Converter<String, Version>{
		@Override
		public Version decode(final String value){
			final int major = Integer.parseInt(value.substring(0, 2), 16);
			final int minor = Integer.parseInt(value.substring(2, 4), 16);
			return Version.of(major, minor);
		}

		@Override
		public String encode(final Version value){
			if(value.isEmpty())
				return "";

			final Integer major = value.getMajor();
			final Integer minor = value.getMinor();
			final String maj = StringHelper.toHexString(major);
			final String min = StringHelper.toHexString(minor);
			return (major < 10? "0": "") + maj + (minor < 10? "0": "") + min;
		}
	}


	public static class IMEIConverter implements Converter<byte[], String>{

		private static final Pattern PATTERN = Pattern.compile("(?<=\\G\\d{2})");


		@Override
		public String decode(final byte[] value){
			final StringBuilder sb = new StringBuilder(15);
			for(int i = 0; i < 7; i ++){
				final int val = value[i] & 255;
				if(val < 10)
					sb.append('0');
				sb.append(val);
			}
			sb.append(applyMaskAndShift(value[7], (byte)0x0F));
			return sb.toString();
		}

		@Override
		public byte[] encode(final String value){
			final byte[] imei = new byte[8];
			final String[] components = PATTERN.split(value, 8);
			for(int i = 0; i < 8; i ++)
				imei[i] = Integer.valueOf(components[i]).byteValue();
			return imei;
		}
	}


	public static class DateTimeYYYYMMDDHHMMSSConverter implements Converter<byte[], LocalDateTime>{
		@Override
		public LocalDateTime decode(final byte[] value){
			final ByteBuffer bb = ByteBuffer.wrap(value);
			final int year = bb.getShort();
			final int month = bb.get();
			final int dayOfMonth = bb.get();
			final int hour = bb.get();
			final int minute = bb.get();
			final int second = bb.get();
			return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
		}

		@Override
		public byte[] encode(final LocalDateTime value){
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

	static class StringDateTimeYYYYMMDDHHMMSSConverter implements Converter<String, LocalDateTime>{
		@Override
		public LocalDateTime decode(final String value){
			final int year = Integer.parseInt(value.substring(0, 4));
			final int month = Integer.parseInt(value.substring(4, 6));
			final int dayOfMonth = Integer.parseInt(value.substring(6, 8));
			final int hour = Integer.parseInt(value.substring(8, 10));
			final int minute = Integer.parseInt(value.substring(10, 12));
			final int second = Integer.parseInt(value.substring(12, 14));
			return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second);
		}

		@Override
		public String encode(final LocalDateTime value){
			return StringHelper.leftPad(Integer.toString(value.getYear()), 4, '0')
				+ StringHelper.leftPad(Integer.toString(value.getMonthValue()), 2, '0')
				+ StringHelper.leftPad(Integer.toString(value.getDayOfMonth()), 2, '0')
				+ StringHelper.leftPad(Integer.toString(value.getHour()), 2, '0')
				+ StringHelper.leftPad(Integer.toString(value.getMinute()), 2, '0')
				+ StringHelper.leftPad(Integer.toString(value.getSecond()), 2, '0');
		}
	}


	static class HexStringToByteConverter implements Converter<String, Byte>{
		@Override
		public Byte decode(final String value){
			return (byte)Integer.parseInt(value, 16);
		}

		@Override
		public String encode(final Byte value){
			return StringHelper.leftPad(StringHelper.toHexString(value & 0xFF), 2, '0');
		}
	}


	static class HexStringToShortConverter implements Converter<String, Short>{
		@Override
		public Short decode(final String value){
			return (short)Integer.parseInt(value, 16);
		}

		@Override
		public String encode(final Short value){
			return StringHelper.leftPad(StringHelper.toHexString(value & 0xFFFF), 4, '0');
		}
	}


	/**
	 * Apply mask and shift right ({@code maskByte(27, 0x18) = 3}).
	 *
	 * @param value	The value to which to apply the mask and the right shift.
	 * @param mask	The mask.
	 * @return	The masked and shifter value.
	 */
	private static long applyMaskAndShift(final long value, final long mask){
		final int ctz = Long.numberOfTrailingZeros(mask);
		return ((value & mask) >>> ctz);
	}

}
