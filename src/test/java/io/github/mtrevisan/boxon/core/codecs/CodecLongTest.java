/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecLong;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.core.CodecInterface;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


@SuppressWarnings("ALL")
class CodecLongTest{

	private static final Random RANDOM = new Random();


	@Test
	void longLittleEndianNegative() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x8FFF_0000_FFFF_0000l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000FFFF0000FF8F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianSmall() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x0000_0000_7F00_FF00l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("00FF007F00000000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianPositive() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x7F00_FF00_0000_0000l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0000000000FF007F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longLittleEndianRandom() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(encodedValue)).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianNegative() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x8FFF_0000_0000_0011l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("8FFF000000000011", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianSmall() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x0000_0000_7F00_FF00l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("000000007F00FF00", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianPositive() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = 0x7F00_FF00_0000_0000l;
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00FF0000000000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndianRandom() throws FieldException{
		CodecInterface<BindLong> codec = new CodecLong();
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		long decoded = (long)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
