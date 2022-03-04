/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("ALL")
class PatternMatcherTest{

	@Test
	void indexOfBNDM(){
		PatternMatcher pm = BNDMPatternMatcher.getInstance();
		indexOf(pm);
	}

	@Test
	void indexOfKMP(){
		PatternMatcher pm = KMPPatternMatcher.getInstance();
		indexOf(pm);
	}

	private void indexOf(PatternMatcher pm){
		byte[] source = "".getBytes();
		byte[] pattern = "".getBytes();
		int index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(0, index);

		source = "ab".getBytes();
		pattern = "".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(0, index);


		source = "a".getBytes();
		pattern = "a".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(0, index);

		source = "b".getBytes();
		pattern = "a".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(-1, index);


		source = "aaaaa".getBytes();
		pattern = "aaa".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(0, index);

		source = "abaaba".getBytes();
		pattern = "aaa".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(-1, index);

		source = "abacababc".getBytes();
		pattern = "abab".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(4, index);

		source = "babacaba".getBytes();
		pattern = "abab".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(-1, index);


		source = "aaacacaacaaacaaaacaaaaac".getBytes();
		pattern = "aaacaaaaac".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(14, index);

		source = "ababcababdabababcababdaba".getBytes();
		pattern = "ababcababdabababcababdaba".getBytes();
		index = pm.indexOf(source, 0, pattern, pm.preProcessPattern(pattern));
		Assertions.assertEquals(0, index);
	}

}
