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


/**
 * Holds information about size in memory and primitive-objective types of each data type.
 */
public final class DataType{

	private static final String METHOD_VALUE_OF = "valueOf";
	private static final String CLASS_DESCRIPTOR = Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(),
		int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});


	private DataType(){}


	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param typeName	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toTypeOrSelf(final String typeName) throws ClassNotFoundException{
		Class<?> type = switch(typeName){
			case "boolean" -> boolean.class;
			case "Boolean" -> Boolean.class;
			case "byte" -> byte.class;
			case "Byte" -> Byte.class;
			case "char" -> char.class;
			case "Character" -> Character.class;
			case "short" -> short.class;
			case "Short" -> Short.class;
			case "int" -> int.class;
			case "Integer" -> Integer.class;
			case "long" -> long.class;
			case "Long" -> Long.class;
			case "float" -> float.class;
			case "Float" -> Float.class;
			case "double" -> double.class;
			case "Double" -> Double.class;
			case "void" -> void.class;
			case "Void" -> Void.class;
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
			case "boolean" -> Boolean.class;
			case "byte" -> Byte.class;
			case "char" -> Character.class;
			case "short" -> Short.class;
			case "int" -> Integer.class;
			case "long" -> Long.class;
			case "float" -> Float.class;
			case "double" -> Double.class;
			case "void" -> Void.class;
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
			case "Boolean" -> boolean.class;
			case "Byte" -> byte.class;
			case "Character" -> char.class;
			case "Short" -> short.class;
			case "Integer" -> int.class;
			case "Long" -> long.class;
			case "Float" -> float.class;
			case "Double" -> double.class;
			case "Void" -> void.class;
			default -> objectiveType;
		};
	}

	//the number of bits used to represent the value
	public static int toPrimitiveSize(final Class<?> objectiveType){
		return switch(objectiveType.getSimpleName()){
			case "byte", "Byte" -> Byte.SIZE;
			case "short", "Short" -> Short.SIZE;
			case "int", "Integer" -> Integer.SIZE;
			case "long", "Long" -> Long.SIZE;
			case "float", "Float" -> Float.SIZE;
			case "double", "Double" -> Double.SIZE;
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
			case "byte", "Byte" -> Byte.valueOf(value);
			case "short", "Short" -> Short.valueOf(value);
			case "int", "Integer" -> Integer.valueOf(value);
			case "long", "Long" -> Long.valueOf(value);
			case "float", "Float" -> Float.valueOf(value);
			case "double", "Double" -> Double.valueOf(value);
			default -> null;
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
		if(targetType == null)
			return value;

		return switch(targetType.getSimpleName()){
			case "byte", "Byte" -> value.byteValue();
			case "short", "Short" -> value.shortValue();
			case "int", "Integer" -> value.intValue();
			case "long", "Long" -> value.longValue();
			case "float", "Float" -> value.floatValue();
			case "double", "Double" -> value.doubleValue();
			default -> value;
		};
	}

	public static Object cast(final BigInteger[] array, final Class<?> targetType){
		final int length = Array.getLength(array);
		final Object convertedArray = Array.newInstance(targetType, length);
		for(int i = 0; i < length; i ++){
			Object element = Array.get(array, i);
			element = cast((BigInteger)element, targetType);
			Array.set(convertedArray, i, element);
		}
		return convertedArray;
	}

	/**
	 * Read a specific data type from the reader, using the given byte order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @param targetType	The target data type to cast the value to.
	 * @return	The read value.
	 */
	static Object read(final BitReaderInterface reader, final ByteOrder byteOrder, final Class<?> targetType) throws AnnotationException{
		return switch(targetType.getSimpleName()){
			case "byte", "Byte" -> reader.readByte();
			case "short", "Short" -> reader.readShort(byteOrder);
			case "int", "Integer" -> reader.readInt(byteOrder);
			case "long", "Long" -> reader.readLong(byteOrder);
			case "float", "Float" -> Float.intBitsToFloat(reader.readInt(byteOrder));
			case "double", "Double" -> Double.longBitsToDouble(reader.readLong(byteOrder));
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
			case "byte", "Byte" -> writer.writeByte((Byte)value);
			case "short", "Short" -> writer.writeShort((Short)value, byteOrder);
			case "int", "Integer" -> writer.writeInt((Integer)value, byteOrder);
			case "long", "Long" -> writer.writeLong((Long)value, byteOrder);
			case "float", "Float" -> writer.writeInt(Float.floatToIntBits((Float)value), byteOrder);
			case "double", "Double" -> writer.writeLong(Double.doubleToLongBits((Double)value), byteOrder);
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
			final int size = toPrimitiveSize(objectiveType);
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
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(converterType, Converter.class)
				.getFirst();
		if(inputType == null && validatorType != NullValidator.class)
			inputType = (Class<?>)GenericHelper.resolveGenericTypes(validatorType, Validator.class)
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
