package unit731.boxon.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Assign{

	/**
	 * The expression to be evaluated.
	 * @see <a href="https://docs.spring.io/spring/docs/5.2.7.RELEASE/spring-framework-reference/core.html#expressions">Spring Expression Language (SpEL)</a>
	 *
	 * @return	The expression to be evaluated.
	 */
	String value();

}
