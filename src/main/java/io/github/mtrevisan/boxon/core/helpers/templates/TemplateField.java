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
package io.github.mtrevisan.boxon.core.helpers.templates;

import io.github.mtrevisan.boxon.core.helpers.FieldMapper;
import io.github.mtrevisan.boxon.core.helpers.FieldRetriever;
import io.github.mtrevisan.boxon.core.helpers.MethodHelper;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/** Data associated with an annotated field. */
public final class TemplateField implements FieldRetriever{

	/** An empty {@code SkipCore} array. */
	private static final SkipParams[] EMPTY_SKIP_ARRAY = new SkipParams[0];


	private final Field field;
	/** List of skips that happen BEFORE the reading/writing of this variable. */
	private final SkipParams[] skips;
	private final Annotation binding;
	private final Annotation collectionBinding;

	private String condition;


	static TemplateField create(final Field field, final Annotation binding){
		return new TemplateField(field, binding, null, Collections.emptyList());
	}

	static TemplateField create(final Field field, final Annotation binding, final Annotation collectionBinding,
			final List<SkipParams> skips){
		return new TemplateField(field, binding, collectionBinding, skips);
	}


	private TemplateField(final Field field, final Annotation binding, final Annotation collectionBinding, final List<SkipParams> skips){
		Objects.requireNonNull(skips, "Skips must not be null");

		this.field = field;
		this.binding = binding;
		this.collectionBinding = collectionBinding;
		this.skips = (!skips.isEmpty()? skips.toArray(EMPTY_SKIP_ARRAY): EMPTY_SKIP_ARRAY);

		if(binding != null){
			//pre-fetch condition method
			final Method conditionMethod = MethodHelper.getAccessibleMethodFromClassHierarchy(binding.annotationType(),
				DescriberKey.CONDITION.toString(), String.class);
			condition = MethodHelper.invokeMethod(binding, conditionMethod, JavaHelper.EMPTY_STRING);
		}
	}


	/**
	 * Returns the field associated with this object.
	 *
	 * @return	The field associated with this object.
	 */
	public Field getField(){
		return field;
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

	@Override
	public Object getFieldValue(final Object obj){
		return FieldMapper.getFieldValue(obj, field);
	}

	/**
	 * The skips that must be made before reading the field value.
	 *
	 * @return	The annotations of the skips that must be made before reading the field value.
	 */
	public SkipParams[] getSkips(){
		return skips.clone();
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
	 * The collection annotation bound to the field.
	 *
	 * @return	The collection annotation bound to the field.
	 */
	public Annotation getCollectionBinding(){
		return collectionBinding;
	}

	/**
	 * The condition under which this field should be read.
	 *
	 * @return	The condition under which this field should be read.
	 */
	public String getCondition(){
		return condition;
	}

}
