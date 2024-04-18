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
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.core.parsers.ConfigurationParser;
import io.github.mtrevisan.boxon.core.parsers.LoaderConfiguration;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/**
 * The ConfigurationDescriber class provides methods to describe loaded configurations.
 */
class ConfigurationDescriber{

	private final LoaderConfiguration loaderConfiguration;

	private final MessageDescriber messageDescriber;


	/**
	 * Create a describer.
	 *
	 * @param core	The parser core.
	 * @return	A configuration describer.
	 */
	static ConfigurationDescriber create(final Core core, final MessageDescriber messageDescriber){
		return new ConfigurationDescriber(core, messageDescriber);
	}


	private ConfigurationDescriber(final Core core, final MessageDescriber messageDescriber){
		final ConfigurationParser configurationParser = core.getConfigurationParser();
		loaderConfiguration = configurationParser.getLoaderConfiguration();

		this.messageDescriber = messageDescriber;
	}


	/**
	 * Description of all the loaded configuration.
	 *
	 * @return	The list of descriptions.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	List<Map<String, Object>> describeConfiguration() throws FieldException{
		final Collection<ConfigurationMessage<?>> configurations = new HashSet<>(loaderConfiguration.getConfigurations());
		return FieldDescriber.describeEntities(configurations, configuration -> messageDescriber.describeMessage(configuration,
			FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION, FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION));
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
	Map<String, Object> describeConfiguration(final Class<?> configurationClass) throws FieldException, EncodeException{
		final FieldDescriber.ThrowingFunction<Class<?>, ConfigurationMessage<?>, EncodeException> extractor = cls -> {
			final ConfigurationHeader header = configurationClass.getAnnotation(ConfigurationHeader.class);
			return loaderConfiguration.getConfiguration(header.shortDescription());
		};
		return FieldDescriber.describeEntity(ConfigurationHeader.class, configurationClass, extractor,
			configuration -> messageDescriber.describeMessage(configuration, FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION,
				FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION));
	}

	/**
	 * Description of all the configurations in the given package annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	List<Map<String, Object>> describeConfiguration(final Class<?>... configurationClasses) throws FieldException{
		return FieldDescriber.describeEntities(ConfigurationHeader.class, configurationClasses, loaderConfiguration::extractConfiguration,
			configuration -> messageDescriber.describeMessage(configuration, FieldDescriber.MESSAGE_EXTRACTOR_CONFIGURATION,
				FieldDescriber.FIELD_EXTRACTOR_CONFIGURATION));
	}

}
