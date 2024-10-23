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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.logs.EventListener;
import org.springframework.expression.EvaluationException;

import java.util.List;
import java.util.function.Function;


/**
 * Base class providing template processing functionality.
 */
class TemplateCoderBase{

	protected EventListener eventListener;


	TemplateCoderBase(){
		withEventListener(null);
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 */
	final void withEventListener(final EventListener eventListener){
		this.eventListener = (eventListener != null? eventListener: EventListener.getNoOpInstance());
	}


	protected final void processFields(final Template<?> template, final ParserContext<?> parserContext,
			final Function<PostProcess, String> valueExtractor){
		final String templateName = template.getName();
		final List<EvaluatedField<PostProcess>> postProcessedFields = template.getPostProcessedFields();
		for(int i = 0, length = postProcessedFields.size(); i < length; i ++){
			final EvaluatedField<PostProcess> field = postProcessedFields.get(i);

			processField(field, parserContext, templateName, valueExtractor);
		}
	}

	private void processField(final EvaluatedField<PostProcess> field, final ParserContext<?> parserContext, final String templateName,
			final Function<PostProcess, String> valueExtractor){
		final PostProcess binding = field.getBinding();
		final String condition = binding.condition();
		final Object rootObject = parserContext.getRootObject();
		final boolean process = shouldProcessField(condition, rootObject);
		if(!process)
			return;

		final String fieldName = field.getFieldName();
		eventListener.evaluatingField(templateName, fieldName);

		final String expression = valueExtractor.apply(binding);
		final Object value = Evaluator.evaluate(expression, rootObject, field.getFieldType());

		//store value in the current object
		parserContext.setFieldValue(field.getField(), value);

		eventListener.evaluatedField(templateName, fieldName, value);
	}

	protected static boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition != null && (condition.isEmpty() || Evaluator.evaluateBoolean(condition, rootObject)));
	}

	protected static void addContextParameters(final List<ContextParameter> contextParameters){
		for(int i = 0, length = contextParameters.size(); i < length; i ++){
			final ContextParameter contextParameterBinding = contextParameters.get(i);

			final String name = contextParameterBinding.name();
			final Object value = tryEvaluateContextValue(contextParameterBinding.value());
			Evaluator.putToContext(name, value);
		}
	}

	protected static void clearContextParameters(final List<ContextParameter> contextParameters){
		for(int i = 0, length = contextParameters.size(); i < length; i ++){
			final ContextParameter contextParameterBinding = contextParameters.get(i);

			final String name = contextParameterBinding.name();
			Evaluator.removeFromContext(name);
		}
	}

	private static Object tryEvaluateContextValue(final String contextValue){
		Object value;
		try{
			value = Evaluator.evaluate(contextValue, null, Object.class);
		}
		catch(final EvaluationException ignored){
			value = contextValue;
		}
		return value;
	}

}
