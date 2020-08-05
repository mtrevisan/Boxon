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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.BindShort;
import io.github.mtrevisan.boxon.annotations.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.enums.ByteOrder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


class CodecShortTest{

	private static final Random RANDOM = new Random();


	@Test
	void shortLittleEndianPositive1(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = 0x0010;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("1000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianPositive2(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = 0x1000;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("0010", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianNegative(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = (short)0x8010;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("1080", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortLittleEndianRandom(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(Short.reverseBytes(encodedValue) & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianNegative(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = (short)0x8F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("8F00", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianSmall(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = 0x007F;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("007F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianPositive(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = 0x7F00;
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("7F00", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void shortBigEndianRandom(){
		CodecInterface<BindShort> codec = new CodecShort();
		short encodedValue = (short)RANDOM.nextInt(0x0000_FFFF);
		BindShort annotation = new BindShort(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindShort.class;
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
			public String match(){
				return null;
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

		BitWriter writer = new BitWriter();
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(StringUtils.leftPad(Integer.toHexString(encodedValue & 0x0000_FFFF).toUpperCase(Locale.ROOT), 4, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		short decoded = (short)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
