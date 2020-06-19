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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.util.Random;


class CoderByteTest{

	private static final Random RANDOM = new Random();


	@Test
	void testByte(){
		Coder coder = Coder.BYTE;
		byte encodedValue = (byte)(RANDOM.nextInt() & 0x0000_00FF);
		BindByte annotation = new BindByte(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindByte.class;
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
			public Class<? extends Validator> validator(){
				return NullValidator.class;
			}

			@Override
			public Class<? extends Transformer> transformer(){
				return NullTransformer.class;
			}
		};

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals(1, writer.array().length);
		Assertions.assertEquals(encodedValue, writer.array()[0]);

		BitBuffer reader = BitBuffer.wrap(writer);

		byte decoded = (byte)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

}
