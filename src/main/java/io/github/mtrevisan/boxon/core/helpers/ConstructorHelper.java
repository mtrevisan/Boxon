/*
 * Copyright (c) 2024 Mauro Trevisan
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

import io.github.mtrevisan.boxon.helpers.Memoizer;
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Provides static methods to extract creator functions for classes.
 */
public final class ConstructorHelper{

	private static Function<Class<?>, Supplier<?>> EMPTY_CREATORS;
	private static Function<Map.Entry<Class<?>, Class<?>[]>, Function<Object[], ?>> NON_EMPTY_CREATORS;
	static{
		initialize(-1, -1);
	}

	private static final Objenesis OBJENESIS = new ObjenesisStd();


	private ConstructorHelper(){}


	public static void initialize(final int maxEmptyConstructorMemoizerSize, final int maxNonEmptyConstructorMemoizerSize){
		EMPTY_CREATORS = Memoizer.memoize(ConstructorHelper::getEmptyCreatorInner, maxEmptyConstructorMemoizerSize);
		NON_EMPTY_CREATORS = Memoizer.memoize(ConstructorHelper::getNonEmptyCreatorInner, maxNonEmptyConstructorMemoizerSize);
	}


	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	public static <T> Supplier<T> getEmptyCreator(final Class<T> type){
		return (Supplier<T>)EMPTY_CREATORS.apply(type);
	}

	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param parametersClass	Array of types of constructor parameters.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	public static <T> Function<Object[], T> getNonEmptyCreator(final Class<T> type, final Class<?>[] parametersClass){
		return (Function<Object[], T>)NON_EMPTY_CREATORS.apply(new AbstractMap.SimpleEntry<>(type, parametersClass));
	}

	private static <T> Supplier<T> getEmptyCreatorInner(final Class<T> type){
		ObjectInstantiator<T> instantiator;
		try{
			instantiator = getConstructor(type);
		}
		catch(final ReflectiveOperationException ignored){
			instantiator = OBJENESIS.getInstantiatorOf(type);
		}
		return instantiator::newInstance;
	}

	private static <T> ObjectInstantiator<T> getConstructor(final Class<T> type) throws ReflectiveOperationException{
		final Constructor<T> constructor = type.getDeclaredConstructor();
		FieldAccessor.makeAccessible(constructor);

		//try creating an instance
		constructor.newInstance();

		return () -> {
			try{
				return constructor.newInstance();
			}
			catch(final Exception e){
				throw new ObjenesisException(e);
			}
		};
	}

	private static Function<Object[], ?> getNonEmptyCreatorInner(final Map.Entry<Class<?>, Class<?>[]> tuple){
		Function<Object[], ?> instantiator = null;
		try{
			final Class<?> type = tuple.getKey();
			final Class<?>[] parametersClass = tuple.getValue();
			final Constructor<?> constructor = type.getDeclaredConstructor(parametersClass);
			FieldAccessor.makeAccessible(constructor);

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


	static <T> T constructRecordWithUpdatedField(final T obj, final String fieldName, final Object value)
			throws ReflectiveOperationException{
		final Class<T> objClass = (Class<T>)obj.getClass();
		final RecordComponent[] recordComponents = objClass.getRecordComponents();

		//extract the current field values from the record class
		final Object[] recordValues = FieldAccessor.retrieveCurrentFieldValues(obj, recordComponents);

		//find the index of the field to update
		FieldAccessor.updateFieldValue(fieldName, value, recordComponents, recordValues);

		return createRecordInstance(recordComponents, objClass, recordValues);
	}

	private static <T> T createRecordInstance(final RecordComponent[] recordComponents, final Class<T> objClass,
			final Object[] recordValues){
		//extract the field types from the record class
		final Class<?>[] parametersClass = extractFieldTypes(recordComponents);

		//creates a new instance of the record class with the updated values
		return getNonEmptyCreator(objClass, parametersClass)
			.apply(recordValues);
	}

	private static Class<?>[] extractFieldTypes(final RecordComponent[] components){
		final int length = components.length;
		final Class<?>[] parametersClass = new Class<?>[length];
		for(int i = 0; i < length; i ++)
			parametersClass[i] = components[i].getType();
		return parametersClass;
	}

}
