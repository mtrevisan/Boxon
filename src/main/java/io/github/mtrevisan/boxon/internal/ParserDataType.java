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

import java.util.HashMap;
import java.util.Map;


public enum ParserDataType{

	BYTE(Byte.TYPE, Byte.class),
	SHORT(Short.TYPE, Short.class),
	INTEGER(Integer.TYPE, Integer.class),
	LONG(Long.TYPE, Long.class),
	FLOAT(Float.TYPE, Float.class),
	DOUBLE(Double.TYPE, Double.class);

	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class}. */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>(6);
	/** Maps wrapper {@code Class}es to their corresponding primitive types. */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new HashMap<>(6);
	private static final Map<Class<?>, ParserDataType> TYPE_MAP = new HashMap<>(12);
	static{
		for(final ParserDataType dt : values()){
			PRIMITIVE_WRAPPER_MAP.put(dt.primitiveType, dt.objectiveType);
			WRAPPER_PRIMITIVE_MAP.put(dt.objectiveType, dt.primitiveType);
			TYPE_MAP.put(dt.primitiveType, dt);
			TYPE_MAP.put(dt.objectiveType, dt);
		}
	}

	private final Class<?> primitiveType;
	private final Class<?> objectiveType;


	public static ParserDataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}

	ParserDataType(final Class<?> primitiveType, final Class<?> objectiveType){
		this.primitiveType = primitiveType;
		this.objectiveType = objectiveType;
	}

	public static Class<?> toObjectiveTypeOrDefault(final Class<?> primitiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(primitiveType, primitiveType);
	}

	public static Class<?> toPrimitiveTypeOrDefault(final Class<?> objectiveType){
		return WRAPPER_PRIMITIVE_MAP.getOrDefault(objectiveType, objectiveType);
	}

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

	private static boolean isPrimitiveWrapper(final Class<?> type){
		return WRAPPER_PRIMITIVE_MAP.containsKey(type);
	}

	/**
	 * Returns whether the given {@code type} is a primitive or primitive wrapper.
	 * <p>NOTE: {@code Character} and {@code void}/{@code Void} are NOT considered as primitives!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive or primitive wrapper.
	 */
	public static boolean isPrimitiveOrWrapper(final Class<?> type){
		return (isPrimitive(type) || isPrimitiveWrapper(type));
	}

}
