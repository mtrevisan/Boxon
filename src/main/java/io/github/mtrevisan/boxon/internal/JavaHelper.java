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
package io.github.mtrevisan.boxon.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;


public final class JavaHelper{

	private JavaHelper(){}

	public static Logger getLoggerFor(final Class<?> type){
		try{
			//This is to check whether an optional SLF4J binding is available.
			//While SLF4J recommends that libraries "should not declare a dependency on any SLF4J binding but only
			//depend on slf4j-api", doing so forces users of the library to either add a binding to the classpath
			//(even if just slf4j-nop) or to set the "slf4j.suppressInitError" system property in order to avoid
			//the warning, which both is inconvenient.
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
			return LoggerFactory.getLogger(type);
		}
		catch(final Throwable e){
			return null;
		}
	}


	public static <R> Set<R> filter(final Collection<R> result, final Predicate<? super R> predicate){
		return filter(result, Function.identity(), predicate);
	}

	public static <R, T> Set<R> filter(final Collection<T> result, final Function<T, R> converter, final Predicate<? super R> predicate){
		final Set<R> set = new HashSet<>(result.size());
		for(final T t : result){
			final R r = converter.apply(t);
			if(predicate.test(r))
				set.add(r);
		}
		return set;
	}


	/**
	 * Converts an array of bytes into a string representing the hexadecimal values of each byte in order.
	 *
	 * @param array	Array to be converted to hexadecimal characters.
	 * @return	The hexadecimal characters.
	 */
	public static String toHexString(final byte[] array){
		final int length = JavaHelper.lengthOrZero(array);
		final StringBuilder sb = new StringBuilder(length << 1);
		for(int i = 0; i < length; i ++){
			final byte elem = array[i];
			sb.append(Character.forDigit((elem >>> 4) & 0x0F, 16));
			sb.append(Character.forDigit((elem & 0x0F), 16));
		}
		return sb.toString().toUpperCase();
	}

	/**
	 * Converts a string representing the hexadecimal values of each byte to an array of bytes in order.
	 *
	 * @param hexString	The hexadecimal string.
	 * @return	Array of converted hexadecimal characters.
	 */
	public static byte[] toByteArray(final String hexString){
		final int len = JavaHelper.lengthOrZero(hexString);
		if(len % 2 != 0)
			throw new IllegalArgumentException("Malformed input");

		final byte[] data = new byte[len >>> 1];
		for(int i = 0; i < len; i += 2)
			data[i >>> 1] = (byte)((Character.digit(hexString.charAt(i), 16) << 4) + Character.digit(hexString.charAt(i + 1), 16));
		return data;
	}


	public static <T> T nonNullOrDefault(final T obj, final T defaultObject){
		return (obj != null? obj: defaultObject);
	}

	public static int lengthOrZero(final String text){
		return (text != null? text.length(): 0);
	}

	public static int lengthOrZero(final byte[] array){
		return (array != null? array.length: 0);
	}

	public static <T> int lengthOrZero(final T[] array){
		return (array != null? array.length: 0);
	}

	public static boolean isNotBlank(final String text){
		return (text != null && !text.isBlank());
	}

}
