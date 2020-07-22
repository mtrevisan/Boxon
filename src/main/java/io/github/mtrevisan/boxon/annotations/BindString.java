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
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages a {@link String} (... before the application of a converter)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BindString{

	/**
	 * The SpEL expression that determines if an evaluation has to be made.
	 *
	 * @return	The condition that determines if an evaluation has to be made (can be an expression).
	 */
	String condition() default "";

	/**
	 * The type of encoding used for the {@link String}
	 *
	 * @return	The type of encoding used (defaults to `UTF-8`).
	 */
	String charset() default "UTF-8";

	/**
	 * The number of bytes to be interpreted as a {@link String}
	 *
	 * @return	The number of bytes (can be an expression).
	 */
	String size();

	/**
	 * The value to match (can be a regex expression or a SpEL expression), if any.
	 *
	 * @return	The value, or regex, or SpEL expression to be checked for equality
	 */
	String match() default "";

	/**
	 * The validator to be applied <i>after</i> applying the converter, in the decoding phase (<i>before</i> if in the encoding one), if any.
	 * <p>Usually the fully qualified name of an implementation class of a {@link Validator}</p>
	 *
	 * @return	The class of a {@link Validator}
	 */
	Class<? extends Validator> validator() default NullValidator.class;

	/**
	 * The converter to be applied just <i>before</i> writing the parameter value (<i>after</i> if reading), if any.
	 * <p>Usually the fully qualified name of an implementation class of a {@link Converter}</p>
	 *
	 * @return	The class of a {@link Converter}
	 */
	Class<? extends Converter> converter() default NullConverter.class;

	/**
	 * The choices to select from to apply a given converter.
	 *
	 * @return The choices to select from to apply a given converter.
	 */
	ConverterChoices selectConverterFrom() default @ConverterChoices();

}
