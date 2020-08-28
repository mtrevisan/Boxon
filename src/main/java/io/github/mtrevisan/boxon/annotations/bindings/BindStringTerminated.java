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
package io.github.mtrevisan.boxon.annotations.bindings;

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages a {@link String} with a given terminator {@code byte} (... before the application of a converter).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface BindStringTerminated{

	/**
	 * The SpEL expression that determines if an evaluation has to be made.
	 *
	 * @return	The condition that determines if an evaluation has to be made (defaults to empty, that means &quot;accept&quot;).
	 */
	String condition() default "";

	/**
	 * The type of encoding used for the {@link String}.
	 *
	 * @return	The type of encoding used (defaults to `UTF-8`).
	 */
	String charset() default "UTF-8";

	/**
	 * The byte that terminates the {@link String}.
	 *
	 * @return	The terminator byte (defaults to `\0`).
	 */
	byte terminator() default '\0';

	/**
	 * Whether to consume the terminator.
	 *
	 * @return	Whether to consume the terminator (defaults to {@code true}).
	 */
	boolean consumeTerminator() default true;

	/**
	 * The value, regex, or SpEL expression evaluating to the value to match, if any.
	 *
	 * @return	The value, or regex, or SpEL expression to be checked for equality (defaults to empty, that means &quot;accept anything&quot;).
	 */
	String match() default "";

	/**
	 * The validator to be applied <i>after</i> applying the converter, in the decoding phase (<i>before</i> if in the encoding one), if any.
	 * <p>Usually the fully qualified name of an implementation class of a {@link Validator}.</p>
	 *
	 * @return	The class of a {@link Validator} (defaults to {@link NullValidator null validator}).
	 */
	Class<? extends Validator<?>> validator() default NullValidator.class;

	/**
	 * The converter to be applied just <i>before</i> writing the parameter value (<i>after</i> if reading), if any.
	 * <p>Usually the fully qualified name of an implementation class of a {@link Converter}.</p>
	 * <p>Can be used as a fallback if no converters can be selected from {@link #selectConverterFrom()}.</p>
	 *
	 * @return	The class of a {@link Converter} (defaults to {@link NullConverter null converter}).
	 */
	Class<? extends Converter<?, ?>> converter() default NullConverter.class;

	/**
	 * The choices to select from to apply a given converter.
	 *
	 * @return The choices to select from to apply a given converter (defaults to empty {@link ConverterChoices}).
	 */
	ConverterChoices selectConverterFrom() default @ConverterChoices;

}
