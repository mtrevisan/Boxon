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
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


class CoderShortTest{

	private static final Random RANDOM = new Random();


	@Test
	void shortLittleEndianPositive(){
		Coder coder = Coder.SHORT;
		short encodedValue = 0x0010;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("1000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianNegative(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)0x8010;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("1080", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianRandom(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Short.reverseBytes(encodedValue) & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianUnsignedNegative(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)0x8F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("008F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

	@Test
	void shortLittleEndianUnsignedPositive(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)0x7F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("007F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

	@Test
	void shortLittleEndianUnsignedRandom(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
		if(encodedValue > 0)
			encodedValue = (short)-encodedValue;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Short.reverseBytes(encodedValue) & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

	@Test
	void shortBigEndianNegative(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)0x8F00;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("8F00", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianPositive(){
		Coder coder = Coder.SHORT;
		short encodedValue = 0x7F00;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianRandom(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		short decoded = (short)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianUnsignedNegative(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)0x8F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("8F00", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

	@Test
	void shortBigEndianUnsignedPositive(){
		Coder coder = Coder.SHORT;
		short encodedValue = 0x7F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

	@Test
	void shortBigEndianUnsignedRandom(){
		Coder coder = Coder.SHORT;
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
		if(encodedValue > 0)
			encodedValue = (short)-encodedValue;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public Class<? extends Converter> converter(){
				return NullConverter.class;
			}
		};

		MessageParser messageParser = new MessageParser();
		BitWriter writer = new BitWriter();
		coder.encode(messageParser, writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(messageParser, reader, annotation, null);

		Assertions.assertEquals((((int)encodedValue << Short.SIZE) >>> Short.SIZE) & 0xFFFF, decoded);
	}

}
