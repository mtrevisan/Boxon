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
public @interface BindString{

	/**
	 * The type of encoding used for the {@link String}
	 *
	 * @return	The type of encoding used. Defaults to UTF-8.
	 */
	String charset() default "UTF-8";

	/**
	 * The number of bytes to be interpreted as a {@link String}
	 *
	 * @return	The number of bytes (can be an expression).
	 */
	String size();

	/** The value to match (can be a regex expression or a SpEL expression). */
	String match() default "";

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
