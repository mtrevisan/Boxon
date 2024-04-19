/*
 * Copyright (c) 2021-2024 Mauro Trevisan
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
import io.github.mtrevisan.boxon.semanticversioning.VersionBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class ExtractorTest{

	@Test
	void plain() throws JSONPathException{
		Version version = VersionBuilder.of(1, 2);
		Extractor extractor = Extractor.create(version);

		int actual = extractor.get("/minor", null);

		Assertions.assertEquals(2, actual);
	}

	@Test
	void plainEscape() throws JSONPathException{
		Map<String, Object> map = Map.of("~1/è\\\"  %", "value");
		Extractor extractor = Extractor.create(map);

		String actual = extractor.get("/~01~1%C3%A8\\%x22+%20%25", null);

		Assertions.assertEquals("value", actual);
	}

	@Test
	void fromMap() throws JSONPathException{
		Map<String, Object> map = Map.of("key", "value");
		Extractor extractor = Extractor.create(map);

		String actual = extractor.get("/key", null);

		Assertions.assertEquals("value", actual);
	}

	@Test
	void fromList() throws JSONPathException{
		List<String> list = List.of("un", "do", "trè", "kuatro", "ŧinkue");

		String actual = Extractor.get("/2", list, null);

		Assertions.assertEquals("trè", actual);
	}

	@Test
	void fromArray() throws JSONPathException{
		int[] array = {12, 23};
		Extractor extractor = Extractor.create(array);

		int actual = extractor.get("/1", null);

		Assertions.assertEquals(23, actual);
	}

	@Test
	void failIndex(){
		int[] array = {12, 23};
		Extractor extractor = Extractor.create(array);

		Exception exc = Assertions.assertThrows(JSONPathException.class,
			() -> extractor.get("/01", null));
		Assertions.assertEquals("No array field '01' found on path '/01'", exc.getMessage());
	}

	@Test
	void defaultValue() throws JSONPathException{
		Version version = VersionBuilder.of(1, 2);
		Extractor extractor = Extractor.create(version);

		int fakeValue = extractor.get("/fake", -1);

		Assertions.assertEquals(-1, fakeValue);
	}

}
