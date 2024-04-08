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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;


final class ParserContext<T>{

	private Object rootObject;
	private T currentObject;

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


	Object getRootObject(){
		return rootObject;
	}

	void setRootObject(final Object parentObject){
		rootObject = JavaHelper.nonNullOrDefault(parentObject, currentObject);
	}

	T getCurrentObject(){
		return currentObject;
	}

	/**
	 * Set the field value on the current object.
	 *
	 * @param field	The field.
	 * @param value	The value.
	 */
	@SuppressWarnings("unchecked")
	void setFieldValue(final Field field, final Object value){
		//NOTE: record classes must be created anew, therefore `currentObject` must be updated
		currentObject = (T)FieldAccessor.setFieldValue(currentObject, field, value);
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

	void setField(final Object field){
		this.field = field;
	}

	Object getFieldValue(){
		if(field instanceof final TemplateField f)
			return f.getFieldValue(currentObject);
		if(field instanceof final ConfigurationField f)
			return f.getFieldValue(currentObject);

		throw DataException.create("Field not of type {} or {}",
			TemplateField.class.getSimpleName(), ConfigurationField.class.getSimpleName());
	}

	/**
	 * The annotation bound to the field.
	 *
	 * @return	The annotation bound to the field.
	 */
	Annotation getBinding(){
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
