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
package io.github.mtrevisan.boxon.helpers;

import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A collection of convenience methods for working with constructors.
 */
public final class ConstructorHelper{

	private static final Function<Class<?>, Supplier<?>> EMPTY_CREATORS = Memoizer.memoize(ConstructorHelper::getEmptyCreatorInner);
	private static final Function<NonEmptyConstructorTuple<?>, Function<Object[], ?>> NON_EMPTY_CREATORS
		= Memoizer.memoize(ConstructorHelper::getNonEmptyCreatorInner);

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
	public static <T> Supplier<T> getEmptyCreator(final Class<T> type){
		return (Supplier<T>)EMPTY_CREATORS.apply(type);
	}

	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param constructorClasses	Array of types of constructor parameters.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Function<Object[], T> getNonEmptyCreator(final Class<T> type, final Class<?>[] constructorClasses){
		return (Function<Object[], T>)NON_EMPTY_CREATORS.apply(new NonEmptyConstructorTuple<>(type, constructorClasses));
	}

	private static <T> Supplier<T> getEmptyCreatorInner(final Class<T> type){
		ObjectInstantiator<T> instantiator;
		try{
			final Constructor<T> constructor = type.getDeclaredConstructor();
			ReflectionHelper.makeAccessible(constructor);

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

	private static <T> Function<Object[], T> getNonEmptyCreatorInner(final NonEmptyConstructorTuple<T> tuple){
		Function<Object[], T> instantiator = null;
		try{
			final Class<T> type = tuple.type;
			final Class<?>[] constructorClasses = tuple.constructorClasses;
			final Constructor<T> constructor = type.getDeclaredConstructor(constructorClasses);
			ReflectionHelper.makeAccessible(constructor);

			instantiator = (final Object[] constructorValues) -> {
				try{
					return constructor.newInstance(constructorValues);
				}
				catch(final Exception e){
					throw new ObjenesisException(e);
				}
			};
		}
		catch(final Exception ignored){}
		return instantiator;
	}


	private record NonEmptyConstructorTuple<T>(Class<T> type, Class<?>[] constructorClasses){
		@Override
		public boolean equals(final Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;
			final NonEmptyConstructorTuple<?> other = (NonEmptyConstructorTuple<?>)obj;
			return (Objects.equals(type, other.type) && Arrays.equals(constructorClasses, other.constructorClasses));
		}

		@Override
		public int hashCode(){
			return 31 * Objects.hash(type) + Arrays.hashCode(constructorClasses);
		}
	}
}
