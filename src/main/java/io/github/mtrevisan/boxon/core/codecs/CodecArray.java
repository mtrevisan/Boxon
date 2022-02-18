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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.managers.BindingData;
import io.github.mtrevisan.boxon.core.managers.BindingDataBuilder;
import io.github.mtrevisan.boxon.core.managers.helpers.Evaluator;
import io.github.mtrevisan.boxon.core.managers.helpers.Injected;
import io.github.mtrevisan.boxon.core.managers.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;


final class CodecArray implements CodecInterface<BindArray>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;
	@SuppressWarnings("unused")
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindArray binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		final int size = bindingData.evaluateSize();
		CodecHelper.assertSizePositive(size);

		final Class<?> bindingType = binding.type();
		final Object[] array = createArray(bindingType, size);
		if(bindingData.hasSelectAlternatives())
			decodeWithAlternatives(reader, array, bindingData, rootObject);
		else
			decodeWithoutAlternatives(reader, array, bindingType);

		return CodecHelper.convertValue(bindingData, array);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] createArray(final Class<? extends T> type, final int length) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.create("Argument cannot be a primitive: {}", type);

		return (T[])Array.newInstance(type, length);
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Object[] array, final BindingData bindingData,
			final Object rootObject) throws FieldException{
		for(int i = 0; i < array.length; i ++){
			final Class<?> chosenAlternativeType = bindingData.chooseAlternativeType(reader);

			//read object
			final Template<?> subTemplate = templateParser.createTemplate(chosenAlternativeType);
			array[i] = templateParser.decode(subTemplate, reader, rootObject);
		}
	}

	private void decodeWithoutAlternatives(final BitReaderInterface reader, final Object[] array, final Class<?> type)
			throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0; i < array.length; i ++)
			array[i] = templateParser.decode(template, reader, null);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindArray binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = bindingData.evaluateSize();
		CodecHelper.assertSizePositive(size);
		CodecHelper.assertSizeEquals(size, Array.getLength(array));

		if(bindingData.hasSelectAlternatives())
			encodeWithAlternatives(writer, array, binding.selectFrom());
		else
			encodeWithoutAlternatives(writer, array, binding.type());
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectChoices selectFrom)
			throws FieldException{
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0; i < array.length; i ++){
			final Object elem = array[i];
			final Class<?> type = elem.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);

			final Template<?> template = templateParser.createTemplate(type);

			templateParser.encode(template, writer, null, elem);
		}
	}

	private void encodeWithoutAlternatives(final BitWriterInterface writer, final Object[] array, final Class<?> type) throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0; i < array.length; i ++)
			templateParser.encode(template, writer, null, array[i]);
	}

}
