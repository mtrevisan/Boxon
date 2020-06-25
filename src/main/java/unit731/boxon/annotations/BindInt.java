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

import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.codecs.ByteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages an <code>int</code>/{@link Integer} (... before the application of a converter)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BindInt{

	/**
	 * The type of value: either signed or unsigned.
	 * <p>NOTE: in case of unsigned value, the read data is a long.</p>
	 *
	 * @return	The type of value. Defaults to <code>false</code>.
	 */
	boolean unsigned() default false;

	/**
	 * The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 *
	 * @return	The type of endianness. Defaults to {@link ByteOrder#BIG_ENDIAN}.
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

	/**
	 * The value to match (can be a regex expression or a SpEL expression).
	 *
	 * @return	The value, or regex, or SpEL expression to be checked for equality
	 */
	String match() default "";

	/**
	 * The validator to be applied before applying the converter, if any. Usually the fully qualified
	 * name of an implementation class of a {@link Validator}
	 *
	 * @return	The class of a {@link Validator}
	 */
	Class<? extends Validator> validator() default NullValidator.class;

	/**
	 * The converter to be applied before writing the parameter value. Usually the fully qualified
	 * name of an implementation class of a {@link Converter}
	 *
	 * @return	The class of a {@link Converter}
	 */
	Class<? extends Converter> converter() default NullConverter.class;

}