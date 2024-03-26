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
package io.github.mtrevisan.boxon.annotations.bindings;

import io.github.mtrevisan.boxon.io.ByteOrder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Allow to define a number of choices, based on a header of a certain {@link #prefixLength() length}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface ObjectChoices{

	/**
	 * The number of bits to be read for determining the header.
	 * <p>It MUST BE in the range {@code [0, }{@link Integer#SIZE}{@code ]}.</p>
	 *
	 * @return	The number of bits to be read for determining the header (defaults to {@code 0}).
	 */
	int prefixLength() default 0;

	/**
	 * The type of endianness of the prefix: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 *
	 * @return	The type of endianness of the prefix (defaults to {@link ByteOrder#BIG_ENDIAN}).
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

	/**
	 * The choices to select from.
	 *
	 * @return	The choices to select from (defaults to no alternatives).
	 */
	ObjectChoice[] alternatives() default {};


	/** The annotation holding a single choice. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@Documented
	@interface ObjectChoice{

		/**
		 * The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 * <p>A SpEL expression with the header value in the context under the name {@code header}.</p>
		 *
		 * @return	The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 */
		String condition();

		/**
		 * The header to be written when serializing the object.
		 * <p>NOTE: this is the inverse of {@link #condition() condition}, if it contains a `#prefix` reference.</p>
		 *
		 * @return	The inverse of {@link #condition() condition}, if it contains a `#prefix` reference (defaults to {@code 0}).
		 */
		int prefix() default 0;

		/**
		 * The type to decode in case the {@link #condition()} holds.
		 *
		 * @return	The type to decode in case the {@link #condition()} holds.
		 */
		Class<?> type();

	}

}
