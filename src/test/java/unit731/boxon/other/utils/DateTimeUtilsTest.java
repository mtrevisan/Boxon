package unit731.boxon.other.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import unit731.boxon.codecs.queclink.DateTimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DateTimeUtilsTest{

	@Test
	void getDate(){
		ZonedDateTime datetime = DateTimeUtils.createFrom(2019, 1, 21, 21, 30, 0);

		assertEquals(ZonedDateTime.of(2019, 1, 21, 21, 30, 0, 0, ZoneId.of("UTC")), datetime);


		Throwable exception = Assertions.assertThrows(DateTimeException.class,
			() -> DateTimeUtils.createFrom(2019, 13, 21, 21, 30, 0));
		Assertions.assertEquals("Invalid value for MonthOfYear (valid values 1 - 12): 13", exception.getMessage());


		datetime = DateTimeUtils.createFrom(1_548_106_200l);

		assertEquals(ZonedDateTime.of(2019, 1, 21, 21, 30, 0, 0, ZoneId.of("UTC")), datetime);


		exception = Assertions.assertThrows(DateTimeException.class, () -> DateTimeUtils.createFrom(100_000_000_000_000_000l));
		Assertions.assertEquals("Instant exceeds minimum or maximum instant", exception.getMessage());
	}

	@Test
	void parseDateIso8601(){
		ZonedDateTime datetime = DateTimeUtils.parseDateTimeIso8601("2019-01-21T21:30:00Z");

		assertEquals(ZonedDateTime.of(2019, 1, 21, 21, 30, 0, 0, ZoneId.of("UTC")), datetime);


		Throwable exception = Assertions.assertThrows(DateTimeParseException.class, () -> DateTimeUtils.parseDateTimeIso8601("2019-13-21T21:30:00Z"));
		Assertions.assertEquals("Text '2019-13-21T21:30:00Z' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13", exception.getMessage());
	}

	@Test
	void formatDate(){
		ZoneId zoneId = ZoneId.of("UTC");
		String format = DateTimeUtils.formatDateTime(ZonedDateTime.of(2019, 1, 21, 21, 30, 0, 0, zoneId), "u/MM/dd HH:mm:ss'Z'", zoneId);

		assertEquals("2019/01/21 21:30:00Z", format);


		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.formatDateTime(ZonedDateTime.of(2019, 1, 21, 21, 30, 0, 0, zoneId), "r/MM/dd HH:mm:ss'Z'", zoneId));
		Assertions.assertEquals("Unknown pattern letter: r", exception.getMessage());


		format = DateTimeUtils.formatDateTimeFromIso8601("2019-01-21T21:30:00Z", "u/MM/dd HH:mm:ss'Z'", zoneId);

		assertEquals("2019/01/21 21:30:00Z", format);


		exception = Assertions.assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.formatDateTimeFromIso8601("2019-01-21T21:30:00Z", "r/MM/dd HH:mm:ss'Z'", zoneId));
		Assertions.assertEquals("Unknown pattern letter: r", exception.getMessage());
	}

}
