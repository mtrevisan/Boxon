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
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Holds information about size in memory and primitive-objective types of each data type.
 */
//FIXME refactor
public enum ParserDataType{

	/** Represents the byte data type. */
	BYTE(Byte.class),

	/** Represents the short data type. */
	SHORT(Short.class),

	/** Represents the int/integer data type. */
	INTEGER(Integer.class),

	/** Represents the long data type. */
	LONG(Long.class),

	/** Represents the float data type. */
	FLOAT(Float.class),

	/** Represents the double data type. */
	DOUBLE(Double.class);


	@FunctionalInterface
	private interface TriConsumer<S, T, U>{
		void accept(S s, T t, U u);
	}

	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class}. */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = Map.of(
		Byte.class, Byte.TYPE,
		Short.class, Short.TYPE,
		Integer.class, Integer.TYPE,
		Long.class, Long.TYPE,
		Float.class, Float.TYPE,
		Double.class, Double.TYPE
	);
	/** Maps wrapper {@code Class}es to their corresponding primitive types. */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = JavaHelper.reverseMap(PRIMITIVE_WRAPPER_MAP);
	private static final Map<Class<?>, ParserDataType> TYPE_MAP;
	static{
		final ParserDataType[] values = values();
		final int length = values.length;
		TYPE_MAP = new HashMap<>(length << 1);
		for(int i = 0; i < length; i ++){
			final ParserDataType dt = values[i];

			final Class<?> primitiveType = PRIMITIVE_WRAPPER_MAP.get(dt.objectiveType);
			TYPE_MAP.put(primitiveType, dt);
			TYPE_MAP.put(dt.objectiveType, dt);
		}
	}
	//the number of bits used to represent the value
	private static final Map<Class<?>, Integer> PRIMITIVE_SIZE_MAP = Map.of(
		Byte.class, Byte.SIZE,
		Short.class, Short.SIZE,
		Integer.class, Integer.SIZE,
		Long.class, Long.SIZE,
		Float.class, Float.SIZE,
		Double.class, Double.SIZE
	);
	private static final Map<Class<?>, Function<String, Object>> VALUE_STRATEGIES = Map.of(
		Byte.class, Byte::valueOf,
		Short.class, Short::valueOf,
		Integer.class, Integer::valueOf,
		Long.class, Long::valueOf,
		Float.class, Float::valueOf,
		Double.class, Double::valueOf
	);
	private static final Map<Class<?>, Function<BigInteger, Number>> CAST_STRATEGIES = Map.of(
		Byte.class, BigInteger::byteValue,
		Short.class, BigInteger::shortValue,
		Integer.class, BigInteger::intValue,
		Long.class, BigInteger::longValue,
		Float.class, BigInteger::floatValue,
		Double.class, BigInteger::doubleValue
	);
	private static final Map<Class<?>, BiFunction<BitReaderInterface, ByteOrder, Object>> READER_STRATEGIES = Map.of(
		Byte.class, (reader, byteOrder) -> reader.readByte(),
		Short.class, (reader, byteOrder) -> reader.readShort(byteOrder),
		Integer.class, (reader, byteOrder) -> reader.readInt(byteOrder),
		Long.class, (reader, byteOrder) -> reader.readLong(byteOrder),
		Float.class, (reader, byteOrder) -> Float.intBitsToFloat(reader.readInt(byteOrder)),
		Double.class, (reader, byteOrder) -> Double.longBitsToDouble(reader.readLong(byteOrder))
	);
	private static final Map<Class<?>, TriConsumer<BitWriterInterface, Object, ByteOrder>> WRITER_STRATEGIES = Map.of(
		Byte.class, (writer, value, byteOrder) -> writer.writeByte((Byte)value),
		Short.class, (writer, value, byteOrder) -> writer.writeShort((Short)value, byteOrder),
		Integer.class, (writer, value, byteOrder) -> writer.writeInt((Integer)value, byteOrder),
		Long.class, (writer, value, byteOrder) -> writer.writeLong((Long)value, byteOrder),
		Float.class, (writer, rawValue, byteOrder) -> writer.writeInt(Float.floatToIntBits((Float)rawValue), byteOrder),
		Double.class, (writer, rawValue, byteOrder) -> writer.writeLong(Double.doubleToLongBits((Double)rawValue), byteOrder)
	);

	private static final String METHOD_VALUE_OF = "valueOf";
	private static final String CLASS_DESCRIPTOR = Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(),
		int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});


	private final Class<?> objectiveType;


	/**
	 * Extract the enumeration corresponding to the given type.
	 *
	 * @param type	The type to be converted.
	 * @return	The enumeration corresponding to the given type.
	 */
	public static ParserDataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}


	ParserDataType(final Class<?> objectiveType){
		this.objectiveType = objectiveType;
	}


	/**
	 * Convert a type to an objective type, if applicable, otherwise returns the type itself.
	 *
	 * @param primitiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toObjectiveTypeOrSelf(final Class<?> primitiveType){
		return WRAPPER_PRIMITIVE_MAP.getOrDefault(primitiveType, primitiveType);
	}

	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param objectiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toPrimitiveTypeOrSelf(final Class<?> objectiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(objectiveType, objectiveType);
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
	 * <p>NOTE: {@code Character} and {@code void}/{@code Void} are NOT considered as primitives!</p>
	 *
	 * @param type	The class to query.
	 * @return	Whether the given {@code type} is a primitive or primitive wrapper.
	 */
	public static boolean isPrimitiveOrWrapper(final Class<?> type){
		return (isPrimitive(type) || isPrimitiveWrapper(type));
	}

	private static boolean isPrimitiveWrapper(final Class<?> type){
		return PRIMITIVE_WRAPPER_MAP.containsKey(type);
	}

	/**
	 * Describe the data types handled by this class.
	 *
	 * @return	A list of data types.
	 */
	static String describe(){
		return CLASS_DESCRIPTOR;
	}


	/**
	 * Retrieves the computed value based on the objective type.
	 *
	 * @param value	The input value to be processed.
	 * @return	The computed value based on the objective type.
	 */
	private Object value(final String value){
		return VALUE_STRATEGIES.get(objectiveType)
			.apply(value);
	}

	private static int size(final Class<?> objectiveType){
		return PRIMITIVE_SIZE_MAP.get(objectiveType);
	}

	/**
	 * Casts the given `BigInteger` value to a `Number` object that can be a `byte`, a `short`, an `integer`, a `float`, or a `double`.
	 *
	 * @param value	The `BigInteger` value to be cast.
	 * @return	The cast `Number` object.
	 */
	private Number cast(final BigInteger value){
		return CAST_STRATEGIES.get(objectiveType)
			.apply(value);
	}

	/**
	 * Read a specific data type from the reader, using the given byte order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The read value.
	 */
	Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
		return READER_STRATEGIES.get(objectiveType)
			.apply(reader, byteOrder);
	}

	/**
	 * Write a specific data to the writer, using the given byte order.
	 * @param writer	The writer used to write the data to.
	 * @param value	The value to be written.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
		WRITER_STRATEGIES.get(objectiveType)
			.accept(writer, value, byteOrder);
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
			final ParserDataType dataType = fromType(objectiveType);
			if(dataType != null && value.bitCount() <= size(dataType.objectiveType))
				//convert value to `objectiveType` class
				result = dataType.cast(value);
		}
		return result;
	}


	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		final Object result;
		if(BigDecimal.class.isAssignableFrom(objectiveType))
			result = new BigDecimal(value);
		else if(BigInteger.class.isAssignableFrom(objectiveType))
			result = JavaHelper.convertToBigInteger(value);
		else{
			final ParserDataType dataType = fromType(objectiveType);
			if(dataType != null)
				result = dataType.value(value);
			else{
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
	 * Casts the given {@code value} to the specified {@code inputType}.
	 *
	 * @param value	The value to be cast.
	 * @param inputType	The target data type to cast the value to.
	 * @return	The cast value if successful, otherwise the original value.
	 */
	public static Number castValue(final BigInteger value, final Class<?> inputType){
		if(inputType != null){
			final ParserDataType pdt = fromType(inputType);
			if(pdt != null)
				return pdt.cast(value);
		}
		return value;
	}

	public static Object castValue(final BigInteger[] array, final Class<?> inputType){
		final int length = Array.getLength(array);
		final Object convertedArray = Array.newInstance(inputType, length);
		for(int i = 0; i < length; i ++){
			Object element = Array.get(array, i);
			element = castValue((BigInteger)element, inputType);
			Array.set(convertedArray, i, element);
		}
		return convertedArray;
	}

	/**
	 * Reinterprets a `Number` as a `BigInteger`.
	 *
	 * @param value	The `Number` to reinterpret as a `BigInteger`.
	 * @return	A `BigInteger` representing the same numerical value as the given `Number`.
	 * @see #castValue(BigInteger, Class)
	 */
	public static BigInteger reinterpretToBigInteger(final Number value){
		return (value instanceof final BigInteger bi
			? bi
			: BigInteger.valueOf(value.longValue())
		);
	}

}
