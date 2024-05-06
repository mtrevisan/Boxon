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
package io.github.mtrevisan.boxon.core.similarity.distances.metrics;

import io.github.mtrevisan.boxon.core.similarity.distances.StringMetricData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class DamerauLevenshteinMetricTest{

	@Test
	void testNullThrows(){
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> DamerauLevenshteinMetric.<StringMetricData>create()
				.similarity(null, StringMetricData.of("kEvIn"))
		);
	}

	@Test
	void emptyStringTest(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("");
		StringMetricData data2 = StringMetricData.of("");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(0, distance);
		Assertions.assertEquals(1., similarity, 0.);
	}

	@Test
	void testExactMatchSameCase(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("java");
		StringMetricData data2 = StringMetricData.of("java");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(0, distance);
		Assertions.assertEquals(1., similarity, 0.);
	}

	@Test
	void testNoSimilarity(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("abc");
		StringMetricData data2 = StringMetricData.of("def");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0., similarity, 0.);
	}

	@Test
	void score1(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("he");
		StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score2(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("hd");
		StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score3(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("d");
		StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.25, similarity, 0.0001);
	}

	@Test
	void score4(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("head");
		StringMetricData data2 = StringMetricData.of("he");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score5(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("kitten");
		StringMetricData data2 = StringMetricData.of("sitting");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.5714, similarity, 0.0001);
	}

	@Test
	void score6(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("Saturday");
		StringMetricData data2 = StringMetricData.of("Sunday");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.625, similarity, 0.0001);
	}

	@Test
	void score7(){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create(1, 1, 1,
			2);
		StringMetricData data1 = StringMetricData.of("Saturday");
		StringMetricData data2 = StringMetricData.of("Satudray");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(6./7., similarity, 0.0001);
	}

}