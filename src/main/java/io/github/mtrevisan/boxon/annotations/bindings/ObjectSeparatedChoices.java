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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Allow to define a number of choices, based on a prefix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface ObjectSeparatedChoices{

	/**
	 * The type of encoding used for the {@link String}.
	 *
	 * @return	The type of encoding used (defaults to {@value io.github.mtrevisan.boxon.helpers.CharsetHelper#DEFAULT_CHARSET}).
	 */
	String charset() default "UTF-8";

	/**
	 * The byte that terminates the {@link String}.
	 *
	 * @return	The terminator byte (defaults to {@code \0}).
	 */
	byte terminator() default '\0';

	/**
	 * The choices to select from.
	 *
	 * @return	The choices to select from (defaults to no alternatives).
	 */
	ObjectSeparatedChoice[] alternatives() default {};


	/** The annotation holding a single choice. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@Documented
	@interface ObjectSeparatedChoice{

		/**
		 * The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 * <p>A SpEL expression with the prefix value in the context under the name {@code prefix}.</p>
		 *
		 * @return	The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 */
		String condition();

		/**
		 * The prefix to be written when serializing the object.
		 * <p>NOTE: this is the inverse of {@link #condition() condition}, if it contains a `#header` reference.</p>
		 *
		 * @return	The inverse of {@link #condition() condition}, if it contains a `#header` reference (defaults to {@code 0}).
		 */
		String header();

		/**
		 * The type to decode in case the {@link #condition()} holds.
		 *
		 * @return	The type to decode in case the {@link #condition()} holds.
		 */
		Class<?> type();

	}

}
