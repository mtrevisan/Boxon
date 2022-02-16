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
package io.github.mtrevisan.boxon.annotations.configurations;

import io.github.mtrevisan.boxon.internal.helpers.CharsetHelper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages multiple {@link CompositeSubField} annotations.
 *
 * <p>This field is mandatory only if one of its children is mandatory.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface CompositeConfigurationField{

	/**
	 * The array holding the composite field annotations.
	 *
	 * @return	The array of composite field annotations.
	 */
	CompositeSubField[] value();


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
	 * The pattern of the value, expressed as a regex.
	 * <p>Not compatible with enumeration field.</p>
	 * <p>Not compatible with non-string field.</p>
	 *
	 * @return	The pattern of the value, expressed as a regex.
	 */
	String pattern() default "";

	/**
	 * How the composition is made (freemarker style).
	 * <p>Ex. there are two configuration fields, and with this field set to `{1}@{2}`, the composition of both are done appending
	 * the second field to the first using a `@` as a separator.</p>
	 *
	 * @return	The composition pattern of the fields, expressed as a regex.
	 */
	String composition() default "";


	/**
	 * The type of encoding used for string-typed field.
	 *
	 * @return	The type of encoding used (defaults to {@value CharsetHelper#DEFAULT_CHARSET}).
	 */
	String charset() default "UTF-8";


	/**
	 * The string that terminates the field.
	 *
	 * @return	The terminator string (defaults to empty).
	 */
	String terminator() default "";

}
