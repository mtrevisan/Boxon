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
package io.github.mtrevisan.boxon.core.helpers.validators;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;

import java.lang.reflect.Field;


/**
 * Builder for the {@link ConfigFieldData configuration field data}.
 */
final class ConfigFieldDataBuilder{

	private ConfigFieldDataBuilder(){}


	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	static ConfigFieldData create(final Field field, final ConfigurationField annotation){
		final ConfigFieldData configData = new ConfigFieldData(field, annotation.annotationType().getSimpleName());
		configData.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		configData.setMinMaxValues(annotation.minValue(), annotation.maxValue());
		configData.setPattern(annotation.pattern());
		configData.setEnumeration(annotation.enumeration());
		configData.setDefaultValue(annotation.defaultValue());
		configData.setCharset(annotation.charset());
		configData.setRadix(annotation.radix());
		return configData;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	static ConfigFieldData create(final Field field, final CompositeConfigurationField annotation){
		final ConfigFieldData configData = new ConfigFieldData(field, annotation.annotationType().getSimpleName());
		configData.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		configData.setPattern(annotation.pattern());
		configData.setCharset(annotation.charset());
		return configData;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	static ConfigFieldData create(final Field field, final CompositeSubField annotation){
		final ConfigFieldData configData = new ConfigFieldData(field, annotation.annotationType().getSimpleName());
		configData.setPattern(annotation.pattern());
		configData.setDefaultValue(annotation.defaultValue());
		return configData;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	static ConfigFieldData create(final Field field, final AlternativeConfigurationField annotation){
		final ConfigFieldData configData = new ConfigFieldData(field, annotation.annotationType().getSimpleName());
		configData.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		configData.setEnumeration(annotation.enumeration());
		return configData;
	}

	/**
	 * Create an instance with the given annotation.
	 *
	 * @param field	The field.
	 * @param annotation	The annotation.
	 * @return	The created instance.
	 */
	static ConfigFieldData create(final Field field, final AlternativeSubField annotation){
		final ConfigFieldData configData = new ConfigFieldData(field, annotation.annotationType().getSimpleName());
		configData.setProtocolMinMaxVersions(annotation.minProtocol(), annotation.maxProtocol());
		configData.setMinMaxValues(annotation.minValue(), annotation.maxValue());
		configData.setPattern(annotation.pattern());
		configData.setDefaultValue(annotation.defaultValue());
		configData.setCharset(annotation.charset());
		configData.setRadix(annotation.radix());
		return configData;
	}

}
