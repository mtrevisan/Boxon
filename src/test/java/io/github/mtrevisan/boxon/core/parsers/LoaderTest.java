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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageASCII;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.core.helpers.BitReader;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LoaderTest{

	@Test
	void loadFromMap(){
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
	}

	@Test
	void loadFromScan() throws Exception{
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();

		TemplateLoader templateLoader = TemplateLoader.create();
		templateLoader.loadTemplatesFrom(ACKMessageHex.class);
	}

	@Test
	void loadFromScanWithBasePackage() throws Exception{
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();

		TemplateLoader templateLoader = TemplateLoader.create();
		templateLoader.loadTemplatesFrom(ACKMessageASCII.class);
	}

	@Test
	void loadCodecsAfterTemplates(){
		CodecLoader.clearCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		Exception exc = Assertions.assertThrows(AnnotationException.class,
			() -> templateLoader.loadTemplatesFrom(LoaderTest.class));
		Assertions.assertTrue(exc.getMessage().startsWith("No data can be extracted from this class: "));
	}

	@Test
	void loadTemplate() throws Exception{
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		templateLoader.loadTemplate(ACKMessageHex.class);

		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		Template<?> template = templateLoader.getTemplate(reader);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(ACKMessageHex.class, template.getType());
	}

	@Test
	void cannotLoadTemplate(){
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();

		byte[] payload = StringHelper.hexToByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		TemplateException exc = Assertions.assertThrows(TemplateException.class, () -> templateLoader.getTemplate(reader));
		Assertions.assertEquals("Cannot find any template for given raw message", exc.getMessage());
	}

	@Test
	void findNextTemplate() throws Exception{
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		templateLoader.loadTemplate(TemplateTest.Message.class);

		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		int position = templateLoader.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextTemplate() throws Exception{
		CodecLoader.clearCodecs();
		CodecLoader.loadDefaultCodecs();
		TemplateLoader templateLoader = TemplateLoader.create();
		templateLoader.loadTemplate(TemplateTest.Message.class);

		byte[] payload = StringHelper.hexToByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		int position = templateLoader.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
