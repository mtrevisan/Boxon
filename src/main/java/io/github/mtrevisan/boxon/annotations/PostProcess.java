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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Defines a variable to be evaluated at the end of all other annotations, with the possibility of indicating the decoding or encoding
 * phase.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface PostProcess{

	/**
	 * The SpEL expression that determines if an evaluation has to be made.
	 *
	 * @return	The condition that determines if an evaluation has to be made (defaults to empty, that means &quot;accept&quot;).
	 */
	String condition() default "";

	/**
	 * The SpEL expression to be evaluated in the decode phase.
	 *
	 * @see <a href="https://docs.spring.io/spring-framework/docs/6.1.x/reference/html/core.html#expressions">Spring Expression Language (SpEL)</a>
	 *
	 * @return	The SpEL expression to be evaluated.
	 */
	String valueDecode();

	/**
	 * The SpEL expression to be evaluated in the encode phase.
	 *
	 * @see <a href="https://docs.spring.io/spring-framework/docs/6.1.x/reference/html/core.html#expressions">Spring Expression Language (SpEL)</a>
	 *
	 * @return	The SpEL expression to be evaluated.
	 */
	String valueEncode();

}
