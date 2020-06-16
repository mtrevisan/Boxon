package unit731.boxon.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * A simple annotation for marking particular fields to be optional, depending on the condition.
 * The condition is based on the Limbo notation. Variables are expected to be resolved relatively to the object holding the annotated field.
 * <p>Example snippet:</p>
 * <pre><code>
 * private int databaseVersion;
 *
 * &#64;If(&quot;databaseVersion &gt; 700&quot;)
 * &#64;BoundInteger
 * private int foobar;
 * </code></pre>
 * In the above case, <code>foobar</code> is only expected to be read only if the condition holds,
 * so if <code>databaseVersion</code> is 300, it will be skipped.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindIf{

	/**
	 * The expression to be evaluated.
	 *
	 * @return	The expression to be evaluated.
	 */
	String value();

}
