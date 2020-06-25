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
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.helpers.ByteHelper;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Locale;
import java.util.Random;


class CoderIntegerTest{

	private static final Random RANDOM = new Random();


	@Test
	void smallNumberLittleEndianUnsigned(){
		Coder coder = Coder.INTEGER;
		long encodedValue = (RANDOM.nextLong() & 0x007F_FFFF);
		if(encodedValue > 0l)
			encodedValue = -encodedValue;
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(BigInteger.valueOf(-encodedValue), 24));
		ByteHelper.reverseBits(bits, 24);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(-encodedValue, decoded);
	}

	@Test
	void smallNegativeNumberLittleEndian(){
		Coder coder = Coder.INTEGER;
		long encodedValue = -(RANDOM.nextLong() & 0x007F_FFFF);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(BigInteger.valueOf(encodedValue), 24));
		ByteHelper.reverseBits(bits, 24);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallNumberBigEndianUnsigned(){
		Coder coder = Coder.INTEGER;
		long encodedValue = (RANDOM.nextLong() & 0x007F_FFFF);
		if(encodedValue > 0l)
			encodedValue = -encodedValue;
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(BigInteger.valueOf(-encodedValue), 24));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(-encodedValue, decoded);
	}

	@Test
	void smallNegativeNumberBigEndian(){
		Coder coder = Coder.INTEGER;
		long encodedValue = -(RANDOM.nextLong() & 0x007F_FFFF);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(BigInteger.valueOf(Math.abs(encodedValue)).negate(), 24));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigNumberLittleEndianUnsigned(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM);
		}while(encodedValue.signum() <= 0 || encodedValue.toByteArray().length > 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 128));
		ByteHelper.reverseBits(bits, 128);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberLittleEndian(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 128));
		ByteHelper.reverseBits(bits, 128);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNumberBigEndianUnsigned(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM);
		}while(encodedValue.signum() <= 0 || encodedValue.toByteArray().length > 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bb = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 128));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberBigEndian(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(128, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
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
			public boolean allowPrimitive(){
				return true;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bb = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 128));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigNumberLittleEndianUnsignedDisallowPrimitive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(32, RANDOM);
		}while(encodedValue.signum() <= 0 || encodedValue.toByteArray().length > 4);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String size(){
				return "32";
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
			public boolean allowPrimitive(){
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 32));
		ByteHelper.reverseBits(bits, 32);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberLittleEndianDisallowPrimitive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(32, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 4);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String size(){
				return "32";
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
			public boolean allowPrimitive(){
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bits = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 32));
		ByteHelper.reverseBits(bits, 32);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 6, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNumberBigEndianUnsignedDisallowPrimitive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(32, RANDOM);
		}while(encodedValue.signum() <= 0 || encodedValue.toByteArray().length > 4);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String size(){
				return "32";
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
			public boolean allowPrimitive(){
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bb = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 32));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigNegativeNumberBigEndianDisallowPrimitive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue;
		do{
			encodedValue = new BigInteger(32, RANDOM)
				.negate();
		}while(encodedValue.toByteArray().length > 4);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String size(){
				return "32";
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
			public boolean allowPrimitive(){
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bb = BitSet.valueOf(ByteHelper.createUnsignedByteArray(encodedValue, 32));
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bb.toByteArray()), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
