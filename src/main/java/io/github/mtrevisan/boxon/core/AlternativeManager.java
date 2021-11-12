package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


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
	public Object getDefaultValue(final Field field, final Version protocol){
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			final Class<? extends Enum<?>> enumeration = annotation.enumeration();
			final String value = fieldBinding.defaultValue();
			if(enumeration != NullEnum.class){
				final Object valEnum;
				final Enum<?>[] enumConstants = enumeration.getEnumConstants();
				if(field.getType().isArray()){
					final String[] defaultValues = JavaHelper.split(value, '|', -1);
					valEnum = Array.newInstance(enumeration, defaultValues.length);
					for(int i = 0; i < defaultValues.length; i ++)
						Array.set(valEnum, i, JavaHelper.extractEnum(enumConstants, defaultValues[i]));
				}
				else
					valEnum = enumeration
						.cast(JavaHelper.extractEnum(enumConstants, value));
				return valEnum;
			}
			if(field.getType() != String.class)
				return JavaHelper.getValue(field.getType(), value);
			return value;
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
			if(shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				match = fieldBinding;
		}

		final boolean shouldBeExtracted = (match != null && shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()));
		return (shouldBeExtracted? match: PlainManager.EMPTY_ANNOTATION);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return (!isEmptyAnnotation(annotation) && JavaHelper.isBlank(((AlternativeSubField)annotation).defaultValue()));
	}

	private static boolean isEmptyAnnotation(final Annotation annotation){
		return (annotation.annotationType() == Annotation.class);
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException{
		if(!shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
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

				putIfNotEmpty(fieldMap, LoaderConfiguration.KEY_MIN_PROTOCOL, alternativeField.minProtocol());
				putIfNotEmpty(fieldMap, LoaderConfiguration.KEY_MAX_PROTOCOL, alternativeField.maxProtocol());
				putValueIfNotEmpty(fieldMap, LoaderConfiguration.KEY_DEFAULT_VALUE, fieldType, annotation.enumeration(),
					alternativeField.defaultValue());

				fieldMap.putAll(alternativeMap);

				alternatives.add(fieldMap);
			}
			alternativesMap = new HashMap<>(3);
			alternativesMap.put(LoaderConfiguration.KEY_ALTERNATIVES, alternatives);
			putIfNotEmpty(alternativesMap, LoaderConfiguration.KEY_MIN_PROTOCOL, annotation.minProtocol());
			putIfNotEmpty(alternativesMap, LoaderConfiguration.KEY_MAX_PROTOCOL, annotation.maxProtocol());
		}
		else{
			//extract the specific alternative, because it was requested the configuration of a particular protocol:
			final AlternativeSubField fieldBinding = extractField(protocol);
			if(fieldBinding != null){
				alternativesMap = extractMap(fieldBinding, fieldType);

				putValueIfNotEmpty(alternativesMap, LoaderConfiguration.KEY_DEFAULT_VALUE, fieldType, annotation.enumeration(),
					fieldBinding.defaultValue());

				alternativesMap.putAll(alternativeMap);

			}
		}
		return alternativesMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, LoaderConfiguration.KEY_LONG_DESCRIPTION, annotation.longDescription());
		putIfNotEmpty(map, LoaderConfiguration.KEY_UNIT_OF_MEASURE, annotation.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		if(annotation.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = annotation.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, LoaderConfiguration.KEY_ENUMERATION, enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, LoaderConfiguration.KEY_MUTUALLY_EXCLUSIVE, true);
		}

		return map;
	}

	private static Map<String, Object> extractMap(final AlternativeSubField binding, final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, LoaderConfiguration.KEY_LONG_DESCRIPTION, binding.longDescription());
		putIfNotEmpty(map, LoaderConfiguration.KEY_UNIT_OF_MEASURE, binding.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, LoaderConfiguration.KEY_MIN_VALUE, JavaHelper.getValue(fieldType, binding.minValue()));
		putIfNotEmpty(map, LoaderConfiguration.KEY_MAX_VALUE, JavaHelper.getValue(fieldType, binding.maxValue()));
		putIfNotEmpty(map, LoaderConfiguration.KEY_PATTERN, binding.pattern());

		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, LoaderConfiguration.KEY_CHARSET, binding.charset());

		return map;
	}

	private static void putIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Object value) throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !JavaHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	private static void putValueIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Class<?> fieldType, final Class<? extends Enum<?>> enumeration, final String value) throws ConfigurationException{
		if(!JavaHelper.isBlank(value)){
			Object val = value;
			if(enumeration != NullEnum.class && fieldType.isArray())
				val = JavaHelper.split(value, '|', -1);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			if(map.put(key, val) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	private AlternativeSubField extractField(final Version protocol){
		final AlternativeSubField[] alternativeFields = annotation.value();
		for(int i = 0; i < alternativeFields.length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];
			if(shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				return fieldBinding;
		}
		return EMPTY_ALTERNATIVE;
	}

	@Override
	public void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType){}

	@Override
	public void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field, final Version protocol)
			throws EncodeException{
		final AlternativeSubField fieldBinding = extractField(protocol);
		if(fieldBinding != null){
			validateValue(fieldBinding, dataKey, dataValue, field.getType());

			if(String.class.isInstance(dataValue))
				dataValue = JavaHelper.getValue(field.getType(), (String)dataValue);
			setValue(field, configurationObject, dataValue);
		}
	}

	@SuppressWarnings("ConstantConditions")
	private static void validateValue(final AlternativeSubField binding, final String dataKey, final Object dataValue,
			final Class<?> fieldType) throws EncodeException{
		//check pattern
		final String pattern = binding.pattern();
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//value compatible with data type and pattern
			if(!String.class.isInstance(dataValue) || !formatPattern.matcher((CharSequence)dataValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					dataKey, dataValue, pattern);
		}
		//check minValue
		final String minValue = binding.minValue();
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(fieldType, minValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, minValue.getClass().getSimpleName());
		}
		//check maxValue
		final String maxValue = binding.maxValue();
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(fieldType, maxValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, maxValue.getClass().getSimpleName());
		}
	}

	private static void setValue(final Field field, final Object configurationObject, final Object dataValue){
		ReflectionHelper.setFieldValue(field, configurationObject, dataValue);
	}

	private static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}

}
