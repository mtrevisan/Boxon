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
	String shortDescription();

	/**
	 * A long description of the field.
	 *
	 * @return	A long description of the field.
	 */
	String longDescription() default "";

	/**
	 * The unit of measure of the value.
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
	 * The lowest value the field can have.
	 * <p>Not compatible with enumeration field.</p>
	 * <p>Compatible with numeric field.</p>
	 *
	 * @return	The lowest value the field can have.
	 */
	String minValue() default "";

	/**
	 * The highest value the field can have.
	 * <p>Not compatible with enumeration field.</p>
	 * <p>Compatible with numeric field.</p>
	 *
	 * @return	The highest value the field can have.
	 */
	String maxValue() default "";

	/**
	 * The format of the value, expressed as a regex.
	 * <p>Not compatible with enumeration field.</p>
	 * <p>Not compatible with non-string field.</p>
	 *
	 * @return	The format of the value, expressed as a regex.
	 */
	String format() default "";

	/**
	 * The enumeration that represents the finite possible values for this field.
	 * <p>Not compatible with format field.</p>
	 *
	 * @return	The enumeration that represents the finite possible values for this field.
	 */
	Class<? extends Enum<?>> enumeration() default NullEnum.class;


	/**
	 * The field is mandatory.
	 * <p>Non-writable fields are all mandatory by default.</p>
	 *
	 * @return	Whether the value is mandatory.
	 */
	boolean mandatory() default false;

	/**
	 * The default value of the field.
	 * <p>For non-mutually exclusive enumeration fields this is an array.</p>
	 *
	 * @return	The default value of the field.
	 */
	String defaultValue() default "";

	/**
	 * The type of encoding used for string-typed field.
	 *
	 * @return	The type of encoding used (defaults to `UTF-8`).
	 */
	String charset() default "UTF-8";

	/**
	 * The numeral system (base, or radix) of this field.
	 * <p>Compatible with numeric or enumeration field.</p>
	 *
	 * @return	The numeral system (base, or radix) of this field.
	 */
	int radix() default 10;

	/**
	 * Whether the field is writable.
	 * <p>Non-writable fields are all mandatory by default.</p>
	 *
	 * @return	Whether the field is writable.
	 */
	boolean writable() default true;


	/**
	 * The numeral system (base, or radix), or the type (e.g. URL, email, etc.) of this field.
	 * <p>Compatible with numeric or enumeration field.</p>
	 *
	 * @return	The numeral system (base, or radix), or the type (e.g. URL, email, etc.) of this field.
	 */
	FieldType type() default FieldType.DECIMAL;

	/**
	 * The string that terminates the field.
	 *
	 * @return	The terminator string (defaults to empty).
	 */
	String terminator() default "";

}
