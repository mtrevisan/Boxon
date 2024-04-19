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

import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class MethodHelper{

	private MethodHelper(){}


	/**
	 * Invokes the underlying method represented by the given {@code Method} object, on the specified object.
	 *
	 * @param obj	The object the underlying method is invoked from.
	 * @param method	The method to be called on the given object.
	 * @param defaultValue	The default value should the method not exists, or returns an error.
	 * @param <T>	The class type of the default value and the returned value.
	 * @return	The value returned by the given method, or the default value if an exception occurs.
	 */
	public static <T> T invokeMethod(final Object obj, final Method method, final T defaultValue){
		T result = defaultValue;
		try{
			result = (T)method.invoke(obj);
		}
		catch(final Exception ignored){}
		return result;
	}

	/**
	 * Invokes the underlying static method represented by the given {@code Method} object.
	 *
	 * @param type	The class containing the method.
	 * @param methodName	The method name.
	 * @param input	The input value.
	 * @return	The value returned by the given method.
	 */
	public static Object invokeStaticMethod(final Class<?> type, final String methodName, final Object input) throws NoSuchMethodException,
			InvocationTargetException, IllegalAccessException{
		final Method method = type.getDeclaredMethod(methodName, input.getClass());
		return method.invoke(null, input);
	}

	/**
	 * Get an accessible method defined in the given class (or one of its parents), with the given name, return type, and parameters' types.
	 *
	 * @param cls	The class from which to extract the method.
	 * @param methodName	The method name.
	 * @param returnType	The method return type.
	 * @param parameterTypes	The method parameters' types.
	 * @return	The method, or {@code null} if not found.
	 */
	public static Method getAccessibleMethodFromClassHierarchy(Class<?> cls, final String methodName, final Class<?> returnType,
			final Class<?>... parameterTypes){
		Method method = null;
		while(method == null && cls != null && cls != Object.class){
			method = getMethod(cls, methodName, returnType, parameterTypes);

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return method;
	}

	/**
	 * Get a method defined in the given class, with the given name, return type, and parameters' types.
	 *
	 * @param cls	The class from which to extract the method.
	 * @param methodName	The method name.
	 * @param returnType	The method return type (if {@code null} then no check on the return type is performed).
	 * @param parameterTypes	The method parameters' types.
	 * @return	The method, or {@code null} if not found.
	 */
	public static Method getMethod(final Class<?> cls, final String methodName, final Class<?> returnType, final Class<?>... parameterTypes){
		Method method = null;
		try{
			method = cls.getDeclaredMethod(methodName, parameterTypes);

			if(isReturnTypeCompatible(method, returnType))
				FieldAccessor.makeAccessible(method);
			else
				method = null;
		}
		catch(final NoSuchMethodException | SecurityException | InaccessibleObjectException ignored){}
		return method;
	}

	private static boolean isReturnTypeCompatible(final Method method, final Class<?> returnType){
		return (returnType == null || method.getReturnType().equals(returnType));
	}

	/**
	 * Get all the declared methods defined in the given class.
	 *
	 * @param cls	The class from which to extract the methods.
	 * @return	The method list, or an empty list if no methods are found.
	 */
	public static Method[] getMethods(final Class<?> cls){
		Method[] methods = null;
		try{
			methods = cls.getDeclaredMethods();
		}
		catch(final SecurityException | InaccessibleObjectException ignored){}
		return methods;
	}

}
