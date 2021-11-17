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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


final class AlternativeManager implements ConfigurationManagerInterface{

	private static final String EMPTY_STRING = "";

	private static final AlternativeSubField EMPTY_ALTERNATIVE = new AlternativeSubField(){
		@Override
		public Class<? extends Annotation> annotationType(){
			return Annotation.class;
		}

		@Override
		public String longDescription(){
			return EMPTY_STRING;
		}

		@Override
		public String unitOfMeasure(){
			return EMPTY_STRING;
		}

		@Override
		public String minProtocol(){
			return EMPTY_STRING;
		}

		@Override
		public String maxProtocol(){
			return EMPTY_STRING;
		}

		@Override
		public String minValue(){
			return EMPTY_STRING;
		}

		@Override
		public String maxValue(){
			return EMPTY_STRING;
		}

		@Override
		public String pattern(){
			return EMPTY_STRING;
		}

		@Override
		public String defaultValue(){
			return EMPTY_STRING;
		}

		@Override
		public String charset(){
			return EMPTY_STRING;
		}

		@Override
		public int radix(){
			return 0;
		}
	};


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
			final Class<? extends Enum<?>> enumeration = annotation.enumeration();
			return ManagerHelper.getDefaultValue(field, value, enumeration);
		}
		return EMPTY_STRING;
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());

		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0; i < alternativeFields.length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];
			protocolVersionBoundaries.add(fieldBinding.minProtocol());
			protocolVersionBoundaries.add(fieldBinding.maxProtocol());
		}
	}

	@Override
	public Annotation shouldBeExtracted(final Version protocol){
		Annotation match = null;
		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int j = 0; match == null && j < alternativeFields.length; j ++){
			final AlternativeSubField fieldBinding = alternativeFields[j];
			if(ManagerHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				match = fieldBinding;
		}

		final boolean shouldBeExtracted = (match != null
			&& ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()));
		return (shouldBeExtracted? match: PlainManager.EMPTY_ANNOTATION);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return (!isEmptyAnnotation(annotation) && StringHelper.isBlank(((AlternativeSubField)annotation).defaultValue()));
	}

	private static boolean isEmptyAnnotation(final Annotation annotation){
		return (annotation.annotationType() == Annotation.class);
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException,
			CodecException{
		if(!ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> alternativeMap = extractMap(fieldType);

		Map<String, Object> alternativesMap = null;
		if(protocol.isEmpty()){
			//extract all the alternatives, because it was requested all the configurations regardless of protocol:
			final AlternativeSubField[] alternativeFields = annotation.value();
			final Collection<Map<String, Object>> alternatives = new ArrayList<>(alternativeFields.length);
			for(int j = 0; j < alternativeFields.length; j ++){
				final AlternativeSubField alternativeField = alternativeFields[j];

				final Map<String, Object> fieldMap = extractMap(alternativeField, fieldType);

				ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MIN_PROTOCOL, alternativeField.minProtocol(), fieldMap);
				ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MAX_PROTOCOL, alternativeField.maxProtocol(), fieldMap);
				ManagerHelper.putValueIfNotEmpty(LoaderConfiguration.KEY_DEFAULT_VALUE, alternativeField.defaultValue(), fieldType,
					annotation.enumeration(), fieldMap);

				fieldMap.putAll(alternativeMap);

				alternatives.add(fieldMap);
			}
			alternativesMap = new HashMap<>(3);
			alternativesMap.put(LoaderConfiguration.KEY_ALTERNATIVES, alternatives);
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MIN_PROTOCOL, annotation.minProtocol(), alternativesMap);
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MAX_PROTOCOL, annotation.maxProtocol(), alternativesMap);
		}
		else{
			//extract the specific alternative, because it was requested the configuration of a particular protocol:
			final AlternativeSubField fieldBinding = extractField(protocol);
			if(fieldBinding != null){
				alternativesMap = extractMap(fieldBinding, fieldType);

				ManagerHelper.putValueIfNotEmpty(LoaderConfiguration.KEY_DEFAULT_VALUE, fieldBinding.defaultValue(), fieldType,
					annotation.enumeration(), alternativesMap);

				alternativesMap.putAll(alternativeMap);

			}
		}
		return alternativesMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_LONG_DESCRIPTION, annotation.longDescription(), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_UNIT_OF_MEASURE, annotation.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(),
				map);
		ManagerHelper.extractEnumeration(fieldType, annotation.enumeration(), map);

		return map;
	}

	private static Map<String, Object> extractMap(final AlternativeSubField binding, final Class<?> fieldType) throws ConfigurationException,
			CodecException{
		final Map<String, Object> map = new HashMap<>(6);

		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_LONG_DESCRIPTION, binding.longDescription(), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_UNIT_OF_MEASURE, binding.unitOfMeasure(), map);

		if(!fieldType.isEnum() && !fieldType.isArray())
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(),
				map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MIN_VALUE, JavaHelper.getValue(fieldType, binding.minValue()), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_MAX_VALUE, JavaHelper.getValue(fieldType, binding.maxValue()), map);
		ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_PATTERN, binding.pattern(), map);

		if(String.class.isAssignableFrom(fieldType))
			ManagerHelper.putIfNotEmpty(LoaderConfiguration.KEY_CHARSET, binding.charset(), map);

		return map;
	}

	private AlternativeSubField extractField(final Version protocol){
		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0; i < alternativeFields.length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];
			if(ManagerHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				return fieldBinding;
		}
		return EMPTY_ALTERNATIVE;
	}

	@Override
	public void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType){}

	@Override
	public void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field, final Version protocol)
			throws EncodeException, CodecException{
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			validateValue(fieldBinding, dataKey, dataValue, field.getType());

			if(String.class.isInstance(dataValue))
				dataValue = JavaHelper.getValue(field.getType(), (String)dataValue);
			ManagerHelper.setValue(field, configurationObject, dataValue);
		}
	}

	private static void validateValue(final AlternativeSubField binding, final String dataKey, final Object dataValue,
			final Class<?> fieldType) throws EncodeException, CodecException{
		final String pattern = binding.pattern();
		final String minValue = binding.minValue();
		final String maxValue = binding.maxValue();
		ManagerHelper.validateValue(dataKey, dataValue, fieldType,
			pattern, minValue, maxValue);
	}

}