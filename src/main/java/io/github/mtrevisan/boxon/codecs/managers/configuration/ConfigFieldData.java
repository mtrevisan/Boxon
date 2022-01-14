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
package io.github.mtrevisan.boxon.codecs.managers.configuration;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.external.configurations.ConfigurationEnum;

import java.lang.reflect.Field;


/** Data associated to an annotated field. */
public final class ConfigFieldData{

	public final Field field;
	private final String annotationName;

	public String minProtocol;
	public String maxProtocol;

	public String minValue;
	public String maxValue;

	public String pattern;
	public Class<? extends ConfigurationEnum> enumeration;

	public String defaultValue;

	public String charset;

	public int radix;


	public static ConfigFieldData create(final Field field, final ConfigurationField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.setMinMaxValues(annotation.minValue(), annotation.maxValue());
		data.pattern = annotation.pattern();
		data.enumeration = annotation.enumeration();
		data.defaultValue = annotation.defaultValue();
		data.charset = annotation.charset();
		data.radix = annotation.radix();
		return data;
	}

	public static ConfigFieldData create(final Field field, final CompositeConfigurationField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.pattern = annotation.pattern();
		data.charset = annotation.charset();
		return data;
	}

	public static ConfigFieldData create(final Field field, final CompositeSubField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.pattern = annotation.pattern();
		data.defaultValue = annotation.defaultValue();
		return data;
	}

	public static ConfigFieldData create(final Field field, final AlternativeConfigurationField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.enumeration = annotation.enumeration();
		return data;
	}

	public static ConfigFieldData create(final Field field, final AlternativeSubField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.setMinMaxValues(annotation.minValue(), annotation.maxValue());
		data.pattern = annotation.pattern();
		data.defaultValue = annotation.defaultValue();
		data.charset = annotation.charset();
		data.radix = annotation.radix();
		return data;
	}

	private ConfigFieldData(final Field field, final String annotationName){
		this.field = field;
		this.annotationName = annotationName;
	}

	private void setProtocolMinMaxVersions(final String minProtocol, final String maxProtocol){
		this.minProtocol = minProtocol;
		this.maxProtocol = maxProtocol;
	}

	private void setMinMaxValues(final String minValue, final String maxValue){
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	public String getAnnotationName(){
		return annotationName;
	}

	public Class<?> getFieldType(){
		return field.getType();
	}

	public boolean hasEnumeration(){
		return hasEnumeration(enumeration);
	}

	static boolean hasEnumeration(final Class<? extends ConfigurationEnum> enumeration){
		return (enumeration != NullEnum.class);
	}

}
