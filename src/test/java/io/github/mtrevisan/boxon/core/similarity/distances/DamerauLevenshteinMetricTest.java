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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DamerauLevenshteinMetricTest{

	@Test
	void testNullThrows(){
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> DamerauLevenshteinMetric.<CharSequenceDistanceData>create()
				.similarity(null, CharSequenceDistanceData.of("kEvIn"))
		);
	}

	@Test
	void emptyStringTest(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(1., similarity, 0.);
		Assertions.assertEquals(0, distance);
	}

	@Test
	void testExactMatchSameCase(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("java");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("java");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(1., similarity, 0.);
		Assertions.assertEquals(0, distance);
	}

	@Test
	void testNoSimilarity(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("abc");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("def");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0., similarity, 0.);
		Assertions.assertEquals(3, distance);
	}

	@Test
	void score1(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("he");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("head");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.5, similarity, 0.0001);
		Assertions.assertEquals(2, distance);
	}

	@Test
	void score2(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("hd");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("head");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.5, similarity, 0.0001);
		Assertions.assertEquals(2, distance);
	}

	@Test
	void score3(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("d");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("head");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.25, similarity, 0.0001);
		Assertions.assertEquals(3, distance);
	}

	@Test
	void score4(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("head");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("he");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.5, similarity, 0.0001);
		Assertions.assertEquals(2, distance);
	}

	@Test
	void score5(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("kitten");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("sitting");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.5714, similarity, 0.0001);
		Assertions.assertEquals(3, distance);
	}

	@Test
	void score6(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create();
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("Saturday");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("Sunday");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.625, similarity, 0.0001);
		Assertions.assertEquals(3, distance);
	}

	@Test
	void score7(){
		DamerauLevenshteinMetric<CharSequenceDistanceData> metric = DamerauLevenshteinMetric.create(1, 1, 1,
			2);
		final CharSequenceDistanceData data1 = CharSequenceDistanceData.of("Saturday");
		final CharSequenceDistanceData data2 = CharSequenceDistanceData.of("Satudray");

		double similarity = metric.similarity(data1, data2);
		int distance = metric.distance(data1, data2);

		Assertions.assertEquals(0.75, similarity, 0.0001);
		Assertions.assertEquals(2, distance);
	}

}