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
package io.github.mtrevisan.boxon.annotations.bindings;


/** An enumeration for byte orders. */
public enum ByteOrder{

	/** Little-endian byte order. */
	LITTLE_ENDIAN{
		@Override
		public short correctEndianness(final short value){
			return value;
		}

		@Override
		public int correctEndianness(final int value){
			return value;
		}

		@Override
		public long correctEndianness(final long value){
			return value;
		}
	},
	/** Big-endian byte order. */
	BIG_ENDIAN{
		@Override
		public short correctEndianness(final short value){
			return Short.reverseBytes(value);
		}

		@Override
		public int correctEndianness(final int value){
			return Integer.reverseBytes(value);
		}

		@Override
		public long correctEndianness(final long value){
			return Long.reverseBytes(value);
		}
	};


	/**
	 * Corrects the endianness of a short value according to the specified byte order.
	 *
	 * @param value	The short value to correct the endianness of.
	 * @return	The corrected short value.
	 */
	public abstract short correctEndianness(short value);

	/**
	 * Corrects the endianness of a int value according to the specified byte order.
	 *
	 * @param value	The int value to correct the endianness of.
	 * @return	The corrected int value.
	 */
	public abstract int correctEndianness(int value);

	/**
	 * Corrects the endianness of a long value according to the specified byte order.
	 *
	 * @param value	The long value to correct the endianness of.
	 * @return	The corrected long value.
	 */
	public abstract long correctEndianness(long value);

}
