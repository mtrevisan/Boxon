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

import io.github.mtrevisan.boxon.core.helpers.FieldRetriever;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigInteger;


final class ParserContext<T>{

	private Object rootObject;
	private T currentObject;

	private String className;
	private String fieldName;
	private Object field;
	private Annotation binding;
	private Annotation collectionBinding;


	static <T> ParserContext<T> create(final T currentObject){
		return new ParserContext<T>(currentObject);
	}

	static <T> ParserContext<T> create(final T currentObject, final Object parentObject){
		return new ParserContext<T>(currentObject, parentObject);
	}


	private ParserContext(final T currentObject){
		this(currentObject, null);
	}

	private ParserContext(final T currentObject, final Object parentObject){
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
	 * @throws DataException	If the value cannot be set to the field.
	 */
	void setFieldValue(final Field field, Object value){
		if(value instanceof final BigInteger bi)
			value = ParserDataType.castValue(bi, field.getType());
		else if(field.getType().isArray() && value.getClass().getComponentType() == BigInteger.class)
			value = ParserDataType.castValue((BigInteger[])value, field.getType().getComponentType());

		//NOTE: record classes must be created anew, therefore `currentObject` must be updated
		currentObject = FieldAccessor.setFieldValue(currentObject, field, value);
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
		if(!(field instanceof final FieldRetriever retriever))
			throw DataException.create("Field not of type {} nor {}",
				TemplateField.class.getSimpleName(), ConfigurationField.class.getSimpleName());

		return retriever.getFieldValue(currentObject);
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

	/**
	 * The collection annotation bound to the field.
	 *
	 * @return	The collection annotation bound to the field.
	 */
	Annotation getCollectionBinding(){
		return collectionBinding;
	}

	/**
	 * Set the collection annotation bound to the field.
	 *
	 * @param collectionBinding	The collection annotation.
	 */
	void setCollectionBinding(final Annotation collectionBinding){
		this.collectionBinding = collectionBinding;
	}

}
