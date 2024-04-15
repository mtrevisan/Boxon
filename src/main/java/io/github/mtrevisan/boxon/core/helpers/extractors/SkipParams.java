/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers.extractors;

import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;


public final class SkipParams{

	/** The SpEL expression that determines if an evaluation has to be made (an empty string means &quot;accept&quot;). */
	private final String condition;

	/** The SpEL expression evaluating to the number of bits to be skipped. */
	private String size;

	/** The byte that terminates the skip. */
	private byte terminator;

	/** Whether to consume the terminator. */
	private boolean consumeTerminator;


	public static SkipParams create(final SkipBits annotation){
		return new SkipParams(annotation);
	}

	public static SkipParams create(final SkipUntilTerminator annotation){
		return new SkipParams(annotation);
	}


	private SkipParams(final SkipBits annotation){
		condition = annotation.condition();
		size = annotation.value();
	}

	private SkipParams(final SkipUntilTerminator annotation){
		condition = annotation.condition();
		terminator = annotation.value();
		consumeTerminator = annotation.consumeTerminator();
	}


	public boolean isSkipBits(){
		return !StringHelper.isBlank(size);
	}

	public Class<? extends Annotation> annotationType(){
		return (isSkipBits()? SkipBits.class: SkipUntilTerminator.class);
	}

	public String condition(){
		return condition;
	}

	public String size(){
		return size;
	}

	public byte terminator(){
		return terminator;
	}

	public boolean consumeTerminator(){
		return consumeTerminator;
	}

}
