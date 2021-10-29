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
package io.github.mtrevisan.boxon.annotations.configurations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Describe a configuration field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface ConfigurationField{

	/**
	 * A short description of the field.
	 *
	 * @return	A short description of the field.
	 */
	String shortDescription() default "";

	/**
	 * A long description of the field.
	 *
	 * @return	A long description of the field.
	 */
	String longDescription() default "";

	/**
	 * The lowest protocol the field is in.
	 *
	 * @return	The lowest protocol the field is in.
	 */
	String minProtocol() default "";

	/**
	 * The highest protocol the field is in.
	 *
	 * @return	The highest protocol the field is in.
	 */
	String maxProtocol() default "";

	/**
	 * The lowest value the field can have.
	 *
	 * @return	The lowest value the field can have.
	 */
	String minValue() default "";

	/**
	 * The highest value the field can have.
	 *
	 * @return	The highest value the field can have.
	 */
	String maxValue() default "";

	/**
	 * The format of the value, expressed as a regex.
	 *
	 * @return	The format of the value, expressed as a regex.
	 */
	String format() default "";

	/**
	 * The default value of the field.
	 *
	 * @return	The default value of the field.
	 */
	String defaultValue() default "";

	/**
	 * The unit of measure of the value.
	 *
	 * @return	The unit of measure of the value.
	 */
	String unitOfMeasure() default "";

	/**
	 * Whether the field is writable.
	 *
	 * @return	Whether the field is writable.
	 */
	boolean writable() default true;

	/**
	 * The byte that terminates the field.
	 *
	 * @return	The terminator byte (defaults to `\0`).
	 */
	byte terminator() default '\0';

}
