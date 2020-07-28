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

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Allow to define a number of choices on the type of converter to use, based on a condition
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ConverterChoices{

	/**
	 * The choices to select from.
	 *
	 * @return	The choices to select from.
	 */
	ConverterChoice[] alternatives() default {};


	/** The annotation holding a single choice. */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface ConverterChoice{

		/**
		 * The condition that needs to hold, if an instance of {@link #converter() converter} is to be applied.
		 * <p>A SpEL expression with the prefix value in the context under the name `{@code prefix}`.</p>
		 *
		 * @return	The condition that needs to hold, if an instance of {@link #converter() converter} is to be applied.
		 */
		String condition();

		/**
		 * The converter to be applied just <i>before</i> writing the parameter value (<i>after</i> if reading), if any.
		 * <p>Usually the fully qualified name of an implementation class of a {@link Converter}</p>
		 *
		 * @return	The class of a {@link Converter}
		 */
		Class<? extends Converter<?, ?>> converter() default NullConverter.class;

	}

}
