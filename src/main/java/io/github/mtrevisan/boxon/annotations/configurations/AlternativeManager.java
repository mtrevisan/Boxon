package io.github.mtrevisan.boxon.annotations.configurations;

import io.github.mtrevisan.boxon.core.ConfigurationValidatorHelper;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class AlternativeManager implements ConfigurationManagerInterface{

	private static final String EMPTY_STRING = "";


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
		//TODO get alternative default value
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
			else if(field.getType() != String.class)
				return JavaHelper.getValue(field.getType(), value);
			else
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
			if(ConfigurationValidatorHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				match = fieldBinding;
		}

		final boolean shouldBeExtracted = (match != null
			&& ConfigurationValidatorHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()));
		return (shouldBeExtracted? match: null);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return JavaHelper.isBlank(((AlternativeSubField)annotation).defaultValue());
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException{
		if(!ConfigurationValidatorHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return null;

		final Map<String, Object> alternativeMap = extractMap(fieldType);

		Map<String, Object> alternativesMap = null;
		if(protocol.isEmpty()){
			//extract all the alternatives, because it was requested all the configurations regardless of protocol:
			final AlternativeSubField[] alternativeFields = annotation.value();
			final Collection<Map<String, Object>> alternatives = new ArrayList<>(alternativeFields.length);
			for(int j = 0; j < alternativeFields.length; j ++){
				final AlternativeSubField alternativeField = alternativeFields[j];

				final Map<String, Object> fieldMap = extractMap(alternativeField, fieldType);

				putIfNotEmpty(fieldMap, "minProtocol", alternativeField.minProtocol());
				putIfNotEmpty(fieldMap, "maxProtocol", alternativeField.maxProtocol());
				putValueIfNotEmpty(fieldMap, "defaultValue", fieldType, annotation.enumeration(), alternativeField.defaultValue());

				fieldMap.putAll(alternativeMap);

				alternatives.add(fieldMap);
			}
			alternativesMap = new HashMap<>(3);
			alternativesMap.put("alternatives", alternatives);
			putIfNotEmpty(alternativesMap, "minProtocol", annotation.minProtocol());
			putIfNotEmpty(alternativesMap, "maxProtocol", annotation.maxProtocol());
		}
		else{
			//extract the specific alternative, because it was requested the configuration of a particular protocol:
			final AlternativeSubField fieldBinding = extractField(protocol);
			if(fieldBinding != null){
				alternativesMap = extractMap(fieldBinding, fieldType);

				putValueIfNotEmpty(alternativesMap, "defaultValue", fieldType, annotation.enumeration(), fieldBinding.defaultValue());

				alternativesMap.putAll(alternativeMap);

			}
		}
		return alternativesMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", annotation.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", annotation.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		if(annotation.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = annotation.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, "enumeration", enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, "mutuallyExclusive", true);
		}

		return map;
	}

	private static Map<String, Object> extractMap(final AlternativeSubField binding, final Class<?> fieldType)
		throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, "minValue", JavaHelper.getValue(fieldType, binding.minValue()));
		putIfNotEmpty(map, "maxValue", JavaHelper.getValue(fieldType, binding.maxValue()));
		putIfNotEmpty(map, "pattern", binding.pattern());

		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, "charset", binding.charset());

		return map;
	}

	private static void putIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Object value) throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !JavaHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	private static void putValueIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key, final Class<?> fieldType,
			final Class<? extends Enum<?>> enumeration, final String value) throws ConfigurationException{
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
			if(ConfigurationValidatorHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				return fieldBinding;
		}
		return null;
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

	private void validateValue(final AlternativeSubField binding, final String dataKey, final Object dataValue, final Class<?> fieldType)
			throws EncodeException{
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

}
