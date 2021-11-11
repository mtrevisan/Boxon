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
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.DecodeException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Declarative data binding parser for binary encoded data.
 */
public final class Parser{

	private final LoaderCodec loaderCodec;
	private final LoaderTemplate loaderTemplate;
	private final LoaderConfiguration loaderConfiguration;

	private final TemplateParser templateParser;
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
		loaderCodec = new LoaderCodec(eventListener);
		loaderTemplate = new LoaderTemplate(loaderCodec, eventListener);
		loaderConfiguration = new LoaderConfiguration(eventListener);
		templateParser = new TemplateParser(loaderCodec, loaderTemplate, eventListener);
		configurationParser = new ConfigurationParser(loaderCodec, loaderTemplate, templateParser, eventListener);
	}

	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser addToContext(final String key, final Object value){
		Evaluator.addToContext(key, value);
		return this;
	}

	/**
	 * Loads the context for the {@link Evaluator}.
	 *
	 * @param context	The context map.
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withDefaultCodecs(){
		loaderCodec.loadDefaultCodecs();
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load codecs.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withCodecs(final Class<?>... basePackageClasses){
		loaderCodec.loadCodecs(basePackageClasses);
		return this;
	}

	/**
	 * Loads all the codecs that extends {@link CodecInterface}.
	 *
	 * @param codecs	The list of codecs to be loaded.
	 * @return	The {@link Parser}, used for chaining.
	 */
	public Parser withCodecs(final CodecInterface<?>... codecs){
		loaderCodec.addCodecs(codecs);
		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
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
	 * @return	The {@link Parser}, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public Parser withConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadConfigurations(basePackageClasses);
		return this;
	}

	/**
	 * Retrieve all the protocol version boundaries.
	 *
	 * @return	The protocol version boundaries.
	 */
	public List<String> getProtocolVersionBoundaries(){
		return loaderConfiguration.getProtocolVersionBoundaries();
	}

	/**
	 * Retrieve all the configuration given a protocol version.
	 *
	 * @param protocol	The protocol used to extract the configurations.
	 * @return	The configuration messages for a given protocol version.
	 */
	public List<Map<String, Object>> getConfigurations(final String protocol) throws ConfigurationException{
		return loaderConfiguration.getConfigurations(protocol);
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
					break;

				reader.position(position);
			}
		}

		//check if there are unread bytes
		assertNoLeftBytes(reader, start, response);

		return response;
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
		for(final Map.Entry<String, Map<String, Object>> entry : data.entrySet()){
			composeConfiguration(writer, entry.getKey(), entry.getValue(), protocol, response);
		}
		writer.flush();

		response.setComposedMessage(writer.array());

		return response;
	}

	/**
	 * Compose a single configuration message.
	 *
	 * @param data   The configuration message to be composed.
	 */
	private void composeConfiguration(final BitWriter writer, final String configurationType, final Map<String, Object> data,
			final Version protocol, final ComposeResponse response){
		try{
			final LoaderConfiguration.ConfigurationPair configurationPair = loaderConfiguration.getConfiguration(configurationType, data,
				protocol);

			final Configuration<?> configuration = configurationPair.getConfiguration();
			final Object configurationData = configurationPair.getConfigurationData();
			configurationParser.encode(configuration, writer, configurationData, protocol);
		}
		catch(final Exception e){
			response.addError(EncodeException.create(e));
		}
	}

}
