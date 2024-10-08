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


/**
 * Takes care of the conversion between primitive types and their wrappers, and vice versa.
 */
public final class DataTypeMapper{

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
		Class<?> type = DataTypeHelper.TYPE_MAP.get(typeName);
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

		return switch(primitiveType.getSimpleName()){
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_BOOLEAN -> Boolean.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_BYTE -> Byte.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_CHAR -> Character.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_SHORT -> Short.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_INT -> Integer.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_LONG -> Long.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_FLOAT -> Float.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_DOUBLE -> Double.class;
			case DataTypeHelper.PRIMITIVE_TYPE_NAME_VOID -> Void.class;
			default -> primitiveType;
		};
	}

	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param objectiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toPrimitiveTypeOrSelf(final Class<?> objectiveType){
		if(objectiveType == null)
			return null;
		if(isPrimitive(objectiveType))
			return objectiveType;

		return switch(objectiveType.getSimpleName()){
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_BOOLEAN -> boolean.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_BYTE -> byte.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_CHARACTER -> char.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_SHORT -> short.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_INTEGER -> int.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_LONG -> long.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_FLOAT -> float.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_DOUBLE -> double.class;
			case DataTypeHelper.OBJECTIVE_TYPE_NAME_VOID -> void.class;
			default -> objectiveType;
		};
	}

}
