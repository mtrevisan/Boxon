/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.helpers.converters;

import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.math.BigInteger;
import java.util.BitSet;


public final class LittleEndianConverter implements BitSetConverter{

	/**
	 * Creates a {@link BitSet} with the given value.
	 *
	 * @param bitmapSize	The size in bits of the value.
	 * @param value	The value, must not be {@code null}.
	 * @return	A new bit set initialized with the given value.
	 */
	@Override
	public BitSet createBitSet(final int bitmapSize, final BigInteger value){
		final BitSet bitmap = new BitSet(bitmapSize);
		//transfer bits one by one from the most significant byte to the {@link BitSet}
		for(int i = 0, length = JavaHelper.getSizeInBytes(bitmapSize); i < length; i ++){
			final byte currentByte = value.shiftRight(i << 3)
				.byteValue();

			BitSetConverter.fillBits(bitmap, currentByte, i, bitmapSize);
		}
		return bitmap;
	}

	@Override
	public BigInteger toObjectiveType(final BitSet bitmap, final int bitmapSize){
		BigInteger result;
		if(BitSetConverter.isMultipleOfByte(bitmapSize)){
			result = BitSetConverter.toBigIntegerLittleEndian(bitmap);
			if(bitmap.get(bitmapSize - 1))
				result = BitSetConverter.negateValue(result, bitmapSize);
		}
		else
			result = BitSetConverter.toBigIntegerLittleEndian(invertBitSetOrder(bitmap, bitmapSize));
		return result;
	}

	private static BitSet invertBitSetOrder(final BitSet bitmap, final int bitmapSize){
		final BitSet invertedBitmap = new BitSet(bitmapSize);
		for(int i = bitmap.nextSetBit(0); i >= 0; i = bitmap.nextSetBit(i + 1))
			invertedBitmap.set(bitmapSize - i - 1);
		return invertedBitmap;
	}

}
