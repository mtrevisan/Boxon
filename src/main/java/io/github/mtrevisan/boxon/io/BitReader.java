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

import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;


/**
 * A reader bit-by-bit from a byte buffer or byte array.
 *
 * @see <a href="https://github.com/jhg023/BitBuffer/blob/master/src/main/java/bitbuffer/BitBuffer.java">BitBuffer</a>
 */
public final class BitReader extends BitReaderData implements BitReaderInterface{

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
				final FileChannel fc = fis.getChannel()){
			//map file into memory
			final ByteBuffer inputBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0l, fc.size());

			return wrap(inputBuffer);
		}
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
		return wrap(ByteBuffer.wrap(array));
	}

	/**
	 * Wraps a {@link ByteBuffer} into a buffer.
	 * <p>The new buffer will be backed by the given byte buffer.</p>
	 *
	 * @param buffer	The buffer that will back this buffer.
	 * @return	The new bit buffer.
	 */
	public static BitReader wrap(final ByteBuffer buffer){
		return new BitReader(buffer);
	}


	private BitReader(final ByteBuffer buffer){
		super(buffer);
	}


	@Override
	public void skip(final int length){
		getBitSet(length);
	}

	@Override
	public void skipUntilTerminator(final byte terminator){
		getTextUntilTerminator(terminator, Charset.defaultCharset());
	}

	@Override
	public Object get(final Class<?> type, final ByteOrder byteOrder) throws AnnotationException{
		final ParserDataType pdt = ParserDataType.fromType(type);
		if(pdt == null)
			throw AnnotationException.create("Cannot read type {}, should be one of {}, or their objective counterparts",
				type.getSimpleName(), ParserDataType.describe());

		return pdt.read(this, byteOrder);
	}

	@Override
	public byte getByte(){
		return getBigInteger(Byte.SIZE, ByteOrder.LITTLE_ENDIAN)
			.byteValue();
	}

	@Override
	public byte[] getBytes(final int length){
		//FIXME remove array creation?
		final byte[] array = new byte[length];
		for(int i = 0; i < length; i ++)
			array[i] = getByte();
		return array;
	}

	@Override
	public short getShort(final ByteOrder byteOrder){
		return getBigInteger(Short.SIZE, byteOrder)
			.shortValue();
	}

	@Override
	public int getInt(final ByteOrder byteOrder){
		return getBigInteger(Integer.SIZE, byteOrder)
			.intValue();
	}

	@Override
	public long getLong(final ByteOrder byteOrder){
		return getBigInteger(Long.SIZE, byteOrder)
			.longValue();
	}

	@Override
	public BigInteger getBigInteger(final int size, final ByteOrder byteOrder){
		final BitSet bits = getBitSet(size);
		return BitSetHelper.toBigInteger(bits, size, byteOrder);
	}

	@Override
	public float getFloat(final ByteOrder byteOrder){
		return Float.intBitsToFloat(getInt(byteOrder));
	}

	@Override
	public double getDouble(final ByteOrder byteOrder){
		return Double.longBitsToDouble(getLong(byteOrder));
	}

	@Override
	public String getText(final int length, final Charset charset){
		return new String(getBytes(length), charset);
	}

	@Override
	public String getText(final int length){
		return getText(length, StandardCharsets.UTF_8);
	}

	@Override
	public String getTextUntilTerminator(final byte terminator){
		return getTextUntilTerminator(terminator, StandardCharsets.UTF_8);
	}

	@Override
	public String getTextUntilTerminator(final byte terminator, final Charset charset){
		String text = null;
		try(
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(baos, charset)){
			getTextUntilTerminator(osw, terminator);

			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

	@Override
	public String getTextUntilTerminatorWithoutConsuming(final byte terminator, final Charset charset){
		String text = null;
		try(
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final OutputStreamWriter osw = new OutputStreamWriter(baos, charset)){
			getTextUntilTerminatorWithoutConsuming(osw, terminator);

			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

}
