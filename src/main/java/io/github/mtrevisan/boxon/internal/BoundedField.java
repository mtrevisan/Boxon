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
package io.github.mtrevisan.boxon.internal;

import io.github.mtrevisan.boxon.annotations.Skip;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/** Data associated to an annotated field. */
public final class BoundedField{

	/** NOTE: MUST match the name of the method in all the annotations that defines a condition! */
	private static final String CONDITION = "condition";

	private static final String EMPTY_STRING = "";
	private static final Skip[] EMPTY_ARRAY = new Skip[0];


	private final Field field;
	/** List of skips that happen BEFORE the reading/writing of this variable. */
	private final Skip[] skips;
	private final Annotation binding;

	private final String condition;


	BoundedField(final Field field, final Annotation binding){
		this(field, binding, null);
	}

	BoundedField(final Field field, final Annotation binding, final Skip[] skips){
		this.field = field;
		this.binding = binding;
		this.skips = (skips != null? skips.clone(): null);

		//pre-fetch condition method
		final Method conditionMethod = ReflectionHelper.getAccessibleMethod(binding.annotationType(), CONDITION, String.class);
		condition = ReflectionHelper.invokeMethod(binding, conditionMethod, EMPTY_STRING);
	}

	public String getFieldName(){
		return field.getName();
	}

	public Class<?> getFieldType(){
		return field.getType();
	}

	public <T> T getFieldValue(final Object obj){
		return ReflectionHelper.getValue(field, obj);
	}

	public void setFieldValue(final Object obj, final Object value){
		ReflectionHelper.setValue(field, obj, value);
	}

	public Skip[] getSkips(){
		return (skips != null? skips.clone(): EMPTY_ARRAY);
	}

	public Annotation getBinding(){
		return binding;
	}

	public String getCondition(){
		return condition;
	}

}
