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
final class DataTypeHelper{

	static final String PRIMITIVE_TYPE_NAME_BOOLEAN = "boolean";
	static final String PRIMITIVE_TYPE_NAME_BYTE = "byte";
	static final String PRIMITIVE_TYPE_NAME_CHAR = "char";
	static final String PRIMITIVE_TYPE_NAME_SHORT = "short";
	static final String PRIMITIVE_TYPE_NAME_INT = "int";
	static final String PRIMITIVE_TYPE_NAME_LONG = "long";
	static final String PRIMITIVE_TYPE_NAME_FLOAT = "float";
	static final String PRIMITIVE_TYPE_NAME_DOUBLE = "double";
	static final String PRIMITIVE_TYPE_NAME_VOID = "void";
	static final String OBJECTIVE_TYPE_NAME_BOOLEAN = "Boolean";
	static final String OBJECTIVE_TYPE_NAME_BYTE = "Byte";
	static final String OBJECTIVE_TYPE_NAME_CHARACTER = "Character";
	static final String OBJECTIVE_TYPE_NAME_SHORT = "Short";
	static final String OBJECTIVE_TYPE_NAME_INTEGER = "Integer";
	static final String OBJECTIVE_TYPE_NAME_LONG = "Long";
	static final String OBJECTIVE_TYPE_NAME_FLOAT = "Float";
	static final String OBJECTIVE_TYPE_NAME_DOUBLE = "Double";
	static final String OBJECTIVE_TYPE_NAME_VOID = "Void";

	static final Map<String, Class<?>> TYPE_MAP = new HashMap<>(18);
	static{
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_BOOLEAN, boolean.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_BOOLEAN, Boolean.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_BYTE, byte.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_BYTE, Byte.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_CHAR, char.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_CHARACTER, Character.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_SHORT, short.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_SHORT, Short.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_INT, int.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_INTEGER, Integer.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_LONG, long.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_LONG, Long.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_FLOAT, float.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_FLOAT, Float.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_DOUBLE, double.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_DOUBLE, Double.class);
		TYPE_MAP.put(PRIMITIVE_TYPE_NAME_VOID, void.class);
		TYPE_MAP.put(OBJECTIVE_TYPE_NAME_VOID, Void.class);
	}


	private DataTypeHelper(){}


	/**
	 * Return the number of bits used to represent a value of the given type.
	 *
	 * @param objectiveType	The type of the number.
	 * @return	The number of bits.
	 */
	public static int getSize(final Class<?> objectiveType){
		return switch(objectiveType.getSimpleName()){
			case PRIMITIVE_TYPE_NAME_BYTE, OBJECTIVE_TYPE_NAME_BYTE -> Byte.SIZE;
			case PRIMITIVE_TYPE_NAME_SHORT, OBJECTIVE_TYPE_NAME_SHORT -> Short.SIZE;
			case PRIMITIVE_TYPE_NAME_INT, OBJECTIVE_TYPE_NAME_INTEGER -> Integer.SIZE;
			case PRIMITIVE_TYPE_NAME_LONG, OBJECTIVE_TYPE_NAME_LONG -> Long.SIZE;
			case PRIMITIVE_TYPE_NAME_FLOAT, OBJECTIVE_TYPE_NAME_FLOAT -> Float.SIZE;
			case PRIMITIVE_TYPE_NAME_DOUBLE, OBJECTIVE_TYPE_NAME_DOUBLE -> Double.SIZE;
			default -> -1;
		};
	}

}
