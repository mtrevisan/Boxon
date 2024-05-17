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

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;


/** Interface for a reader bit-by-bit. */
public interface BitReaderInterface{

	/**
	 * Returns the byte array that backs this reader.
	 *
	 * @return	The array that backs this reader.
	 */
	byte[] array();

	/**
	 * Gets the position of the backing {@link ByteBuffer} in integral number of {@code byte}s (lower bound).
	 *
	 * @return	The position of the backing buffer in {@code byte}s.
	 */
	int position();


	/**
	 * Skips a given amount of bits.
	 *
	 * @param length	The amount of bits to be skipped.
	 */
	void skip(int length);

	/**
	 * Skips an integral number of bytes until a terminator is found.
	 * <p>The terminator is NOT consumed!</p>
	 *
	 * @param terminator	The terminator at which to stop.
	 */
	void skipUntilTerminator(byte terminator);


	/**
	 * Reads the given type using the give byte order.
	 *
	 * @param type	The type of data to read. Here, the length of the types (in bits) are those defined by java
	 * 	(see {@link Byte#SIZE}, {@link Short#SIZE}, {@link Integer#SIZE}, {@link Long#SIZE}, {@link Float#SIZE},
	 * 	and {@link Double#SIZE}).
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	The read value of the given type.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	Object read(Class<?> type, ByteOrder byteOrder) throws AnnotationException;

	/**
	 * Reads the next {@code length} bits and composes a {@link BitSet}.
	 *
	 * @param length	The amount of bits to read.
	 * @return	A {@link BitSet} value at the {@link io.github.mtrevisan.boxon.core.helpers.BitReader BitReader}'s current position.
	 */
	BitSet readBitSet(int length);

	/**
	 * Reads {@link Byte#SIZE} bits and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	byte readByte();

	/**
	 * Reads the specified amount of {@code byte}s into an array of {@code byte}s.
	 *
	 * @param length	The number of {@code byte}s to read.
	 * @return	An array of {@code byte}s of length {@code n} that contains {@code byte}s read.
	 */
	byte[] readBytes(int length);

	/**
	 * Reads {@link Short#SIZE} bits and composes a {@code short} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code short}.
	 */
	short readShort(ByteOrder byteOrder);

	/**
	 * Reads {@link Integer#SIZE} bits and composes an {@code int} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	An {@code int}.
	 */
	int readInt(ByteOrder byteOrder);

	/**
	 * Reads {@link Long#SIZE} bits and composes a {@code long} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code long}.
	 */
	long readLong(ByteOrder byteOrder);

	/**
	 * Reads the next {@code size} bits and composes a {@link BigInteger}.
	 *
	 * @param size	The amount of bits to read.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@link BigInteger} value at the current position.
	 */
	BigInteger readBigInteger(int size, ByteOrder byteOrder);

	/**
	 * Reads the specified amount of {@code char}s with a given {@link Charset}.
	 *
	 * @param length	The number of {@code char}s to read.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	String readText(int length, Charset charset);

	/**
	 * Reads the specified amount of {@code char}s with an {@link StandardCharsets#UTF_8 UTF-8} charset.
	 *
	 * @param length	The number of {@code char}s to read.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	String readText(int length);

	/**
	 * Reads a string, with an {@link StandardCharsets#UTF_8 UTF-8} charset, until a terminator is found.
	 * <p>The terminator is NOT consumed!</p>
	 *
	 * @param terminator	The terminator of the string to be read.
	 * @return	A {@link String} of length {@code n} coded in {@link StandardCharsets#UTF_8 UTF-8} that contains {@code char}s read.
	 */
	String readTextUntilTerminator(byte terminator);

	/**
	 * Reads a string until a terminator is found.
	 * <p>The terminator is NOT consumed!</p>
	 *
	 * @param terminator	The terminator of the string to be read.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	String readTextUntilTerminator(byte terminator, Charset charset);

	/**
	 * Reads a string until a terminator is found without consuming the bytes read.
	 *
	 * @param terminator	The terminator of the string to be read.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	String readTextUntilTerminatorWithoutConsuming(byte terminator, Charset charset);

}
