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

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecObject implements CodecInterface<BindObject>{

	@Injected
	private Evaluator evaluator;
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindObject binding = interpretBinding(annotation);

		final Class<?> type = chooseAlternativeType(reader, binding, rootObject);

		final Template<?> template = templateParser.createTemplate(type);
		final Object instance = templateParser.decode(template, reader, rootObject);
		evaluator.addCurrentObjectToEvaluatorContext(instance);

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Validator<?>> validator = binding.validator();
		return CodecHelper.decodeValue(converterChoices, defaultConverter, validator, instance, evaluator, rootObject);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindObject binding = interpretBinding(annotation);

		CodecHelper.validate(value, binding.validator());

		final ObjectChoices objectChoices = binding.selectFrom();
		final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
		Class<?> type = binding.type();
		if(CodecHelper.hasSelectAlternatives(alternatives)){
			type = value.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(objectChoices.alternatives(), type);

			CodecHelper.writeHeader(writer, chosenAlternative, objectChoices, evaluator, rootObject);
		}

		evaluator.addCurrentObjectToEvaluatorContext(value);

		final Template<?> template = templateParser.createTemplate(type);

		final ConverterChoices converterChoices = binding.selectConverterFrom();
		final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(converterChoices, defaultConverter, evaluator,
			rootObject);
		final Object obj = CodecHelper.converterEncode(chosenConverter, value);

		templateParser.encode(template, writer, rootObject, obj);
	}


	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 * @throws CodecException	If a codec cannot be found for the chosen alternative.
	 */
	private Class<?> chooseAlternativeType(final BitReaderInterface reader, final BindObject binding, final Object rootObject)
			throws CodecException{
		final ObjectChoices objectChoices = binding.selectFrom();
		final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
		if(!CodecHelper.hasSelectAlternatives(alternatives))
			return binding.type();

		CodecHelper.addPrefixToContext(reader, objectChoices, evaluator);

		final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, evaluator, rootObject);
		final Class<?> chosenAlternativeType = (!CodecHelper.isEmptyChoice(chosenAlternative)
			? chosenAlternative.type()
			: binding.selectDefault());

		if(chosenAlternativeType == void.class)
			throw CodecException.create("Cannot find a valid codec from given alternatives for {}",
				rootObject.getClass().getSimpleName());

		return chosenAlternativeType;
	}

}
