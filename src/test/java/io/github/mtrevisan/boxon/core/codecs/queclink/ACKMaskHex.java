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
import io.github.mtrevisan.boxon.utils.TestHelper;


public final class ACKMaskHex{

	static final class ACKMaskConverter implements Converter<Byte, ACKMaskHex>{
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


	private ACKMaskHex(final byte mask){
		this.mask = mask;
	}


	public int getMaskLength(){
		return 1;
	}

	public int getMessageLengthLength(){
		return 1;
	}

	public boolean hasMessageId(){
		return TestHelper.hasBit(mask, 6);
	}

	public boolean hasEventTime(){
		return TestHelper.hasBit(mask, 5);
	}

	public boolean hasIMEI(){
		//NOTE: negated logic!
		return !TestHelper.hasBit(mask, 4);
	}

	public boolean hasFirmwareVersion(){
		return TestHelper.hasBit(mask, 3);
	}

	public boolean hasProtocolVersion(){
		return TestHelper.hasBit(mask, 2);
	}

	public boolean hasDeviceType(){
		return TestHelper.hasBit(mask, 1);
	}

	public boolean hasLength(){
		return TestHelper.hasBit(mask, 0);
	}

}
