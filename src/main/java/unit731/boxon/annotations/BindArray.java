package unit731.boxon.annotations;

import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindArray{

	/**
	 * The type of object to be inserted into the array.
	 * Note that this allows you to have a field of a super type of the actual type that
	 * you expect to inject. So you might have something like this:
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
	 * &#064;BoundArray(type = B.class)
	 * private A[] a; // Array will contain instances of B.
	 * </code></pre>
	 *
	 * @return	The type of object to be inserted in the array.
	 */
	Class<?> type();

	/**
	 * The size of the array
	 *
	 * @return	The size of the array (can be an expression).
	 */
	String size();

	/**
	 * The validator to be applied before applying the transformer, if any. Usually the fully qualified
	 * name of an implementation class of a {@link Validator}
	 *
	 * @return	The class of a {@link Validator}
	 */
	Class<? extends Validator> validator() default NullValidator.class;

	/**
	 * The transformer to be applied before writing the parameter value. Usually the fully qualified
	 * name of an implementation class of a {@link Transformer}
	 *
	 * @return	The class of a {@link Transformer}
	 */
	Class<? extends Transformer> transformer() default NullTransformer.class;

}
