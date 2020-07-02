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
package unit731.boxon.coders;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindInt;
import unit731.boxon.annotations.ByteOrder;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


class CoderIntTest{

	private static final Random RANDOM = new Random();


	@Test
	void intLittleEndianNegative(){
		CoderInterface coder = new CoderInt();
		int encodedValue = 0x80FF_0000;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000FF80", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intLittleEndianSmall(){
		CoderInterface coder = new CoderInt();
		int encodedValue = 0x0000_7FFF;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("FF7F0000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intLittleEndianPositive(){
		CoderInterface coder = new CoderInt();
		int encodedValue = 0x7FFF_0000;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000FF7F", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intLittleEndianRandom(){
		CoderInterface coder = new CoderInt();
		int encodedValue = RANDOM.nextInt();
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Integer.reverseBytes(encodedValue)).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intBigEndianNegative(){
		CoderInterface coder = new CoderInt();
		int encodedValue = (int)0x80FF_0000;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("80FF0000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intBigEndianSmall(){
		CoderInterface coder = new CoderInt();
		int encodedValue = 0x0000_7FFF;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00007FFF", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intBigEndianPositive(){
		CoderInterface coder = new CoderInt();
		int encodedValue = 0x7FFF_0000;
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode( writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7FFF0000", writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void intBigEndianRandom(){
		CoderInterface coder = new CoderInt();
		int encodedValue = RANDOM.nextInt();
		BindInt annotation = new BindInt(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInt.class;
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

		BitWriter writer = new BitWriter();
		coder.encode( writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);
		int decoded = (int)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
