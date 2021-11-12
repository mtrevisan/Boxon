package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


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
	public Object getDefaultValue(final Field field, final Version protocol){
		final String value = annotation.defaultValue();
		if(!JavaHelper.isBlank(value)){
			final Class<? extends Enum<?>> enumeration = annotation.enumeration();
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
		}
		return value;
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());
	}

	@Override
	public Annotation shouldBeExtracted(final Version protocol){
		final boolean shouldBeExtracted = shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: EMPTY_ANNOTATION);
	}

	@Override
	public boolean isMandatory(final Annotation annotation){
		return JavaHelper.isBlank(this.annotation.defaultValue());
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException{
		if(!shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> fieldMap = extractMap(fieldType);

		if(protocol.isEmpty()){
			putIfNotEmpty(fieldMap, LoaderConfiguration.KEY_MIN_PROTOCOL, annotation.minProtocol());
			putIfNotEmpty(fieldMap, LoaderConfiguration.KEY_MAX_PROTOCOL, annotation.maxProtocol());
		}
		return fieldMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, LoaderConfiguration.KEY_LONG_DESCRIPTION, annotation.longDescription());
		putIfNotEmpty(map, LoaderConfiguration.KEY_UNIT_OF_MEASURE, annotation.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, LoaderConfiguration.KEY_FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, LoaderConfiguration.KEY_MIN_VALUE, JavaHelper.getValue(fieldType, annotation.minValue()));
		putIfNotEmpty(map, LoaderConfiguration.KEY_MAX_VALUE, JavaHelper.getValue(fieldType, annotation.maxValue()));
		putIfNotEmpty(map, LoaderConfiguration.KEY_PATTERN, annotation.pattern());
		if(annotation.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = annotation.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, LoaderConfiguration.KEY_ENUMERATION, enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, LoaderConfiguration.KEY_MUTUALLY_EXCLUSIVE, true);
		}

		putValueIfNotEmpty(map, LoaderConfiguration.KEY_DEFAULT_VALUE, fieldType, annotation.enumeration(), annotation.defaultValue());
		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, LoaderConfiguration.KEY_CHARSET, annotation.charset());

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

	@Override
	@SuppressWarnings("ConstantConditions")
	public void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType) throws EncodeException{
		//check pattern
		final String pattern = annotation.pattern();
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//value compatible with data type and pattern
			if(!String.class.isInstance(dataValue) || !formatPattern.matcher((CharSequence)dataValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					dataKey, dataValue, pattern);
		}
		//check minValue
		final String minValue = annotation.minValue();
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(fieldType, minValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, minValue.getClass().getSimpleName());
		}
		//check maxValue
		final String maxValue = annotation.maxValue();
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(fieldType, maxValue);
			if(Number.class.isInstance(dataValue) && ((Number)dataValue).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}",
					dataKey, dataValue, maxValue.getClass().getSimpleName());
		}
	}

	@Override
	public void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field, final Version protocol)
			throws EncodeException{
		if(dataValue == null)
			return;

		final Class<?> fieldType = field.getType();
		final Class<? extends Enum<?>> enumeration = annotation.enumeration();
		if(enumeration != NullEnum.class){
			//convert `or` between enumerations
			if(String.class.isInstance(dataValue)){
				final Enum<?>[] enumConstants = enumeration.getEnumConstants();
				if(field.getType().isArray()){
					final String[] defaultValues = JavaHelper.split((String)dataValue, '|', -1);
					dataValue = Array.newInstance(enumeration, defaultValues.length);
					for(int i = 0; i < defaultValues.length; i ++)
						Array.set(dataValue, i, JavaHelper.extractEnum(enumConstants, defaultValues[i]));
				}
				else
					dataValue = enumeration
						.cast(JavaHelper.extractEnum(enumConstants, (String)dataValue));
			}

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

			setValue(field, configurationObject, dataValue);
		}
		else if(String.class.isInstance(dataValue)){
			final Object val = JavaHelper.getValue(fieldType, (String)dataValue);
			setValue(field, configurationObject, val);
		}
		else
			setValue(field, configurationObject, dataValue);
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
