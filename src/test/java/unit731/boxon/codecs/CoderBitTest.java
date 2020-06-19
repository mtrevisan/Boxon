/**
 * Copyright (c) 2020 Mauro Trevisan
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
package unit731.boxon.codecs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindBit;
import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.utils.ByteHelper;

import java.lang.annotation.Annotation;
import java.util.BitSet;
import java.util.Random;


class CoderBitTest{

	private static final Random RANDOM = new Random();


	@Test
	void bitLittleEndian(){
		Coder coder = Coder.BIT;
		byte[] randomBytes = new byte[123];
		RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBit annotation = new BindBit(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBit.class;
			}

			@Override
			public String size(){
				return Integer.toString(randomBytes.length * Byte.SIZE);
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public String match(){
				return null;
			}

			@Override
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Transformer> transformer(){
				return NullTransformer.class;
			}
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		if(bb.length < randomBytes.length){
			byte[] b = new byte[randomBytes.length];
			System.arraycopy(bb, 0, b, 0, bb.length);
			bb = b;
		}
		Assertions.assertEquals(ByteHelper.byteArrayToHexString(bb), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BitSet decoded = (BitSet)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bitBigEndian(){
		Coder coder = Coder.BIT;
		byte[] randomBytes = new byte[123];
		RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBit annotation = new BindBit(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBit.class;
			}

			@Override
			public String size(){
				return Integer.toString(randomBytes.length * Byte.SIZE);
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public String match(){
				return null;
			}

			@Override
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Transformer> transformer(){
				return NullTransformer.class;
			}
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		if(bb.length < randomBytes.length){
			byte[] b = new byte[randomBytes.length];
			System.arraycopy(bb, 0, b, 0, bb.length);
			bb = b;
		}
		Assertions.assertEquals(ByteHelper.byteArrayToHexString(bb), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BitSet decoded = (BitSet)coder.decode(reader, annotation, null);

		ByteHelper.reverseBits(encodedValue, randomBytes.length * Byte.SIZE);
		Assertions.assertEquals(encodedValue, decoded);
	}

}
