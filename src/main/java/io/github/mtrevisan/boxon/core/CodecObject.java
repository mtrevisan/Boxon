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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.external.EventListener;

import java.lang.annotation.Annotation;


final class CodecObject implements CodecInterface<BindObject>{

	@InjectEventListener
	@SuppressWarnings({"unused", "StaticVariableMayNotBeInitialized"})
	private static EventListener eventListener;


	/** Automatically injected by {@link TemplateParser} */
	@SuppressWarnings("unused")
	private Loader loader;
	/** Automatically injected by {@link TemplateParser} */
	@SuppressWarnings("unused")
	private TemplateParser templateParser;


	@Override
	public Object decode(final BitReader reader, final Annotation annotation, final Object rootObject){
		final BindObject binding = extractBinding(annotation);

		Object value = null;
		try{
			final Class<?> type = extractType(reader, binding, rootObject);

			final Template<?> template = loader.createTemplate(type);

			final Object instance = templateParser.decode(template, reader, rootObject);
			Evaluator.addToContext(CodecHelper.CONTEXT_SELF, instance);

			final ConverterChoices selectConverterFrom = binding.selectConverterFrom();
			final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
			final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(selectConverterFrom, defaultConverter,
				rootObject);
			value = CodecHelper.converterDecode(chosenConverter, instance);

			CodecHelper.validateData(binding.validator(), value);
		}
		catch(final Exception e){
			eventListener.processingAlternative(e);
		}
		return value;
	}

	private Class<?> extractType(final BitReader reader, final BindObject binding, final Object rootObject) throws CodecException{
		Class<?> chosenAlternativeType = binding.type();
		final ObjectChoices selectFrom = binding.selectFrom();
		if(selectFrom.alternatives().length > 0){
			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(reader, selectFrom, rootObject);
			chosenAlternativeType = (chosenAlternative != null? chosenAlternative.type(): binding.selectDefault());
			if(chosenAlternativeType == void.class)
				throw CodecException.create("Cannot find a valid codec from given alternatives for {}",
					rootObject.getClass().getSimpleName());
		}
		return chosenAlternativeType;
	}

	@Override
	public void encode(final BitWriter writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindObject binding = extractBinding(annotation);

		CodecHelper.validateData(binding.validator(), value);

		Class<?> type = binding.type();
		final ObjectChoices selectFrom = binding.selectFrom();
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		if(alternatives.length > 0){
			type = value.getClass();

			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writePrefix(writer, chosenAlternative, selectFrom);
		}

		Evaluator.addToContext(CodecHelper.CONTEXT_SELF, value);

		final Template<?> template = loader.createTemplate(type);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.chooseConverter(binding.selectConverterFrom(),
			binding.converter(), rootObject);
		final Object obj = CodecHelper.converterEncode(chosenConverter, value);

		templateParser.encode(template, writer, rootObject, obj);
	}

}
