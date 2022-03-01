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

import io.github.mtrevisan.boxon.exceptions.JSONPathException;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


@SuppressWarnings("ALL")
class ExtractorTest{

	@Test
	void plain() throws JSONPathException{
		Version version = Version.of(1, 2);
		Extractor extractor = Extractor.create(version);

		Assertions.assertEquals(2, (int)extractor.get("/minor"));
	}

	@Test
	void plainEscape() throws JSONPathException{
		Map<String, Object> map = Map.of("~1/è\\\"  %", "value");
		Extractor extractor = Extractor.create(map);

		Assertions.assertEquals("value", extractor.get("/~01~1%C3%A8\\%x22+%20%25"));
	}

	@Test
	void fromMap() throws JSONPathException{
		Map<String, Object> map = Map.of("key", "value");
		Extractor extractor = Extractor.create(map);

		Assertions.assertEquals("value", extractor.get("/key"));
	}

	@Test
	void fromList() throws JSONPathException{
		List<String> list = List.of("un", "do", "trè", "kuatro", "ŧinkue");

		Assertions.assertEquals("trè", Extractor.get("/2", list));
	}

	@Test
	void fromArray() throws JSONPathException{
		int[] array = new int[]{12, 23};
		Extractor extractor = Extractor.create(array);

		Assertions.assertEquals(23, (int)extractor.get("/1"));
	}

	@Test
	void failReference(){
		Version version = Version.of(1, 2);
		Extractor extractor = Extractor.create(version);

		Exception e = Assertions.assertThrows(JSONPathException.class,
			() -> extractor.get("/fake"));
		Assertions.assertEquals("No field 'fake' found on path '/fake'", e.getMessage());
	}

	@Test
	void failIndex(){
		int[] array = new int[]{12, 23};
		Extractor extractor = Extractor.create(array);

		Exception e = Assertions.assertThrows(JSONPathException.class,
			() -> extractor.get("/01"));
		Assertions.assertEquals("No array field '01' found on path '/01'", e.getMessage());
	}

}
