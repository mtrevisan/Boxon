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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Describe an alternative configuration field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface AlternativeSubField{

	/**
	 * A long description of the field.
	 *
	 * @return	A long description of the field.
	 */
	String longDescription() default "";

	/**
	 * The unit of measure of the value (the format should follow
	 * <a href="https://ucum.org/ucum.html">UCUM</a>/<a href="https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure">ISO 80000</a>
	 * standard).
	 *
	 * @return	The unit of measure of the value.
	 */
	String unitOfMeasure() default "";


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
	 * The lowest value the field can have.
	 * <p>Not compatible with the `enumeration` field.</p>
	 * <p>Compatible with numeric field.</p>
	 *
	 * @return	The lowest value the field can have.
	 */
	String minValue() default "";

	/**
	 * The highest value the field can have.
	 * <p>Not compatible with the `enumeration` field.</p>
	 * <p>Compatible with numeric field.</p>
	 *
	 * @return	The highest value the field can have.
	 */
	String maxValue() default "";

	/**
	 * The pattern of the value, expressed as a regex.
	 * <p>Not compatible with the `enumeration` field.</p>
	 * <p>Not compatible with non-string field.</p>
	 *
	 * @return	The pattern of the value, expressed as a regex.
	 */
	String pattern() default "";


	/**
	 * The default value of the field.
	 * <p>For non-mutually exclusive enumeration fields this is an array (e.g. 'ONE|TWO|THREE').</p>
	 * <p>If not present, the field is mandatory.</p>
	 *
	 * @return	The default value of the field.
	 */
	String defaultValue() default "";

	/**
	 * The type of encoding used for string-typed field.
	 *
	 * @return	The type of encoding used (defaults to {@value io.github.mtrevisan.boxon.helpers.CharsetHelper#DEFAULT_CHARSET}).
	 */
	String charset() default "UTF-8";

	/**
	 * The numeral system (base, or radix) of this field.
	 * <p>Compatible with numeric or enumeration field.</p>
	 *
	 * @return	The numeral system (base, or radix) of this field.
	 */
	int radix() default 10;

}
