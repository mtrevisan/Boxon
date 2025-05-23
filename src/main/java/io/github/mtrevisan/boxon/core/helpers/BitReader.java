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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	public static BitReader wrap(final File file) throws IOException{
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


	@Override
	public void skip(final int length){
		skipBits(length);
	}

	@Override
	public void skipUntilTerminator(final byte terminator){
		readTextUntilTerminator(terminator, Charset.defaultCharset());
	}

	@Override
	public Object read(final Class<?> type, final ByteOrder byteOrder) throws AnnotationException{
		return DataTypeReaderWriter.read(this, byteOrder, type);
	}

	@Override
	public byte readByte(){
		return (byte)readNumber(Byte.SIZE);
	}

	@Override
	public byte[] readBytes(final int length){
		final byte[] array = new byte[length];
		for(int i = 0; i < length; i ++)
			array[i] = readByte();
		return array;
	}

	@Override
	public short readShort(final ByteOrder byteOrder){
		final short value = (short)readNumber(Short.SIZE);
		return byteOrder.correctEndianness(value);
	}

	@Override
	public int readInt(final ByteOrder byteOrder){
		final int value = (int)readNumber(Integer.SIZE);
		return byteOrder.correctEndianness(value);
	}

	@Override
	public long readLong(final ByteOrder byteOrder){
		final long value = readNumber(Long.SIZE);
		return byteOrder.correctEndianness(value);
	}

	@Override
	public BigInteger readBigInteger(final int size, final ByteOrder byteOrder){
		final BitSet bitmap = readBitSet(size);
		return BitSetHelper.toObjectiveType(bitmap, size, byteOrder);
	}

	@Override
	public String readText(final int length, final Charset charset){
		return new String(readBytes(length), charset);
	}

	@Override
	public String readText(final int length){
		return readText(length, StandardCharsets.UTF_8);
	}

	@Override
	public String readTextUntilTerminator(final byte terminator){
		return readTextUntilTerminator(terminator, StandardCharsets.UTF_8);
	}

	@Override
	public String readTextUntilTerminator(final byte terminator, final Charset charset){
		String text = null;
		try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			getTextUntilTerminator(baos, terminator);

			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

	@Override
	public String readTextUntilTerminatorWithoutConsuming(final byte terminator, final Charset charset){
		String text = null;
		try(final ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			getTextUntilTerminatorWithoutConsuming(baos, terminator);

			text = baos.toString(charset);
		}
		catch(final IOException ignored){}
		return text;
	}

}
