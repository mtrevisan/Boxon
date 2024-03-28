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

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper.putIfNotEmpty;


final class PlainManager implements ConfigurationManagerInterface{

	static final Annotation EMPTY_ANNOTATION = () -> Annotation.class;

	private static final String ARRAY_VARIABLE = "[]";


	private final ConfigurationField annotation;


	PlainManager(final ConfigurationField annotation){
		this.annotation = annotation;
	}


	@Override
	public String getShortDescription(){
		return annotation.shortDescription();
	}

	@Override
	public Object getDefaultValue(final Field field, final Version protocol) throws CodecException{
		final String value = annotation.defaultValue();
		final Class<? extends ConfigurationEnum> enumeration = annotation.enumeration();
		return ConfigurationHelper.convertValue(value, field.getType(), enumeration);
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());
	}

	@Override
	public Annotation annotationToBeProcessed(final Version protocol){
		final boolean shouldBeExtracted = ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: EMPTY_ANNOTATION);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return StringHelper.isBlank(this.annotation.defaultValue());
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws CodecException,
			ConfigurationException{
		if(!ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> fieldMap = extractMap(fieldType);

		if(protocol.isEmpty())
			ConfigurationHelper.extractMinMaxProtocol(annotation.minProtocol(), annotation.maxProtocol(), fieldMap);

		return Collections.unmodifiableMap(fieldMap);
	}

	@SuppressWarnings("DuplicatedCode")
	private Map<String, Object> extractMap(final Class<?> fieldType) throws CodecException, ConfigurationException{
		final Map<String, Object> map = new HashMap<>(9);

		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, annotation.longDescription(), map);
		putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, annotation.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(ConfigurationKey.FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(), map);
		putIfNotEmpty(ConfigurationKey.MIN_VALUE, JavaHelper.convertToBigDecimal(annotation.minValue()), map);
		putIfNotEmpty(ConfigurationKey.MAX_VALUE, JavaHelper.convertToBigDecimal(annotation.maxValue()), map);
		putIfNotEmpty(ConfigurationKey.PATTERN, annotation.pattern(), map);
		ConfigurationHelper.extractEnumeration(fieldType, annotation.enumeration(), map);

		final Object defaultValue = ConfigurationHelper.convertValue(annotation.defaultValue(), fieldType, annotation.enumeration());
		putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, defaultValue, map);
		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(ConfigurationKey.CHARSET, annotation.charset(), map);

		return map;
	}

	@Override
	public void validateValue(final Field field, final String dataKey, final Object dataValue) throws AnnotationException{
		final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, annotation);
		ValidationHelper.validatePattern(configData, dataValue);
		ValidationHelper.validateMinMaxDataValues(configData, dataValue);
	}

	@Override
	public Object convertValue(final Field field, final String dataKey, Object dataValue, final Version protocol) throws CodecException,
			EncodeException{
		if(dataValue != null){
			final Class<?> fieldType = field.getType();
			final Class<? extends ConfigurationEnum> enumeration = annotation.enumeration();
			if(ConfigFieldData.hasEnumeration(enumeration))
				dataValue = extractEnumerationValue(dataKey, dataValue, field, enumeration);
			else if(dataValue instanceof final String v)
				dataValue = ParserDataType.getValue(fieldType, v);
		}
		return dataValue;
	}

	private static Object extractEnumerationValue(final String dataKey, Object dataValue, final Field field,
			final Class<? extends ConfigurationEnum> enumeration) throws EncodeException{
		final Class<?> fieldType = field.getType();

		//convert `or` between enumerations
		if(dataValue instanceof final String v)
			dataValue = ConfigurationHelper.extractEnumerationValue(fieldType, v, enumeration);

		validateEnumerationValue(dataKey, dataValue, enumeration, fieldType);

		return dataValue;
	}

	private static void validateEnumerationValue(final String dataKey, final Object dataValue,
			final Class<? extends ConfigurationEnum> enumeration, final Class<?> fieldType) throws EncodeException{
		if(dataValue == null)
			throw EncodeException.create("Data value incompatible with field type {}; found {}, expected {}[] for enumeration type",
				dataKey, getFieldBaseType(fieldType), enumeration.getSimpleName());

		final Class<?> dataValueClass = dataValue.getClass();
		if(dataValueClass.isArray()){
			final Class<?> componentType = dataValueClass.getComponentType();
			if(!enumeration.isAssignableFrom(componentType))
				throw EncodeException.create("Data value incompatible with field type {}; found {}[], expected {}[] for enumeration type",
					dataKey, componentType, enumeration.getSimpleName());
		}
		else if(!enumeration.isInstance(dataValue) || dataValue instanceof final String v && !StringHelper.isBlank(v))
			throw EncodeException.create("Data value incompatible with field type {}; found {}, expected {} for enumeration type",
				dataKey, dataValueClass, enumeration.getSimpleName());
	}

	private static String getFieldBaseType(final Class<?> fieldType){
		return (fieldType.isArray()
			? fieldType.getComponentType() + ARRAY_VARIABLE
			: fieldType.toString());
	}

}
