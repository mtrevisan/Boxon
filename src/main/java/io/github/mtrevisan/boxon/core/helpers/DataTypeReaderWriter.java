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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.util.Arrays;


/**
 * Handles reading and writing data using {@link BitReaderInterface} and {@link BitWriterInterface}.
 */
final class DataTypeReaderWriter{

	private static final String CLASS_DESCRIPTOR = Arrays.toString(new String[]{byte.class.getSimpleName(), short.class.getSimpleName(),
		int.class.getSimpleName(), long.class.getSimpleName(), float.class.getSimpleName(), double.class.getSimpleName()});


	private DataTypeReaderWriter(){}


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
	 * Write specific data to the writer, using the given byte order.
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
		}
	}

}
