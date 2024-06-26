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


/**
 * Holds information about size in memory and primitive-objective types of each data type.
 */
public enum ParserDataType{

	/** Represents the byte data type. */
	BYTE(Byte.TYPE, Byte.class, Byte.SIZE){
		@Override
		Object value(final String value){
			return Byte.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.byteValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.readByte();
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.writeByte((Byte)value);
		}
	},

	/** Represents the short data type. */
	SHORT(Short.TYPE, Short.class, Short.SIZE){
		@Override
		Object value(final String value){
			return Short.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.shortValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.readShort(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.writeShort((Short)value, byteOrder);
		}
	},

	/** Represents the int/integer data type. */
	INTEGER(Integer.TYPE, Integer.class, Integer.SIZE){
		@Override
		Object value(final String value){
			return Integer.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.intValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.readInt(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.writeInt((Integer)value, byteOrder);
		}
	},

	/** Represents the long data type. */
	LONG(Long.TYPE, Long.class, Long.SIZE){
		@Override
		Object value(final String value){
			return Long.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.longValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.readLong(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.writeLong((Long)value, byteOrder);
		}
	},

	/** Represents the float data type. */
	FLOAT(Float.TYPE, Float.class, Float.SIZE){
		@Override
		Object value(final String value){
			return Float.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.floatValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			final int rawValue = reader.readInt(byteOrder);
			return Float.intBitsToFloat(rawValue);
		}

		@Override
		void write(final BitWriterInterface writer, final Object rawValue, final ByteOrder byteOrder){
			final int value = Float.floatToIntBits((Float)rawValue);
			writer.writeInt(value, byteOrder);
		}
	},

	/** Represents the double data type. */
	DOUBLE(Double.TYPE, Double.class, Double.SIZE){
		@Override
		Object value(final String value){
			return Double.valueOf(value);
		}

		@Override
		public Number cast(final BigInteger value){
			return value.doubleValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			final long rawValue = reader.readLong(byteOrder);
			return Double.longBitsToDouble(rawValue);
		}

		@Override
		void write(final BitWriterInterface writer, final Object rawValue, final ByteOrder byteOrder){
			final long value = Double.doubleToLongBits((Double)rawValue);
			writer.writeLong(value, byteOrder);
		}
	};


	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class}. */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP;
	/** Maps wrapper {@code Class}es to their corresponding primitive types. */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP;
	private static final Map<Class<?>, ParserDataType> TYPE_MAP;
	static{
		final ParserDataType[] values = values();
		final int length = values.length;
		PRIMITIVE_WRAPPER_MAP = new HashMap<>(length);
		WRAPPER_PRIMITIVE_MAP = new HashMap<>(length);
		TYPE_MAP = new HashMap<>(length << 1);
		for(int i = 0; i < length; i ++){
			final ParserDataType dt = values[i];

			PRIMITIVE_WRAPPER_MAP.put(dt.primitiveType, dt.objectiveType);
			WRAPPER_PRIMITIVE_MAP.put(dt.objectiveType, dt.primitiveType);
			TYPE_MAP.put(dt.primitiveType, dt);
			TYPE_MAP.put(dt.objectiveType, dt);
		}
	}

	private static final String METHOD_VALUE_OF = "valueOf";
	private static final String CLASS_DESCRIPTOR = Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(),
		int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});


	private final Class<?> primitiveType;
	private final Class<?> objectiveType;
	//the number of bits used to represent the value
	private final int size;


	/**
	 * Extract the enumeration corresponding to the given type.
	 *
	 * @param type	The type to be converted.
	 * @return	The enumeration corresponding to the given type.
	 */
	public static ParserDataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}


	ParserDataType(final Class<?> primitiveType, final Class<?> objectiveType, final int size){
		this.primitiveType = primitiveType;
		this.objectiveType = objectiveType;
		this.size = size;
	}


	/**
	 * Convert a type to an objective type, if applicable, otherwise returns the type itself.
	 *
	 * @param primitiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toObjectiveTypeOrSelf(final Class<?> primitiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(primitiveType, primitiveType);
	}

	/**
	 * Convert a type to a primitive type, if applicable, otherwise returns the type itself.
	 *
	 * @param objectiveType	The type to be converted.
	 * @return	The converted type;
	 */
	public static Class<?> toPrimitiveTypeOrSelf(final Class<?> objectiveType){
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
		return WRAPPER_PRIMITIVE_MAP.containsKey(type);
	}

	/**
	 * The number of bits used to represent the value.
	 *
	 * @param value	The value from which to extract its memory size.
	 * @return	The size of the value as stored in memory.
	 */
	public static int getSize(final Object value){
		return fromType(value.getClass()).size;
	}

	/**
	 * Describe the data types handled by this class.
	 *
	 * @return	A list of data types.
	 */
	static String describe(){
		return CLASS_DESCRIPTOR;
	}


	abstract Object value(String value);


	/**
	 * Casts the given `BigInteger` value to a `Number` object that can be a `byte`, a `short`, an `integer`, a `float`, or a `double`.
	 *
	 * @param value	The `BigInteger` value to be cast.
	 * @return	The cast `Number` object.
	 */
	protected abstract Number cast(BigInteger value);

	/**
	 * Read a specific data type from the reader, using the given byte order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The read value.
	 */
	abstract Object read(BitReaderInterface reader, ByteOrder byteOrder);

	/**
	 * Write a specific data to the writer, using the given byte order.
	 * @param writer	The writer used to write the data to.
	 * @param value	The value to be written.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	abstract void write(BitWriterInterface writer, Object value, ByteOrder byteOrder);


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
			final ParserDataType objectiveDataType = fromType(objectiveType);
			if(objectiveDataType != null && value.bitCount() <= objectiveDataType.size)
				//convert value to `objectiveType` class
				result = objectiveDataType.cast(value);
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
			final ParserDataType objectiveDataType = fromType(objectiveType);
			if(objectiveDataType != null)
				result = objectiveDataType.value(value);
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
