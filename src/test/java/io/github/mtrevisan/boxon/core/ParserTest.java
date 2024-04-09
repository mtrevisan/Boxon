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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.teltonika.MessageHex;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.utils.TestHelper;
import io.github.mtrevisan.boxon.utils.TimeWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;


class ParserTest{

	public static void main(String[] args) throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		//if it is wanted `headerLength` to be a variable and not a method:
		//- remove Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes); above
		//- change @BindString(size = "#prefixLength()") into @BindString(size = "#headerLength") in ACKMessageHex.messageHeader
		//- remove .withContext(ParserTest.class, "headerLength") below
		//- uncomment the below context map
//		Map<String, Object> context = Map.of(
//			"deviceTypes", deviceTypes,
//			"headerLength", 4);
		Core core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.withContext(context)
			.withContext(ParserTest.class, "headerLength")
			.create();
		Parser parser = Parser.create(core);

		//213-223 µs/msg = 4.5-4.7 kHz
		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");

		//warm-up
		for(int i = 0; i < 2_000; i ++)
			parser.parse(payload);

		TimeWatch watch = TimeWatch.start();
		for(int i = 0; i < 20_000; i ++)
			parser.parse(payload);
		watch.stop();

		System.out.println(watch.toString(20_000));
	}

	private static int headerLength(){
		return 4;
	}


	@Test
	void parseMultipleMessagesHex() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}

	@Test
	void parseMultipleMessagesASCII() throws AnnotationException, TemplateException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,40.5,2,0020,20170101123542,11F0$");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}

	@Test
	void parseMultipleMessagesHexASCII() throws NoSuchMethodException, AnnotationException, TemplateException, ConfigurationException,
			JSONPathException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S")
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		byte[] payload1 = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = TestHelper.toByteArray("+BCK:GTIOB,CF8002,359464038116666,45.5,2,0020,20170101123542,11F0$");
		byte[] payload = addAll(payload1, payload2);
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
		Assertions.assertEquals("+ACK", Extractor.get("/messageHeader", result.get(1).getMessage(), null));

		Response<Object, byte[]> compose = composer.compose(result.get(1).getMessage());
		if(compose.hasError())
			Assertions.fail(compose.getError());
		Assertions.assertEquals("+BCK:GTIOB,CF8002,359464038116666,45.5,2,0020,20170101123542,11F0$", StringHelper.toASCIIString(compose.getMessage()));
	}

	@Test
	void parseMultipleMessagesASCIIHex() throws AnnotationException, TemplateException, NoSuchMethodException, ConfigurationException{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S")
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplatesFrom(ACKMessageHex.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload1 = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,20170101123542,11F0$");
		byte[] payload = addAll(payload2, payload1);
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}


	private static byte[] addAll(final byte[] array1, final byte[] array2){
		final byte[] joinedArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}



	@Test
	void parseTeltonika() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplatesFrom(MessageHex.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000003608010000016B40D8EA30010000000000000000000000000000000105021503010101425E0F01F10000601A014E0000000000000000010000C7CF");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(1, result.size());
		if(result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
	}

}
