/**
 * Copyright (c) 2020 Mauro Trevisan
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
package unit731.boxon.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.coders.queclink.VersionHelper;


class VersionHelperTest{

	@Test
	void compare(){
		Assertions.assertEquals(1, VersionHelper.compare("1.1", "1.0"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0", "1.0"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0", "1.1"));

		Assertions.assertEquals(Integer.MAX_VALUE, VersionHelper.compare("1.0a", "1.0"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0a", "1.0a"));
		Assertions.assertEquals(-Integer.MAX_VALUE, VersionHelper.compare("1.0", "1.0a"));

		Assertions.assertEquals(1, VersionHelper.compare("1.0b", "1.0a"));
		Assertions.assertEquals(0, VersionHelper.compare("1.0a", "1.0A"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0a", "1.0b"));

		Assertions.assertEquals(1, VersionHelper.compare("1.1b", "1.0a"));
		Assertions.assertEquals(-1, VersionHelper.compare("1.0a", "1.1b"));
	}

}
