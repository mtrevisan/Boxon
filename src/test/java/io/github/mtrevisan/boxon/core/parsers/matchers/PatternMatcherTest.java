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
package io.github.mtrevisan.boxon.core.parsers.matchers;

import io.github.mtrevisan.boxon.utils.TimeWatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


class PatternMatcherTest{

	@Test
	void indexOfBNDM(){
		PatternMatcher pm = BNDMPatternMatcher.getInstance();

		testIndexOf1(pm);
		testIndexOf2(pm);
		testIndexOf3(pm);
		testIndexOf4(pm);
		testIndexOf5(pm);
		testIndexOf6(pm);
		testIndexOf7(pm);
		testIndexOf8(pm);
		testIndexOf9(pm);
		testIndexOf10(pm);
		testIndexOf11(pm);
	}

	@Test
	void indexOfKMP(){
		PatternMatcher pm = KMPPatternMatcher.getInstance();

		testIndexOf1(pm);
		testIndexOf2(pm);
		testIndexOf3(pm);
		testIndexOf4(pm);
		testIndexOf5(pm);
		testIndexOf6(pm);
		testIndexOf7(pm);
		testIndexOf8(pm);
		testIndexOf9(pm);
		testIndexOf10(pm);
		testIndexOf11(pm);
	}

	@Test
	void indexOfKR(){
		PatternMatcher pm = KRPatternMatcher.getInstance();

		testIndexOf1(pm);
		testIndexOf2(pm);
		testIndexOf3(pm);
		testIndexOf4(pm);
		testIndexOf5(pm);
		testIndexOf6(pm);
		testIndexOf7(pm);
		testIndexOf8(pm);
		testIndexOf9(pm);
		testIndexOf10(pm);
		testIndexOf11(pm);
	}

	@Test
	void speed(){
		PatternMatcher bndm = BNDMPatternMatcher.getInstance();
		PatternMatcher kmp = KMPPatternMatcher.getInstance();
		PatternMatcher kr = KRPatternMatcher.getInstance();
		Map<String, PatternMatcher> matchers = new HashMap<>(3);
		matchers.put("BNDM", bndm);
		matchers.put("KMP", kmp);
		matchers.put("KR", kr);

		TimeWatch watch = TimeWatch.start();
		byte[] source = "2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "0d0a".getBytes(StandardCharsets.US_ASCII);
		for(Map.Entry<String, PatternMatcher> entry : matchers.entrySet()){
			String key = entry.getKey();
			PatternMatcher m = entry.getValue();

			//warm-up
			for(int i = 0; i < 2_000; i ++)
				m.indexOf(source, 0, pattern, m.preProcessPattern(pattern));

			watch.reset();
			for(int i = 0; i < 20_000; i ++)
				m.indexOf(source, 0, pattern, m.preProcessPattern(pattern));
			watch.stop();

			System.out.println(key + ": " + watch.toString(20_000));
		}
	}

	private static void testIndexOf1(PatternMatcher pm){
		byte[] source = "".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(0, index);
	}

	private static void testIndexOf2(PatternMatcher pm){
		byte[] source = "ab".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(0, index);
	}

	private static void testIndexOf3(PatternMatcher pm){
		byte[] source = "a".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "a".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(0, index);
	}

	private static void testIndexOf4(PatternMatcher pm){
		byte[] source = "b".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "a".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(-1, index);
	}

	private static void testIndexOf5(PatternMatcher pm){
		byte[] source = "aaaaa".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "aaa".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(0, index);
	}

	private static void testIndexOf6(PatternMatcher pm){
		byte[] source = "abaaba".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "aaa".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(-1, index);
	}

	private static void testIndexOf7(PatternMatcher pm){
		byte[] source = "abacababc".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "abab".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(4, index);
	}

	private static void testIndexOf8(PatternMatcher pm){
		byte[] source = "babacaba".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "abab".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(-1, index);
	}

	private static void testIndexOf9(PatternMatcher pm){
		byte[] source = "aaacacaacaaacaaaacaaaaac".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "aaacaaaaac".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(14, index);
	}

	private static void testIndexOf10(PatternMatcher pm){
		byte[] source = "ababcababdabababcababdaba".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "ababcababdabababcababdaba".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(0, index);
	}

	private static void testIndexOf11(PatternMatcher pm){
		byte[] source = "2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a2b41434b066f2446010a0311235e40035110420600ffff07e30405083639001265b60d0a".getBytes(StandardCharsets.US_ASCII);
		byte[] pattern = "0d0a".getBytes(StandardCharsets.US_ASCII);

		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));

		Assertions.assertEquals(68, index);
	}

}
