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
import org.junit.jupiter.api.RepeatedTest;
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

	@Test
	void smallLittleEndianSmall(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x00_1020l;
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

		Assertions.assertEquals("201000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianUnsignedPositive(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x7F_00FFl;
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

		Assertions.assertEquals("FF007F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianUnsignedNegative(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x8F_0011l;
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

		Assertions.assertEquals("11008F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianPositive(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x7F_00FFl;
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

		Assertions.assertEquals("FF007F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianNegative(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x8F_0011l;
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

		Assertions.assertEquals("11008F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(ByteHelper.extendSign(encodedValue, 24), decoded);
	}


	@Test
	void smallBigEndianSmall(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x00_1020l;
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

		Assertions.assertEquals("001020", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianUnsignedPositive(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x7F_00FFl;
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

		Assertions.assertEquals("7F00FF", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianUnsignedNegative(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x8F_0011l;
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

		Assertions.assertEquals("8F0011", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianPositive(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x7F_00FFl;
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

		Assertions.assertEquals("7F00FF", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianNegative(){
		Coder coder = Coder.INTEGER;
		long encodedValue = 0x8F_0011l;
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

		Assertions.assertEquals("8F0011", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		long decoded = (long)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(ByteHelper.extendSign(encodedValue, 24), decoded);
	}


	@Test
	void bigLittleEndianSmall(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
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

		BitSet bits = ByteHelper.bigIntegerToBitSet(encodedValue, 128, ByteOrder.LITTLE_ENDIAN);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianUnsignedPositive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
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

		BitSet bits = ByteHelper.bigIntegerToBitSet(encodedValue, 128, ByteOrder.LITTLE_ENDIAN);
		Assertions.assertEquals(StringUtils.rightPad(ByteHelper.byteArrayToHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 32, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianUnsignedNegative(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("00000000000000000000FFFF0000FF80", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianPositive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("00000000000000000000FFFF0000FF7F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianNegative(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("00000000000000000000FFFF0000FF80", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigBigEndianSmall(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("00FF0000FFFF00000000000000000000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianUnsignedPositive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("7FFF0000FFFF00000000000000000000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianUnsignedNegative(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("80FF0000FFFF00000000000000000000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianPositive(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("7FFF0000FFFF00000000000000000000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianNegative(){
		Coder coder = Coder.INTEGER;
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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

		Assertions.assertEquals("80FF0000FFFF00000000000000000000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		BigInteger decoded = (BigInteger)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
