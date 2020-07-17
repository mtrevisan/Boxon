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
package unit731.boxon.helpers;

import java.util.HashMap;
import java.util.Map;


public enum DataType{

	BYTE(Byte.TYPE, Byte.class),
	SHORT(Short.TYPE, Short.class),
	INTEGER(Integer.TYPE, Integer.class),
	LONG(Long.TYPE, Long.class),
	FLOAT(Float.TYPE, Float.class),
	DOUBLE(Double.TYPE, Double.class);

	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class} */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>(6);
	/** Maps wrapper {@code Class}es to their corresponding primitive types */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new HashMap<>(6);
	private static final Map<Class<?>, DataType> TYPE_MAP = new HashMap<>(12);
	static{
		for(final DataType te : values()){
			PRIMITIVE_WRAPPER_MAP.put(te.primitiveType, te.objectiveType);
			WRAPPER_PRIMITIVE_MAP.put(te.objectiveType, te.primitiveType);
			TYPE_MAP.put(te.primitiveType, te);
			TYPE_MAP.put(te.objectiveType, te);
		}
	}

	final Class<?> primitiveType;
	final Class<?> objectiveType;


	DataType(final Class<?> primitiveType, final Class<?> objectiveType){
		this.primitiveType = primitiveType;
		this.objectiveType = objectiveType;
	}

	public static Class<?> toObjectiveTypeOrDefault(final Class<?> primitiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(primitiveType, primitiveType);
	}

	public static Class<?> toPrimitiveTypeOrDefault(final Class<?> objectiveType){
		return WRAPPER_PRIMITIVE_MAP.getOrDefault(objectiveType, objectiveType);
	}

	public static DataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}

	public static boolean isObjectivePrimitive(final Class<?> type){
		return WRAPPER_PRIMITIVE_MAP.containsKey(type);
	}

}
