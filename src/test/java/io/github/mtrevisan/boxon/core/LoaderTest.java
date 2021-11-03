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
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
class LoaderTest{

	@Test
	void loadFromMap(){
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();
	}

	@Test
	void loadFromScan() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();

		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadDefaultTemplates();
	}

	@Test
	void loadFromScanWithBasePackage() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();

		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadTemplates(LoaderTest.class);
	}

	@Test
	void loadCodecsAfterTemplates(){
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		Exception e = Assertions.assertThrows(AnnotationException.class,
			() -> loaderTemplate.loadTemplates(LoaderTest.class));
		Assertions.assertTrue(e.getMessage().startsWith("No data can be extracted from this class: "));
	}

	@Test
	void loadTemplate() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		Template<?> template = loaderTemplate.getTemplate(reader);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(ACKMessageHex.class, template.getType());
	}

	@Test
	void loadConfiguration() throws AnnotationException, ConfigurationException, JsonProcessingException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderConfiguration loaderConfiguration = new LoaderConfiguration(eventListener);
		loaderConfiguration.loadConfigurations(LoaderTest.class);

		List<Map<String, Object>> configuration = loaderConfiguration.getConfigurations("1.19");

		Assertions.assertEquals(1, configuration.size());

		ObjectMapper mapper = new ObjectMapper();
		String jsonHeader = mapper.writeValueAsString(configuration.get(0).get("header"));
		String jsonFields = mapper.writeValueAsString(configuration.get(0).get("fields"));

		Assertions.assertEquals("{\"longDescription\":\"The command AT+GTREG is used to do things.\",\"shortDescription\":\"AT+GTREG\"}", jsonHeader);
		Assertions.assertEquals("{\"Maximum download retry count\":{\"minValue\":0,\"maxValue\":3,\"defaultValue\":0},\"Download timeout\":{\"minValue\":5,\"unitOfMeasure\":\"min\",\"maxValue\":30,\"defaultValue\":20},\"Weekday\":{\"defaultValue\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"],\"enumeration\":[\"MONDAY\",\"TUESDAY\",\"WEDNESDAY\",\"THURSDAY\",\"FRIDAY\",\"SATURDAY\",\"SUNDAY\"]},\"Update Over-The-Air\":{\"defaultValue\":\"FALSE\",\"mutuallyExclusive\":true,\"enumeration\":[\"TRUE\",\"FALSE\"]},\"Header\":{\"charset\":\"UTF-8\",\"defaultValue\":\"GTREG\"},\"Download protocol\":{\"defaultValue\":\"HTTP\",\"mutuallyExclusive\":true,\"enumeration\":[\"HTTP\",\"HTTPS\"]},\"Download URL\":{\"pattern\":\".{0,100}\",\"charset\":\"UTF-8\",\"fields\":{\"URL\":{\"pattern\":\"https?://.{0,92}\"},\"password\":{\"pattern\":\".{1,32}\"},\"username\":{\"pattern\":\".{1,32}\"}}},\"Update mode\":{\"minValue\":0,\"maxValue\":1,\"defaultValue\":1},\"Message counter\":{\"minValue\":0,\"maxValue\":65535},\"Operation mode\":{\"minValue\":0,\"maxValue\":3,\"defaultValue\":0},\"Motion report interval\":{\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600},\"Password\":{\"charset\":\"UTF-8\",\"defaultValue\":\"gb200s\",\"pattern\":\"[0-9a-zA-Z]{4,20}\"},\"Motionless report interval\":{\"minValue\":90,\"unitOfMeasure\":\"s\",\"maxValue\":86400,\"defaultValue\":3600}}", jsonFields);
	}

	@Test
	void cannotLoadTemplate() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		Assertions.assertThrows(TemplateException.class, () -> loaderTemplate.getTemplate(reader));
	}

	@Test
	void findNextTemplate() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loaderTemplate.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextTemplate() throws AnnotationException, TemplateException{
		EventListener eventListener = EventListener.getNoOpInstance();
		LoaderCodec loaderCodec = new LoaderCodec(eventListener);
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderTemplate.loadTemplates(LoaderTest.class);

		byte[] payload = JavaHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReader reader = BitReader.wrap(payload);
		int position = loaderTemplate.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
