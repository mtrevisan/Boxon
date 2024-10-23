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
 * Takes care of the conversion between primitive types and their wrappers, and vice versa.
 */
public final class DataTypeMapper{

	private static final Map<String, Class<?>> PRIMITIVE_TYPE_MAP = new HashMap<>(9);
	static{
		PRIMITIVE_TYPE_MAP.put(Boolean.class.getName(), boolean.class);
		PRIMITIVE_TYPE_MAP.put(Byte.class.getName(), byte.class);
		PRIMITIVE_TYPE_MAP.put(Character.class.getName(), char.class);
		PRIMITIVE_TYPE_MAP.put(Short.class.getName(), short.class);
		PRIMITIVE_TYPE_MAP.put(Integer.class.getName(), int.class);
		PRIMITIVE_TYPE_MAP.put(Long.class.getName(), long.class);
		PRIMITIVE_TYPE_MAP.put(Float.class.getName(), float.class);
		PRIMITIVE_TYPE_MAP.put(Double.class.getName(), double.class);
		PRIMITIVE_TYPE_MAP.put(Void.class.getName(), void.class);
	}
	private static final Map<String, Class<?>> OBJECTIVE_TYPE_MAP = new HashMap<>(9);
	static{
		OBJECTIVE_TYPE_MAP.put(boolean.class.getSimpleName(), Boolean.class);
		OBJECTIVE_TYPE_MAP.put(byte.class.getSimpleName(), Byte.class);
		OBJECTIVE_TYPE_MAP.put(char.class.getSimpleName(), Character.class);
		OBJECTIVE_TYPE_MAP.put(short.class.getSimpleName(), Short.class);
		OBJECTIVE_TYPE_MAP.put(int.class.getSimpleName(), Integer.class);
		OBJECTIVE_TYPE_MAP.put(long.class.getSimpleName(), Long.class);
		OBJECTIVE_TYPE_MAP.put(float.class.getSimpleName(), Float.class);
		OBJECTIVE_TYPE_MAP.put(double.class.getSimpleName(), Double.class);
		OBJECTIVE_TYPE_MAP.put(void.class.getSimpleName(), Void.class);
	}


	private DataTypeMapper(){}


	/**
	 * Returns whether the given {@code type} is a primitive.
	 * <p>NOTE: {@code void} is NOT considered a primitive!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive.
	 */
	public static boolean isPrimitive(final Class<?> type){
		return (type.isPrimitive() && type != void.class);
	}

	/**
	 * Returns whether the given {@code type} is a primitive or primitive wrapper.
	 * <p>NOTE: {@code Character} and {@code void}/{@code Void} are NOT considered primitives!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive or primitive wrapper.
	 */
	public static boolean isPrimitiveOrWrapper(final Class<?> type){
		return (isPrimitive(type) || isPrimitiveWrapper(type));
	}

	private static boolean isPrimitiveWrapper(final Class<?> type){
		return (toPrimitiveTypeOrSelf(type) != type);
	}


	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param typeName	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toTypeOrSelf(final String typeName) throws ClassNotFoundException{
		//check if it's an objective or primitive type
		final Class<?> objectiveType = OBJECTIVE_TYPE_MAP.get(typeName);
		Class<?> type = PRIMITIVE_TYPE_MAP.getOrDefault(typeName,
			(objectiveType != null? PRIMITIVE_TYPE_MAP.get(objectiveType.getName()): null));

		//try to extract the class
		if(type == null){
			try{
				type = Class.forName(typeName);
			}
			catch(final Exception ignored){
				throw new ClassNotFoundException("Cannot find class for `" + typeName + "`");
			}
		}
		return type;
	}

	/**
	 * Convert a type to an objective type, if applicable, otherwise returns the type itself.
	 *
	 * @param primitiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toObjectiveTypeOrSelf(final Class<?> primitiveType){
		if(primitiveType == null)
			return null;
		if(!isPrimitive(primitiveType))
			return primitiveType;

		return OBJECTIVE_TYPE_MAP.getOrDefault(primitiveType.getSimpleName(), primitiveType);
	}

	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param objectiveType	The type to be converted.
	 * @return	The converted type;
	 */
	private static Class<?> toPrimitiveTypeOrSelf(final Class<?> objectiveType){
		if(objectiveType == null)
			return null;
		if(isPrimitive(objectiveType))
			return objectiveType;

		return PRIMITIVE_TYPE_MAP.getOrDefault(objectiveType.getSimpleName(), objectiveType);
	}

}
