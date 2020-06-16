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

import unit731.boxon.annotations.checksummers.Checksummer;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.codecs.ByteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindChecksum{

	/**
	 * The type of object to be inserted into the checksum field.
	 * Note that this allows you to have a field of a super type of the actual type that
	 * you expect to inject. So you might have something like this:
	 * <pre><code>
	 * &#064;BoundArray(type = int[].class)
	 * private int[] c; // Array of integers
	 * </code></pre>
	 *
	 * @return	The type of object to be inserted in the array.
	 */
	Class<?> type();

	/**
	 * The byte(s) to skip from the start of the message.
	 *
	 * @return	The byte(s) to skip from the start of the message.
	 */
	int skipStart() default 0;

	/**
	 * The byte(s) to skip from the end of the message.
	 *
	 * @return	The byte(s) to skip from the end of the message.
	 */
	int skipEnd() default 0;

	/**
	 * The validator to be applied before applying the transformer, if any. Usually the fully qualified
	 * name of an implementation class of a {@link Validator}
	 *
	 * @return	The class of a {@link Checksummer}
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Checksummer> algorithm();

	/**
	 * The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 *
	 * @return	The type of endianness. Defaults to {@link ByteOrder#BIG_ENDIAN}.
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

}
