/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers;

import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A collection of convenience methods for working with constructors.
 */
public final class ConstructorHelper{

	private static final Function<Class<?>, Supplier<?>> CREATORS = Memoizer.memoize(ConstructorHelper::getCreatorInner);


	private static final Objenesis OBJENESIS = new ObjenesisStd();


	private ConstructorHelper(){}


	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> getCreator(final Class<T> type){
		return (Supplier<T>)CREATORS.apply(type);
	}

	private static <T> Supplier<T> getCreatorInner(final Class<T> type){
		ObjectInstantiator<T> instantiator;
		try{
			final Constructor<T> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			//try creating an instance
			constructor.newInstance();

			instantiator = () -> {
				try{
					return constructor.newInstance();
				}
				catch(final Exception e){
					throw new ObjenesisException(e);
				}
			};
		}
		catch(final Exception ignored){
			instantiator = OBJENESIS.getInstantiatorOf(type);
		}
		return instantiator::newInstance;
	}

}
