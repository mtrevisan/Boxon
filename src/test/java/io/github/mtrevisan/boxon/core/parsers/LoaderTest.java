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

import io.github.mtrevisan.boxon.core.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.codecs.queclink.ACKMessageHex;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
class LoaderTest{

	@Test
	void loadFromMap(){
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
	}

	@Test
	void loadFromScan() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();

		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplatesFrom(ACKMessageHex.class);
	}

	@Test
	void loadFromScanWithBasePackage() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();

		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplatesFrom(LoaderTest.class);
	}

	@Test
	void loadCodecsAfterTemplates(){
		LoaderCodec loaderCodec = LoaderCodec.create();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		Exception e = Assertions.assertThrows(AnnotationException.class,
			() -> loaderTemplate.loadTemplatesFrom(LoaderTest.class));
		Assertions.assertTrue(e.getMessage().startsWith("No data can be extracted from this class: "));
	}

	@Test
	void loadTemplate() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplate(ACKMessageHex.class);

		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		Template<?> template = loaderTemplate.getTemplate(reader);

		Assertions.assertNotNull(template);
		Assertions.assertEquals(ACKMessageHex.class, template.getType());
	}

	@Test
	void cannotLoadTemplate() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplatesFrom(LoaderTest.class);

		byte[] payload = StringHelper.toByteArray("3b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		Assertions.assertThrows(TemplateException.class, () -> loaderTemplate.getTemplate(reader));
	}

	@Test
	void findNextTemplate() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplatesFrom(LoaderTest.class);

		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		int position = loaderTemplate.findNextMessageIndex(reader);

		Assertions.assertEquals(36, position);
	}

	@Test
	void cannotFindNextTemplate() throws AnnotationException, TemplateException{
		LoaderCodec loaderCodec = LoaderCodec.create();
		loaderCodec.loadDefaultCodecs();
		LoaderTemplate loaderTemplate = LoaderTemplate.create(loaderCodec);
		loaderTemplate.loadTemplatesFrom(LoaderTest.class);

		byte[] payload = StringHelper.toByteArray("2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a");
		BitReaderInterface reader = BitReader.wrap(payload);
		int position = loaderTemplate.findNextMessageIndex(reader);

		Assertions.assertEquals(-1, position);
	}

}
