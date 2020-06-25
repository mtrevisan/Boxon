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

import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Manages an annotated {@link Class} (... before the application of a converter)
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindObject{

	/**
	 * The type of object.
	 * <p>Note that this allows you to have a field of a super type of the actual type that
	 * you expect to inject. So you might have something like this:</p>
	 * <pre><code>
	 * class A {
	 * }
	 *
	 * class B extends A {
	 * 	&#064;BoundLong
	 * 	...
	 * }
	 *
	 * ...
	 *
	 * &#064;BoundObject(type = B.class)
	 * private A a; // Array will contain instances of B.
	 * </code></pre>
	 *
	 * @return	The (super) type of object to be inserted in the array.
	 */
	Class<?> type() default Object.class;

	/**
	 * The choices to select from, based on a prefix of a certain size.
	 *
	 * @return The choices to select from, based on a prefix of a certain size.
	 */
	Choices selectFrom() default @Choices();

	/**
	 * The validator to be applied before applying the converter, if any. Usually the fully qualified
	 * name of an implementation class of a {@link Validator}
	 *
	 * @return	The class of a {@link Validator}
	 */
	Class<? extends Validator> validator() default NullValidator.class;

	/**
	 * The converter to be applied before writing the parameter value. Usually the fully qualified
	 * name of an implementation class of a {@link Converter}
	 *
	 * @return	The class of a {@link Converter}
	 */
	Class<? extends Converter> converter() default NullConverter.class;

}
