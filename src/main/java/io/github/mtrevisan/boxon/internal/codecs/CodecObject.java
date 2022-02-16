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
package io.github.mtrevisan.boxon.internal.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.internal.helpers.BindingData;
import io.github.mtrevisan.boxon.internal.helpers.BindingDataBuilder;
import io.github.mtrevisan.boxon.internal.helpers.Evaluator;
import io.github.mtrevisan.boxon.internal.helpers.Injected;
import io.github.mtrevisan.boxon.internal.managers.Template;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.io.BitReaderInterface;
import io.github.mtrevisan.boxon.external.io.BitWriterInterface;
import io.github.mtrevisan.boxon.external.io.CodecInterface;

import java.lang.annotation.Annotation;


final class CodecObject implements CodecInterface<BindObject>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;
	@SuppressWarnings("unused")
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindObject binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);

		final Class<?> type = bindingData.chooseAlternativeType(reader);

		final Template<?> template = templateParser.createTemplate(type);
		final Object instance = templateParser.decode(template, reader, rootObject);
		bindingData.addToContext(instance);

		return CodecHelper.convertValue(bindingData, instance);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindObject binding = extractBinding(annotation);

		final BindingData bindingData = BindingDataBuilder.create(binding, rootObject, evaluator);
		bindingData.validate(value);

		Class<?> type = binding.type();
		if(bindingData.hasSelectAlternatives()){
			final ObjectChoices selectFrom = binding.selectFrom();
			type = value.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(selectFrom.alternatives(), type);

			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);
		}

		bindingData.addToContext(value);

		final Template<?> template = templateParser.createTemplate(type);

		final Class<? extends Converter<?, ?>> chosenConverter = bindingData.getChosenConverter();
		final Object obj = CodecHelper.converterEncode(chosenConverter, value);

		templateParser.encode(template, writer, rootObject, obj);
	}

}
