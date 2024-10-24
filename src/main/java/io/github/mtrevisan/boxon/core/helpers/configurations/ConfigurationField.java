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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.core.helpers.FieldMapper;
import io.github.mtrevisan.boxon.core.helpers.FieldRetriever;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;


/** Data associated with an annotated field. */
public final class ConfigurationField implements FieldRetriever{

	/** An empty {@code ConfigurationSkip} array. */
	private static final ConfigurationSkip[] EMPTY_CONFIGURATION_SKIP_ARRAY = new ConfigurationSkip[0];


	private final Field field;
	/** List of skips that happen BEFORE the reading/writing of this variable. */
	private final ConfigurationSkip[] skips;
	private final Annotation binding;


	static ConfigurationField create(final Field field, final Annotation binding, final ConfigurationSkip[] skips){
		return new ConfigurationField(field, binding, skips);
	}


	private ConfigurationField(final Field field, final Annotation binding, final ConfigurationSkip[] skips){
		Objects.requireNonNull(skips, "Configuration skips must not be null");

		this.field = field;
		this.binding = binding;
		this.skips = (skips.length > 0? skips.clone(): EMPTY_CONFIGURATION_SKIP_ARRAY);
	}


	/**
	 * The configuration field.
	 *
	 * @return	The configuration field.
	 */
	public Field getField(){
		return field;
	}

	/**
	 * The type of the configuration field.
	 *
	 * @return	The type of the configuration field.
	 */
	public Class<?> getFieldType(){
		return field.getType();
	}

	/**
	 * The name of the configuration field.
	 *
	 * @return	The name of the configuration field.
	 */
	public String getFieldName(){
		return field.getName();
	}

	@Override
	public Object getFieldValue(final Object obj){
		return FieldMapper.getFieldValue(obj, field);
	}

	/**
	 * The skips that must be made before the field value.
	 *
	 * @return	The annotations of the skips that must be made before the field value.
	 */
	public ConfigurationSkip[] getSkips(){
		return skips;
	}

	/**
	 * The annotation bound to the field.
	 *
	 * @return	The annotation bound to the field.
	 */
	public Annotation getBinding(){
		return binding;
	}

}
