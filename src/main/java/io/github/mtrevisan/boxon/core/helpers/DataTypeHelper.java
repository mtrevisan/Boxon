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
package io.github.mtrevisan.boxon.core.helpers;

import java.util.HashMap;
import java.util.Map;


/**
 * Contains utility methods such as calculating the size of primitive or wrapper types and checking whether a type is primitive or not.
 */
public final class DataTypeHelper{

	private static final Map<Class<?>, Integer> SIZE_MAP = new HashMap<>(12);
	static{
		SIZE_MAP.put(byte.class, Byte.SIZE);
		SIZE_MAP.put(Byte.class, Byte.SIZE);
		SIZE_MAP.put(short.class, Short.SIZE);
		SIZE_MAP.put(Short.class, Short.SIZE);
		SIZE_MAP.put(int.class, Integer.SIZE);
		SIZE_MAP.put(Integer.class, Integer.SIZE);
		SIZE_MAP.put(long.class, Long.SIZE);
		SIZE_MAP.put(Long.class, Long.SIZE);
		SIZE_MAP.put(float.class, Float.SIZE);
		SIZE_MAP.put(Float.class, Float.SIZE);
		SIZE_MAP.put(double.class, Double.SIZE);
		SIZE_MAP.put(Double.class, Double.SIZE);
	}
	private static final Integer SIZE_DEFAULT_VALUE = -1;


	private DataTypeHelper(){}


	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param objectiveType	The type of the number.
	 * @return	The number of bits.
	 */
	public static int getSize(final Class<?> objectiveType){
		return SIZE_MAP.getOrDefault(objectiveType, SIZE_DEFAULT_VALUE);
	}

	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param size	The size of the number in bits.
	 * @return	The primitive number class.
	 */
	public static Class<?> getPrimitive(final int size){
		for(final Map.Entry<Class<?>, Integer> entry : SIZE_MAP.entrySet()){
			final Class<?> key = entry.getKey();
			if(key.isPrimitive() && !isFloatType(key) && entry.getValue() == size)
				return key;
		}
		return null;
	}

	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param size	The size of the number in bits.
	 * @return	The primitive number class.
	 */
	public static Class<?> getFloatPrimitive(final int size){
		for(final Map.Entry<Class<?>, Integer> entry : SIZE_MAP.entrySet()){
			final Class<?> key = entry.getKey();
			if(key.isPrimitive() && isFloatType(key) && entry.getValue() == size)
				return key;
		}
		return null;
	}

	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param size	The size of the number in bits.
	 * @return	The wrapper number class.
	 */
	public static Class<?> getWrapper(final int size){
		for(final Map.Entry<Class<?>, Integer> entry : SIZE_MAP.entrySet()){
			final Class<?> key = entry.getKey();
			if(!key.isPrimitive() && !isFloatType(key) && entry.getValue() == size)
				return key;
		}
		return null;
	}

	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param size	The size of the number in bits.
	 * @return	The wrapper number class.
	 */
	public static Class<?> getFloatWrapper(final int size){
		for(final Map.Entry<Class<?>, Integer> entry : SIZE_MAP.entrySet()){
			final Class<?> key = entry.getKey();
			if(!key.isPrimitive() && isFloatType(key) && entry.getValue() == size)
				return key;
		}
		return null;
	}

	/**
	 * Tests whether a class represents a float or double type.
	 *
	 * @param type	The class to test.
	 * @return	Whether it's a float type.
	 */
	public static boolean isFloatType(final Class<?> type){
		return (type == float.class || type == Float.class
			|| type == double.class || type == Double.class);
	}

}
