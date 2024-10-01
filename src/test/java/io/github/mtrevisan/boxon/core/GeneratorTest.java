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

import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.DeviceTypes;
import io.github.mtrevisan.boxon.core.codecs.queclink.REGConfigurationASCII;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.utils.PrettyPrintMap;
import io.github.mtrevisan.boxon.utils.TestHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


class GeneratorTest{

	@Test
	void generateParsing() throws Exception{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.create();
		Describer describer = Describer.create(core);
		Map<String, Object> description = new HashMap<>(describer.describeParsing(ACKMessageASCII.class));
		final String template = (String)description.get(DescriberKey.TEMPLATE.toString());
		description.put(DescriberKey.TEMPLATE.toString(), template + "_other");

		Generator generator = Generator.create(core);
		Class<?> dynamicType = generator.generateTemplate(description);

		Assertions.assertNotNull(dynamicType);


		core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withTemplate(dynamicType)
			.create();
		Map<String, Object> description2 = new HashMap<>(describer.describeParsing(dynamicType));
		Assertions.assertEquals(PrettyPrintMap.toString(description), PrettyPrintMap.toString(description2));
		Parser parser = Parser.create(core);
		byte[] payload = TestHelper.toByteArray("+ACK:GTIOB,468002,359464038116666,45.5,2,0020,,,20170101123542,11F0$+ACK:GTIOB,468002,359464038116666,40.5,2,0020,,,20270101123542,11F0$");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertEquals(2, result.size());
		if(result.get(0).hasError())
			Assertions.fail(result.get(0).getError());
		if(result.get(1).hasError())
			Assertions.fail(result.get(1).getError());
	}

	@Test
	void generateTemplate() throws Exception{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.create();
		Describer describer = Describer.create(core);
		Map<String, Object> description = new HashMap<>(describer.describeParsing(ACKMessageASCII.class));
		final String template = (String)description.get(DescriberKey.TEMPLATE.toString());
		description.put(DescriberKey.TEMPLATE.toString(), template + "_other");

		Generator generator = Generator.create(core);
		Class<?> dynamicType = generator.generateTemplate(description);

		Assertions.assertNotNull(dynamicType);
	}

	@Test
	void generateConfigurations() throws Exception{
		DeviceTypes deviceTypes = DeviceTypes.create()
			.with((byte)0x46, "QUECLINK_GB200S");
		Core core = CoreBuilder.builder()
//			.withEventListener(EventLogger.getInstance())
			.withContext("deviceTypes", deviceTypes)
			.withContext(ParserTest.class.getDeclaredMethod("headerLength"))
			.withDefaultCodecs()
			.withConfiguration(REGConfigurationASCII.class)
			.create();
		Describer describer = Describer.create(core);
		List<Map<String, Object>> descriptions = describer.describeConfiguration();
		Map<String, Object> description = new HashMap<>(descriptions.getFirst());
		final String configuration = (String)description.get(DescriberKey.CONFIGURATION.toString());
		description.put(DescriberKey.CONFIGURATION.toString(), configuration + "_other");

		Generator generator = Generator.create(core);
		Class<?> dynamicType = generator.generateConfiguration(description);

		Assertions.assertNotNull(dynamicType);
	}

}
