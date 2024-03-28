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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper.putIfNotEmpty;


final class AlternativeManager implements ConfigurationManagerInterface{

	private static final AlternativeSubField EMPTY_ALTERNATIVE = new NullAlternativeSubField();


	private final AlternativeConfigurationField annotation;


	AlternativeManager(final AlternativeConfigurationField annotation){
		this.annotation = annotation;
	}


	@Override
	public String getShortDescription(){
		return annotation.shortDescription();
	}

	@Override
	public Object getDefaultValue(final Field field, final Version protocol) throws CodecException{
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			final String value = fieldBinding.defaultValue();
			final Class<? extends ConfigurationEnum> enumeration = annotation.enumeration();
			return ConfigurationHelper.convertValue(value, field.getType(), enumeration);
		}
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());

		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0, length = alternativeFields.length; i < length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];

			protocolVersionBoundaries.add(fieldBinding.minProtocol());
			protocolVersionBoundaries.add(fieldBinding.maxProtocol());
		}
	}

	@Override
	public Annotation annotationToBeProcessed(final Version protocol){
		final Annotation match = findAlternative(protocol);
		final boolean shouldBeExtracted = (match != null
			&& ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()));
		return (shouldBeExtracted? match: PlainManager.EMPTY_ANNOTATION);
	}

	private Annotation findAlternative(final Version protocol){
		Annotation match = null;
		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0, length = alternativeFields.length; match == null && i < length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];

			if(ConfigurationHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				match = fieldBinding;
		}
		return match;
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return (!isEmptyAnnotation(annotation) && StringHelper.isBlank(((AlternativeSubField)annotation).defaultValue()));
	}

	private static boolean isEmptyAnnotation(final Annotation annotation){
		return (annotation.annotationType() == Annotation.class);
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws CodecException,
			ConfigurationException{
		if(!ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> alternativeMap = extractMap(fieldType);

		final Map<String, Object> alternativesMap;
		if(protocol.isEmpty())
			//extract all the alternatives, because it was requested all the configurations regardless of protocol:
			alternativesMap = extractConfigurationMapWithoutProtocol(fieldType, alternativeMap);
		else
			//extract the specific alternative, because it was requested the configuration of a particular protocol:
			alternativesMap = extractConfigurationMapWithProtocol(fieldType, alternativeMap, protocol);
		return Collections.unmodifiableMap(alternativesMap);
	}

	private Map<String, Object> extractConfigurationMapWithoutProtocol(final Class<?> fieldType, final Map<String, Object> alternativeMap)
			throws CodecException, ConfigurationException{
		final AlternativeSubField[] alternativeFields = annotation.value();
		final int length = alternativeFields.length;
		final Collection<Map<String, Object>> alternatives = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final AlternativeSubField alternativeField = alternativeFields[i];

			final Map<String, Object> fieldMap = extractMap(alternativeField, fieldType);

			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, alternativeField.minProtocol(), fieldMap);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, alternativeField.maxProtocol(), fieldMap);
			final Object defaultValue = ConfigurationHelper.convertValue(alternativeField.defaultValue(), fieldType, annotation.enumeration());
			putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, defaultValue, fieldMap);

			fieldMap.putAll(alternativeMap);

			alternatives.add(fieldMap);
		}
		final Map<String, Object> alternativesMap = new HashMap<>(3);
		putIfNotEmpty(ConfigurationKey.ALTERNATIVES, alternatives, alternativesMap);
		putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, annotation.minProtocol(), alternativesMap);
		putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, annotation.maxProtocol(), alternativesMap);
		return alternativesMap;
	}

	private Map<String, Object> extractConfigurationMapWithProtocol(final Class<?> fieldType, final Map<String, Object> alternativeMap,
			final Version protocol) throws CodecException, ConfigurationException{
		final Map<String, Object> alternativesMap;
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			alternativesMap = new HashMap<>(alternativeMap.size() + 2);

			alternativesMap.putAll(extractMap(fieldBinding, fieldType));

			final Object defaultValue = ConfigurationHelper.convertValue(fieldBinding.defaultValue(), fieldType, annotation.enumeration());
			putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, defaultValue, alternativesMap);

			alternativesMap.putAll(alternativeMap);
		}
		else
			alternativesMap = Collections.emptyMap();
		return alternativesMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(4);

		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, annotation.longDescription(), map);
		putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, annotation.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(ConfigurationKey.FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(), map);
		ConfigurationHelper.extractEnumeration(fieldType, annotation.enumeration(), map);

		return map;
	}

	@SuppressWarnings("DuplicatedCode")
	private static Map<String, Object> extractMap(final AlternativeSubField binding, final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(7);

		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), map);
		putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, binding.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(ConfigurationKey.FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(), map);
		putIfNotEmpty(ConfigurationKey.MIN_VALUE, JavaHelper.convertToBigDecimal(binding.minValue()), map);
		putIfNotEmpty(ConfigurationKey.MAX_VALUE, JavaHelper.convertToBigDecimal(binding.maxValue()), map);
		putIfNotEmpty(ConfigurationKey.PATTERN, binding.pattern(), map);

		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(ConfigurationKey.CHARSET, binding.charset(), map);

		return map;
	}

	private AlternativeSubField extractField(final Version protocol){
		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0, length = alternativeFields.length; i < length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];

			if(ConfigurationHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				return fieldBinding;
		}
		return EMPTY_ALTERNATIVE;
	}

	@Override
	public void validateValue(final Field field, final String dataKey, final Object dataValue){}

	@Override
	public Object convertValue(final Field field, final String dataKey, Object dataValue, final Version protocol) throws AnnotationException,
			CodecException{
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			final Class<?> fieldType = field.getType();
			if(dataValue instanceof final String v)
				dataValue = ParserDataType.getValue(fieldType, v);

			final ConfigFieldData configData = ConfigFieldDataBuilder.create(field, annotation);
			ValidationHelper.validatePattern(configData, dataValue);
			ValidationHelper.validateMinMaxDataValues(configData, dataValue);
		}
		return dataValue;
	}

}
