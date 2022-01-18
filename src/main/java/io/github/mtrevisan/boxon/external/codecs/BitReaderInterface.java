/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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

import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.math.BigInteger;
import java.nio.charset.Charset;


public interface BitReaderInterface{

	Object get(Class<?> type, ByteOrder byteOrder) throws AnnotationException;

	BitSet getBits(int length);

	byte getByte();

	byte[] getBytes(int length);

	short getShort(ByteOrder byteOrder);

	int getInt(ByteOrder byteOrder);

	long getLong(ByteOrder byteOrder);

	BigInteger getInteger(int size, ByteOrder byteOrder);

	float getFloat(ByteOrder byteOrder);

	double getDouble(ByteOrder byteOrder);

	String getText(int length, Charset charset);

	String getText(int length);

	String getTextUntilTerminator(byte terminator, Charset charset);

	String getTextUntilTerminator(byte terminator);

}
