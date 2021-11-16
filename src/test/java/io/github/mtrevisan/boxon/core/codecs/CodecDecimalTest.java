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

import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.BindFloat;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.codecs.CodecDecimal;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.ByteOrder;
import io.github.mtrevisan.boxon.core.CodecInterface;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Random;


@SuppressWarnings("ALL")
class CodecDecimalTest{

	private static final Random RANDOM = new Random();


	@Test
	void decimalPositiveLittleEndian() throws FieldException{
		CodecInterface<BindDecimal> codec = new CodecDecimal();
		BigDecimal encodedValue = BigDecimal.valueOf(RANDOM.nextDouble());
		BindDecimal annotation = new BindDecimal(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindFloat.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return Double.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(Double.doubleToRawLongBits(encodedValue.doubleValue()))).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigDecimal decoded = (BigDecimal)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void decimalNegativeLittleEndian() throws FieldException{
		CodecInterface<BindDecimal> codec = new CodecDecimal();
		BigDecimal encodedValue = BigDecimal.valueOf(-RANDOM.nextDouble());
		BindDecimal annotation = new BindDecimal(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindDecimal.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return Double.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(Double.doubleToRawLongBits(encodedValue.doubleValue()))).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigDecimal decoded = (BigDecimal)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void decimalPositiveBigEndian() throws FieldException{
		CodecInterface<BindDecimal> codec = new CodecDecimal();
		BigDecimal encodedValue = BigDecimal.valueOf(RANDOM.nextDouble());
		BindDecimal annotation = new BindDecimal(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindDecimal.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return Double.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Double.doubleToRawLongBits(encodedValue.doubleValue())).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigDecimal decoded = (BigDecimal)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void decimalNegativeBigEndian() throws FieldException{
		CodecInterface<BindDecimal> codec = new CodecDecimal();
		BigDecimal encodedValue = BigDecimal.valueOf(-RANDOM.nextDouble());
		BindDecimal annotation = new BindDecimal(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindDecimal.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return Double.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Double.doubleToRawLongBits(encodedValue.doubleValue())).toUpperCase(Locale.ROOT), 8, '0'), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BigDecimal decoded = (BigDecimal)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
