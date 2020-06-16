package unit731.boxon.codecs.queclink;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


public class ZonedDateTimeDeserializer extends JsonDeserializer<ZonedDateTime>{

	static final DateTimeFormatter ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX][X]");


	@Override
	public ZonedDateTime deserialize(final JsonParser jsonparser, final DeserializationContext deserializationcontext)
			throws IOException{
		final String dt = jsonparser.getText();
		return ZonedDateTime.from(ZONED_DATE_TIME_FORMATTER.parse(dt));
	}

}
