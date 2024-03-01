/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.similarity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DamerauLevenshteinDistanceTest{

	@Test
	public void testNullThrows(){
		Assertions.assertThrows(
			NullPointerException.class,
			() -> {
				DamerauLevenshteinDistance.similarity(null, "kEvIn");
			}
		);
	}

	@Test
	public void emptyStringTest(){
		double response = DamerauLevenshteinDistance.similarity("", "");

		Assertions.assertEquals(1., response, 0.);
	}

	@Test
	public void testExactMatchSameCase(){
		double response = DamerauLevenshteinDistance.similarity("java", "java");

		Assertions.assertEquals(1., response, 0.);
	}

	@Test
	public void testNoSimilarity(){
		double response = DamerauLevenshteinDistance.similarity("abc", "def");

		Assertions.assertEquals(0., response, 0.);
	}

	@Test
	public void score1(){
		double response = DamerauLevenshteinDistance.similarity("he", "head");

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	public void score2(){
		double response = DamerauLevenshteinDistance.similarity("hd", "head");

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	public void score3(){
		double response = DamerauLevenshteinDistance.similarity("d", "head");

		Assertions.assertEquals(0.25, response, 0.0001);
	}

	@Test
	public void score4(){
		double response = DamerauLevenshteinDistance.similarity("head", "he");

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	public void score5(){
		double response = DamerauLevenshteinDistance.similarity("kitten", "sitting");

		Assertions.assertEquals(0.5714, response, 0.0001);
	}

	@Test
	public void score6(){
		double response = DamerauLevenshteinDistance.similarity("Saturday", "Sunday");

		Assertions.assertEquals(0.625, response, 0.0001);
	}

}