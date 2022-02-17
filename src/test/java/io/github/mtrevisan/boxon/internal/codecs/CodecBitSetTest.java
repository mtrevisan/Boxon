/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.internal.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.internal.helpers.Evaluator;
import io.github.mtrevisan.boxon.internal.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.BitSetHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;


@SuppressWarnings("ALL")
class CodecBitSetTest{

	private static final Random RANDOM = new Random();


	@Test
	void bitsLittleEndian() throws FieldException{
		CodecInterface<BindBitSet> codec = new CodecBitSet();
//		byte[] randomBytes = new byte[123];
//		RANDOM.nextBytes(randomBytes);
byte[] randomBytes = new byte[]{(byte)0xAB, (byte)0xCD};
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBitSet annotation = new BindBitSet(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBitSet.class;
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
			public ByteOrder bitOrder(){
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
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		BitSet bbb = BitSetHelper.changeBitOrder(encodedValue, ByteOrder.LITTLE_ENDIAN);
		byte[] bb = bbb.toByteArray();
		if(bb.length > randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(bb), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	/**
	 * In-place reverse the endianness bit by bit.
	 */
	private static void bitReverse(final byte[] array){
		for(int i = 0; i < array.length; i ++)
			array[i] = reverseBits(array[i]);
		reverse(array);
	}

	private static byte reverseBits(byte number){
		byte reverse = 0;
		for(int i = Byte.SIZE - 1; i >= 0; i --){
			reverse += ((number & 1) << i);
			number >>= 1;
		}
		return reverse;
	}

	private static void reverse(final byte[] array){
		for(int start = 0, end = array.length - 1; start < end; start ++, end --)
			array[start] ^= array[end] ^ (array[end] = array[start]);
	}

	@Test
	void bitsBigEndian() throws FieldException{
		CodecInterface<BindBitSet> codec = new CodecBitSet();
		byte[] randomBytes = new byte[123];
		RANDOM.nextBytes(randomBytes);
		BitSet encodedValue = BitSet.valueOf(randomBytes);
		BindBitSet annotation = new BindBitSet(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindBitSet.class;
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
			public ByteOrder bitOrder(){
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
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		byte[] bb = encodedValue.toByteArray();
		if(bb.length > randomBytes.length)
			bb = Arrays.copyOf(bb, randomBytes.length);
		Assertions.assertEquals(StringHelper.toHexString(bb), writer.toString());

		BitReaderInterface reader = BitReader.wrap(writer);
		BitSet decoded = (BitSet)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
