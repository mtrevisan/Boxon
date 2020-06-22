package unit731.boxon.annotations;


import unit731.boxon.codecs.ByteOrder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * The annotation allowing you to define a number of choices, based a prefix of a certain {@link #prefixSize() size}.
 *
 * @author Wilfred Springer (wis)
 */
public @interface Choices{

	/**
	 * The number of bits to be read for determining the prefix.
	 *
	 * @return	The number of bits to be read for determining the prefix.
	 */
	int prefixSize() default 0;

	/**
	 * The byte order to take into account when returning a representation of the first {@link #prefixSize() size} bits
	 * read as a prefix.
	 *
	 * @return	The byte order to take into account when returning a representation of the first {@link #prefixSize()
	 * 			size} bits read as a prefix.
	 */
	ByteOrder byteOrder() default ByteOrder.BIG_ENDIAN;

	/**
	 * The choices to select from.
	 *
	 * @return	The choices to select from.
	 */
	Choice[] alternatives() default {};


	/** The annotation holding a single choice. */
	@interface Choice{

		/**
		 * The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 * A SpEL expression with the prefix value in the context under the name `__prefix`.
		 *
		 * @return	The condition that needs to hold, if an instance of {@link #type() type} is to be decoded.
		 */
		String condition();

		/**
		 * The type to decode in case the {@link #condition()} holds.
		 *
		 * @return	The type to decode in case the {@link #condition()} holds.
		 */
		Class<?> type();

	}


	/** The annotation used to indicate the discriminator used to recognize an instance of this class. */
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Prefix{

		/**
		 * The value that will be used to match this particular record.
		 *
		 * @return The value.
		 */
		long value();

	}

}
