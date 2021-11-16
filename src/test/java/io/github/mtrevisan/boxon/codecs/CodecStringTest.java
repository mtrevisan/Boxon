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

import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.CodecInterface;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;


@SuppressWarnings("ALL")
class CodecStringTest{

	@Test
	void stringUS_ASCII() throws FieldException{
		CodecInterface<BindString> codec = new CodecString();
		String encodedValue = "123ABC";
		Annotation annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String condition(){
				return null;
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

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.US_ASCII));

		BitReader reader = BitReader.wrap(writer);
		String decoded = (String)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringUTF_8() throws FieldException{
		CodecInterface<BindString> codec = new CodecString();
		String encodedValue = "123ABCíïóúüđɉƚñŧ";
		Annotation annotation = new BindString(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindString.class;
			}

			@Override
			public String condition(){
				return null;
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

		Assertions.assertEquals(encodedValue, new String(writer.array(), StandardCharsets.UTF_8));

		BitReader reader = BitReader.wrap(writer);
		String decoded = (String)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue, decoded);
	}

	@Test
	void stringTerminated() throws FieldException{
		CodecInterface<BindStringTerminated> codec = new CodecStringTerminated();
		String encodedValue = "123ABC";
		Annotation annotation = new BindStringTerminated(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindStringTerminated.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public byte terminator(){
				return 'C';
			}

			@Override
			public boolean consumeTerminator(){
				return false;
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

		BitReader reader = BitReader.wrap(encodedValue.getBytes(StandardCharsets.US_ASCII));
		Object decoded = codec.decode(reader, annotation, null);

		Assertions.assertEquals("123AB", decoded);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, decoded);
		writer.flush();

		Assertions.assertArrayEquals(new byte[]{49, 50, 51, 65, 66}, writer.array());
	}

	@Test
	void stringTerminatedButEndOfStream() throws FieldException{
		CodecInterface<BindStringTerminated> codec = new CodecStringTerminated();
		String encodedValue = "123ABC";
		Annotation annotation = new BindStringTerminated(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindStringTerminated.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public String charset(){
				return StandardCharsets.US_ASCII.name();
			}

			@Override
			public byte terminator(){
				return 'D';
			}

			@Override
			public boolean consumeTerminator(){
				return false;
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

		BitReader reader = BitReader.wrap(encodedValue.getBytes(StandardCharsets.US_ASCII));
		Object decoded = codec.decode(reader, annotation, null);

		Assertions.assertEquals("123ABC", decoded);

		BitWriter writer = BitWriter.create();
		codec.encode(writer, annotation, null, decoded);
		writer.flush();

		//this seems strange, but it has to work like this
		Assertions.assertArrayEquals(new byte[]{49, 50, 51, 65, 66, 67}, writer.array());
	}

}
