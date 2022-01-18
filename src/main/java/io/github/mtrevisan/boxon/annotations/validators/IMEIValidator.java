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
package io.github.mtrevisan.boxon.annotations.validators;


/**
 * Validator for <a href="https://en.wikipedia.org/wiki/International_Mobile_Equipment_Identity">IMEI</a> codes.
 */
public final class IMEIValidator implements Validator<CharSequence>{

	IMEIValidator(){}

	/**
	 * Validate an IMEI with the Luhn algorithm.
	 *
	 * @param imei	The IMEI to be validated.
	 * @return	Whether the IMEI is valid.
	 * @see <a href="https://en.wikipedia.org/wiki/Luhn_algorithm">Luhn algorithm</a>
	 */
	@Override
	public boolean isValid(final CharSequence imei){
		if(imei == null)
			return false;

		int sum = 0;
		final int parity = imei.length() % 2;
		for(int i = imei.length() - 1; i >= 0; i --){
			int k = Character.getNumericValue(imei.charAt(i));
			if(i % 2 == parity)
				k <<= 1;

			sum += k / 10;
			sum += k % 10;
		}
		return (sum % 10 == 0);
	}

}
