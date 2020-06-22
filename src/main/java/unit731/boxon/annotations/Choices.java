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
package unit731.boxon.annotations;

import unit731.boxon.codecs.ByteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * The annotation allowing you to define a number of choices, based a prefix of a certain {@link #prefixSize() size}.
 *
 * @author Wilfred Springer (wis)
 */
public @interface Choices{

	/**
	 * The number of bits to be read for determining the prefix.
	 *
	 * @return	The number of bits to be read for determining the prefix.
	 */
	int prefixSize() default 0;

	/**
	 * The byte order to take into account when returning a representation of the first {@link #prefixSize() size} bits
	 * read as a prefix.
	 *
	 * @return	The byte order to take into account when returning a representation of the first {@link #prefixSize()
	 * 			size} bits read as a prefix.
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

	/**
	 * The choices to select from.
	 *
	 * @return	The choices to select from.
	 */
	Choice[] alternatives() default {};


	/** The annotation holding a single choice. */
	@interface Choice{

		/**
		 * The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 * A SpEL expression with the prefix value in the context under the name `__prefix`.
		 *
		 * @return	The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 */
		String condition();

		/**
		 * The type to decode in case the {@link #condition()} holds.
		 *
		 * @return	The type to decode in case the {@link #condition()} holds.
		 */
		Class<?> type();

	}


	/** The annotation used to indicate the discriminator used to recognize an instance of this class. */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Prefix{

		/**
		 * The value that will be used to match this particular record.
		 *
		 * @return The value.
		 */
		long value();

	}

}
