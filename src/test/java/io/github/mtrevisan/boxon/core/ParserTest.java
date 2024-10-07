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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.teltonika.MessageHex;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.utils.TestHelper;
import io.github.mtrevisan.boxon.utils.TimeWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;


class ParserTest{

	public static void main(String[] args) throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.withContext(context)
			.withContext(ParserTest.class, "headerLength")
			.build();
		Parser parser = Parser.create(core)
			.withMaxSpELMemoizerSize(50);

		//20220301: 109-111 µs/msg		= 9-9.2 kHz
		//20240424: 50-53.8 µs/msg		= 18.6-20 kHz (2.1×)
		//20240513: 49-51 µs/msg		= 19.8-20.4 kHz (2.2×)
		//20240607: 43.9-47.6 µs/msg	= 21-22.8 kHz (2.4×)
		//20241007: 21.3-22.1 µs/msg	= 45.2-46.9 kHz (5.1×) - cached SpEL
		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");

		//warm-up
		for(int i = 0; i < 2_000; i ++)
			parser.parse(payload);

		TimeWatch watch = TimeWatch.start();
		for(int i = 0; i < 20_000; i ++)
			parser.parse(payload);
		watch.stop();

		System.out.println(watch.toString(20_000 * 2) + " (" + watch.toStringAsFrequency(20_000 * 2) + ")");
	}

	private static int headerLength(){
		return 4;
	}

	private static int headerLength2(final int size){
		return size;
	}


	@TemplateHeader(start = "+UNV")
	static class NonByteMultipleLengths{
		@BindString(size = "4")
		String messageHeader;
		@BindInteger(size = "3")
		byte number0;
		@BindString(size = "3")
		public String text;
		@BindInteger(size = "5")
		byte number1;
	}

	@TemplateHeader(start = "+UNV")
	static class NonByteMultipleLengths2{
		@BindString(size = "4")
		String messageHeader;
		@BindInteger(size = "11")
		short number0;
		@BindString(size = "3")
		public String text;
		@BindInteger(size = "5")
		byte number1;
	}

	@TemplateHeader(start = "+UNV")
	static class NonByteMultipleLengths3{
		@BindString(size = "4")
		String messageHeader;
		@BindInteger(size = "19")
		int number0;
		@BindString(size = "3")
		public String text;
		@BindInteger(size = "5")
		byte number1;
	}

	@Test
	void parseMultipleMessagesHex() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.build();
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
	void parseMultipleMessagesASCII() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withDefaultCodecs()
			.withTemplate(ACKMessageASCII.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$+ACK:GTIOB,CF8002,359464038116666,40.5,2,0020,,,20170101123542,11F0$");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}

	@Test
	void parseMultipleMessagesHexASCII() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S")
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.withTemplate(ACKMessageASCII.class)
			.build();
		Parser parser = Parser.create(core);
		Composer composer = Composer.create(core);

		byte[] payload1 = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = TestHelper.toByteArray("+BCK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$");
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
		Assertions.assertEquals("+BCK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$",
			StringHelper.toASCIIString(compose.getMessage()));
	}

	@Test
	void parseMultipleMessagesASCIIHex() throws Exception{
		DeviceTypes<Byte> deviceTypes = DeviceTypes.<Byte>create()
			.with((byte)0x46, "QUECLINK_GB200S")
			.with((byte)0xCF, "QUECLINK_GV350M");
		Map<String, Object> context = Collections.singletonMap("deviceTypes", deviceTypes);
		Core core = CoreBuilder.builder()
			.withContext(context)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(ACKMessageHex.class)
			.withTemplate(ACKMessageASCII.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload1 = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		byte[] payload2 = TestHelper.toByteArray("+ACK:GTIOB,CF8002,359464038116666,45.5,2,0020,,,20170101123542,11F0$");
		byte[] payload = addAll(payload2, payload1);
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}

	@Test
	void parseNonByteMultipleLengthsMessage() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(NonByteMultipleLengths.class)
			.build();
		Parser parser = Parser.create(core);

		//0x2B 0x55 0x4E 0x56 0b110 0x42 0x43 0x44 0b10110
		//2B 55 4E 56 110 0100_0010 0100_0011 0100_0100 10110
		//2B 55 4E 56 1100_1000 0100_1000 0110_1000 1001_0110
		//2B 55 4E 56 C8 48 68 96
		byte[] payload = StringHelper.hexToByteArray("2B554E56C8486896");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		NonByteMultipleLengths message = (NonByteMultipleLengths)response.getMessage();
		Assertions.assertEquals("+UNV", message.messageHeader);
		Assertions.assertEquals(0b0000_0110, message.number0);
		Assertions.assertEquals("BCD", message.text);
		Assertions.assertEquals(0b0001_0110, message.number1);
	}

	@Test
	void parseNonByteMultipleLengths2Message() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(NonByteMultipleLengths2.class)
			.build();
		Parser parser = Parser.create(core);

		//0x2B 0x55 0x4E 0x56 0b110 0x00 0x42 0x43 0x44 0b10110
		//2B 55 4E 56 110 0000_0000 0100_0010 0100_0011 0100_0100 10110
		//2B 55 4E 56 1100_0000 0000_1000 0100_1000 0110_1000 1001_0110
		//2B 55 4E 56 C0 08 48 68 96
		byte[] payload = StringHelper.hexToByteArray("2B554E56C008486896");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		NonByteMultipleLengths2 message = (NonByteMultipleLengths2)response.getMessage();
		Assertions.assertEquals("+UNV", message.messageHeader);
		Assertions.assertEquals(0b0000_0110_0000_0000, message.number0);
		Assertions.assertEquals("BCD", message.text);
		Assertions.assertEquals(0b0001_0110, message.number1);
	}

	@Test
	void parseNonByteMultipleLengths3Message() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(NonByteMultipleLengths3.class)
			.build();
		Parser parser = Parser.create(core);

		//0x2B 0x55 0x4E 0x56 0b110 0x00 0x42 0x43 0x44 0b10110
		//2B 55 4E 56 110 0000_0000 0100_0010 0100_0011 0100_0100 10110
		//2B 55 4E 56 1100_0000 0000_1000 0100_1000 0110_1000 1001_0110
		//2B 55 4E 56 C0 08 48 68 96
		byte[] payload = StringHelper.hexToByteArray("2B554E56C00008486896");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.getFirst();
		if(response.hasError())
			Assertions.fail(response.getError());
		NonByteMultipleLengths3 message = (NonByteMultipleLengths3)response.getMessage();
		Assertions.assertEquals("+UNV", message.messageHeader);
		Assertions.assertEquals(0b0000_0110_0000_0000_0000_0000, message.number0);
		Assertions.assertEquals("BCD", message.text);
		Assertions.assertEquals(0b0001_0110, message.number1);
	}


	private static byte[] addAll(final byte[] array1, final byte[] array2){
		final byte[] joinedArray = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}



	@Test
	void parseTeltonika08_1() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(MessageHex.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000003608010000016B40D8EA30010000000000000000000000000000000105021503010101425E0F01F10000601A014E0000000000000000010000C7CF");
		List<Response<byte[], Object>> result = parser.parse(payload);

		if(result.size() > 1 || result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
		Assertions.assertEquals(1, result.size());
	}

	@Test
	void parseTeltonika08_2() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(MessageHex.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000002808010000016B40D9AD80010000000000000000000000000000000103021503010101425E100000010000F22A");
		List<Response<byte[], Object>> result = parser.parse(payload);

		if(result.size() > 1 || result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
		Assertions.assertEquals(1, result.size());
	}

	@Test
	void parseTeltonika08_3() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(MessageHex.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000004308020000016B40D57B480100000000000000000000000000000001010101000000000000016B40D5C198010000000000000000000000000000000101010101000000020000252C");
		List<Response<byte[], Object>> result = parser.parse(payload);

		if(result.size() > 1 || result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
		Assertions.assertEquals(1, result.size());
	}

	@Test
	void parseTeltonika8E() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(MessageHex.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000004A8E010000016B412CEE000100000000000000000000000000000000010005000100010100010011001D00010010015E2C880002000B000000003544C87A000E000000001DD7E06A00000100002994");
		List<Response<byte[], Object>> result = parser.parse(payload);

		if(result.size() > 1 || result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
		Assertions.assertEquals(1, result.size());
	}

	@Test
	void parseTeltonika10() throws Exception{
		Core core = CoreBuilder.builder()
			.withDefaultCodecs()
			.withTemplate(MessageHex.class)
			.build();
		Parser parser = Parser.create(core);

		byte[] payload = StringHelper.hexToByteArray("000000000000005F10020000016BDBC7833000000000000000000000000000000000000B05040200010000030002000B00270042563A00000000016BDBC7871800000000000000000000000000000000000B05040200010000030002000B00260042563A00000200005FB3");
		List<Response<byte[], Object>> result = parser.parse(payload);

		if(result.size() > 1 || result.getFirst().hasError())
			Assertions.fail(result.getFirst().getError());
		Assertions.assertEquals(1, result.size());
	}

}
