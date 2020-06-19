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
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.transformers.NullTransformer;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.NullValidator;
import unit731.boxon.annotations.validators.Validator;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Random;


class CoderStringTest{

	private static final Random RANDOM = new Random();


	@Test
	void stringUS_ASCII(){
		Coder coder = Coder.STRING;
		String encodedValue = "123ABC";
		BindString annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.getBytes(StandardCharsets.US_ASCII).length);
			}

			@Override
			public String match(){
				return ".*";
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

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));

		BitBuffer reader = BitBuffer.wrap(writer);

		String decoded = (String)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringUTF_8(){
		Coder coder = Coder.STRING;
		String encodedValue = "123ABCíïóúüđɉƚñŧ";
		BindString annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.UTF_8.name();
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.getBytes(StandardCharsets.UTF_8).length);
			}

			@Override
			public String match(){
				return encodedValue;
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

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.UTF_8));

		BitBuffer reader = BitBuffer.wrap(writer);

		String decoded = (String)coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringUS_ASCIINotMatch(){
		Coder coder = Coder.STRING;
		String encodedValue = "123ABC";
		BindString annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public String size(){
				return Integer.toString(encodedValue.getBytes(StandardCharsets.US_ASCII).length);
			}

			@Override
			public String match(){
				return encodedValue + "-not-match";
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
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> coder.encode(writer, annotation, null, encodedValue));
	}

	@Test
	void stringTerminated(){
		Coder coder = Coder.STRING_TERMINATED;
		String encodedValue = "123ABC";
		BindStringTerminated annotation = new BindStringTerminated(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindStringTerminated.class;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public byte terminator(){
				return '\0';
			}

			@Override
			public boolean consumeTerminator(){
				return false;
			}

			@Override
			public String match(){
				return "";
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

		BitBuffer reader = BitBuffer.wrap(encodedValue.getBytes(StandardCharsets.US_ASCII));

		Object decoded = coder.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);

		BitWriter writer = new BitWriter();
		coder.encode(writer, annotation, null, decoded);
		writer.flush();

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));
	}

}
