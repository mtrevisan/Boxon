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

import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.core.helpers.FieldMapper;
import io.github.mtrevisan.boxon.core.helpers.FieldRetriever;
import io.github.mtrevisan.boxon.core.helpers.MethodHelper;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


/** Data associated with an annotated field. */
public final class TemplateField implements FieldRetriever{

	/** An empty {@code SkipCore} array. */
	private static final SkipParams[] EMPTY_SKIP_ARRAY = new SkipParams[0];


	private final Field field;
	/** List of skips that happen BEFORE the reading/writing of this variable. */
	private SkipParams[] skips = EMPTY_SKIP_ARRAY;
	private final Annotation binding;
	private Annotation collectionBinding;
	private List<ContextParameter> contextParameters = Collections.emptyList();

	private String condition;


	static TemplateField create(final Field field, final Annotation binding){
		return new TemplateField(field, binding);
	}


	private TemplateField(final Field field, final Annotation binding){
		this.field = field;
		this.binding = binding;

		if(binding != null){
			//pre-fetch condition method
			final Method conditionMethod = MethodHelper.getAccessibleMethodFromClassHierarchy(binding.annotationType(),
				DescriberKey.CONDITION.toString(), String.class);
			condition = MethodHelper.invokeMethod(binding, conditionMethod, JavaHelper.EMPTY_STRING);
		}
	}


	TemplateField withCollectionBinding(final Annotation collectionBinding){
		this.collectionBinding = collectionBinding;

		return this;
	}

	TemplateField withSkips(final List<SkipParams> skips){
		this.skips = (skips != null && !skips.isEmpty()? skips.toArray(EMPTY_SKIP_ARRAY): EMPTY_SKIP_ARRAY);

		return this;
	}

	TemplateField withContextParameters(final Collection<ContextParameter> contextParameters){
		this.contextParameters = List.copyOf(contextParameters);

		return this;
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
		return skips;
	}

	/**
	 * The parameter annotations associated with this object.
	 *
	 * @return	The parameter annotations associated with this object.
	 */
	public List<ContextParameter> getContextParameters(){
		return Collections.unmodifiableList(contextParameters);
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


	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final TemplateField other = (TemplateField)obj;
		return (Objects.equals(field, other.field)
			&& Objects.deepEquals(skips, other.skips)
			&& Objects.equals(binding, other.binding)
			&& Objects.equals(collectionBinding, other.collectionBinding)
			&& Objects.equals(contextParameters, other.contextParameters)
			&& Objects.equals(condition, other.condition));
	}

	@Override
	public int hashCode(){
		int result = field.hashCode();
		result = 31 * result + Arrays.hashCode(skips);
		result = 31 * result + Objects.hashCode(binding);
		result = 31 * result + Objects.hashCode(collectionBinding);
		result = 31 * result + Objects.hashCode(contextParameters);
		result = 31 * result + Objects.hashCode(condition);
		return result;
	}

}
