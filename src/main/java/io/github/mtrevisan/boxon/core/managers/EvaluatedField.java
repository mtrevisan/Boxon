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
package io.github.mtrevisan.boxon.core.managers;

import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.core.helpers.ReflectionHelper;

import java.lang.reflect.Field;


/** Data associated to a directly evaluable field. */
public final class EvaluatedField{

	private final Field field;
	private final Evaluate binding;


	EvaluatedField(final Field field, final Evaluate binding){
		this.field = field;
		this.binding = binding;
	}

	/**
	 * The name of the field.
	 *
	 * @return	The name of the field.
	 */
	public String getFieldName(){
		return field.getName();
	}

	/**
	 * The type of the field.
	 *
	 * @return	The type of the field.
	 */
	public Class<?> getFieldType(){
		return field.getType();
	}

	/**
	 * Set the field value.
	 *
	 * @param obj	The object in which the value is to be loaded.
	 * @param value	The value.
	 */
	public void setFieldValue(final Object obj, final Object value){
		ReflectionHelper.setValue(obj, field, value);
	}

	/**
	 * The annotation bound to the field.
	 *
	 * @return	The annotation bound to the field.
	 */
	public Evaluate getBinding(){
		return binding;
	}

	@Override
	public String toString(){
		return "EvaluatedField{" + "field=" + field + ", binding=" + binding + '}';
	}
}
