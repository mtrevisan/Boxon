/*
 * Copyright (c) 2019-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.semanticversioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionCoreTest{

	@Test
	void shouldIgnoreBuildMetadataWhenDeterminingVersionPrecedence(){
		Version v1 = VersionBuilder.of("1.3.7-beta");
		Version v2 = VersionBuilder.of("1.3.7-beta+build.1");
		Version v3 = VersionBuilder.of("1.3.7-beta+build.2");

		Assertions.assertEquals(0, v1.compareTo(v2));
		Assertions.assertEquals(0, v1.compareTo(v3));
		Assertions.assertEquals(0, v2.compareTo(v3));
	}

	@Test
	void shouldHaveGreaterThanMethodReturningBoolean(){
		Version v1 = VersionBuilder.of("2.3.7");
		Version v2 = VersionBuilder.of("1.3.7");

		Assertions.assertTrue(v1.isGreaterThan(v2));
		Assertions.assertFalse(v2.isGreaterThan(v1));
		Assertions.assertFalse(v1.isGreaterThan(v1));
	}

	@Test
	void shouldHaveGreaterThanOrEqualToMethodReturningBoolean(){
		Version v1 = VersionBuilder.of("2.3.7");
		Version v2 = VersionBuilder.of("1.3.7");

		Assertions.assertTrue(v1.isGreaterThanOrEqualTo(v2));
		Assertions.assertFalse(v2.isGreaterThanOrEqualTo(v1));
		Assertions.assertTrue(v1.isGreaterThanOrEqualTo(v1));
	}

	@Test
	void shouldHaveLessThanMethodReturningBoolean(){
		Version v1 = VersionBuilder.of("2.3.7");
		Version v2 = VersionBuilder.of("1.3.7");

		Assertions.assertFalse(v1.isLessThan(v2));
		Assertions.assertTrue(v2.isLessThan(v1));
		Assertions.assertFalse(v1.isLessThan(v1));
	}

	@Test
	void shouldHaveLessThanOrEqualToMethodReturningBoolean(){
		Version v1 = VersionBuilder.of("2.3.7");
		Version v2 = VersionBuilder.of("1.3.7");

		Assertions.assertFalse(v1.isLessThanOrEqualTo(v2));
		Assertions.assertTrue(v2.isLessThanOrEqualTo(v1));
		Assertions.assertTrue(v1.isLessThanOrEqualTo(v1));
	}

	@Test
	void shouldOverrideEqualsMethod(){
		Version v1 = VersionBuilder.of("2.3.7");
		Version v2 = VersionBuilder.of("2.3.7");
		Version v3 = VersionBuilder.of("1.3.7");

		Assertions.assertEquals(v1, v2);
		Assertions.assertNotEquals(v1, v3);
	}

	@Test
	void shouldCorrectlyCompareAllVersionsFromSpecification(){
		String[] versions = {"1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta", "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1",
			"1.0.0", "2.0.0", "2.1.0", "2.1.1"};
		for(int i = 1, length = versions.length; i < length; i ++){
			Version v1 = VersionBuilder.of(versions[i - 1]);
			Version v2 = VersionBuilder.of(versions[i]);

			Assertions.assertTrue(v1.isLessThan(v2));
		}
	}

	@Test
	void shouldBeAbleToCompareWithoutIgnoringBuildMetadata(){
		Version v1 = VersionBuilder.of("1.3.7-beta+build.1");
		Version v2 = VersionBuilder.of("1.3.7-beta+build.2");
		Assertions.assertEquals(0, v1.compareTo(v2));
		Assertions.assertTrue(0 > v1.compareToWithBuilds(v2));
	}

}
