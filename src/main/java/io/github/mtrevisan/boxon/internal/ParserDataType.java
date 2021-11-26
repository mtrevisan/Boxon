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
package io.github.mtrevisan.boxon.internal;

import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;

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

}
