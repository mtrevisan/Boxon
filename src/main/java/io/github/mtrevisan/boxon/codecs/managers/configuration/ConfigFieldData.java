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
@SuppressWarnings("ClassWithTooManyFields")
public final class ConfigFieldData{

	private final Field field;
	private final String annotationName;

	private String minProtocol;
	private String maxProtocol;

	private String minValue;
	private String maxValue;

	private String pattern;
	private Class<? extends ConfigurationEnum> enumeration;

	private String defaultValue;

	private String charset;

	private int radix;


	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
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

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	public static ConfigFieldData create(final Field field, final CompositeConfigurationField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.pattern = annotation.pattern();
		data.charset = annotation.charset();
		return data;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	public static ConfigFieldData create(final Field field, final CompositeSubField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.pattern = annotation.pattern();
		data.defaultValue = annotation.defaultValue();
		return data;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	public static ConfigFieldData create(final Field field, final AlternativeConfigurationField annotation){
		final ConfigFieldData data = new ConfigFieldData(field, annotation.getClass().getSimpleName());
		data.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		data.enumeration = annotation.enumeration();
		return data;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
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

	/**
	 * The name of the annotation.
	 *
	 * @return	The name of the annotation.
	 */
	public String getAnnotationName(){
		return annotationName;
	}

	/**
	 * The name of the configuration field.
	 *
	 * @return	The name of the configuration field.
	 */
	public String getFieldName(){
		return field.getName();
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
	 * The minimum protocol of the configuration field.
	 *
	 * @return	The minimum protocol of the configuration field.
	 */
	public String getMinProtocol(){
		return minProtocol;
	}

	/**
	 * The maximum protocol of the configuration field.
	 *
	 * @return	The maximum protocol of the configuration field.
	 */
	public String getMaxProtocol(){
		return maxProtocol;
	}

	/**
	 * The minimum value for the configuration field.
	 *
	 * @return	The minimum value for the configuration field.
	 */
	public String getMinValue(){
		return minValue;
	}

	/**
	 * The maximum value for the configuration field.
	 *
	 * @return	The maximum value for the configuration field.
	 */
	public String getMaxValue(){
		return maxValue;
	}

	/**
	 * The pattern for the configuration field.
	 *
	 * @return	The pattern for the configuration field.
	 */
	public String getPattern(){
		return pattern;
	}

	/**
	 * The enumeration for the configuration field.
	 *
	 * @return	The enumeration for the configuration field.
	 */
	public Class<? extends ConfigurationEnum> getEnumeration(){
		return enumeration;
	}

	/**
	 * Whether the configuration field is an enumeration.
	 *
	 * @return	Whether the configuration field is an enumeration.
	 */
	public boolean hasEnumeration(){
		return hasEnumeration(enumeration);
	}

	/**
	 * The default value for the configuration field.
	 *
	 * @return	The default value for the configuration field.
	 */
	public String getDefaultValue(){
		return defaultValue;
	}

	/**
	 * The charset for the configuration field.
	 *
	 * @return	The charset for the configuration field.
	 */
	public String getCharset(){
		return charset;
	}

	/**
	 * The radix for the configuration field.
	 *
	 * @return	The radix for the configuration field.
	 */
	public int getRadix(){
		return radix;
	}

	/**
	 * Whether the given class is an enumeration.
	 *
	 * @param enumeration	The class to check.
	 * @return	Whether the given class is an enumeration.
	 */
	static boolean hasEnumeration(final Class<? extends ConfigurationEnum> enumeration){
		return (enumeration != NullEnum.class);
	}

}
