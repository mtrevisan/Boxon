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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.codecs.managers.BoundedField;
import io.github.mtrevisan.boxon.codecs.managers.ConfigField;
import io.github.mtrevisan.boxon.codecs.managers.ContextHelper;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.lang.annotation.Annotation;


final class ParserContext<T>{

	private Object rootObject;
	private final T currentObject;

	private String className;
	private String fieldName;
	private Object field;
	private Annotation binding;


	ParserContext(final T currentObject){
		this(currentObject, null);
	}

	ParserContext(final T currentObject, final Object parentObject){
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

	void addCurrentObjectToEvaluatorContext(final Evaluator evaluator){
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
		if(BoundedField.class.isInstance(field))
			return ((BoundedField)field).getFieldValue(currentObject);
		else if(ConfigField.class.isInstance(field))
			return ((ConfigField)field).getFieldValue(currentObject);

		throw new IllegalArgumentException("Field not of type " + BoundedField.class.getSimpleName() + " or "
			+ ConfigField.class.getSimpleName());
	}

	public Annotation getBinding(){
		return binding;
	}

	void setBinding(final Annotation binding){
		this.binding = binding;
	}

}
