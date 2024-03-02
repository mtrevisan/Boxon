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
package io.github.mtrevisan.boxon.core.similarity.distances;

import io.github.mtrevisan.boxon.core.similarity.distances.CharSequenceDistanceData;
import io.github.mtrevisan.boxon.core.similarity.distances.LevenshteinDistance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LevenshteinDistanceTest{

	@Test
	void testNullThrows(){
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> {
				LevenshteinDistance.similarity(null, CharSequenceDistanceData.of("kEvIn"));
			}
		);
	}

	@Test
	void emptyStringTest(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of(""),
			CharSequenceDistanceData.of(""));

		Assertions.assertEquals(1., response, 0.);
	}

	@Test
	void testExactMatchSameCase(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("java"),
			CharSequenceDistanceData.of("java"));

		Assertions.assertEquals(1., response, 0.);
	}

	@Test
	void testNoSimilarity(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("abc"),
			CharSequenceDistanceData.of("def"));

		Assertions.assertEquals(0., response, 0.);
	}

	@Test
	void score1(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("he"),
			CharSequenceDistanceData.of("head"));

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	void score2(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("hd"),
			CharSequenceDistanceData.of("head"));

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	void score3(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("d"),
			CharSequenceDistanceData.of("head"));

		Assertions.assertEquals(0.25, response, 0.0001);
	}

	@Test
	void score4(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("head"),
			CharSequenceDistanceData.of("he"));

		Assertions.assertEquals(0.5, response, 0.0001);
	}

	@Test
	void score5(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("kitten"),
			CharSequenceDistanceData.of("sitting"));

		Assertions.assertEquals(0.5714, response, 0.0001);
	}

	@Test
	void score6(){
		double response = LevenshteinDistance.similarity(
			CharSequenceDistanceData.of("Saturday"),
			CharSequenceDistanceData.of("Sunday"));

		Assertions.assertEquals(0.625, response, 0.0001);
	}

}