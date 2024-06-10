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
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * The ConfigurationDescriber class provides methods to describe loaded configurations.
 */
public final class ConfigurationDescriber{

	private final LoaderConfiguration loaderConfiguration;
	private final MessageDescriber messageDescriber;


	/**
	 * Create a describer.
	 *
	 * @param configurationParser	The configuration parser.
	 * @return	A configuration describer.
	 */
	public static ConfigurationDescriber create(final ConfigurationParser configurationParser, final MessageDescriber messageDescriber){
		return new ConfigurationDescriber(configurationParser, messageDescriber);
	}


	private ConfigurationDescriber(final ConfigurationParser configurationParser, final MessageDescriber messageDescriber){
		loaderConfiguration = configurationParser.getLoaderConfiguration();
		this.messageDescriber = messageDescriber;
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public List<Map<String, Object>> describeConfiguration() throws BoxonException{
		final Collection<ConfigurationMessage<?>> configurations = new HashSet<>(loaderConfiguration.getConfigurations());
		final ThrowingFunction<ConfigurationMessage<?>, Map<String, Object>, BoxonException> mapper
			= configuration -> messageDescriber.describeMessage(configuration, FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION,
			FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION);
		return EntityDescriber.describeEntities(configurations, mapper);
	}

	/**
	 * Description of a single configuration annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class to be described.
	 * @return	The description.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public Map<String, Object> describeConfiguration(final Class<?> configurationClass) throws BoxonException{
		final ThrowingFunction<Class<?>, ConfigurationMessage<?>, EncodeException> extractor = cls -> {
			final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);
			return loaderConfiguration.getConfiguration(header.shortDescription());
		};
		final ThrowingFunction<ConfigurationMessage<?>, Map<String, Object>, BoxonException> mapper
			= configuration -> messageDescriber.describeMessage(configuration, FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION,
			FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION);
		return EntityDescriber.describeEntity(ConfigurationHeader.class, configurationClass, extractor, mapper);
	}

	/**
	 * Description of all the configurations in the given package annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public List<Map<String, Object>> describeConfiguration(final Class<?>... configurationClasses) throws BoxonException{
		final ThrowingFunction<ConfigurationMessage<?>, Map<String, Object>, BoxonException> mapper
			= configuration -> messageDescriber.describeMessage(configuration, FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION,
			FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION);
		return EntityDescriber.describeEntities(ConfigurationHeader.class, configurationClasses, loaderConfiguration::extractConfiguration, mapper);
	}

}
