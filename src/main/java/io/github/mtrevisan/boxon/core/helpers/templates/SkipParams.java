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
package io.github.mtrevisan.boxon.core.helpers.templates;

import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.util.Objects;


/**
 * Encapsulates the parameters necessary for handling skip operations based on the annotations {@link SkipBits} and
 * {@link SkipUntilTerminator}.
 */
public final class SkipParams{

	/**
	 * The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression that determines if an
	 * evaluation has to be made (an empty string means “accept”).
	 */
	private final String condition;

	/**
	 * The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression evaluating to the number
	 * of bits to be skipped.
	 */
	private final String size;

	/** The byte that terminates the skip. */
	private final byte terminator;

	/** Whether to consume the terminator. */
	private final boolean consumeTerminator;


	public static SkipParams create(final SkipBits annotation){
		return new SkipParams(annotation);
	}

	public static SkipParams create(final SkipUntilTerminator annotation){
		return new SkipParams(annotation);
	}


	private SkipParams(final SkipBits annotation){
		condition = annotation.condition();
		size = annotation.value();

		terminator = 0;
		consumeTerminator = false;
	}

	private SkipParams(final SkipUntilTerminator annotation){
		condition = annotation.condition();
		terminator = annotation.value();
		consumeTerminator = annotation.consumeTerminator();

		size = null;
	}


	/**
	 * Returns the annotation type of this skip parameter.
	 *
	 * @return	The annotation type of this skip parameter, either ̧̧{@code {@link SkipUntilTerminator}.class} or
	 * 	{@code {@link SkipBits}.class}.
	 */
	public Class<? extends Annotation> annotationType(){
		return (StringHelper.isBlank(size)? SkipUntilTerminator.class: SkipBits.class);
	}

	/**
	 * Returns the condition of the skip parameter.
	 *
	 * @return	The condition that determines if an evaluation has to be made.
	 */
	public String condition(){
		return condition;
	}

	/**
	 * Returns the size value of the SkipParams object.
	 *
	 * @return	The size value of the SkipParams object.
	 */
	public String size(){
		return size;
	}

	/**
	 * Returns the byte value of the terminator used in the skip operation.
	 *
	 * @return	The byte value of the terminator.
	 */
	public byte value(){
		return terminator;
	}

	/**
	 * Indicates whether the terminator should be consumed or not.
	 *
	 * @return	Whether the terminator should be consumed.
	 */
	public boolean consumeTerminator(){
		return consumeTerminator;
	}


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final SkipParams other = (SkipParams)obj;
		return (terminator == other.terminator
			&& consumeTerminator == other.consumeTerminator
			&& Objects.equals(condition, other.condition)
			&& Objects.equals(size, other.size));
	}

	@Override
	public int hashCode(){
		int result = condition.hashCode();
		result = 31 * result + Objects.hashCode(size);
		result = 31 * result + Byte.hashCode(terminator);
		result = 31 * result + Boolean.hashCode(consumeTerminator);
		return result;
	}

}
