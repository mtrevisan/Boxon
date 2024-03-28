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
package io.github.mtrevisan.boxon.io;

import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.MethodHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Holds information about size in memory and primitive-objective types of each data type.
 */
public enum ParserDataType{

	BYTE(Byte.TYPE, Byte.class, Byte.SIZE){
		@Override
		Object value(final String value){
			return Byte.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.byteValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getByte();
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putByte((Byte)value);
		}
	},

	SHORT(Short.TYPE, Short.class, Short.SIZE){
		@Override
		Object value(final String value){
			return Short.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.shortValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getShort(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putShort((Short)value, byteOrder);
		}
	},

	INTEGER(Integer.TYPE, Integer.class, Integer.SIZE){
		@Override
		Object value(final String value){
			return Integer.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.intValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getInt(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putInt((Integer)value, byteOrder);
		}
	},

	LONG(Long.TYPE, Long.class, Long.SIZE){
		@Override
		Object value(final String value){
			return Long.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.longValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getLong(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putLong((Long)value, byteOrder);
		}
	},

	FLOAT(Float.TYPE, Float.class, Float.SIZE){
		@Override
		Object value(final String value){
			return Float.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.floatValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getFloat(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putFloat((Float)value, byteOrder);
		}
	},

	DOUBLE(Double.TYPE, Double.class, Double.SIZE){
		@Override
		Object value(final String value){
			return Double.valueOf(value);
		}

		@Override
		Object cast(final BigInteger value){
			return value.doubleValue();
		}

		@Override
		Object read(final BitReaderInterface reader, final ByteOrder byteOrder){
			return reader.getDouble(byteOrder);
		}

		@Override
		void write(final BitWriterInterface writer, final Object value, final ByteOrder byteOrder){
			writer.putDouble((Double)value, byteOrder);
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
	 * The bit mask for the given type.
	 *
	 * @return	The bit mask for the given type.
	 */
	public int getMask(){
		return (size == 0? 0: (1 << size) - 1);
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


	abstract Object cast(BigInteger value);

	/**
	 * Read a specific data type from the reader, using the given byte order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param byteOrder	The byte order.
	 * @return	The read value.
	 */
	abstract Object read(BitReaderInterface reader, ByteOrder byteOrder);

	/**
	 * Write a specific data to the writer, using the given byte order.
	 * @param writer	The writer used to write the data to.
	 * @param value	The value to be written.
	 * @param byteOrder	The byte order.
	 */
	abstract void write(BitWriterInterface writer, Object value, ByteOrder byteOrder);


	/**
	 * Returns the primitive or objective type (depending on the field type) data stored as a string value, if the type is not string,
	 * in that case the value will be returned.
	 *
	 * @param fieldType	The type of the field that will hold the value represented as a string.
	 * @param value	The string value to be interpreted.
	 * @return	The primitive or objective value, if the field type is not string, the given value otherwise.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	public static Object getValueOrSelf(final Class<?> fieldType, final Object value) throws CodecException{
		return (value instanceof final String v
			? getValue(fieldType, v)
			: value);
	}

	/**
	 * Returns the primitive or objective type (depending on the field type) data stored as a string value.
	 *
	 * @param fieldType	The type of the field that will hold the value represented as a string.
	 * @param value	The string value to be interpreted.
	 * @return	The primitive or objective value.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	public static Object getValue(final Class<?> fieldType, final String value) throws CodecException{
		if(fieldType == String.class)
			return value;
		if(StringHelper.isBlank(value))
			return null;

		final Class<?> objectiveType = toObjectiveTypeOrSelf(fieldType);
		//try convert to a number...
		final Object val = toNumber(value, objectiveType);
		//... otherwise convert it to an object
		return (val == null
			? toObjectValue(value, objectiveType)
			: val);
	}

	private static Object toNumber(final String text, final Class<?> objectiveType){
		Object result = null;
		final BigInteger value = JavaHelper.toBigInteger(text);
		if(value != null){
			final ParserDataType objectiveDataType = fromType(objectiveType);
			if(objectiveDataType != null && value.bitCount() <= objectiveDataType.size)
				//convert value to `objectiveType` class
				result = objectiveDataType.cast(value);
		}
		return result;
	}


	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		Object result;
		if(BigDecimal.class.isAssignableFrom(objectiveType))
			result = new BigDecimal(value);
		else if(BigInteger.class.isAssignableFrom(objectiveType))
			result = JavaHelper.toBigInteger(value);
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

}
