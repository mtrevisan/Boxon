/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external.codecs;

import io.github.mtrevisan.boxon.exceptions.CodecException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public enum ParserDataType{

	BYTE(Byte.TYPE, Byte.class, Byte.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getByte();
		}
	},

	SHORT(Short.TYPE, Short.class, Short.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getShort(byteOrder);
		}
	},

	INTEGER(Integer.TYPE, Integer.class, Integer.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getInt(byteOrder);
		}
	},

	LONG(Long.TYPE, Long.class, Long.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getLong(byteOrder);
		}
	},

	FLOAT(Float.TYPE, Float.class, Float.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getFloat(byteOrder);
		}
	},

	DOUBLE(Double.TYPE, Double.class, Double.SIZE){
		@Override
		public Object read(final BitReader reader, final ByteOrder byteOrder){
			return reader.getDouble(byteOrder);
		}
	};

	/** Maps primitive {@code Class}es to their corresponding wrapper {@code Class}. */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP;
	/** Maps wrapper {@code Class}es to their corresponding primitive types. */
	private static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP;
	private static final Map<Class<?>, ParserDataType> TYPE_MAP;
	static{
		final ParserDataType[] values = values();
		final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap<>(values.length);
		final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<>(values.length);
		final Map<Class<?>, ParserDataType> typeMap = new HashMap<>(values.length * 2);
		for(final ParserDataType dt : values){
			primitiveWrapperMap.put(dt.primitiveType, dt.objectiveType);
			wrapperPrimitiveMap.put(dt.objectiveType, dt.primitiveType);
			typeMap.put(dt.primitiveType, dt);
			typeMap.put(dt.objectiveType, dt);
		}
		PRIMITIVE_WRAPPER_MAP = Collections.unmodifiableMap(primitiveWrapperMap);
		WRAPPER_PRIMITIVE_MAP = Collections.unmodifiableMap(wrapperPrimitiveMap);
		TYPE_MAP = Collections.unmodifiableMap(typeMap);
	}

	private final Class<?> primitiveType;
	private final Class<?> objectiveType;
	private final int size;


	public static ParserDataType fromType(final Class<?> type){
		return TYPE_MAP.get(type);
	}

	ParserDataType(final Class<?> primitiveType, final Class<?> objectiveType, final int size){
		this.primitiveType = primitiveType;
		this.objectiveType = objectiveType;
		this.size = size;
	}

	public static Class<?> toObjectiveTypeOrSelf(final Class<?> primitiveType){
		return PRIMITIVE_WRAPPER_MAP.getOrDefault(primitiveType, primitiveType);
	}

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
	 */
	public static int getSize(final Object value){
		return fromType(value.getClass()).size;
	}

	public static String describe(){
		return Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(), int.class.getSimpleName(),
			long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});
	}

	public abstract Object read(final BitReader reader, final ByteOrder byteOrder);


	public static Object getValueOrDefault(final Class<?> fieldType, final Object value) throws CodecException{
		return (String.class.isInstance(value)
			? getValue(fieldType, (String)value)
			: value);
	}

	@SuppressWarnings("ReturnOfNull")
	public static Object getValue(final Class<?> fieldType, final String value) throws CodecException{
		if(fieldType == String.class)
			return value;
		if(value == null || value.isEmpty())
			return null;

		final Class<?> objectiveType = ParserDataType.toObjectiveTypeOrSelf(fieldType);
		//try convert to a number...
		final Object val = toNumber(value, objectiveType);
		//... otherwise convert it to an object
		return (val == null? toObjectValue(value, objectiveType): val);
	}

	private static final String METHOD_VALUE_OF = "valueOf";

	/**
	 * <p>Checks if the text contains only Unicode digits.
	 * A decimal point is not a Unicode digit and returns false.</p>
	 *
	 * <p>{@code null} will return {@code false}.
	 * An empty text ({@code length() = 0}) will return {@code false}.</p>
	 *
	 * <p>Note that the method does not allow for a leading sign, either positive or negative.
	 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
	 * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
	 * for int or long respectively.</p>
	 *
	 * <pre>
	 * isNumeric(null)   = false
	 * isNumeric("")     = false
	 * isNumeric("  ")   = false
	 * isNumeric("123")  = true
	 * isNumeric("\u0967\u0968\u0969")  = true
	 * isNumeric("12 3") = false
	 * isNumeric("ab2c") = false
	 * isNumeric("12-3") = false
	 * isNumeric("12.3") = false
	 * isNumeric("-123") = false
	 * isNumeric("+123") = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the given text contains only digits and is non-{@code null}.
	 */
	private static Object toNumber(final String text, final Class<?> objectiveType){
		Object response = null;
		if(isNumeric(text)){
			try{
				final Method method = objectiveType.getDeclaredMethod(METHOD_VALUE_OF, String.class, int.class);
				final boolean hexadecimal = text.startsWith("0x");
				response = method.invoke(null, (hexadecimal? text.substring(2): text), (hexadecimal? 16: 10));
			}
			catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored){}
		}
		return response;
	}

	private static boolean isNumeric(final String text){
		return (isHexadecimalNumber(text) || isDecimalNumber(text));
	}

	/**
	 * <p>Checks if the text contains only Unicode digits.
	 * A decimal point is not a Unicode digit and returns false.</p>
	 *
	 * <p>{@code null} will return {@code false}.
	 * An empty text ({@code length() = 0}) will return {@code false}.</p>
	 *
	 * <p>Note that the method does not allow for a leading sign, either positive or negative.
	 * Also, if a String passes the numeric test, it may still generate a NumberFormatException
	 * when parsed by Integer.parseInt or Long.parseLong, e.g. if the value is outside the range
	 * for int or long respectively.</p>
	 *
	 * <pre>
	 * isNumeric(null)   = false
	 * isNumeric("")     = false
	 * isNumeric("  ")   = false
	 * isNumeric("123")  = true
	 * isNumeric("\u0967\u0968\u0969")  = true
	 * isNumeric("12 3") = false
	 * isNumeric("ab2c") = false
	 * isNumeric("12-3") = false
	 * isNumeric("12.3") = false
	 * isNumeric("-123") = false
	 * isNumeric("+123") = false
	 * </pre>
	 *
	 * @param text	The text to check, may be {@code null}.
	 * @return	Whether the given text contains only digits and is non-{@code null}.
	 */
	private static boolean isDecimalNumber(final String text){
		return (text != null && !text.isEmpty() && !isBaseNumber(text, 0, 10));
	}

	private static boolean isHexadecimalNumber(final String text){
		return (text != null && text.startsWith("0x") && !isBaseNumber(text, 2, 16));
	}

	private static boolean isBaseNumber(final CharSequence text, final int offset, final int radix){
		for(int i = offset; i < text.length(); i ++){
			final char chr = text.charAt(i);
			if(Character.digit(chr, radix) < 0)
				return true;
		}
		return false;
	}

	private static Object toObjectValue(final String value, final Class<?> objectiveType) throws CodecException{
		try{
			final Method method = objectiveType.getDeclaredMethod(METHOD_VALUE_OF, String.class);
			return method.invoke(null, value);
		}
		catch(final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored){
			throw CodecException.create("Cannot interpret {} as {}", value, objectiveType.getSimpleName());
		}
	}

}
