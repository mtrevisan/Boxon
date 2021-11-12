package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Map;


final class ManagerHelper{

	private ManagerHelper(){}

	static void putIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key, final Object value)
			throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !JavaHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	static void putValueIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key, final Class<?> fieldType,
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

	static void setValue(final Field field, final Object configurationObject, final Object dataValue){
		ReflectionHelper.setFieldValue(field, configurationObject, dataValue);
	}

	static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}

}
