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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;

import java.lang.reflect.Field;


/** Data associated to an annotated field. */
@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
final class ConfigFieldData{

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


	ConfigFieldData(final Field field, final String annotationName){
		this.field = field;
		this.annotationName = annotationName;
	}

	void setProtocolMinMaxVersions(final String minProtocol, final String maxProtocol){
		this.minProtocol = minProtocol;
		this.maxProtocol = maxProtocol;
	}

	void setMinMaxValues(final String minValue, final String maxValue){
		this.minValue = minValue;
		this.maxValue = maxValue;
	}

	/**
	 * The name of the annotation.
	 *
	 * @return	The name of the annotation.
	 */
	String getAnnotationName(){
		return annotationName;
	}

	/**
	 * The name of the configuration field.
	 *
	 * @return	The name of the configuration field.
	 */
	String getFieldName(){
		return field.getName();
	}

	/**
	 * The type of the configuration field.
	 *
	 * @return	The type of the configuration field.
	 */
	Class<?> getFieldType(){
		return field.getType();
	}

	/**
	 * The minimum protocol of the configuration field.
	 *
	 * @return	The minimum protocol of the configuration field.
	 */
	String getMinProtocol(){
		return minProtocol;
	}

	/**
	 * The maximum protocol of the configuration field.
	 *
	 * @return	The maximum protocol of the configuration field.
	 */
	String getMaxProtocol(){
		return maxProtocol;
	}

	/**
	 * The minimum value for the configuration field.
	 *
	 * @return	The minimum value for the configuration field.
	 */
	String getMinValue(){
		return minValue;
	}

	/**
	 * The maximum value for the configuration field.
	 *
	 * @return	The maximum value for the configuration field.
	 */
	String getMaxValue(){
		return maxValue;
	}

	/**
	 * The pattern for the configuration field.
	 *
	 * @return	The pattern for the configuration field.
	 */
	String getPattern(){
		return pattern;
	}

	/**
	 * Set the pattern for the configuration field.
	 *
	 * @param pattern	The pattern.
	 */
	void setPattern(final String pattern){
		this.pattern = pattern;
	}

	/**
	 * The enumeration for the configuration field.
	 *
	 * @return	The enumeration for the configuration field.
	 */
	Class<? extends ConfigurationEnum> getEnumeration(){
		return enumeration;
	}

	/**
	 * Set the enumeration for the configuration field.
	 *
	 * @param enumeration	The enumeration.
	 */
	void setEnumeration(final Class<? extends ConfigurationEnum> enumeration){
		this.enumeration = enumeration;
	}

	/**
	 * Whether the configuration field is an enumeration.
	 *
	 * @return	Whether the configuration field is an enumeration.
	 */
	boolean hasEnumeration(){
		return hasEnumeration(enumeration);
	}

	/**
	 * The default value for the configuration field.
	 *
	 * @return	The default value for the configuration field.
	 */
	String getDefaultValue(){
		return defaultValue;
	}

	/**
	 * Set the default value for the configuration field.
	 *
	 * @param defaultValue	The default value.
	 */
	void setDefaultValue(final String defaultValue){
		this.defaultValue = defaultValue;
	}

	/**
	 * The charset for the configuration field.
	 *
	 * @return	The charset for the configuration field.
	 */
	String getCharset(){
		return charset;
	}

	/**
	 * Set the charset for the configuration field.
	 *
	 * @param charset	The charset.
	 */
	void setCharset(final String charset){
		this.charset = charset;
	}

	/**
	 * The radix for the configuration field.
	 *
	 * @return	The radix for the configuration field.
	 */
	int getRadix(){
		return radix;
	}

	/**
	 * Set the radix for the configuration field.
	 *
	 * @param radix	The radix.
	 */
	void setRadix(final int radix){
		this.radix = radix;
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
