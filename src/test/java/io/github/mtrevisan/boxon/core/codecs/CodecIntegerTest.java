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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.io.CodecInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.util.BitSet;


class CodecIntegerTest{

	@Test
	void smallLittleEndianSmall1() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x00_1020l);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("201000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianSmall2() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x10_2000l);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("002010", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndian() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x7F_00FFl);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("FF007F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void smallBigEndianSmall1() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x00_1020l);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("001020", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianSmall2() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x10_2000l);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("102000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndian() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x7F_00FFl);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "24";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00FF", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigLittleEndianSmall() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bitmap = BitSetHelper.createBitSet(128, encodedValue, ByteOrder.LITTLE_ENDIAN);
		Assertions.assertEquals(rightPad(StringHelper.toHexString(bitmap.toByteArray()), 32, '0'), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	/**
	 * Right pad a string with a specified character.
	 *
	 * @param text	The string to pad out, must not be {@code null}.
	 * @param size	The size to pad to.
	 * @param padChar	The character to pad with.
	 * @return	Right padded string or original string if no padding is necessary.
	 */
	private static String rightPad(final String text, final int size, final char padChar){
		final int pads = size - text.length();
		if(pads <= 0)
			return text;

		return new StringBuilder(text)
			.repeat(padChar, pads)
			.toString();
	}

	@Test
	void bigLittleEndian() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.LITTLE_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00000000000000000000FFFF0000FF7F", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigBigEndianSmall() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("00FF0000FFFF00000000000000000000", 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00FF0000FFFF00000000000000000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndian() throws FieldException{
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("7FFF0000FFFF00000000000000000000", 16);
		BindInteger annotation = new BindInteger(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindInteger.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String size(){
				return "128";
			}

			@Override
			public ByteOrder byteOrder(){
				return ByteOrder.BIG_ENDIAN;
			}

			@Override
			public Class<? extends Validator<?>> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Converter<?, ?>> converter(){
				return NullConverter.class;
			}

			@Override
			public ConverterChoices selectConverterFrom(){
				return new ConverterChoices(){
					@Override
					public Class<? extends Annotation> annotationType(){
						return ConverterChoices.class;
					}

					@Override
					public ConverterChoice[] alternatives(){
						return new ConverterChoice[0];
					}
				};
			}
		};

		BitWriter writer = BitWriter.create();
		FieldAccessor.injectValue(codec, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7FFF0000FFFF00000000000000000000", writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
