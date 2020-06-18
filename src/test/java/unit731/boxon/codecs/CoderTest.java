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
import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindBit;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.BindNumber;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.codecs.queclink.Version;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Locale;
import java.util.Random;


class CoderTest{

	private static final Random RANDOM = new Random();


	@Test
	void string(){
		Coder coder = Coder.STRING;
		String encodedValue = "123ABC";
		BindString annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length());
			}

			@Override
			public String match(){
				return "";
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

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));

		BitBuffer reader = BitBuffer.wrap(writer);

		String decoded = (String)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringTerminated(){
		Coder coder = Coder.STRING_TERMINATED;
		String encodedValue = "123ABC";
		BindStringTerminated annotation = new BindStringTerminated(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindStringTerminated.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public byte terminator(){
				return '\0';
			}

			@Override
			public boolean consumeTerminator(){
				return false;
			}

			@Override
			public String match(){
				return "";
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

		BitBuffer reader = BitBuffer.wrap(encodedValue.getBytes(StandardCharsets.US_ASCII));

		Object decoded = coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, decoded);
		writer.flush();

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));
	}

	@Test
	void arrayPrimitive(){
		Coder coder = Coder.ARRAY_PRIMITIVE;
		int[] encodedValue = new int[]{0x0000_0123, 0x0000_0456};
		BindArrayPrimitive annotation = new BindArrayPrimitive(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArrayPrimitive.class;
			}

			@Override
			public Class<?> type(){
				return int[].class;
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length);
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
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

		Assertions.assertArrayEquals(new byte[]{0x00, 0x00, 0x01, 0x23, 0x00, 0x00, 0x04, 0x56}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);

		Object decoded = coder.decode(reader, annotation, null);

		Assertions.assertArrayEquals(encodedValue, (int[])decoded);
	}

	@Test
	void array(){
		Coder coder = Coder.ARRAY;
		Version[] encodedValue = new Version[]{new Version((byte)0, (byte)1, (byte)12), new Version((byte)1, (byte)2, (byte)0)};
		BindArray annotation = new BindArray(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindArray.class;
			}

			@Override
			public Class<?> type(){
				return Version.class;
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.length);
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

		Assertions.assertArrayEquals(new byte[]{0x00, 0x01, 0x01, 0x02}, writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);

		Version[] decoded = (Version[])coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.length, decoded.length);
		Assertions.assertEquals(encodedValue[0].major, decoded[0].major);
		Assertions.assertEquals(encodedValue[0].minor, decoded[0].minor);
		Assertions.assertEquals(encodedValue[1].major, decoded[1].major);
		Assertions.assertEquals(encodedValue[1].minor, decoded[1].minor);
	}

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

		Assertions.assertArrayEquals(encodedValue.toByteArray(), writer.array());

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

		Assertions.assertArrayEquals(encodedValue.toByteArray(), writer.array());

		BitBuffer reader = BitBuffer.wrap(writer);

		BitSet decoded = (BitSet)coder.decode(reader, annotation, null);

		reverseBits(encodedValue, randomBytes.length * Byte.SIZE);
		Assertions.assertEquals(encodedValue, decoded);
	}

	private static void reverseBits(final BitSet input, final int size){
		for(int i = 0; i < size / 2; i ++){
			final boolean t = input.get(i);
			input.set(i, input.get(size - i - 1));
			input.set(size - i - 1, t);
		}
	}

	@Test
	void testByte(){
		Coder coder = Coder.BYTE;
		byte encodedValue = (byte)(RANDOM.nextInt() & 0x0000_00FF);
		BindByte annotation = new BindByte(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindByte.class;
			}

			@Override
			public boolean unsigned(){
				return false;
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

		Assertions.assertEquals(1, writer.array().length);
		Assertions.assertEquals(encodedValue, writer.array()[0]);

		BitBuffer reader = BitBuffer.wrap(writer);

		byte decoded = (byte)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndian(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)(RANDOM.nextInt() & 0x0000_FFFF);
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Short.reverseBytes(encodedValue) & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		short decoded = (short)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndian(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)(RANDOM.nextInt() & 0x0000_FFFF);
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		short decoded = (short)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void integerLittleEndian(){
		Coder coder = Coder.INTEGER;
		int encodedValue = RANDOM.nextInt();
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Integer.reverseBytes(encodedValue)).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void integerBigEndian(){
		Coder coder = Coder.INTEGER;
		int encodedValue = RANDOM.nextInt();
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndian(){
		Coder coder = Coder.LONG;
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(encodedValue)).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndian(){
		Coder coder = Coder.LONG;
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

//	@Test
//	void smallPositiveNumberLittleEndian(){
//		Coder coder = Coder.NUMBER;
//		//the high bit is set to zero to make it positive
//		long encodedValue = RANDOM.nextLong() & 0x007F_FFFF;
//encodedValue=8258995l;
////{0, 1, 4, 5, 7, 8, 10, 17, 18, 19, 20, 21, 22}/24
////{1, 2, 3, 4, 5, 6, 13, 15, 16, 18, 19, 22, 23}
//		BindNumber annotation = new BindNumber(){
//			@Override
//			public Class<? extends Annotation> annotationType(){
//				return BindNumber.class;
//			}
//
//			@Override
//			public String size(){
//				return "24";
//			}
//
//			@Override
//			public boolean unsigned(){
//				return false;
//			}
//
//			@Override
//			public ByteOrder byteOrder(){
//				return ByteOrder.LITTLE_ENDIAN;
//			}
//
//			@Override
//			public String match(){
//				return null;
//			}
//
//			@Override
//			public Class<? extends Validator> validator(){
//				return NullValidator.class;
//			}
//
//			@Override
//			public Class<? extends Transformer> transformer(){
//				return NullTransformer.class;
//			}
//		};
//
//		BitWriter writer = new BitWriter();
//		coder.encode(writer, annotation, null, encodedValue);
//		writer.flush();
//
//		Assertions.assertEquals(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), writer.toString());
//
//		BitBuffer reader = BitBuffer.wrap(writer);
//
//		long decoded = (long)coder.decode(reader, annotation, null);
//
//		Assertions.assertEquals(encodedValue, decoded);
//	}
//
//	@Test
//	void smallPositiveNumberBigEndian(){
//		Coder coder = Coder.NUMBER;
//		//the high bit is set to zero to make it positive
//		long encodedValue = RANDOM.nextLong() & 0x007F_FFFF;
//encodedValue=8258995l;
//		//{0, 1, 4, 5, 7, 8, 10, 17, 18, 19, 20, 21, 22}/24
//		//{1, 2, 3, 4, 5, 6, 13, 15, 16, 18, 19, 22, 23}
//		BindNumber annotation = new BindNumber(){
//			@Override
//			public Class<? extends Annotation> annotationType(){
//				return BindNumber.class;
//			}
//
//			@Override
//			public String size(){
//				return "24";
//			}
//
//			@Override
//			public boolean unsigned(){
//				return false;
//			}
//
//			@Override
//			public ByteOrder byteOrder(){
//				return ByteOrder.BIG_ENDIAN;
//			}
//
//			@Override
//			public String match(){
//				return null;
//			}
//
//			@Override
//			public Class<? extends Validator> validator(){
//				return NullValidator.class;
//			}
//
//			@Override
//			public Class<? extends Transformer> transformer(){
//				return NullTransformer.class;
//			}
//		};
//
//		BitWriter writer = new BitWriter();
//		coder.encode(writer, annotation, null, encodedValue);
//		writer.flush();
//
//		Assertions.assertEquals(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), writer.toString());
//
//		BitBuffer reader = BitBuffer.wrap(writer);
//
//		long decoded = (long)coder.decode(reader, annotation, null);
//
//		Assertions.assertEquals(encodedValue, decoded);
//	}
//
//	@Test
//	void bigPositiveNumberLittleEndian(){
//		Coder coder = Coder.NUMBER;
//		//the high bit is set to zero to make it positive
//		BigInteger encodedValue = RANDOM.nextLong() & 0x007F_FFFF;
//encodedValue=8258995l;
////{0, 1, 4, 5, 7, 8, 10, 17, 18, 19, 20, 21, 22}/24
////{1, 2, 3, 4, 5, 6, 13, 15, 16, 18, 19, 22, 23}
//		BindNumber annotation = new BindNumber(){
//			@Override
//			public Class<? extends Annotation> annotationType(){
//				return BindNumber.class;
//			}
//
//			@Override
//			public String size(){
//				return "124";
//			}
//
//			@Override
//			public boolean unsigned(){
//				return false;
//			}
//
//			@Override
//			public ByteOrder byteOrder(){
//				return ByteOrder.LITTLE_ENDIAN;
//			}
//
//			@Override
//			public String match(){
//				return null;
//			}
//
//			@Override
//			public Class<? extends Validator> validator(){
//				return NullValidator.class;
//			}
//
//			@Override
//			public Class<? extends Transformer> transformer(){
//				return NullTransformer.class;
//			}
//		};
//
//		BitWriter writer = new BitWriter();
//		coder.encode(writer, annotation, null, encodedValue);
//		writer.flush();
//
//		Assertions.assertEquals(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), writer.toString());
//
//		BitBuffer reader = BitBuffer.wrap(writer);
//
//		long decoded = (long)coder.decode(reader, annotation, null);
//
//		Assertions.assertEquals(encodedValue, decoded);
//	}
//
//	@Test
//	void bigPositiveNumberBigEndian(){
//		Coder coder = Coder.NUMBER;
//		//the high bit is set to zero to make it positive
//		BigInteger encodedValue = RANDOM.nextLong() & 0x007F_FFFF;
//encodedValue=8258995l;
////{0, 1, 4, 5, 7, 8, 10, 17, 18, 19, 20, 21, 22}/24
////{1, 2, 3, 4, 5, 6, 13, 15, 16, 18, 19, 22, 23}
//		BindNumber annotation = new BindNumber(){
//			@Override
//			public Class<? extends Annotation> annotationType(){
//				return BindNumber.class;
//			}
//
//			@Override
//			public String size(){
//				return "124";
//			}
//
//			@Override
//			public boolean unsigned(){
//				return false;
//			}
//
//			@Override
//			public ByteOrder byteOrder(){
//				return ByteOrder.BIG_ENDIAN;
//			}
//
//			@Override
//			public String match(){
//				return null;
//			}
//
//			@Override
//			public Class<? extends Validator> validator(){
//				return NullValidator.class;
//			}
//
//			@Override
//			public Class<? extends Transformer> transformer(){
//				return NullTransformer.class;
//			}
//		};
//
//		BitWriter writer = new BitWriter();
//		coder.encode(writer, annotation, null, encodedValue);
//		writer.flush();
//
//		Assertions.assertEquals(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), writer.toString());
//
//		BitBuffer reader = BitBuffer.wrap(writer);
//
//		long decoded = (long)coder.decode(reader, annotation, null);
//
//		Assertions.assertEquals(encodedValue, decoded);
//	}

}
