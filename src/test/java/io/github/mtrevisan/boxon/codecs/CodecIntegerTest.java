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

import io.github.mtrevisan.boxon.annotations.BindInteger;
import io.github.mtrevisan.boxon.annotations.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.helpers.BitReader;
import io.github.mtrevisan.boxon.helpers.BitWriter;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.util.Locale;


class CodecIntegerTest{

	@Test
	void smallLittleEndianSmall1(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("201000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianSmall2(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("002010", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianPositive(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("FF007F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallLittleEndianNegative(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x8F_0011l);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("11008F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void smallBigEndianSmall1(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("001020", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianSmall2(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("102000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianPositive(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("7F00FF", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianNegative(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0x8F_0011l);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("8F0011", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void smallLittleEndianNegativeUnsigned(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0xFF8F_0011l);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("11008F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(BigInteger.valueOf(0x8F_0011l), decoded);
	}


	@Test
	void smallBigEndianNegativeSigned(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = BigInteger.valueOf(0xFFFF_FFFF_FF8F_0011l);
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
			public boolean unsigned(){
				return false;
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

		Assertions.assertEquals("8F0011", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void smallBigEndianNegativeUnsigned(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("FFFFFFFFFF8F0011", 16);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("8F0011", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(BigInteger.valueOf(0x8F_0011l), decoded);
	}


	@Test
	void bigLittleEndianSmall(){
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
			public boolean unsigned(){
				return true;
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

		BitSet bits = BitSet.valueOf(encodedValue, 128, ByteOrder.LITTLE_ENDIAN);
		Assertions.assertEquals(StringUtils.rightPad(JavaHelper.toHexString(bits.toByteArray()).toUpperCase(Locale.ROOT), 32, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianPositive(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("00000000000000000000FFFF0000FF7F", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigLittleEndianNegative(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("00000000000000000000FFFF0000FF80", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}


	@Test
	void bigBigEndianSmall(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("00FF0000FFFF00000000000000000000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianPositive(){
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("7FFF0000FFFF00000000000000000000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bigBigEndianNegative(){
		CodecInterface<BindInteger> codec = new CodecInteger();
		BigInteger encodedValue = new BigInteger("80FF0000FFFF00000000000000000000", 16);
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
			public boolean unsigned(){
				return true;
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

		Assertions.assertEquals("80FF0000FFFF00000000000000000000", writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigInteger decoded = (BigInteger)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
