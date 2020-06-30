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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages the skipping of a certain amount of bits, or until a given terminator is found.
 * <p>Since this annotation is bound to a field, if it is necessary to skip some amounts of bits from the end, it is necessary
 * to introduce a placeholder field (this can be of any type, since it is not assigned at all):</p>
 * <pre><code>
 * &#x40;Skip(size = "3")
 * &#x40;Skip(size = "1")
 * &#x40;BindString(size = "4")
 * public String text;
 *
 * &#x40;Skip(size = "10")
 * private int unused;
 * </pre></code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(Skips.class)
public @interface Skip{

	/**
	 * The number of bits to be skipped.
	 *
	 * @return	The number of bits to be skipped (can be an expression).
	 */
	String size() default "0";

	/**
	 * The byte that terminates the skip
	 *
	 * @return	The terminator byte.
	 */
	byte terminator() default '\0';

	/**
	 * Whether to consume the terminator.
	 *
	 * @return	Whether to consume the terminator.
	 */
	boolean consumeTerminator() default true;

}
