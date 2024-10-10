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
package io.github.mtrevisan.boxon.core.similarity.metrics;

import io.github.mtrevisan.boxon.core.similarity.distances.StringMetricData;
import io.github.mtrevisan.boxon.utils.TimeWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class LevenshteinMetricTest{

	@Test
	void testNullThrows(){
		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> LevenshteinMetric.<StringMetricData>create()
				.similarity(null, StringMetricData.of("kEvIn"))
		);
	}

	@Test
	void emptyStringTest(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("");
		StringMetricData data2 = StringMetricData.of("");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(0, distance);
		Assertions.assertEquals(1., similarity, 0.);
	}

	@Test
	void testExactMatchSameCase(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("java");
		StringMetricData data2 = StringMetricData.of("java");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(0, distance);
		Assertions.assertEquals(1., similarity, 0.);
	}

	@Test
	void testNoSimilarity(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("abc");
		StringMetricData data2 = StringMetricData.of("def");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0., similarity, 0.);
	}

	@Test
	void score1(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("he");
		StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score2(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("hd");
		StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score3(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("d");
		 StringMetricData data2 = StringMetricData.of("head");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.25, similarity, 0.0001);
	}

	@Test
	void score4(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("head");
		StringMetricData data2 = StringMetricData.of("he");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(2, distance);
		Assertions.assertEquals(0.5, similarity, 0.0001);
	}

	@Test
	void score5(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("kitten");
		StringMetricData data2 = StringMetricData.of("sitting");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.5714, similarity, 0.0001);
	}

	@Test
	void score6(){
		LevenshteinMetric<StringMetricData> metric = LevenshteinMetric.create();
		StringMetricData data1 = StringMetricData.of("Saturday");
		StringMetricData data2 = StringMetricData.of("Sunday");

		int distance = metric.distance(data1, data2);
		double similarity = metric.similarity(data1, data2);

		Assertions.assertEquals(3, distance);
		Assertions.assertEquals(0.625, similarity, 0.0001);
	}


	public static void main(String[] args){
		DamerauLevenshteinMetric<StringMetricData> metric = DamerauLevenshteinMetric.create();
		StringMetricData[] data1 = {
			StringMetricData.of(""),
			StringMetricData.of("java"),
			StringMetricData.of("abc"),
			StringMetricData.of("he"),
			StringMetricData.of("hd"),
			StringMetricData.of("d"),
			StringMetricData.of("head"),
			StringMetricData.of("kitten"),
			StringMetricData.of("Saturday"),
			StringMetricData.of("Saturday")
		};
		StringMetricData[] data2 = {
			StringMetricData.of(""),
			StringMetricData.of("java"),
			StringMetricData.of("def"),
			StringMetricData.of("head"),
			StringMetricData.of("head"),
			StringMetricData.of("head"),
			StringMetricData.of("he"),
			StringMetricData.of("sitting"),
			StringMetricData.of("Sunday"),
			StringMetricData.of("Satudray")
		};


		//warm-up
		for(int i = 0; i < 2_000; i ++){
			final int index = i % data1.length;
			metric.distance(data1[index], data2[index]);
		}

		TimeWatch watch = TimeWatch.start();
		for(int i = 0; i < 10_000_000; i ++){
			final int index = i % data1.length;
			metric.distance(data1[index], data2[index]);
		}
		watch.stop();

		System.out.println(watch.toString(10_000_000 * data1.length) + " (" + watch.toStringAsFrequency(10_000_000 * data1.length) + ")");
	}

}
