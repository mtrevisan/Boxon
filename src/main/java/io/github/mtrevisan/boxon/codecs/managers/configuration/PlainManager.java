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

import io.github.mtrevisan.boxon.external.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.codecs.LoaderConfiguration;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


final class PlainManager implements ConfigurationManagerInterface{

	static final Annotation EMPTY_ANNOTATION = () -> Annotation.class;


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
		return ManagerHelper.getDefaultValue(field, value, enumeration);
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());
	}

	@Override
	public Annotation shouldBeExtracted(final Version protocol){
		final boolean shouldBeExtracted = ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: EMPTY_ANNOTATION);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return StringHelper.isBlank(this.annotation.defaultValue());
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException,
			CodecException{
		if(!ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> fieldMap = extractMap(fieldType);

		if(protocol.isEmpty())
			ManagerHelper.extractMinMaxProtocol(annotation.minProtocol(), annotation.maxProtocol(), fieldMap);

		return fieldMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException, CodecException{
		final Map<String, Object> map = new HashMap<>(10);

		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_LONG_DESCRIPTION, annotation.longDescription(), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_UNIT_OF_MEASURE, annotation.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(),
				map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MIN_VALUE, JavaHelper.getValue(fieldType, annotation.minValue()), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MAX_VALUE, JavaHelper.getValue(fieldType, annotation.maxValue()), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_PATTERN, annotation.pattern(), map);
		ManagerHelper.extractEnumeration(fieldType, annotation.enumeration(), map);

		ManagerHelper.putValueIfNotEmpty(LoaderConfiguration.KEY_DEFAULT_VALUE, annotation.defaultValue(), fieldType,
			annotation.enumeration(), map);
		if(String.class.isAssignableFrom(fieldType))
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_CHARSET, annotation.charset(), map);

		return map;
	}

	@Override
	public void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType) throws EncodeException,
			CodecException{
		ManagerHelper.validatePattern(dataKey, dataValue, annotation.pattern());
		ManagerHelper.validateMinValue(dataKey, dataValue, fieldType, annotation.minValue());
		ManagerHelper.validateMaxValue(dataKey, dataValue, fieldType, annotation.maxValue());
	}

	@Override
	public void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field, final Version protocol)
			throws EncodeException, CodecException{
		if(dataValue == null)
			return;

		final Class<?> fieldType = field.getType();
		final Class<? extends ConfigurationEnum> enumeration = annotation.enumeration();
		if(enumeration != NullEnum.class)
			dataValue = extractEnumerationValue(dataKey, dataValue, field, enumeration);
		else if(String.class.isInstance(dataValue))
			dataValue = JavaHelper.getValue(fieldType, (String)dataValue);
		ManagerHelper.setValue(field, configurationObject, dataValue);
	}

	private static Object extractEnumerationValue(final String dataKey, Object dataValue, final Field field,
			final Class<? extends ConfigurationEnum> enumeration) throws EncodeException{
		//convert `or` between enumerations
		if(String.class.isInstance(dataValue)){
			dataValue = ManagerHelper.extractEnumerationValue(field, (String)dataValue, enumeration);
		}

		validateEnumerationValue(dataKey, dataValue, enumeration, field.getType());

		return dataValue;
	}

	private static void validateEnumerationValue(final String dataKey, final Object dataValue,
			final Class<? extends ConfigurationEnum> enumeration, final Class<?> fieldType) throws EncodeException{
		final Class<?> dataValueClass = (dataValue != null? dataValue.getClass(): null);
		if(dataValueClass == null){
			final Class<?> componentType = (fieldType.isArray()? fieldType.getComponentType(): fieldType);
			throw EncodeException.create("Data value incompatible with field type {}; found {}[], expected {}[] for enumeration type",
				dataKey, componentType, enumeration.getSimpleName());
		}
		if(dataValueClass.isArray()){
			final Class<?> componentType = dataValueClass.getComponentType();
			if(!enumeration.isAssignableFrom(componentType))
				throw EncodeException.create("Data value incompatible with field type {}; found {}[], expected {}[] for enumeration type",
					dataKey, componentType, enumeration.getSimpleName());
		}
		else if(!enumeration.isInstance(dataValue) || String.class.isInstance(dataValue) && !((String)dataValue).isEmpty())
			throw EncodeException.create("Data value incompatible with field type {}; found {}, expected {} for enumeration type",
				dataKey, dataValueClass, enumeration.getSimpleName());
	}

}
