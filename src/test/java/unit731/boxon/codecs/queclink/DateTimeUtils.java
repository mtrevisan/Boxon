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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


public class DateTimeUtils{

	private static final String PATTERN_DATETIME_PLAIN = "uMMddHHmmss";
	private static final String PATTERN_TIME_PLAIN = "HHmmss";
	private static final String PATTERN_DATETIME_ISO8601 = "u-MM-dd'T'HH:mm:ss[.SSS][.S][XXX][X]";

	private static final DateTimeFormatter FORMATTER_DATETIME_PLAIN = DateTimeFormatter.ofPattern(PATTERN_DATETIME_PLAIN);
	private static final DateTimeFormatter FORMATTER_TIME_PLAIN = DateTimeFormatter.ofPattern(PATTERN_TIME_PLAIN);
	private static final DateTimeFormatter FORMATTER_DATETIME_ISO8601 = DateTimeFormatter.ofPattern(PATTERN_DATETIME_ISO8601);
	public static final ZoneId DATE_TIME_ZONE = ZoneId.of("UTC");
	static{
		FORMATTER_DATETIME_PLAIN.withZone(DATE_TIME_ZONE);
		FORMATTER_TIME_PLAIN.withZone(DATE_TIME_ZONE);
		FORMATTER_DATETIME_ISO8601.withZone(DATE_TIME_ZONE);
	}


	private DateTimeUtils(){}

	public static ZonedDateTime createFromNow(){
		return ZonedDateTime.now(DATE_TIME_ZONE)
			.withNano(0);
	}

	public static ZonedDateTime createFrom(final int year, final int month, final int day, final int hour, final int minute, final int second){
		return ZonedDateTime.of(year, month, day, hour, minute, second, 0, DATE_TIME_ZONE);
	}

	public static ZonedDateTime createFrom(final long unixTime){
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(unixTime), DATE_TIME_ZONE);
	}

	public static ZonedDateTime parseDateTimePlain(final String plain){
		return parseDate(plain, FORMATTER_DATETIME_PLAIN);
	}

	public static LocalTime parseTimePlain(final String plain){
		return parseTime(plain, FORMATTER_TIME_PLAIN);
	}

	public static String formatDateTimePlain(final ZonedDateTime dateTime){
		return formatDateTime(dateTime, FORMATTER_DATETIME_PLAIN);
	}

	public static ZonedDateTime parseDateTimeIso8601(final String iso8601){
		return parseDate(iso8601, FORMATTER_DATETIME_ISO8601);
	}

	public static String formatDateTimeIso8601(final ZonedDateTime dateTime){
		return formatDateTime(dateTime, FORMATTER_DATETIME_ISO8601);
	}

	public static String formatDateTimeFromIso8601(final String iso8601DateTime, final String pattern, final ZoneId timezone){
		return formatDateTime(parseDateTimeIso8601(iso8601DateTime), pattern, timezone);
	}

	public static String formatDateTime(final ZonedDateTime dateTime, final String pattern, final ZoneId timezone){
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		formatter.withZone(timezone);
		return formatter.format(dateTime);
	}


	private static String formatDateTime(final ZonedDateTime dateTime, final DateTimeFormatter formatter){
		return formatter.format(dateTime);
	}

	private static ZonedDateTime parseDate(final String dateTime, final DateTimeFormatter formatter){
		return LocalDateTime.parse(dateTime, formatter)
			.atZone(DATE_TIME_ZONE);
	}

	private static LocalTime parseTime(final String dateTime, final DateTimeFormatter formatter){
		return LocalTime.parse(dateTime, formatter);
	}

}
