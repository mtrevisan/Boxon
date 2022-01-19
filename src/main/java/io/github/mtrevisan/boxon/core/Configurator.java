/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.codecs.ConfigurationParser;
import io.github.mtrevisan.boxon.codecs.Evaluator;
import io.github.mtrevisan.boxon.core.codecs.BitWriter;
import io.github.mtrevisan.boxon.core.codecs.BitWriterInterface;
import io.github.mtrevisan.boxon.core.configurations.ConfigurationKey;
import io.github.mtrevisan.boxon.core.semanticversioning.Version;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.internal.ConfigField;
import io.github.mtrevisan.boxon.internal.ConfigurationMessage;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.internal.configuration.ConfigurationHelper;
import io.github.mtrevisan.boxon.internal.configuration.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.internal.configuration.ConfigurationManagerInterface;

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
	 * Create an empty configurator.
	 *
	 * @param parserCore	The parser core.
	 * @return	A basic empty descriptor.
	 */
	public static Configurator create(final ParserCore parserCore){
		return new Configurator(parserCore);
	}


	private Configurator(final ParserCore parserCore){
		configurationParser = parserCore.getConfigurationParser();
		evaluator = parserCore.getEvaluator();
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
		final List<String> protocolVersionBoundaries = new ArrayList<>(configurationValues.size());
		for(int i = 0; i < configurationValues.size(); i ++)
			protocolVersionBoundaries.addAll(configurationValues.get(i).getProtocolVersionBoundaries());
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
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(int i = 0; i < configurationValues.size(); i ++){
			final ConfigurationMessage<?> configuration = configurationValues.get(i);
			final ConfigurationHeader header = configuration.getHeader();
			if(!ConfigurationHelper.shouldBeExtracted(protocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> map = new HashMap<>(3);
			final Map<String, Object> headerMap = extractMap(protocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(protocol, configuration);
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.CONFIGURATION_HEADER, headerMap, map);
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.CONFIGURATION_FIELDS, fieldsMap, map);
			if(protocol.isEmpty()){
				final List<String> protocolVersionBoundaries = configuration.getProtocolVersionBoundaries();
				ConfigurationHelper.putIfNotEmpty(ConfigurationKey.CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES, protocolVersionBoundaries, map);
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
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
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
	 * Compose a list of configuration messages.
	 *
	 * @param protocolVersion	Protocol version.
	 * @param data	The configuration message(s) to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse composeConfiguration(final String protocolVersion, final Map<String, Map<String, Object>> data){
		final Version protocol = Version.of(protocolVersion);
		if(protocol.isEmpty())
			throw new IllegalArgumentException("Invalid protocol version: " + protocolVersion);

		final ComposeResponse response = new ComposeResponse(data.keySet().toArray(JavaHelper.EMPTY_ARRAY));

		final BitWriter writer = BitWriter.create();
		for(final Map.Entry<String, Map<String, Object>> entry : data.entrySet())
			composeConfiguration(writer, entry, protocol, response);

		response.setComposedMessage(writer);

		return response;
	}

	/**
	 * Compose a single configuration message.
	 */
	private void composeConfiguration(final BitWriterInterface writer, final Map.Entry<String, Map<String, Object>> entry,
			final Version protocol, final ComposeResponse response){
		try{
			final String configurationType = entry.getKey();
			final Map<String, Object> data = entry.getValue();
			final ConfigurationMessage<?> configuration = configurationParser.getConfiguration(configurationType);
			final Object configurationData = ConfigurationParser.getConfigurationWithDefaults(configuration, data, protocol);
			configurationParser.encode(configuration, writer, configurationData, evaluator, protocol);
		}
		catch(final Exception e){
			response.addError(EncodeException.create(e));
		}
	}

}
