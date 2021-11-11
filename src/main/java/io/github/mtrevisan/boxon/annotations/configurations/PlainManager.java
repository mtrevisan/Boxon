package io.github.mtrevisan.boxon.annotations.configurations;

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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


class PlainManager implements ConfigurationManagerInterface{

	private final ConfigurationField annotation;


	PlainManager(final ConfigurationField annotation){
		this.annotation = annotation;
	}

	@Override
	public final String getShortDescription(){
		return annotation.shortDescription();
	}

	@Override
	public final Object getDefaultValue(final Field field, final Version protocol){
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
	public final void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());
	}

	@Override
	public final Annotation shouldBeExtracted(final Version protocol){
		final boolean shouldBeExtracted = shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: null);
	}

	@Override
	public final boolean isMandatory(final Annotation annotation){
		return JavaHelper.isBlank(this.annotation.defaultValue());
	}

	@Override
	public final Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException{
		if(!shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return null;

		final Map<String, Object> fieldMap = extractMap(fieldType);

		if(protocol.isEmpty()){
			putIfNotEmpty(fieldMap, "minProtocol", annotation.minProtocol());
			putIfNotEmpty(fieldMap, "maxProtocol", annotation.maxProtocol());
		}
		return fieldMap;
	}

	private Map<String, Object> extractMap(final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, "longDescription", annotation.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", annotation.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, "minValue", JavaHelper.getValue(fieldType, annotation.minValue()));
		putIfNotEmpty(map, "maxValue", JavaHelper.getValue(fieldType, annotation.maxValue()));
		putIfNotEmpty(map, "pattern", annotation.pattern());
		if(annotation.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = annotation.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, "enumeration", enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, "mutuallyExclusive", true);
		}

		putValueIfNotEmpty(map, "defaultValue", fieldType, annotation.enumeration(), annotation.defaultValue());
		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, "charset", annotation.charset());

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
	public final void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType) throws EncodeException{
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
	public final void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field,
			final Version protocol) throws EncodeException{
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

			if(dataValue.getClass().isArray()){
				final Class<?> componentType = dataValue.getClass().getComponentType();
				if(!enumeration.isAssignableFrom(componentType))
					throw EncodeException.create("Data value incompatible with field type {}; found {}[], expected " + enumeration.getSimpleName() + "[] for enumeration type",
						dataKey, componentType);
			}
			else if(!enumeration.isInstance(dataValue))
				throw EncodeException.create("Data value incompatible with field type {}; found {}, expected " + enumeration.getSimpleName() + " for enumeration type",
					dataKey, dataValue.getClass());

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
