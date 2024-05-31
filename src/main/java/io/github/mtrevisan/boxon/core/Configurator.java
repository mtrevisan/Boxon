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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.BitWriter;
import io.github.mtrevisan.boxon.core.helpers.FieldMapper;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManager;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.ProtocolException;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.semanticversioning.VersionBuilder;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper.putIfNotEmpty;


/**
 * Declarative configurator for binary encoded configuration data.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Configurator{

	private final ConfigurationParser configurationParser;


	/**
	 * Create a configurator.
	 *
	 * @param core	The parser core.
	 * @return	A describer.
	 */
	public static Configurator create(final Core core){
		return new Configurator(core);
	}


	private Configurator(final Core core){
		configurationParser = core.getConfigurationParser();
	}


	/**
	 * Retrieve all the configuration regardless the protocol version.
	 *
	 * @return	The configuration messages regardless the protocol version.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 */
	public List<Map<String, Object>> getConfigurations() throws CodecException, ConfigurationException{
		final List<ConfigurationMessage<?>> configurationValues = configurationParser.getConfigurations();
		return extractConfigurations(configurationValues, Version.EMPTY);
	}


	/**
	 * Retrieve all the protocol version boundaries.
	 *
	 * @return	The protocol version boundaries.
	 */
	public List<String> getProtocolVersionBoundaries(){
		final List<ConfigurationMessage<?>> configurationValues = configurationParser.getConfigurations();
		final ArrayList<String> protocolVersionBoundaries = new ArrayList<>(configurationValues.size());
		for(final ConfigurationMessage<?> configuration : configurationValues)
			protocolVersionBoundaries.addAll(configuration.getProtocolVersionBoundaries());
		protocolVersionBoundaries.trimToSize();
		return Collections.unmodifiableList(protocolVersionBoundaries);
	}


	/**
	 * Retrieve all the configuration given a protocol version.
	 *
	 * @param protocolVersion	The protocol version used to extract the configurations.
	 * @return	The configuration messages for a given protocol version.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 */
	public List<Map<String, Object>> getConfigurations(final String protocolVersion) throws CodecException, ConfigurationException{
		final Version protocol = VersionBuilder.of(protocolVersion);
		if(protocol.isEmpty())
			throw ProtocolException.create("Invalid protocol version: {}", protocolVersion);

		final List<ConfigurationMessage<?>> configurationValues = configurationParser.getConfigurations();
		return extractConfigurations(configurationValues, protocol);
	}

	/**
	 * Extracts the configurations from a list of {@link ConfigurationMessage} objects based on the specified protocol version.
	 *
	 * @param configurationValues	The list of {@link ConfigurationMessage} objects containing the configurations.
	 * @param protocol	The protocol version used to extract the configurations.
	 * @return	A list of maps representing the extracted configurations. Each map contains the header and fields of a configuration
	 * 	message.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 */
	private static List<Map<String, Object>> extractConfigurations(final Collection<ConfigurationMessage<?>> configurationValues,
			final Version protocol) throws CodecException, ConfigurationException{
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final ConfigurationMessage<?> configuration : configurationValues){
			final ConfigurationHeader header = configuration.getHeader();
			if(!ConfigurationHelper.shouldBeExtracted(protocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> map = new HashMap<>(3);
			final Map<String, Object> headerMap = extractMap(protocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(protocol, configuration);
			putIfNotEmpty(ConfigurationKey.HEADER, headerMap, map);
			putIfNotEmpty(ConfigurationKey.FIELDS, fieldsMap, map);
			if(protocol.isEmpty()){
				final List<String> protocolVersionBoundaries = configuration.getProtocolVersionBoundaries();
				putIfNotEmpty(ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES, protocolVersionBoundaries, map);
			}
			response.add(map);
		}
		return Collections.unmodifiableList(response);
	}

	/**
	 * Extracts a map of fields from a configuration header based on the specified protocol version.
	 *
	 * @param protocol	The version of the protocol.
	 * @param header	The configuration header.
	 * @return	A map of fields extracted from the configuration header, with the short description of each field as the key and the
	 * 	configuration map as the value.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 */
	private static Map<String, Object> extractMap(final Version protocol, final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(4);
		putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, header.shortDescription(), map);
		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, header.longDescription(), map);
		if(protocol.isEmpty()){
			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, header.minProtocol(), map);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, header.maxProtocol(), map);
		}
		return map;
	}

	/**
	 * Extracts a map of fields from a configuration message, based on the specified version of the protocol.
	 *
	 * @param protocol	The version of the protocol.
	 * @param configuration	The configuration message.
	 * @return	A map of fields extracted from the configuration message, with the short description of each field as the key and the
	 * 	configuration map as the value.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 */
	private static Map<String, Object> extractFieldsMap(final Version protocol, final ConfigurationMessage<?> configuration)
			throws CodecException, ConfigurationException{
		final List<ConfigurationField> fields = configuration.getConfigurationFields();
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(final ConfigurationField field : fields){
			final Annotation annotation = field.getBinding();
			final Class<?> fieldType = field.getFieldType();

			final ConfigurationManager manager = ConfigurationManagerFactory.buildManager(annotation);
			final Map<String, Object> fieldMap = manager.extractConfigurationMap(fieldType, protocol);
			if(!fieldMap.isEmpty())
				fieldsMap.put(manager.getShortDescription(), fieldMap);
		}
		return fieldsMap;
	}


	/**
	 * Compose a configuration message.
	 *
	 * @param protocolVersion	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param shortDescription	The short description identifying a message, see {@link ConfigurationHeader#shortDescription()}.
	 * @param template	The template, or a <a href="https://en.wikipedia.org/wiki/Data_transfer_object">DTO</a>, containing the data
	 * 	to be composed.
	 * @return	The composition response.
	 * @throws ProtocolException	If the given protocol version is not recognized.
	 */
	public Response<String, byte[]> composeConfiguration(final String protocolVersion, final String shortDescription,
			final Object template) throws ProtocolException{
		final Map<String, Object> data = FieldMapper.mapObject(template);
		return composeConfiguration(protocolVersion, shortDescription, data);
	}

	/**
	 * Compose a configuration message.
	 *
	 * @param protocolVersion	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param shortDescription	The short description identifying a message, see {@link ConfigurationHeader#shortDescription()}.
	 * @param data	The configuration message data to be composed.
	 * @return	The composition response.
	 * @throws ProtocolException	If the given protocol version is not recognized.
	 */
	public Response<String, byte[]> composeConfiguration(final String protocolVersion, final String shortDescription,
			final Map<String, Object> data) throws ProtocolException{
		final Version protocol = VersionBuilder.of(protocolVersion);
		if(protocol.isEmpty())
			throw ProtocolException.create("Invalid protocol version: {}", protocolVersion);

		final BitWriter writer = BitWriter.create();
		final EncodeException error = composeConfiguration(writer, shortDescription, data, protocol);

		return Response.create(shortDescription, writer, error);
	}

	/**
	 * Compose a single configuration message.
	 *
	 * @return	The error, if any.
	 */
	private EncodeException composeConfiguration(final BitWriterInterface writer, final String shortDescription,
			final Map<String, Object> data, final Version protocol){
		try{
			final ConfigurationMessage<?> configuration = configurationParser.getConfiguration(shortDescription);
			final Object configurationData = ConfigurationParser.getConfigurationWithDefaults(configuration, data, protocol);
			configurationParser.encode(configuration, writer, configurationData, protocol);

			return null;
		}
		catch(final BoxonException e){
			return EncodeException.create(e);
		}
	}

}
