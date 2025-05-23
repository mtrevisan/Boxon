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
package io.github.mtrevisan.boxon.core.helpers.writers;

import io.github.mtrevisan.boxon.io.BitWriterInterface;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
 * Manages to write numeric values using a specified {@link BitWriterInterface}.
 * <p>
 * Provides functionality to set the numeric base (radix) and handles various numeric types.
 * </p>
 */
final class NumberWriterManager implements WriterManager{

	private final BitWriterInterface writer;
	private int radix;


	static NumberWriterManager create(final BitWriterInterface writer){
		return new NumberWriterManager(writer);
	}


	private NumberWriterManager(final BitWriterInterface writer){
		this.writer = writer;
		radix = 10;
	}


	NumberWriterManager withRadix(final int radix){
		this.radix = radix;

		return this;
	}

	/**
	 * Writes a numeric value to the writer based on its type.
	 *
	 * @param value	The value to be written.
	 */
	@Override
	public void put(final Object value){
		switch(value){
			case final Byte v -> writer.writeText(Integer.toString(v.intValue() & 0xFF, radix));
			case final Short v -> writer.writeText(Integer.toString(v.intValue() & 0xFFFF, radix));
			case final Integer v -> writer.writeText(Integer.toString(v, radix));
			case final Long v -> writer.writeText(Long.toString(v, radix));
			case final BigDecimal v -> writer.writeText(v.toPlainString());
			case final BigInteger v -> writer.writeText(v.toString(radix));
			case final Float v -> writer.writeText(Float.toString(v));
			case final Double v -> writer.writeText(Double.toString(v));
			case null, default -> {}
		}
	}

}
