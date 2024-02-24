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
package io.github.mtrevisan.boxon.core.codecs.queclink;

import io.github.mtrevisan.boxon.annotations.converters.Converter;


public class ACKMaskHex{

	public static class ACKMaskConverter implements Converter<Byte, ACKMaskHex>{
		@Override
		public ACKMaskHex decode(final Byte value){
			return new ACKMaskHex(value);
		}

		@Override
		public Byte encode(final ACKMaskHex value){
			return value.mask;
		}
	}


	private final byte mask;


	ACKMaskHex(byte mask){
		this.mask = mask;
	}

	public int getMaskLength(){
		return 1;
	}

	public int getMessageLengthLength(){
		return 1;
	}

	public boolean hasMessageId(){
		return hasBit(mask, 6);
	}

	public boolean hasEventTime(){
		return hasBit(mask, 5);
	}

	public boolean hasIMEI(){
		//NOTE: negated logic!
		return !hasBit(mask, 4);
	}

	public boolean hasFirmwareVersion(){
		return hasBit(mask, 3);
	}

	public boolean hasProtocolVersion(){
		return hasBit(mask, 2);
	}

	public boolean hasDeviceType(){
		return hasBit(mask, 1);
	}

	public boolean hasLength(){
		return hasBit(mask, 0);
	}


	/**
	 * Checks whether the given {@code mask} has the bit at {@code index} set.
	 *
	 * @param mask	The value to check the bit into.
	 * @param index	The index of the bit (rightmost is zero). The value can range between {@code 0} and {@link Byte#SIZE}.
	 * @return	The state of the bit at a given index in the given byte.
	 */
	private static boolean hasBit(final byte mask, final int index){
		final int bitMask = 1 << (index % Byte.SIZE);
		return ((mask & bitMask) != 0);
	}

}
