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

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;


/**
 * Holds information about size in memory and primitive-objective types of each data type.
 */
public final class DataType{

	private static final String METHOD_VALUE_OF = "valueOf";
	private static final String CLASS_DESCRIPTOR = Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(),
		int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});

	private static final String PRIMITIVE_TYPE_NAME_BOOLEAN = "boolean";
	private static final String PRIMITIVE_TYPE_NAME_BYTE = "byte";
	private static final String PRIMITIVE_TYPE_NAME_CHAR = "char";
	private static final String PRIMITIVE_TYPE_NAME_SHORT = "short";
	private static final String PRIMITIVE_TYPE_NAME_INT = "int";
	private static final String PRIMITIVE_TYPE_NAME_LONG = "long";
	private static final String PRIMITIVE_TYPE_NAME_FLOAT = "float";
	private static final String PRIMITIVE_TYPE_NAME_DOUBLE = "double";
	private static final String PRIMITIVE_TYPE_NAME_VOID = "void";
	private static final String OBJECTIVE_TYPE_NAME_BOOLEAN = "Boolean";
	private static final String OBJECTIVE_TYPE_NAME_BYTE = "Byte";
	private static final String OBJECTIVE_TYPE_NAME_CHARACTER = "Character";
	private static final String OBJECTIVE_TYPE_NAME_SHORT = "Short";
	private static final String OBJECTIVE_TYPE_NAME_INTEGER = "Integer";
	private static final String OBJECTIVE_TYPE_NAME_LONG = "Long";
	private static final String OBJECTIVE_TYPE_NAME_FLOAT = "Float";
	private static final String OBJECTIVE_TYPE_NAME_DOUBLE = "Double";
	private static final String OBJECTIVE_TYPE_NAME_VOID = "Void";


	private DataType(){}


	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param typeName	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toTypeOrSelf(final String typeName) throws ClassNotFoundException{
		Class<?> type = switch(typeName){
			case PRIMITIVE_TYPE_NAME_BOOLEAN -> boolean.class;
			case OBJECTIVE_TYPE_NAME_BOOLEAN -> Boolean.class;
			case PRIMITIVE_TYPE_NAME_BYTE -> byte.class;
			case OBJECTIVE_TYPE_NAME_BYTE -> Byte.class;
			case PRIMITIVE_TYPE_NAME_CHAR -> char.class;
			case OBJECTIVE_TYPE_NAME_CHARACTER -> Character.class;
			case PRIMITIVE_TYPE_NAME_SHORT -> short.class;
			case OBJECTIVE_TYPE_NAME_SHORT -> Short.class;
			case PRIMITIVE_TYPE_NAME_INT -> int.class;
			case OBJECTIVE_TYPE_NAME_INTEGER -> Integer.class;
			case PRIMITIVE_TYPE_NAME_LONG -> long.class;
			case OBJECTIVE_TYPE_NAME_LONG -> Long.class;
			case PRIMITIVE_TYPE_NAME_FLOAT -> float.class;
			case OBJECTIVE_TYPE_NAME_FLOAT -> Float.class;
			case PRIMITIVE_TYPE_NAME_DOUBLE -> double.class;
			case OBJECTIVE_TYPE_NAME_DOUBLE -> Double.class;
			case PRIMITIVE_TYPE_NAME_VOID -> void.class;
			case OBJECTIVE_TYPE_NAME_VOID -> Void.class;
			default -> null;
		};

		if(type == null){
			try{
				type = Class.forName(typeName);
			}
			catch(final Exception ignored){
				throw new ClassNotFoundException("Cannot find class for `" + type + "`");
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

		final String primitiveTypeName = primitiveType.getSimpleName();
		final Class<?> objectiveType = toObjectiveTypeOrNull(primitiveTypeName);
		return (objectiveType != null? objectiveType: primitiveType);
	}

	private static Class<?> toObjectiveTypeOrNull(final String primitiveTypeName){
		return switch(primitiveTypeName){
			case PRIMITIVE_TYPE_NAME_BOOLEAN -> Boolean.class;
			case PRIMITIVE_TYPE_NAME_BYTE -> Byte.class;
			case PRIMITIVE_TYPE_NAME_CHAR -> Character.class;
			case PRIMITIVE_TYPE_NAME_SHORT -> Short.class;
			case PRIMITIVE_TYPE_NAME_INT -> Integer.class;
			case PRIMITIVE_TYPE_NAME_LONG -> Long.class;
			case PRIMITIVE_TYPE_NAME_FLOAT -> Float.class;
			case PRIMITIVE_TYPE_NAME_DOUBLE -> Double.class;
			case PRIMITIVE_TYPE_NAME_VOID -> Void.class;
			default -> 	null;
		};
	}

	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param objectiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toPrimitiveTypeOrSelf(final Class<?> objectiveType){
		return switch(objectiveType.getSimpleName()){
			case OBJECTIVE_TYPE_NAME_BOOLEAN -> boolean.class;
			case OBJECTIVE_TYPE_NAME_BYTE -> byte.class;
			case OBJECTIVE_TYPE_NAME_CHARACTER -> char.class;
			case OBJECTIVE_TYPE_NAME_SHORT -> short.class;
			case OBJECTIVE_TYPE_NAME_INTEGER -> int.class;
			case OBJECTIVE_TYPE_NAME_LONG -> long.class;
			case OBJECTIVE_TYPE_NAME_FLOAT -> float.class;
			case OBJECTIVE_TYPE_NAME_DOUBLE -> double.class;
			case OBJECTIVE_TYPE_NAME_VOID -> void.class;
			default -> objectiveType;
		};
	}

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
	 * Retrieves the computed value based on the objective type.
	 *
	 * @param value	The input value to be processed.
	 * @param type	The class type from which to convert.
	 * @return	The computed value based on the objective type.
	 */
	private static Number value(final String value, final Class<?> type){
		return switch(type.getSimpleName()){
			case PRIMITIVE_TYPE_NAME_BYTE, OBJECTIVE_TYPE_NAME_BYTE -> Byte.valueOf(value);
			case PRIMITIVE_TYPE_NAME_SHORT, OBJECTIVE_TYPE_NAME_SHORT -> Short.valueOf(value);
			case PRIMITIVE_TYPE_NAME_INT, OBJECTIVE_TYPE_NAME_INTEGER -> Integer.valueOf(value);
			case PRIMITIVE_TYPE_NAME_LONG, OBJECTIVE_TYPE_NAME_LONG -> Long.valueOf(value);
			case PRIMITIVE_TYPE_NAME_FLOAT, OBJECTIVE_TYPE_NAME_FLOAT -> Float.valueOf(value);
			case PRIMITIVE_TYPE_NAME_DOUBLE, OBJECTIVE_TYPE_NAME_DOUBLE -> Double.valueOf(value);
			default -> null;
		};
	}

	/**
	 * Returns the method that perform the cast to the specified {@code inputType}.
	 *
	 * @param targetType The target data type to cast the value to.
	 * @return The cast value if successful, otherwise the original value.
	 */
	public static Function<BigInteger, Number> castFunction(final Class<?> targetType){
		if(targetType == null)
			return (value -> value);

		return switch(targetType.getSimpleName()){
			case PRIMITIVE_TYPE_NAME_BYTE, OBJECTIVE_TYPE_NAME_BYTE -> (value -> value.byteValue());
			case PRIMITIVE_TYPE_NAME_SHORT, OBJECTIVE_TYPE_NAME_SHORT -> (value -> value.shortValue());
			case PRIMITIVE_TYPE_NAME_INT, OBJECTIVE_TYPE_NAME_INTEGER -> (value -> value.intValue());
			case PRIMITIVE_TYPE_NAME_LONG, OBJECTIVE_TYPE_NAME_LONG -> (value -> value.longValue());
			case PRIMITIVE_TYPE_NAME_FLOAT, OBJECTIVE_TYPE_NAME_FLOAT -> (value -> value.floatValue());
			case PRIMITIVE_TYPE_NAME_DOUBLE, OBJECTIVE_TYPE_NAME_DOUBLE -> (value -> value.doubleValue());
			default -> (value -> value);
		};
	}

	/**
	 * Casts the given {@code value} to the specified {@code inputType}.
	 *
	 * @param value	The value to be cast.
	 * @param targetType	The target data type to cast the value to.
	 * @return	The cast value if successful, otherwise the original value.
	 */
	public static Number cast(final BigInteger value, final Class<?> targetType){
		return castFunction(targetType).apply(value);
	}

	public static Object cast(final BigInteger[] array, final Class<?> targetType){
		final int length = Array.getLength(array);
		final Class<?> type = allElementsSameClassType(array, length);
		final Function<BigInteger, Number> fun = (type != null? castFunction(targetType): null);
		final Object convertedArray = Array.newInstance(targetType, length);
		for(int i = 0; i < length; i ++){
			Object element = Array.get(array, i);
			if(element == null)
				continue;

			element = (fun != null
				? fun.apply((BigInteger)element)
				: cast((BigInteger)element, targetType));

			Array.set(convertedArray, i, element);
		}
		return convertedArray;
	}

	public static Class<?> allElementsSameClassType(final Object array, final int length){
		Class<?> firstClass = null;
		for(int i = 0; i < length; i ++){
			final Object element = Array.get(array, i);
			if(element != null){
				firstClass = element.getClass();
				break;
			}
		}

		if(firstClass == null)
			return null;

		for(int i = 0; i < length; i ++){
			final Object element = Array.get(array, i);
			if(element != null && !element.getClass().equals(firstClass))
				return null;
		}
		return firstClass;
	}

	/**
	 * Read a specific data type from the reader, using the given byte order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @param targetType	The target data type to cast the value to.
	 * @return	The read value.
	 */
	static Number read(final BitReaderInterface reader, final ByteOrder byteOrder, final Class<?> targetType) throws AnnotationException{
		return switch(targetType.getSimpleName()){
			case PRIMITIVE_TYPE_NAME_BYTE, OBJECTIVE_TYPE_NAME_BYTE -> reader.readByte();
			case PRIMITIVE_TYPE_NAME_SHORT, OBJECTIVE_TYPE_NAME_SHORT -> reader.readShort(byteOrder);
			case PRIMITIVE_TYPE_NAME_INT, OBJECTIVE_TYPE_NAME_INTEGER -> reader.readInt(byteOrder);
			case PRIMITIVE_TYPE_NAME_LONG, OBJECTIVE_TYPE_NAME_LONG -> reader.readLong(byteOrder);
			case PRIMITIVE_TYPE_NAME_FLOAT, OBJECTIVE_TYPE_NAME_FLOAT -> Float.intBitsToFloat(reader.readInt(byteOrder));
			case PRIMITIVE_TYPE_NAME_DOUBLE, OBJECTIVE_TYPE_NAME_DOUBLE -> Double.longBitsToDouble(reader.readLong(byteOrder));
			default -> throw AnnotationException.create("Cannot read type {}, should be one of {}, or their objective counterparts",
				targetType.getSimpleName(), CLASS_DESCRIPTOR);
		};
	}

	/**
	 * Write a specific data to the writer, using the given byte order.
	 * @param writer	The writer used to write the data to.
	 * @param value	The value to be written.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	static void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder) throws AnnotationException{
		switch(value.getClass().getSimpleName()){
			case PRIMITIVE_TYPE_NAME_BYTE, OBJECTIVE_TYPE_NAME_BYTE -> writer.writeByte((Byte)value);
			case PRIMITIVE_TYPE_NAME_SHORT, OBJECTIVE_TYPE_NAME_SHORT -> writer.writeShort((Short)value, byteOrder);
			case PRIMITIVE_TYPE_NAME_INT, OBJECTIVE_TYPE_NAME_INTEGER -> writer.writeInt((Integer)value, byteOrder);
			case PRIMITIVE_TYPE_NAME_LONG, OBJECTIVE_TYPE_NAME_LONG -> writer.writeLong((Long)value, byteOrder);
			case PRIMITIVE_TYPE_NAME_FLOAT, OBJECTIVE_TYPE_NAME_FLOAT -> writer.writeInt(Float.floatToIntBits((Float)value), byteOrder);
			case PRIMITIVE_TYPE_NAME_DOUBLE, OBJECTIVE_TYPE_NAME_DOUBLE -> writer.writeLong(Double.doubleToLongBits((Double)value), byteOrder);
			default -> throw AnnotationException.create("Cannot write type {}", value.getClass().getSimpleName());
		};
	}


	/**
	 * Returns the primitive or objective type data stored as a string value, or the value itself if not a string or the field type is a
	 * string.
	 *
	 * @param fieldType	The type of the field that will hold the value represented as a string.
	 * @param value	The string value to be interpreted.
	 * @return	The primitive or objective value, or the value passed if not a string or the field type is a string.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	public static Object getValueOrSelf(final Class<?> fieldType, final Object value) throws CodecException{
		if(fieldType == String.class || !(value instanceof final String valueAsString))
			return value;
		if(StringHelper.isBlank(valueAsString))
			return null;

		final Class<?> objectiveType = toObjectiveTypeOrSelf(fieldType);
		return convertStringValue(valueAsString, objectiveType);
	}

	private static Object convertStringValue(final String value, final Class<?> objectiveType) throws CodecException{
		//try convert to a number...
		final Object valueAsNumber = toNumber(value, objectiveType);
		//... otherwise convert it to an object
		return (valueAsNumber == null
			? toObjectValue(value, objectiveType)
			: valueAsNumber
		);
	}

	private static Object toNumber(final String text, final Class<?> objectiveType){
		Object result = null;
		final BigInteger value = JavaHelper.convertToBigInteger(text);
		if(value != null){
			final int size = getSize(objectiveType);
			if(size > 0 && value.bitCount() <= size)
				//convert value to `objectiveType` class
				result = cast(value, objectiveType);
		}
		return result;
	}

	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		Object result;
		if(BigDecimal.class.isAssignableFrom(objectiveType))
			result = new BigDecimal(value);
		else if(BigInteger.class.isAssignableFrom(objectiveType))
			result = JavaHelper.convertToBigInteger(value);
		else{
			result = value(value, objectiveType);
			if(result == null){
				//try with `.valueOf()`
				try{
					result = MethodHelper.invokeStaticMethod(objectiveType, METHOD_VALUE_OF, value);
				}
				catch(final Exception ignored){
					throw CodecException.create("Cannot interpret {} as {}", value, objectiveType.getSimpleName());
				}
			}
		}
		return result;
	}


	/**
	 * Resolves the input type for a sequence of a converter, if given, and/or a following validator.
	 *
	 * @param converterType	The type of the converter.
	 * @param validatorType	The type of the validator.
	 * @return	The resolved input type.
	 */
	public static Class<?> resolveInputType(final Class<? extends Converter<?, ?>> converterType,
			final Class<? extends Validator<?>> validatorType){
		Class<?> inputType = null;
		if(converterType != NullConverter.class)
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(converterType)
				.getFirst();
		if(inputType == null && validatorType != NullValidator.class)
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(validatorType)
				.getFirst();
		return inputType;
	}

	/**
	 * Reinterprets a `Number` as a `BigInteger`.
	 *
	 * @param value	The `Number` to reinterpret as a `BigInteger`.
	 * @return	A `BigInteger` representing the same numerical value as the given `Number`.
	 * @see #cast(BigInteger, Class)
	 */
	public static BigInteger reinterpretToBigInteger(final Number value){
		return (value instanceof final BigInteger bi
			? bi
			: BigInteger.valueOf(value.longValue())
		);
	}

}
