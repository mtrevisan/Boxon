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
package io.github.mtrevisan.boxon.annotations;

import io.github.mtrevisan.boxon.internal.managers.CharsetHelper;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Defines a message with a given header and footer (optional).
 * <p>This will enable automatic loading through the {@code Loader}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface MessageHeader{

	/**
	 * The initial bytes that determines the type of message.
	 * <p>This SHOULD be read by the protocol of a single message.</p>
	 *
	 * @return	The header bytes of this message.
	 */
	String[] start();

	/**
	 * The final bytes that determines the type of message.
	 * <p>This SHOULD NOT be read by the protocol of a single message.</p>
	 *
	 * @return	The tail bytes of this message (defaults to empty string).
	 */
	String end() default "";

	/**
	 * The type of encoding used for the {@link #start()} and {@link #end()} fields.
	 *
	 * @return	The type of encoding used (defaults to {@value CharsetHelper#DEFAULT_CHARSET}).
	 */
	String charset() default "UTF-8";

}
