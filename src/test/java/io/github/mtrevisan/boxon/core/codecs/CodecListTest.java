/*
 * Copyright (c) 2024 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindList;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Core;
import io.github.mtrevisan.boxon.core.CoreBuilder;
import io.github.mtrevisan.boxon.core.Parser;
import io.github.mtrevisan.boxon.core.Response;
import io.github.mtrevisan.boxon.core.parsers.TemplateParser;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.io.BitReader;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@SuppressWarnings("ALL")
class CodecListTest{

	private static class Version{
		@BindStringTerminated(terminator = ',')
		private final String type;
		@BindStringTerminated(terminator = ',')
		private final String major;
		@BindStringTerminated(terminator = ',')
		private final String minor;
		@BindStringTerminated(terminator = ',')
		private final String build;

		private Version(final String type, final String major, final String minor, final String build){
			this.type = type;
			this.major = major;
			this.minor = minor;
			this.build = build;
		}
	}

	static class TestType3{}

	static class TestType4 extends TestType3{
		@BindStringTerminated(terminator = ',')
		String subtype;
		@BindStringTerminated(terminator = '.')
		String value;
	}

	static class TestType5 extends TestType3{
		@BindStringTerminated(terminator = ',')
		String subtype;
		@BindStringTerminated(terminator = '.')
		String value1;
		@BindStringTerminated(terminator = '.')
		String value2;
	}

	@MessageHeader(start = "tc6")
	static class TestChoice6{
		@BindStringTerminated(terminator = ',')
		String type;
		@BindList(type = TestType3.class, selectFrom = @ObjectChoicesList(terminator = ',',
			alternatives = {
				@ObjectChoicesList.ObjectChoiceList(condition = "#prefix == '1'", prefix = "1", type = TestType4.class),
				@ObjectChoicesList.ObjectChoiceList(condition = "#prefix == '2'", prefix = "2", type = TestType5.class)
			}))
		List<TestType3> value;
	}


	@Test
	void listOfSameObject() throws FieldException{
		CodecList codec = new CodecList();
		List<Version> encodedValue = new ArrayList<>(
			Arrays.asList(new Version("2", "0", "1", "12"), new Version("2", "1", "2", "0")));
		BindList annotation = new BindList(){
			@Override
			public Class<? extends Annotation> annotationType(){
				return BindList.class;
			}

			@Override
			public String condition(){
				return null;
			}

			@Override
			public Class<?> type(){
				return CodecListTest.Version.class;
			}

			@Override
			public ObjectChoicesList selectFrom(){
				return new ObjectChoicesList(){
					@Override
					public String charset(){
						return StandardCharsets.US_ASCII.name();
					}

					@Override
					public Class<? extends Annotation> annotationType(){
						return null;
					}

					@Override
					public byte terminator(){
						return ',';
					}

					@Override
					public ObjectChoiceList[] alternatives(){
						return new ObjectChoiceList[]{
							new ObjectChoiceList(){
								@Override
								public Class<? extends Annotation> annotationType(){
									return ObjectChoiceList.class;
								}

								@Override
								public String condition(){
									return "#prefix == '2'";
								}

								@Override
								public String prefix(){
									return "2";
								}

								@Override
								public Class<?> type(){
									return Version.class;
								}
							}
						};
					}
				};
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

		LoaderCodec loaderCodec = LoaderCodec.create();
		Evaluator evaluator = Evaluator.create();
		TemplateParserInterface templateParser = TemplateParser.create(loaderCodec, evaluator);
		loaderCodec.loadDefaultCodecs();
		ReflectionHelper.injectValue(codec, TemplateParserInterface.class, templateParser);
		ReflectionHelper.injectValue(codec, Evaluator.class, Evaluator.create());
		BitWriter writer = BitWriter.create();
 		codec.encode(writer, annotation, null, encodedValue);
		writer.flush();

		Assertions.assertEquals("2,0,1,12,2,1,2,0,", new String(writer.array(), StandardCharsets.UTF_8));

		BitReaderInterface reader = BitReader.wrap(writer);
		List<Version> decoded = (List<Version>)codec.decode(reader, annotation, null);

		Assertions.assertEquals(encodedValue.size(), decoded.size());
		Assertions.assertEquals(encodedValue.get(0).type, decoded.get(0).type);
		Assertions.assertEquals(encodedValue.get(0).major, decoded.get(0).major);
		Assertions.assertEquals(encodedValue.get(0).minor, decoded.get(0).minor);
		Assertions.assertEquals(encodedValue.get(1).type, decoded.get(1).type);
		Assertions.assertEquals(encodedValue.get(1).major, decoded.get(1).major);
		Assertions.assertEquals(encodedValue.get(1).minor, decoded.get(1).minor);
	}

	@Test
	void listOfDifferentObjects() throws AnnotationException, TemplateException, ConfigurationException{
		Core core = CoreBuilder.builder()
			.withCodecsFrom(CodecChecksum.class, CodecCustomTest.VariableLengthByteArray.class)
			.withTemplatesFrom(TestChoice6.class)
			.create();
		Parser parser = Parser.create(core);

		byte[] payload = toByteArray("tc6,1,1.2,v1.v2.1,2.");
		List<Response<byte[], Object>> result = parser.parse(payload);

		Assertions.assertNotNull(result);
		Assertions.assertEquals(1, result.size());
		Response<byte[], Object> response = result.get(0);
		Assertions.assertFalse(response.hasError());
		Assertions.assertEquals(TestChoice6.class, response.getMessage().getClass());
		TestChoice6 parsedMessage = (TestChoice6)response.getMessage();
		List<TestType3> values = parsedMessage.value;
		Assertions.assertEquals(TestType4.class, values.get(0).getClass());
		Assertions.assertEquals("1", ((TestType4)values.get(0)).value);
		Assertions.assertEquals(TestType5.class, values.get(1).getClass());
		Assertions.assertEquals("v1", ((TestType5)values.get(1)).value1);
		Assertions.assertEquals("v2", ((TestType5)values.get(1)).value2);
		Assertions.assertEquals(TestType4.class, values.get(2).getClass());
		Assertions.assertEquals("2", ((TestType4)values.get(2)).value);
	}


	private byte[] toByteArray(final String payload){
		return payload.getBytes(StandardCharsets.ISO_8859_1);
	}

}
