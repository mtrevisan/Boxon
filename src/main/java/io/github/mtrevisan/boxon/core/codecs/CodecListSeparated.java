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

import io.github.mtrevisan.boxon.annotations.bindings.BindListSeparated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectSeparatedChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
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
import java.util.ArrayList;
import java.util.List;


final class CodecListSeparated implements CodecInterface<BindListSeparated>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;
	@SuppressWarnings("unused")
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindListSeparated binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);

		final Class<?> bindingType = binding.type();
		final List<Object> list = createList(bindingType);
		if(bindingData.hasSelectSeparatedAlternatives())
			decodeWithAlternatives(reader, list, bindingData, rootObject);
		else
			decodeWithoutAlternatives(reader, list, bindingType);

		return CodecHelper.convertValue(bindingData, list);
	}

	private static <T> List<T> createList(final Class<? extends T> type) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.create("Argument cannot be a primitive: {}", type);

		return new ArrayList<>();
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final List<Object> list, final BindingData bindingData,
		final Object rootObject) throws FieldException{
		for(int i = 0; i < list.size(); i ++){
			final Class<?> chosenAlternativeType = bindingData.chooseAlternativeType(reader);

			//read object
			final Template<?> subTemplate = templateParser.createTemplate(chosenAlternativeType);
			list.set(i, templateParser.decode(subTemplate, reader, rootObject));
		}
	}

	private void decodeWithoutAlternatives(final BitReaderInterface reader, final List<Object> list, final Class<?> type)
		throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0; i < list.size(); i ++)
			list.set(i, templateParser.decode(template, reader, null));
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindListSeparated binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		if(bindingData.hasSelectSeparatedAlternatives())
			encodeWithAlternatives(writer, array, binding.selectFrom());
		else
			encodeWithoutAlternatives(writer, array, binding.type());
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectSeparatedChoices selectFrom)
			throws FieldException{
		final ObjectSeparatedChoices.ObjectSeparatedChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0; i < array.length; i ++){
			final Object elem = array[i];
			final Class<?> type = elem.getClass();

			final ObjectSeparatedChoices.ObjectSeparatedChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writeHeader(writer, chosenAlternative, selectFrom);
			final byte terminator = selectFrom.terminator();
			writer.putByte(terminator);

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
