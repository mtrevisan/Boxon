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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.core.managers.templates.BoundedField;
import io.github.mtrevisan.boxon.core.managers.configurations.ConfigField;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;


final class ParserContext<T>{

	private final Evaluator evaluator;

	private Object rootObject;
	private final T currentObject;

	private String className;
	private String fieldName;
	private Object field;
	private Annotation binding;


	ParserContext(final Evaluator evaluator, final T currentObject){
		this(evaluator, currentObject, null);
	}

	ParserContext(final Evaluator evaluator, final T currentObject, final Object parentObject){
		this.evaluator = evaluator;

		this.currentObject = currentObject;
		setRootObject(parentObject);
	}


	public Object getRootObject(){
		return rootObject;
	}

	void setRootObject(final Object parentObject){
		rootObject = JavaHelper.nonNullOrDefault(parentObject, currentObject);
	}

	T getCurrentObject(){
		return currentObject;
	}

	void addCurrentObjectToEvaluatorContext(){
		evaluator.addToContext(ContextHelper.CONTEXT_SELF, currentObject);
	}

	String getClassName(){
		return className;
	}

	void setClassName(final String className){
		this.className = className;
	}

	String getFieldName(){
		return fieldName;
	}

	void setFieldName(final String fieldName){
		this.fieldName = fieldName;
	}

	public Object getField(){
		return field;
	}

	void setField(final Object field){
		this.field = field;
	}

	Object getFieldValue(){
		if(field instanceof BoundedField)
			return ((BoundedField)field).getFieldValue(currentObject);
		if(field instanceof ConfigField)
			return ((ConfigField)field).getFieldValue(currentObject);

		throw new IllegalArgumentException("Field not of type " + BoundedField.class.getSimpleName() + " or "
			+ ConfigField.class.getSimpleName());
	}

	/**
	 * The annotation bound to the field.
	 *
	 * @return	The annotation bound to the field.
	 */
	public Annotation getBinding(){
		return binding;
	}

	/**
	 * Set the annotation bound to the field.
	 *
	 * @param binding	The annotation.
	 */
	void setBinding(final Annotation binding){
		this.binding = binding;
	}

}
