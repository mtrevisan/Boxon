/*
 * Copyright (c) 2021-2022 Mauro Trevisan
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

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
class ExtractorTest{

	@Test
	void extractPlain() throws AnnotationException, TemplateException, ConfigurationException, JSONPathException{
		Version version = Version.of(1, 2);
		Extractor extractor = Extractor.create(version);

		Assertions.assertEquals(2, (int)extractor.get("/minor"));
	}

	@Test
	void extractFromArray() throws AnnotationException, TemplateException, ConfigurationException, JSONPathException{
		int[] array = new int[]{12, 23};
		Extractor extractor = Extractor.create(array);

		Assertions.assertEquals(23, (int)extractor.get("/1"));
	}

	@Test
	void extractFail() throws AnnotationException, TemplateException, ConfigurationException, JSONPathException{
		Version version = Version.of(1, 2);
		Extractor extractor = Extractor.create(version);

		Exception e = Assertions.assertThrows(JSONPathException.class,
			() -> extractor.get("/fake"));
		Assertions.assertEquals("No field 'fake' found on path '/fake'", e.getMessage());
	}

}
