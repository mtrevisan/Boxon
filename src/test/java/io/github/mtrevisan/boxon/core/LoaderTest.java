/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mtrevisan.boxon.core.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
class LoaderTest{

	@Test
	void loadFromMap(){
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();
	}

	@Test
	void loadFromScan() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();

		loader.loadDefaultTemplates();
	}

	@Test
	void loadFromScanWithBasePackage() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();

		loader.loadTemplates(LoaderTest.class);
	}

	@Test
	void loadCodecsAfterTemplates(){
		Loader loader = Loader.create();
		Exception e = Assertions.assertThrows(AnnotationException.class,
			() -> loader.loadTemplates(LoaderTest.class));
		Assertions.assertTrue(e.getMessage().startsWith("No data can be extracted from this class: "));
	}

	@Test
	void loadTemplate() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();
		loader.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		Template<?> template = loader.getTemplate(reader);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(ACKMessageHex.class, template.getType());
	}

	@Test
	void loadConfiguration() throws AnnotationException, ConfigurationException, JsonProcessingException{
		Loader loader = Loader.create();
		loader.loadConfigurations(LoaderTest.class);

		List<Map<String, Object>> configuration = loader.getConfiguration("1.19");

		Assertions.assertEquals(1, configuration.size());

		ObjectMapper mapper = new ObjectMapper();
		String jsonHeader = mapper.writeValueAsString(configuration.get(0).get("header"));
		String jsonFields = mapper.writeValueAsString(configuration.get(0).get("fields"));

		Assertions.assertEquals("{\"longDescription\":\"The command AT+GTREG is used to do things.\",\"maxProtocol\":\"2.8\",\"charset\":\"UTF-8\",\"shortDescription\":\"AT+GTREG\"}", jsonHeader);
		Assertions.assertEquals("{\"Weekday\":{\"defaultValue\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"mutuallyExclusive\":false,\"enumeration\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"writable\":true},\"Update Over-The-Air\":{\"defaultValue\":[\"FALSE\"],\"mutuallyExclusive\":true,\"enumeration\":[\"TRUE\",\"FALSE\"],\"writable\":true},\"Header\":{\"defaultValue\":\"GTREG\",\"writable\":false},\"Download protocol\":{\"defaultValue\":[\"HTTP\"],\"mutuallyExclusive\":true,\"enumeration\":[\"HTTP\",\"HTTPS\"],\"writable\":false},\"Download URL\":{\"format\":\".{0,100}\",\"writable\":true},\"Update mode\":{\"defaultValue\":1,\"format\":\"[0-1]\",\"writable\":true},\"Maximum download retry count\":{\"defaultValue\":0,\"format\":\"[0-3]\",\"writable\":true},\"Message counter\":{\"minValue\":0,\"maxValue\":65535,\"writable\":false},\"Operation mode\":{\"defaultValue\":0,\"format\":\"[0-3]\",\"writable\":true},\"Password\":{\"defaultValue\":\"gb200s\",\"format\":\"[0-9a-zA-Z]{4,20}\",\"writable\":true},\"Download timeout\":{\"minValue\":5,\"unitOfMeasure\":\"min\",\"maxValue\":30,\"defaultValue\":20,\"writable\":true}}", jsonFields);
	}

	@Test
	void cannotLoadTemplate() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();
		loader.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		Assertions.assertThrows(TemplateException.class, () -> loader.getTemplate(reader));
	}

	@Test
	void findNextTemplate() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();
		loader.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextTemplate() throws AnnotationException, TemplateException{
		Loader loader = Loader.create();
		loader.loadDefaultCodecs();
		loader.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loader.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
