/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArray implements CodecInterface<BindArray>{

	@Injected
	private Evaluator evaluator;
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindArray binding = (BindArray)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);

		final Class<?> bindingType = binding.type();
		final Object[] array = createArray(bindingType, size);
		final ObjectChoices objectChoices = binding.selectFrom();
		if(CodecHelper.hasSelectAlternatives(objectChoices.alternatives()))
			decodeWithAlternatives(reader, array, binding, rootObject);
		else
			decodeWithoutAlternatives(reader, array, bindingType, rootObject);

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		return CodecHelper.decodeValue(converterType, validator, array);
	}

	private static <T> T[] createArray(final Class<? extends T> type, final int length) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.createNotPrimitiveValue(type);

		return (T[])Array.newInstance(type, length);
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Object[] array, final BindArray binding,
			final Object rootObject) throws FieldException{
		for(int i = 0, length = array.length; i < length; i ++){
			final Class<?> chosenAlternativeType = CodecHelper.chooseAlternativeType(reader, binding.type(), binding.selectFrom(),
				binding.selectDefault(), evaluator, rootObject);

			//read object
			final Template<?> subTemplate = templateParser.createTemplate(chosenAlternativeType);
			array[i] = templateParser.decode(subTemplate, reader, rootObject);
		}
	}

	private void decodeWithoutAlternatives(final BitReaderInterface reader, final Object[] array, final Class<?> type,
			final Object rootObject) throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0, length = array.length; i < length; i ++)
			array[i] = templateParser.decode(template, reader, rootObject);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindArray binding = (BindArray)annotation;

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);

		CodecHelper.validate(value, binding.validator());

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		CodecHelper.assertSizeEquals(size, Array.getLength(array));

		final ObjectChoices objectChoices = binding.selectFrom();
		if(CodecHelper.hasSelectAlternatives(objectChoices.alternatives()))
			encodeWithAlternatives(writer, array, objectChoices, rootObject);
		else
			encodeWithoutAlternatives(writer, array, binding.type());
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectChoices selectFrom,
			final Object rootObject) throws FieldException{
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0, length = array.length; i < length; i ++){
			final Object elem = array[i];

			final Class<?> type = elem.getClass();
			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writeHeader(writer, chosenAlternative, selectFrom, evaluator, rootObject);

			final Template<?> template = templateParser.createTemplate(type);

			templateParser.encode(template, writer, null, elem);
		}
	}

	private void encodeWithoutAlternatives(final BitWriterInterface writer, final Object[] array, final Class<?> type) throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0, length = array.length; i < length; i ++)
			templateParser.encode(template, writer, null, array[i]);
	}

}
