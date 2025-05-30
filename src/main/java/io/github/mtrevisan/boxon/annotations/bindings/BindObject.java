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
 * Manages an annotated {@link Class} of the given {@link #type()} (... before the application of a converter).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface BindObject{

	/**
	 * The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression that determines if an
	 * evaluation has to be made.
	 *
	 * @return	The condition that determines if an evaluation has to be made (defaults to empty, that means &quot;accept&quot;).
	 */
	String condition() default "";

	/**
	 * The type of object.
	 * <p>Note that this allows you to declare a field with a supertype of the actual type you expect to inject.</p>
	 * <p>So you might have something like this:</p>
	 * <pre>{@code
	 * class A { ... }
	 *
	 * class B extends A { ... }
	 *
	 * ...
	 *
	 * &#064;BindObject(type = B.class)
	 * private A array;	//object will contain instances of B
	 * }</pre>
	 *
	 * @return	The (super) type of object to be inserted in the array.
	 */
	Class<?> type();

	/**
	 * The choices to select from, based on a prefix of a certain size.
	 *
	 * @return	The choices to select from, based on a prefix of a certain size (defaults to empty {@link ObjectChoices}).
	 */
	ObjectChoices selectFrom() default @ObjectChoices;

	/**
	 * The choices to select from, based on a prefix.
	 *
	 * @return	The choices to select from, based on a prefix (defaults to empty {@link ObjectChoices}).
	 */
	ObjectChoicesList selectFromList() default @ObjectChoicesList;

	/**
	 * The type to decode in case none of the selectors from {@link #selectFrom()} can be chosen.
	 *
	 * @return	The type to decode in case none of the selectors from {@link #selectFrom()} can be chosen.
	 */
	Class<?> selectDefault() default void.class;

	/**
	 * The validator to be applied <i>after</i> applying the converter, in the decoding phase (<i>before</i> if in the encoding one), if any.
	 *
	 * @return	The class of a {@link Validator} (defaults to {@link NullValidator null validator}).
	 */
	Class<? extends Validator<?>> validator() default NullValidator.class;

	/**
	 * The converter to be applied just <i>before</i> writing the parameter value (<i>after</i> if reading), if any.
	 * <p>Can be used as a fallback if no converters can be selected from {@link #selectConverterFrom()}.</p>
	 *
	 * @return	The class of a {@link Converter} (defaults to {@link NullConverter null converter}).
	 */
	Class<? extends Converter<?, ?>> converter() default NullConverter.class;

	/**
	 * The choices to select from to apply a given converter.
	 *
	 * @return	The choices to select from to apply a given converter (defaults to empty {@link ConverterChoices}).
	 */
	ConverterChoices selectConverterFrom() default @ConverterChoices;


	/**
	 * A long description of the field.
	 *
	 * @return	A long description of the field.
	 */
	String longDescription() default "";

	/**
	 * The unit of measure of the value (the format should follow
	 * <a href="https://ucum.org/ucum.html">UCUM</a>/<a href="https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure">ISO 80000</a>
	 * standard).
	 *
	 * @return	The unit of measure of the value.
	 */
	String unitOfMeasure() default "";

}
