package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.core.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;


interface ConfigurationManagerInterface{

	String getShortDescription();

	Object getDefaultValue(final Field field, final Version protocol) throws EncodeException;

	void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries);

	Annotation shouldBeExtracted(final Version protocol);

	boolean isMandatory(final Annotation annotation);

	Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException;

	void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType) throws EncodeException;

	void setValue(final Object configurationObject, final String dataKey, final Object dataValue, final Field field, final Version protocol)
		throws EncodeException;

}
