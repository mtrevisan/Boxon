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
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitWriter;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Declarative configurator for binary encoded configuration data.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Configurator{

	private final ConfigurationParser configurationParser;
	private final Evaluator evaluator;


	/**
	 * Create a configurator.
	 *
	 * @param core	The parser core.
	 * @return	A descriptor.
	 */
	public static Configurator create(final Core core){
		return new Configurator(core);
	}


	private Configurator(final Core core){
		configurationParser = core.getConfigurationParser();
		evaluator = core.getEvaluator();
	}


	/**
	 * Retrieve all the configuration regardless the protocol version.
	 *
	 * @return	The configuration messages regardless the protocol version.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 */
	public List<Map<String, Object>> getConfigurations() throws ConfigurationException, CodecException{
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
		final int length = configurationValues.size();
		final List<String> protocolVersionBoundaries = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigurationMessage<?> configuration = configurationValues.get(i);

			protocolVersionBoundaries.addAll(configuration.getProtocolVersionBoundaries());
		}
		return Collections.unmodifiableList(protocolVersionBoundaries);
	}


	/**
	 * Retrieve all the configuration given a protocol version.
	 *
	 * @param protocol	The protocol used to extract the configurations.
	 * @return	The configuration messages for a given protocol version.
	 * @throws ConfigurationException	Thrown when a duplicated short description is found.
	 * @throws CodecException	Thrown when the value as a string cannot be interpreted as a basic type.
	 */
	public List<Map<String, Object>> getConfigurations(final String protocol) throws ConfigurationException, CodecException{
		if(StringHelper.isBlank(protocol))
			throw new IllegalArgumentException(StringHelper.format("Invalid protocol: {}", protocol));

		final List<ConfigurationMessage<?>> configurationValues = configurationParser.getConfigurations();
		final Version currentProtocol = Version.of(protocol);
		return extractConfigurations(configurationValues, currentProtocol);
	}

	private static List<Map<String, Object>> extractConfigurations(final List<ConfigurationMessage<?>> configurationValues,
			final Version protocol) throws ConfigurationException, CodecException{
		final int length = configurationValues.size();
		final List<Map<String, Object>> response = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigurationMessage<?> configuration = configurationValues.get(i);

			final ConfigurationHeader header = configuration.getHeader();
			if(!ConfigurationHelper.shouldBeExtracted(protocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> map = new HashMap<>(3);
			final Map<String, Object> headerMap = extractMap(protocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(protocol, configuration);
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.HEADER, headerMap, map);
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.FIELDS, fieldsMap, map);
			if(protocol.isEmpty()){
				final List<String> protocolVersionBoundaries = configuration.getProtocolVersionBoundaries();
				ConfigurationHelper.putIfNotEmpty(ConfigurationKey.PROTOCOL_VERSION_BOUNDARIES, protocolVersionBoundaries, map);
			}
			response.add(map);
		}
		return Collections.unmodifiableList(response);
	}

	private static Map<String, Object> extractMap(final Version protocol, final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, header.shortDescription(), map);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, header.longDescription(), map);
		if(protocol.isEmpty()){
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, header.minProtocol(), map);
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, header.maxProtocol(), map);
		}
		return map;
	}

	private static Map<String, Object> extractFieldsMap(final Version protocol, final ConfigurationMessage<?> configuration)
			throws ConfigurationException, CodecException{
		final List<ConfigField> fields = configuration.getConfigurationFields();
		final int length = fields.size();
		final Map<String, Object> fieldsMap = new HashMap<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigField field = fields.get(i);

			final Annotation annotation = field.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
			final Map<String, Object> fieldMap = manager.extractConfigurationMap(field.getFieldType(), protocol);
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
	 * @param template	The template, or a <a href="https://en.wikipedia.org/wiki/Data_transfer_object">DTO</a>, containing the data to be composed.
	 * @return	The composition response.
	 */
	public Response<String, byte[]> composeConfiguration(final String protocolVersion, final String shortDescription,
			final Object template){
		final Map<String, Object> data = ReflectionHelper.mapObject(template);
		return composeConfiguration(protocolVersion, shortDescription, data);
	}

	/**
	 * Compose a configuration message.
	 *
	 * @param protocolVersion	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param shortDescription	The short description identifying a message, see {@link ConfigurationHeader#shortDescription()}.
	 * @param data	The configuration message data to be composed.
	 * @return	The composition response.
	 */
	public Response<String, byte[]> composeConfiguration(final String protocolVersion, final String shortDescription,
			final Map<String, Object> data){
		final Version protocol = Version.of(protocolVersion);
		if(protocol.isEmpty())
			throw new IllegalArgumentException("Invalid protocol version: " + protocolVersion);

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
			configurationParser.encode(configuration, writer, configurationData, evaluator, protocol);

			return null;
		}
		catch(final EncodeException | FieldException e){
			return EncodeException.create(e);
		}
	}

}
