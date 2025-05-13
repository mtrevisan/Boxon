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
package io.github.mtrevisan.boxon.annotations.configurations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages the skipping of a certain number of bits, or until a given terminator is found.
 * <p>Since this annotation is bound to a field, if it is necessary to skip some numbers of bits from the end. It is
 * necessary to introduce a placeholder field (this can be of any type, since it is not assigned at all):</p>
 * <pre>{@code
 * &#x40;Skip(size = "3")
 * &#x40;Skip(size = "1")
 * &#x40;BindString(size = "4")
 * public String text;
 *
 * &#x40;Skip(size = "10")
 * private int unused;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(ConfigurationSkip.Skips.class)
@Documented
public @interface ConfigurationSkip{

	/**
	 * Manages multiple {@link ConfigurationSkip} annotations.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
	@Documented
	@interface Skips{

		/**
		 * The array holding the Skip annotations.
		 *
		 * @return	The array of Skip annotations.
		 */
		ConfigurationSkip[] value() default {};

	}


	/**
	 * The lowest protocol in which the field is located.
	 *
	 * @return	The lowest protocol in which the field is located.
	 */
	String minProtocol() default "";

	/**
	 * The highest protocol in which the field is located.
	 *
	 * @return	The highest protocol in which the field is located.
	 */
	String maxProtocol() default "";

	/**
	 * The string that terminates the skip (charset is UTF-8).
	 *
	 * @return	The terminator string (defaults to empty).
	 */
	String terminator() default "";

}
