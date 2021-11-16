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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;


/**
 * @see <a href="https://github.com/jhg023/BitBuffer/blob/master/src/main/java/bitbuffer/BitBuffer.java">BitBuffer</a>
 */
public final class BitReader extends BitReaderData{

	/**
	 * Wraps a {@link File} containing a binary stream into a buffer.
	 *
	 * @param file	The file containing the binary stream.
	 * @return	The new bit buffer.
	 * @throws IOException	If an I/O error occurs.
	 * @throws FileNotFoundException	If the file does not exist, is a directory rather than a regular file,
	 * 	or for some other reason cannot be opened for reading.
	 * @throws SecurityException	If a security manager exists and its {@code checkRead} method denies read access to the file.
	 */
	public static BitReader wrap(final File file) throws IOException, FileNotFoundException{
		try(
			final FileInputStream fis = new FileInputStream(file);
			final FileChannel fc = fis.getChannel();
		){
			//map file into memory
			final ByteBuffer inputByteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0l, fc.size());

			return wrap(inputByteBuffer);
		}
	}

	/**
	 * Wraps a {@link ByteBuffer} into a buffer.
	 * <p>The new buffer will be backed by the given byte buffer</p>.
	 *
	 * @param buffer	The buffer that will back this buffer.
	 * @return	The new bit buffer.
	 */
	public static BitReader wrap(final ByteBuffer buffer){
		return new BitReader(buffer);
	}

	/**
	 * Wraps a byte array into a buffer.
	 * <p>The new buffer will be backed by the given byte array; that is, modifications to the buffer will cause the array
	 * to be modified and vice versa. The new buffer's capacity and limit will be {@code array.length}, its position will
	 * be zero, its mark will be undefined, and its byte order will be {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.</p>
	 *
	 * @param array	The array that will back this buffer.
	 * @return	The new bit buffer.
	 */
	public static BitReader wrap(final byte[] array){
		return new BitReader(ByteBuffer.wrap(array));
	}

	/**
	 * Wraps a byte array into a buffer.
	 * <p>The new buffer will be backed by the given byte array contained into the {@link BitWriter}.</p>
	 *
	 * @param bitWriter	The {@link BitWriter}.
	 * @return	The new bit buffer.
	 */
	public static BitReader wrap(final BitWriter bitWriter){
		bitWriter.flush();
		return wrap(bitWriter.array());
	}


	private BitReader(final ByteBuffer buffer){
		super(buffer);
	}

	/**
	 * Skips a given amount of bits.
	 *
	 * @param length	The amount of bits to be skipped.
	 */
	public void skip(final int length){
		getBits(length);
	}

	/**
	 * Skips an integral number of bytes until a terminator is found.
	 * <p>The terminator is NOT consumed!</p>
	 *
	 * @param terminator	The terminator at which to stop.
	 */
	public void skipUntilTerminator(final byte terminator){
		getTextUntilTerminator(terminator, Charset.defaultCharset());
	}

	/**
	 * Reads the given type using the give byte order.
	 *
	 * @param type	The type of data to read. Here, the length of the types (in bits) are those defined by java
	 * 	(see {@link Byte#SIZE}, {@link Short#SIZE}, {@link Integer#SIZE}, {@link Long#SIZE}, {@link Float#SIZE},
	 * 	and {@link Double#SIZE}).
	 * @param byteOrder	The byte order used to read the bytes.
	 * @return	The read value of the given type.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 */
	@SuppressWarnings("ReturnOfNull")
	public Object get(final Class<?> type, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType t = ParserDataType.fromType(type);
		if(t == null)
			throw AnnotationException.create("Cannot read type {}, should be one of {}, or their objective counterparts",
				type.getSimpleName(), ParserDataType.describe());

		switch(t){
			case BYTE:
				return getByte();

			case SHORT:
				return getShort(byteOrder);

			case INTEGER:
				return getInt(byteOrder);

			case LONG:
				return getLong(byteOrder);

			case FLOAT:
				return getFloat(byteOrder);

			case DOUBLE:
				return getDouble(byteOrder);
		}

		//cannot happen
		return null;
	}

	/**
	 * Reads {@link Byte#SIZE} bits and composes a {@code byte}.
	 *
	 * @return	A {@code byte}.
	 */
	@Override
	public byte getByte(){
		return getInteger(Byte.SIZE, ByteOrder.LITTLE_ENDIAN).byteValue();
	}

	/**
	 * Reads the specified amount of {@code byte}s into an array of {@code byte}s.
	 *
	 * @param length	The number of {@code byte}s to read.
	 * @return	An array of {@code byte}s of length {@code n} that contains {@code byte}s read.
	 */
	public byte[] getBytes(final int length){
		final byte[] array = new byte[length];
		for(int i = 0; i < length; i ++)
			array[i] = getByte();
		return array;
	}

	/**
	 * Reads {@link Short#SIZE} bits and composes a {@code short} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code short}.
	 */
	public short getShort(final ByteOrder byteOrder){
		return getInteger(Short.SIZE, byteOrder).shortValue();
	}

	/**
	 * Reads {@link Integer#SIZE} bits and composes an {@code int} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	An {@code int}.
	 */
	public int getInt(final ByteOrder byteOrder){
		return getInteger(Integer.SIZE, byteOrder).intValue();
	}

	/**
	 * Reads {@link Long#SIZE} bits and composes a {@code long} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code long}.
	 */
	public long getLong(final ByteOrder byteOrder){
		return getInteger(Long.SIZE, byteOrder).longValue();
	}

	/**
	 * Reads the next {@code size} bits and composes a {@link BigInteger}.
	 *
	 * @param size	The amount of bits to read.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@link BigInteger} value at the current position.
	 */
	public BigInteger getInteger(final int size, final ByteOrder byteOrder){
		final BitSet bits = getBits(size);
		return bits.toInteger(size, byteOrder);
	}

	/**
	 * Reads {@link Float#SIZE} bits and composes a {@code float} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code float}.
	 */
	public float getFloat(final ByteOrder byteOrder){
		return Float.intBitsToFloat(getInt(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@code double}.
	 */
	public double getDouble(final ByteOrder byteOrder){
		return Double.longBitsToDouble(getLong(byteOrder));
	}

	/**
	 * Reads {@link Double#SIZE} bits and composes a {@code double} with the specified
	 * {@link ByteOrder}.
	 *
	 * @param cls	Either a {@code Float} or a {@link Double} class.
	 * @param byteOrder	The type of endianness: either {@link ByteOrder#LITTLE_ENDIAN} or {@link ByteOrder#BIG_ENDIAN}.
	 * @return	A {@link BigDecimal}.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 */
	@SuppressWarnings("ChainOfInstanceofChecks")
	public BigDecimal getDecimal(final Class<?> cls, final ByteOrder byteOrder) throws AnnotationException{
		final Class<?> primitiveClass = ParserDataType.toPrimitiveTypeOrSelf(cls);
		if(primitiveClass == float.class)
			return new BigDecimal(Float.toString(getFloat(byteOrder)));
		if(primitiveClass == double.class)
			return new BigDecimal(Double.toString(getDouble(byteOrder)));

		throw AnnotationException.create("Cannot read {} as a {}", BigDecimal.class.getSimpleName(), cls.getSimpleName());
	}

	/**
	 * Reads the specified amount of {@code char}s with a given {@link Charset}.
	 *
	 * @param length	The number of {@code char}s to read.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	public String getText(final int length, final Charset charset){
		return new String(getBytes(length), charset);
	}

	/**
	 * Reads a string until a terminator is found.
	 * <p>The terminator is NOT consumed!</p>
	 *
	 * @param terminator	The terminator of the string to be read.
	 * @param charset	The charset.
	 * @return	A {@link String} of length {@code n} coded with a given {@link Charset} that contains {@code char}s read.
	 */
	public String getTextUntilTerminator(final byte terminator, final Charset charset){
		String text = null;
		try(
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final OutputStreamWriter osw = new OutputStreamWriter(baos, charset);
		){
			getTextUntilTerminator(osw, terminator);

			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

}
