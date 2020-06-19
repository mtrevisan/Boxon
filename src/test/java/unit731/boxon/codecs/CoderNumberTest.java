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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindNumber;
import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.utils.ByteHelper;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Locale;
import java.util.Random;


class CoderNumberTest{

	private static final Random RANDOM = new Random();


	@Test
	void smallPositiveNumberLittleEndian(){
		Coder coder = Coder.NUMBER;
		long encodedValue = (RANDOM.nextLong() & 0x007F_FFFF);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(encodedValue) >>> 40).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallNegativeNumberLittleEndian(){
		Coder coder = Coder.NUMBER;
		long encodedValue = -(RANDOM.nextLong() & 0x007F_FFFF);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public boolean unsigned(){
				return false;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(encodedValue) >>> 40).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallPositiveNumberBigEndian(){
		Coder coder = Coder.NUMBER;
		long encodedValue = (RANDOM.nextLong() & 0x007F_FFFF);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public boolean unsigned(){
				return true;
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

		BitSet bits = BitSet.valueOf(ByteHelper.reverseBytes(BigInteger.valueOf(encodedValue).toByteArray()));
		ByteHelper.reverseBits(bits, 24);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallNegativeNumberBigEndian(){
		Coder coder = Coder.NUMBER;
		long encodedValue = -(RANDOM.nextLong() & 0x007F_FFFF);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public boolean unsigned(){
				return false;
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

		BitSet bits = BitSet.valueOf(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(BigInteger.valueOf(Math.abs(encodedValue)).negate(), 24)));
		ByteHelper.reverseBits(bits, 24);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigPositiveNumberLittleEndian(){
		Coder coder = Coder.NUMBER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM);
		}while(encodedValue.toByteArray().length > 16);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(encodedValue, 128))), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BigInteger decoded = (BigInteger)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberLittleEndian(){
		Coder coder = Coder.NUMBER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 16);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public boolean unsigned(){
				return false;
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

		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(encodedValue, 128))), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BigInteger decoded = (BigInteger)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigPositiveNumberBigEndian(){
		Coder coder = Coder.NUMBER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM);
		}while(encodedValue.toByteArray().length > 16);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public boolean unsigned(){
				return true;
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

		BitSet bb = BitSet.valueOf(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(encodedValue, 128)));
		ByteHelper.reverseBits(bb, 128);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BigInteger decoded = (BigInteger)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberBigEndian(){
		Coder coder = Coder.NUMBER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 16);
		BindNumber annotation = new BindNumber(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindNumber.class;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public boolean unsigned(){
				return false;
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

		BitSet bb = BitSet.valueOf(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(encodedValue, 128)));
		ByteHelper.reverseBits(bb, 128);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		BigInteger decoded = (BigInteger)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
