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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationHelper;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.logs.EventListener;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


/**
 * Declarative data binding parser for configuration messages.
 */
public final class ConfigurationParser{

	private final LoaderCodecInterface loaderCodec;
	private final LoaderConfiguration loaderConfiguration;

	private final ParserWriterHelper parserWriterHelper;


	/**
	 * Create a configuration parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A configuration parser.
	 */
	public static ConfigurationParser create(final LoaderCodecInterface loaderCodec){
		return new ConfigurationParser(loaderCodec);
	}


	private ConfigurationParser(final LoaderCodecInterface loaderCodec){
		this.loaderCodec = loaderCodec;
		loaderConfiguration = LoaderConfiguration.create();

		parserWriterHelper = ParserWriterHelper.create();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	public ConfigurationParser withEventListener(final EventListener eventListener){
		loaderConfiguration.withEventListener(eventListener);

		parserWriterHelper.withEventListener(eventListener);

		return this;
	}


	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public void loadConfigurationsFrom(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadConfigurationsFrom(basePackageClasses);
	}

	/**
	 * Loads the specified configuration class annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	public void withConfiguration(final Class<?> configurationClass) throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadConfiguration(configurationClass);
	}

	/**
	 * Get a list of configuration messages.
	 *
	 * @return	The list of configuration messages.
	 */
	public List<ConfigurationMessage<?>> getConfigurations(){
		return loaderConfiguration.getConfigurations();
	}

	/**
	 * Retrieve the configuration by class, filled with data, and considering the protocol version.
	 *
	 * @param configuration	The configuration message.
	 * @param data	The data to load into the configuration.
	 * @param protocol	The protocol the data refers to.
	 * @return	The configuration data.
	 * @throws EncodeException	If a placeholder cannot be substituted.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 */
	public static Object getConfigurationWithDefaults(final ConfigurationMessage<?> configuration, final Map<String, Object> data,
			final Version protocol) throws EncodeException, CodecException, AnnotationException{
		return LoaderConfiguration.getConfigurationWithDefaults(configuration, data, protocol);
	}

	/**
	 * Retrieve the configuration by class.
	 *
	 * @param shortDescription	The short description identifying a message, see {@link ConfigurationHeader#shortDescription()}.
	 * @return	The configuration.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public ConfigurationMessage<?> getConfiguration(final String shortDescription) throws EncodeException{
		return loaderConfiguration.getConfiguration(shortDescription);
	}


	/**
	 * Encode the configuration using the given writer with the given object that contains the values.
	 *
	 * @param configuration	The configuration to encode.
	 * @param writer	The writer that holds the encoded template.
	 * @param currentObject	The current object that holds the values.
	 * @param evaluator	An evaluator.
	 * @param protocol	The protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param <T>	The class type of the current object.
	 * @throws FieldException	If a codec is not found.
	 */
	public <T> void encode(final ConfigurationMessage<?> configuration, final BitWriterInterface writer, final T currentObject,
			final Evaluator evaluator, final Version protocol) throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(evaluator, currentObject);
		parserContext.setClassName(configuration.getType().getName());

		final ConfigurationHeader header = configuration.getHeader();
		ParserWriterHelper.writeAffix(header.start(), header.charset(), writer);

		//encode message fields:
		final List<ConfigField> fields = configuration.getConfigurationFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final ConfigField field = fields.get(i);

			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.annotationToBeProcessed(protocol);
			if(annotation.annotationType() == Annotation.class)
				continue;

			//process skip annotations:
			final ConfigurationSkip[] skips = field.getSkips();
			writeSkips(skips, writer, protocol);

			parserContext.setRootObject(field.getFieldType());
			parserContext.setFieldName(field.getFieldName());

			//process value
			parserContext.setField(field);
			parserContext.setBinding(annotation);
			parserWriterHelper.encodeField(parserContext, writer, loaderCodec);
			if(annotation != field.getBinding()){
				parserContext.setBinding(field.getBinding());
				parserWriterHelper.encodeField(parserContext, writer, loaderCodec);
			}
		}

		ParserWriterHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private static void writeSkips(final ConfigurationSkip[] skips, final BitWriterInterface writer, final Version protocol){
		for(int i = 0, length = skips.length; i < length; i ++)
			writeSkip(skips[i], writer, protocol);
	}

	private static void writeSkip(final ConfigurationSkip skip, final BitWriterInterface writer, final Version protocol){
		final boolean process = ConfigurationHelper.shouldBeExtracted(protocol, skip.minProtocol(), skip.maxProtocol());
		if(process)
			writer.putText(skip.terminator());
	}


	/**
	 * The loader for the configurations.
	 *
	 * @return	The loader for the configurations.
	 */
	public LoaderConfiguration getLoaderConfiguration(){
		return loaderConfiguration;
	}

}
