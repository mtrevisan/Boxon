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

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.boxon.annotations.BindByte;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.util.Random;


class CodecByteTest{

	private static final Random RANDOM = new Random();


	@Test
	void testByte(){
		CodecInterface codec = new CodecByte();
		byte encodedValue = (byte)(RANDOM.nextInt() & 0x0000_00FF);
		BindByte annotation = new BindByte(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindByte.class;
			}

			@Override
			public String condition(){
				return null;
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
		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(1, writer.array().length);
		Assertions.assertEquals(encodedValue, writer.array()[0]);

		BitReader reader = BitReader.wrap(writer);
		byte decoded = (byte)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
