/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.annotations.validators;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class IMEIValidatorTest{

	@Test
	void valid(){
		IMEIValidator validator = new IMEIValidator();
		Assertions.assertTrue(validator.isValid("79927398713"));
		Assertions.assertFalse(validator.isValid("79927398710"));
		Assertions.assertFalse(validator.isValid("79927398711"));
		Assertions.assertFalse(validator.isValid("79927398712"));
		Assertions.assertFalse(validator.isValid("79927398714"));
		Assertions.assertFalse(validator.isValid("79927398715"));
		Assertions.assertFalse(validator.isValid("79927398716"));
		Assertions.assertFalse(validator.isValid("79927398717"));
		Assertions.assertFalse(validator.isValid("79927398718"));
		Assertions.assertFalse(validator.isValid("79927398719"));
	}

}
