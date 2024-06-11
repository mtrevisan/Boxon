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
 * Manages multiple {@link AlternativeSubField} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface AlternativeConfigurationField{

	/**
	 * The array holding the alternative field annotations.
	 *
	 * @return	The array of alternative field annotations.
	 */
	AlternativeSubField[] value();


	/**
	 * A short description of the field.
	 *
	 * @return	A short description of the field.
	 */
	String shortDescription();

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
	 * The enumeration that represents the finite possible values for this field.
	 * <p>Not compatible with pattern field.</p>
	 *
	 * @return	The enumeration that represents the finite possible values for this field.
	 */
	Class<? extends ConfigurationEnum<?>> enumeration() default NullEnum.class;


	/**
	 * The string that terminates the field (charset is UTF-8).
	 *
	 * @return	The terminator string (defaults to empty).
	 */
	String terminator() default "";

}
