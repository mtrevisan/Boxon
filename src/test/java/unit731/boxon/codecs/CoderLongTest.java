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
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.converters.NullConverter;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Random;


class CoderLongTest{

	private static final Random RANDOM = new Random();


	@Test
	void longLittleEndian(){
		Coder coder = Coder.LONG;
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(Long.reverseBytes(encodedValue)).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void longBigEndian(){
		Coder coder = Coder.LONG;
		long encodedValue = RANDOM.nextLong();
		BindLong annotation = new BindLong(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindLong.class;
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

		Assertions.assertEquals(StringUtils.leftPad(Long.toHexString(encodedValue).toUpperCase(Locale.ROOT), 16, '0'), writer.toString());

		BitBuffer reader = BitBuffer.wrap(writer);

		long decoded = (long)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
