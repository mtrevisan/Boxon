package unit731.boxon.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageHeader{

	String[] start() default {};

	String end() default "";

	/**
	 * The type of encoding used for the `start`, `end`, and `separator` fields
	 *
	 * @return	The type of encoding used. Defaults to UTF-8.
	 */
	String charset() default "UTF-8";

}
