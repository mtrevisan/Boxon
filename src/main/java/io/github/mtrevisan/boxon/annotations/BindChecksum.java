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
package io.github.mtrevisan.boxon.annotations;

import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.external.ByteOrder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a variable to be used as the checksum for the entire message.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface BindChecksum{

	/**
	 * The type of object to be inserted into the checksum field.
	 *
	 * @return	The type of object to be inserted in the array.
	 */
	Class<?> type();

	/**
	 * The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 *
	 * @return	The type of endianness (defaults to {@link ByteOrder#BIG_ENDIAN}).
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

	/**
	 * The byte(s) to skip from the start of the message, if any.
	 *
	 * @return	The byte(s) to skip from the start of the message (defaults to {@code 0}).
	 */
	int skipStart() default 0;

	/**
	 * The byte(s) to skip from the end of the message, if any.
	 *
	 * @return	The byte(s) to skip from the end of the message (defaults to {@code 0}).
	 */
	int skipEnd() default 0;

	/**
	 * The validator to be applied before applying the converter, if any. Usually the fully qualified
	 * name of an implementation class of a {@link Validator}.
	 *
	 * @return	The class of a {@link Checksummer}.
	 */
	Class<? extends Checksummer> algorithm();

	/**
	 * The starting value of the CRC algorithm.
	 *
	 * @return	The starting value of the CRC algorithm.
	 */
	long startValue();

}
