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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindBits;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.codecs.managers.ReflectionHelper;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitSet;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.ByteOrder;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Random;


@SuppressWarnings("ALL")
class CodecBitsTest{

	private static final Random RANDOM = new Random();


	@Test
	void bitsLittleEndian() throws FieldException{
		CodecInterface<BindBits> codec = new CodecBits();
		byte[] randomBytes = new byte[123];
		RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBits annotation = new BindBits(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBits.class;
			}

			@Override
			public String condition(){
				return null;
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
		ReflectionHelper.setFieldValue(codec, Evaluator.class, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		if(bb.length < randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(bb), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null);

		encodedValue.reverseBits(randomBytes.length * Byte.SIZE);
		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void bitsBigEndian() throws FieldException{
		CodecInterface<BindBits> codec = new CodecBits();
		byte[] randomBytes = new byte[123];
		RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBits annotation = new BindBits(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBits.class;
			}

			@Override
			public String condition(){
				return null;
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
		ReflectionHelper.setFieldValue(codec, Evaluator.class, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		if(bb.length < randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(bb), writer.toString());

		BitReader reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
