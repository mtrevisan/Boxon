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
package io.github.mtrevisan.boxon.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages the skipping of a certain number of bits until a given terminator is found.
 * <p>Since this annotation is bound to a field, it is necessary to introduce a placeholder field if you need to skip
 * some bits from the end. This placeholder field can be of any type, as it is not assigned to anything:</p>
 * <pre>{@code
 * &#x40;SkipUntilTerminator("3")
 * &#x40;SkipUntilTerminator("1")
 * &#x40;BindString(size = "4")
 * public String text;
 *
 * &#x40;SkipUntilTerminator(value = "\0", consumeTerminator = false)
 * private int unused;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(SkipUntilTerminator.Skips.class)
@Documented
public @interface SkipUntilTerminator{

	/**
	 * Manages multiple {@link SkipUntilTerminator} annotations.
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
		SkipUntilTerminator[] value() default {};

	}


	/**
	 * The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression that determines if an
	 * evaluation has to be made.
	 *
	 * @return	The condition that determines if an evaluation has to be made (defaults to empty, that means &quot;accept&quot;).
	 */
	String condition() default "";

	/**
	 * The byte that terminates the skip.
	 *
	 * @return	The terminator byte.
	 */
	byte value();

	/**
	 * Whether to consume the terminator.
	 *
	 * @return	Whether to consume the terminator (defaults to {@code true}).
	 */
	boolean consumeTerminator() default true;

}
