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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.StringHelper;


/**
 * A collection of convenience methods for working with validations.
 */
final class EnumerationValidator{

	static final char MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR = '|';


	private EnumerationValidator(){}


	/**
	 * Validate the enumeration.
	 *
	 * @param configData	The configuration field data.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	static void validateEnumeration(final ConfigFieldData configData) throws AnnotationException{
		if(!configData.hasEnumeration())
			return;

		//enumeration can be encoded
		final Class<? extends ConfigurationEnum<?>> enumeration = configData.getEnumeration();
		validateEnumeration(enumeration, configData);

		//non-empty enumeration
		final ConfigurationEnum<?>[] enumConstants = enumeration.getEnumConstants();
		validateEnumerationEmptiness(enumConstants, configData);

		validateEnumerationValues(enumConstants, configData);
	}

	private static void validateEnumeration(final Class<? extends ConfigurationEnum<?>> enumeration, final ConfigFieldData configData)
			throws AnnotationException{
		if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
			throw AnnotationException.create("Enumeration must implement interface {} in {} in field {}",
				ConfigurationEnum.class.getSimpleName(), configData.getAnnotationName(), configData.getFieldName());
	}

	private static void validateEnumerationEmptiness(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		if(enumConstants.length == 0)
			throw AnnotationException.create("Empty enum in {} in field {}", configData.getAnnotationName(), configData.getFieldName());
	}

	private static void validateEnumerationValues(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		final Class<?> fieldType = configData.getFieldType();
		if(fieldType.isArray())
			validateEnumerationMultipleValues(enumConstants, configData);
		else
			validateEnumerationMutuallyExclusive(enumConstants, configData);
	}

	private static void validateEnumerationMultipleValues(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		//enumeration compatible with the variable type
		final Class<?> fieldType = configData.getFieldType();
		validateTypeCompatibility(fieldType.getComponentType(), configData);

		//default value(s) compatible with enumeration
		validateEnumerationCompatibility(enumConstants, configData);
	}

	private static void validateEnumerationCompatibility(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		final String[] defaultValues = StringHelper.split(configData.getDefaultValue(), MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR);
		for(int i = 0, length = defaultValues.length; i < length; i ++){
			final String defaultValue = defaultValues[i];

			final ConfigurationEnum<?> enumValue = ConfigurationEnum.extractEnum(enumConstants, defaultValue);
			validateDefaultValueCompatibilityWithEnumeration(enumConstants, enumValue, defaultValue, configData);
		}
	}

	private static void validateDefaultValueCompatibilityWithEnumeration(final ConfigurationEnum<?>[] enumConstants,
			final ConfigurationEnum<?> enumValue, final String defaultValue, final ConfigFieldData configData) throws AnnotationException{
		if(enumValue == null)
			throw AnnotationException.createDefaultValueAsEnumeration(configData.getAnnotationName(), defaultValue, enumConstants);
	}

	private static void validateEnumerationMutuallyExclusive(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		//enumeration compatible with the variable type
		final Class<?> fieldType = configData.getFieldType();
		validateTypeCompatibility(fieldType, configData);

		validateFieldValueCompatibility(enumConstants, configData);
	}

	private static void validateTypeCompatibility(final Class<?> fieldType, final ConfigFieldData configData) throws AnnotationException{
		if(!fieldType.isAssignableFrom(configData.getEnumeration()))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				configData.getAnnotationName(), configData.getEnumeration().getSimpleName(), fieldType.toString());
	}

	private static void validateFieldValueCompatibility(final ConfigurationEnum<?>[] enumConstants, final ConfigFieldData configData)
			throws AnnotationException{
		final String defaultValue = configData.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue) && ConfigurationEnum.extractEnum(enumConstants, defaultValue) == null)
			throw AnnotationException.createDefaultValueAsEnumeration(configData.getAnnotationName(), defaultValue, enumConstants);
	}

}
