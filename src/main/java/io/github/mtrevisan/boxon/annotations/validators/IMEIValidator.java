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
package io.github.mtrevisan.boxon.annotations.validators;


/**
 * Validator for <a href="https://en.wikipedia.org/wiki/International_Mobile_Equipment_Identity">IMEI</a> codes.
 */
public final class IMEIValidator implements Validator<String>{

	IMEIValidator(){}


	/**
	 * Validate an IMEI with the Luhn algorithm.
	 *
	 * @param imei	The IMEI to be validated.
	 * @return	Whether the IMEI is valid.
	 * @see <a href="https://en.wikipedia.org/wiki/Luhn_algorithm">Luhn algorithm</a>
	 */
	@Override
	public boolean isValid(final String imei){
		if(imei == null || imei.isEmpty())
			return false;

		int sum = 0;
		final int length = imei.length();
		final int parity = length % 2;
		for(int i = 0; i < length; i ++){
			int digit = imei.charAt(i) - '0';
			if((i % 2) == parity){
				digit <<= 1;
				if(digit > 9)
					digit -= 9;
			}
			sum += digit;
		}
		return (sum % 10 == 0);
	}

}
