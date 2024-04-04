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

import io.github.mtrevisan.boxon.annotations.bindings.BindList;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


final class CodecList implements CodecInterface<BindList>{

	private static final ObjectChoicesList.ObjectChoiceList EMPTY_CHOICE_LIST = new NullObjectChoiceList();


	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;
	@SuppressWarnings("unused")
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindList binding = interpretBinding(annotation);

		final Class<?> bindingType = binding.type();
		final List<Object> list = createList(bindingType);
		decodeWithAlternatives(reader, list, binding, rootObject);

		return convertValue(binding, rootObject, list);
	}

	private static <T> List<T> createList(final Class<? extends T> type) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.create("Argument cannot be a primitive: {}", type);

		return new ArrayList<>(0);
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Collection<Object> list, final BindList binding,
			final Object rootObject) throws FieldException{
		Class<?> chosenAlternativeType;
		while((chosenAlternativeType = chooseAlternativeSeparatedType(reader, binding, rootObject)) != null){
			//read object
			final Template<?> subTemplate = templateParser.createTemplate(chosenAlternativeType);
			final Object element = templateParser.decode(subTemplate, reader, rootObject);
			list.add(element);
		}
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindList binding = interpretBinding(annotation);

		CodecHelper.validate(value, binding.validator());

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final List<Object> list = CodecHelper.converterEncode(chosenConverter, value);

		encodeWithAlternatives(writer, list);
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final List<Object> list)
			throws FieldException{
		for(int i = 0, length = list.size(); i < length; i ++){
			final Object elem = list.get(i);

			final Class<?> type = elem.getClass();
			final Template<?> template = templateParser.createTemplate(type);

			templateParser.encode(template, writer, null, elem);
		}
	}


	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 */
	Class<?> chooseAlternativeSeparatedType(final BitReaderInterface reader, final BindList binding, final Object rootObject){
		if(!hasSelectSeparatedAlternatives(binding))
			return binding.type();

		final boolean hasHeader = addListHeaderToContext(reader, binding);
		if(!hasHeader)
			return null;

		final ObjectChoicesList.ObjectChoiceList[] alternatives = binding.selectFrom().alternatives();
		final ObjectChoicesList.ObjectChoiceList chosenAlternative = chooseAlternative(alternatives, rootObject);
		final Class<?> chosenAlternativeType = (!CodecHelper.isEmptyChoice(chosenAlternative)
			? chosenAlternative.type()
			: void.class);

		return (chosenAlternativeType != void.class? chosenAlternativeType: null);
	}

	/**
	 * Whether the select-object-separated-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-separated-from binding has any alternatives.
	 */
	private boolean hasSelectSeparatedAlternatives(final BindList binding){
		return (binding.selectFrom().alternatives().length > 0);
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the header.
	 * @return	Whether a prefix was retrieved.
	 */
	private boolean addListHeaderToContext(final BitReaderInterface reader, final BindList binding){
		final byte terminator = binding.selectFrom().terminator();
		final Charset charset = CharsetHelper.lookup(binding.selectFrom().charset());
		final String prefix = reader.getTextUntilTerminatorWithoutConsuming(terminator, charset);
		evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		return !prefix.isEmpty();
	}

	private ObjectChoicesList.ObjectChoiceList chooseAlternative(final ObjectChoicesList.ObjectChoiceList[] alternatives,
		final Object rootObject){
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ObjectChoicesList.ObjectChoiceList alternative = alternatives[i];

			final String condition = alternative.condition();
			if(evaluator.evaluateBoolean(condition, rootObject))
				return alternative;
		}
		return EMPTY_CHOICE_LIST;
	}

	private <IN, OUT> OUT convertValue(final BindList binding, final Object rootObject, final IN value){
		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final OUT convertedValue = CodecHelper.converterDecode(converterType, value);
		CodecHelper.validate(convertedValue, binding.validator());
		return convertedValue;
	}

}
