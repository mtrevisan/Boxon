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

import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;


/**
 * Interface for a writer bit-by-bit.
 */
public interface BitWriterInterface{

	/**
	 * Writes the given value using the give byte order.
	 *
	 * @param value	The data to written. Here, the length of the types (in bits) is those defined by java (see {@link Byte#SIZE},
	 * 	{@link Short#SIZE}, {@link Integer#SIZE}, {@link Long#SIZE}, {@link Float#SIZE}, and {@link Double#SIZE}).
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	void write(Object value, ByteOrder byteOrder) throws AnnotationException;

	/**
	 * Skip {@code length} bits.
	 *
	 * @param length	The number of bits to skip.
	 */
	void skipBits(int length);

	/**
	 * Writes {@code value} to this {@link io.github.mtrevisan.boxon.core.helpers.BitWriter BitWriter} using {@code length} bits.
	 *
	 * @param bitmap	The value to write.
	 * @param length	The number of bits to use when writing the {@code bitmap}.
	 */
	void writeBitSet(BitSet bitmap, int length);

	/**
	 * Writes a value using {@link Byte#SIZE} bits.
	 *
	 * @param value	The {@code byte} to write.
	 */
	void writeByte(byte value);

	/**
	 * Writes an array of {@code byte}s using {@link Byte#SIZE} bits for each {@code byte}.
	 *
	 * @param array	The array of {@code byte}s to write.
	 */
	void writeBytes(byte[] array);

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Short#SIZE} bits.
	 *
	 * @param value	The {@code short} to write as an {@code int} for ease-of-use, but internally down-casted to a {@code short}.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	void writeShort(short value, ByteOrder byteOrder);

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Integer#SIZE} bits.
	 *
	 * @param value	The {@code int} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	void writeInt(int value, ByteOrder byteOrder);

	/**
	 * Writes a value with the specified {@link ByteOrder} using {@link Long#SIZE} bits.
	 *
	 * @param value	The {@code long} to write.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 */
	void writeLong(long value, ByteOrder byteOrder);

	/**
	 * Write the text into with a given {@link Charset}.
	 * <p>Note that if a terminator is needed, it must be manually written.</p>
	 *
	 * @param text	The {@code String}s to be written.
	 * @param charset	The charset.
	 */
	void writeText(String text, Charset charset);

	/**
	 * Write the text into with an {@link StandardCharsets#UTF_8 UTF-8} charset.
	 * <p>Note that if a terminator is needed, it must be manually written.</p>
	 *
	 * @param text	The {@code String}s to be written.
	 */
	void writeText(String text);

}
