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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;


/**
 * Interface for managing configurations in a consistent manner.
 */
public interface ConfigurationManager{

	/**
	 * A short description of the field.
	 *
	 * @return	A short description of the field.
	 */
	String getShortDescription();

	/**
	 * The default value of the given field assuming the given protocol number.
	 *
	 * @param fieldType	The field class from which to extract the default value.
	 * @param protocol	The protocol number, used to select the right {@link AlternativeSubField}.
	 * @return	The default value.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 * @throws EncodeException	If a placeholder cannot be substituted.
	 */
	Object getDefaultValue(Class<?> fieldType, Version protocol) throws CodecException, EncodeException;

	/**
	 * Add the minimum and maximum protocol versions to the collection.
	 *
	 * @param protocolVersionBoundaries	The collection to add the protocol versions to.
	 */
	void addProtocolVersionBoundaries(Collection<String> protocolVersionBoundaries);

	/**
	 * Retrieve the annotation to be processed given a protocol version.
	 *
	 * @param protocol	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @return	The annotation to be processed.
	 */
	Annotation annotationToBeProcessed(Version protocol);

	/**
	 * Whether the field is mandatory, that is, a default value is NOT present.
	 *
	 * @param annotation	The annotation.
	 * @return	Whether the field is mandatory.
	 */
	boolean isMandatory(Annotation annotation);

	/**
	 * Extract the configuration map for the given field and protocol version.
	 *
	 * @param fieldType	The field from which to extract the configuration.
	 * @param protocol	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @return	The configuration map.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 * @throws ConfigurationException	If a duplicate is found.
	 */
	Map<String, Object> extractConfigurationMap(Class<?> fieldType, Version protocol) throws CodecException, ConfigurationException;

	/**
	 * Check if the given value can be assigned and is valid for the given field type.
	 *
	 * @param field	The field.
	 * @param dataKey	The short description of the field.
	 * @param dataValue	The value to check against.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws CodecException	If an error was raised reading of interpreting the field value.
	 * @throws EncodeException	If a placeholder cannot be substituted.
	 */
	void validateValue(Field field, String dataKey, Object dataValue) throws BoxonException;

	/**
	 * Convert the given value to the type accepted by the field.
	 *
	 * @param field	The field.
	 * @param dataKey	The short description of the field.
	 * @param dataValue	The value to check against.
	 * @param protocol	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @return	The converted value.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 * @throws EncodeException	If a placeholder cannot be substituted.
	 */
	Object convertValue(Field field, String dataKey, Object dataValue, Version protocol) throws BoxonException;

}
