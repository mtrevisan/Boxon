/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.codecs.managers.ConfigField;
import io.github.mtrevisan.boxon.codecs.managers.ConfigurationMessage;
import io.github.mtrevisan.boxon.codecs.ConfigurationParser;
import io.github.mtrevisan.boxon.codecs.LoaderCodec;
import io.github.mtrevisan.boxon.codecs.LoaderConfiguration;
import io.github.mtrevisan.boxon.codecs.LoaderTemplate;
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.codecs.TemplateParser;
import io.github.mtrevisan.boxon.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.CodecInterface;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Declarative data binding parser for binary encoded data.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Parser{

	private final LoaderCodec loaderCodec;
	private final LoaderTemplate loaderTemplate;
	private final LoaderConfiguration loaderConfiguration;

	private final TemplateParserInterface templateParser;
	private final ConfigurationParser configurationParser;


	/**
	 * Create an empty parser (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @return	A basic empty parser.
	 */
	public static Parser create(){
		return new Parser(EventListener.getNoOpInstance());
	}

	/**
	 * Create an empty parser (context, codecs and templates MUST BE manually loaded! -- templates MUST BE loaded AFTER
	 * the codecs).
	 *
	 * @param eventListener	The event listener.
	 * @return	A basic empty parser.
	 */
	public static Parser create(final EventListener eventListener){
		return new Parser(eventListener != null? eventListener: EventListener.getNoOpInstance());
	}


	private Parser(final EventListener eventListener){
		loaderCodec = LoaderCodec.create(eventListener);
		loaderTemplate = LoaderTemplate.create(loaderCodec, eventListener);
		loaderConfiguration = LoaderConfiguration.create(eventListener);
		templateParser = TemplateParser.create(loaderCodec, eventListener);
		configurationParser = ConfigurationParser.create(loaderCodec, eventListener);
	}

	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	This instance, used for chaining.
	 */
	public Parser addToContext(final String key, final Object value){
		Evaluator.addToContext(key, value);
		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	This instance, used for chaining.
	 */
	public Parser withContext(final Map<String, Object> context){
		Objects.requireNonNull(context, "Context cannot be null");

		context.forEach(Evaluator::addToContext);
		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param method	The method.
	 * @return	This instance, used for chaining.
	 */
	public Parser withContextFunction(final Method method){
		Evaluator.addToContext(method);
		return this;
	}

	/**
	 * Add a method to the context for the {@link Evaluator}.
	 *
	 * @param cls	The class in which the method resides.
	 * @param methodName	The name of the method.
	 * @param parameterTypes	The parameter array.
	 * @return	This instance, used for chaining.
	 * @throws NoSuchMethodException	If a matching method is not found.
	 */
	public Parser withContextFunction(final Class<?> cls, final String methodName, final Class<?>... parameterTypes)
			throws NoSuchMethodException{
		final Method method = cls.getDeclaredMethod(methodName, parameterTypes);
		return withContextFunction(method);
	}


	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the codecs.</p>
	 *
	 * @return	This instance, used for chaining.
	 */
	public Parser withDefaultCodecs(){
		loaderCodec.loadDefaultCodecs();

		//post process codecs
		loaderCodec.injectFieldsInCodecs(loaderTemplate, templateParser);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	This instance, used for chaining.
	 */
	public Parser withCodecs(final Class<?>... basePackageClasses){
		loaderCodec.loadCodecs(basePackageClasses);

		//post process codecs
		loaderCodec.injectFieldsInCodecs(loaderTemplate, templateParser);

		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	This instance, used for chaining.
	 */
	public Parser withCodecs(final CodecInterface<?>... codecs){
		loaderCodec.addCodecs(codecs);

		//post process codecs
		loaderCodec.injectFieldsInCodecs(loaderTemplate, templateParser);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public Parser withDefaultTemplates() throws AnnotationException, TemplateException{
		loaderTemplate.loadDefaultTemplates();
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public Parser withTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplates(basePackageClasses);
		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public Parser withDefaultConfigurations() throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadDefaultConfigurations();
		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public Parser withConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadConfigurations(basePackageClasses);
		return this;
	}


	/**
	 * Parse a message from a file containing a binary stream.
	 *
	 * @param file	The file containing the binary stream.
	 * @return	The parse response.
	 * @throws IOException	If an I/O error occurs.
	 * @throws FileNotFoundException	If the file does not exist, is a directory rather than a regular file,
	 * 	or for some other reason cannot be opened for reading.
	 * @throws SecurityException	If a security manager exists and its {@code checkRead} method denies read access to the file.
	 */
	public ParseResponse parse(final File file) throws IOException, FileNotFoundException{
		final BitReader reader = BitReader.wrap(file);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param payload	The message to be parsed.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final byte[] payload){
		final BitReader reader = BitReader.wrap(payload);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param buffer	The message to be parsed backed by a {@link ByteBuffer}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final ByteBuffer buffer){
		final BitReader reader = BitReader.wrap(buffer);
		return parse(reader);
	}

	/**
	 * Parse a message.
	 *
	 * @param reader	The message to be parsed backed by a {@link BitReader}.
	 * @return	The parse response.
	 */
	public ParseResponse parse(final BitReader reader){
		final byte[] array = reader.array();
		final ParseResponse response = new ParseResponse(array);

		int start = 0;
		while(reader.hasRemaining()){
			start = reader.position();

			//save state of the reader (restored upon a decoding error)
			reader.createFallbackPoint();

			if(parse(reader, start, response))
				break;
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, start, response);

		return response;
	}

	private boolean parse(final BitReader reader, final int start, final ParseResponse response){
		try{
			final Template<?> template = loaderTemplate.getTemplate(reader);

			final Object partialDecodedMessage = templateParser.decode(template, reader, null);

			response.addParsedMessage(start, partialDecodedMessage);
		}
		catch(final Exception e){
			final DecodeException de = DecodeException.create(reader.position(), e);
			response.addError(start, de);

			//restore state of the reader
			reader.restoreFallbackPoint();

			final int position = loaderTemplate.findNextMessageIndex(reader);
			if(position < 0)
				//cannot find any template for message
				return true;

			reader.position(position);
		}
		return false;
	}

	private static void assertNoLeftBytes(final BitReader reader, final int start, final ParseResponse response){
		if(!response.hasErrors() && reader.hasRemaining()){
			final int position = reader.position();
			final IllegalArgumentException error = new IllegalArgumentException("There are remaining unread bytes");
			final DecodeException pe = DecodeException.create(position, error);
			response.addError(start, pe);
		}
	}


	/**
	 * Compose a list of messages.
	 *
	 * @param data	The messages to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse composeMessage(final Collection<Object> data){
		return composeMessage(data.toArray(Object[]::new));
	}

	/**
	 * Compose a list of messages.
	 *
	 * @param data	The message(s) to be composed.
	 * @return	The composition response.
	 */
	public ComposeResponse composeMessage(final Object... data){
		final ComposeResponse response = new ComposeResponse(data);

		final BitWriter writer = BitWriter.create();
		for(int i = 0; i < data.length; i ++)
			composeMessage(writer, data[i], response);
		writer.flush();

		response.setComposedMessage(writer.array());

		return response;
	}

	/**
	 * Compose a single message.
	 *
	 * @param data	The message to be composed.
	 */
	private void composeMessage(final BitWriter writer, final Object data, final ComposeResponse response){
		try{
			final Template<?> template = loaderTemplate.getTemplate(data.getClass());

			templateParser.encode(template, writer, null, data);
		}
		catch(final Exception e){
			response.addError(EncodeException.create(e));
		}
	}


	/**
	 * Retrieve all the configuration regardless the protocol version.
	 *
	 * @return	The configuration messages regardless the protocol version.
	 */
	public List<Map<String, Object>> getConfigurations() throws ConfigurationException, CodecException{
		final List<ConfigurationMessage<?>> configurationValues = loaderConfiguration.getConfigurations();
		return extractConfigurations(configurationValues, Version.EMPTY);
	}

	/**
	 * Retrieve all the protocol version boundaries.
	 *
	 * @return	The protocol version boundaries.
	 */
	public List<String> getProtocolVersionBoundaries(){
		final List<ConfigurationMessage<?>> configurationValues = loaderConfiguration.getConfigurations();
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
	 */
	public List<Map<String, Object>> getConfigurations(final String protocol) throws ConfigurationException, CodecException{
		if(StringHelper.isBlank(protocol))
			throw new IllegalArgumentException(StringHelper.format("Invalid protocol: {}", protocol));

		final List<ConfigurationMessage<?>> configurationValues = loaderConfiguration.getConfigurations();
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
			map.put(LoaderConfiguration.KEY_CONFIGURATION_HEADER, headerMap);
			map.put(LoaderConfiguration.KEY_CONFIGURATION_FIELDS, fieldsMap);
			if(protocol.isEmpty())
				map.put(LoaderConfiguration.KEY_CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES, configuration.getProtocolVersionBoundaries());
			response.add(map);
		}
		return Collections.unmodifiableList(response);
	}

	private static Map<String, Object> extractMap(final Version protocol, final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		ConfigurationHelper.putIfNotEmpty(LoaderConfiguration.KEY_SHORT_DESCRIPTION, header.shortDescription(), map);
		ConfigurationHelper.putIfNotEmpty(LoaderConfiguration.KEY_LONG_DESCRIPTION, header.longDescription(), map);
		if(protocol.isEmpty()){
			ConfigurationHelper.putIfNotEmpty(LoaderConfiguration.KEY_MIN_PROTOCOL, header.minProtocol(), map);
			ConfigurationHelper.putIfNotEmpty(LoaderConfiguration.KEY_MAX_PROTOCOL, header.maxProtocol(), map);
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
		writer.flush();

		response.setComposedMessage(writer.array());

		return response;
	}

	/**
	 * Compose a single configuration message.
	 */
	private void composeConfiguration(final BitWriter writer, final Map.Entry<String, Map<String, Object>> entry,
			final Version protocol, final ComposeResponse response){
		try{
			final String configurationType = entry.getKey();
			final Map<String, Object> data = entry.getValue();
			final LoaderConfiguration.ConfigurationPair configurationPair
				= loaderConfiguration.getConfigurationWithDefaults(configurationType, data, protocol);

			final ConfigurationMessage<?> configuration = configurationPair.getConfiguration();
			final Object configurationData = configurationPair.getConfigurationData();
			configurationParser.encode(configuration, writer, configurationData, protocol);
		}
		catch(final Exception e){
			response.addError(EncodeException.create(e));
		}
	}

}
