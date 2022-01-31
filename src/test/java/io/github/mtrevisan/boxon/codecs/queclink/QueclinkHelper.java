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
package io.github.mtrevisan.boxon.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;


@SuppressWarnings("ALL")
public class QueclinkHelper{

	private static final ZoneId DATE_TIME_ZONE = ZoneId.of("UTC");


	private QueclinkHelper(){}


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
		private static final byte[] HEX_CHAR_TABLE = {
			(byte)'0', (byte)'1', (byte)'2', (byte)'3',
			(byte)'4', (byte)'5', (byte)'6', (byte)'7',
			(byte)'8', (byte)'9', (byte)'a', (byte)'b',
			(byte)'c', (byte)'d', (byte)'e', (byte)'f'
		};

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

		private static String encodeHexString(final byte[] array) throws UnsupportedEncodingException{
			final byte[] hex = new byte[2 * array.length];
			int index = 0;
			for(int i = 0; i < array.length; i ++){
				final int v = array[i] & 0xFF;
				hex[index ++] = HEX_CHAR_TABLE[v >>> 4];
				hex[index ++] = HEX_CHAR_TABLE[v & 0x0F];
			}
			return new String(hex, StandardCharsets.US_ASCII);
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
			return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, DATE_TIME_ZONE);
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


	public static class StringDateTimeYYYYMMDDHHMMSSConverter implements Converter<String, ZonedDateTime>{
		@Override
		public ZonedDateTime decode(final String value){
			final int year = Integer.parseInt(value.substring(0, 4));
			final int month = Integer.parseInt(value.substring(4, 6));
			final int dayOfMonth = Integer.parseInt(value.substring(6, 8));
			final int hour = Integer.parseInt(value.substring(8, 10));
			final int minute = Integer.parseInt(value.substring(10, 12));
			final int second = Integer.parseInt(value.substring(12, 14));
			return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, DATE_TIME_ZONE);
		}

		@Override
		public String encode(final ZonedDateTime value){
			return StringUtils.leftPad(Integer.toString(value.getYear()), 4, '0')
				+ StringUtils.leftPad(Integer.toString(value.getMonthValue()), 2, '0')
				+ StringUtils.leftPad(Integer.toString(value.getDayOfMonth()), 2, '0')
				+ StringUtils.leftPad(Integer.toString(value.getHour()), 2, '0')
				+ StringUtils.leftPad(Integer.toString(value.getMinute()), 2, '0')
				+ StringUtils.leftPad(Integer.toString(value.getSecond()), 2, '0');
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


	/**
	 * Apply mask and shift right ({@code maskByte(27, 0x18) = 3}).
	 *
	 * @param value	The value to which to apply the mask and the right shift.
	 * @param mask	The mask.
	 * @return	The masked and shifter value.
	 */
	public static long applyMaskAndShift(final long value, long mask){
		final int ctz = Long.numberOfTrailingZeros(mask);
		return ((value & mask) >>> ctz);
	}

}
