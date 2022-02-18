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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;
import io.github.mtrevisan.boxon.core.managers.configuration.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.core.managers.configuration.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.core.managers.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.core.managers.ConfigField;
import io.github.mtrevisan.boxon.core.managers.ConfigurationMessage;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.logs.EventListener;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


final class LoaderConfiguration{

	private final ThrowingFunction<Class<?>, ConfigurationMessage<?>, AnnotationException> configurationStore
		= Memoizer.throwingMemoize(ConfigurationMessage::create);

	private final Map<String, ConfigurationMessage<?>> configurations = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));

	private EventListener eventListener;


	/**
	 * Create a configuration loader.
	 *
	 * @return	A template parser.
	 */
	static LoaderConfiguration create(){
		return new LoaderConfiguration();
	}


	private LoaderConfiguration(){
		eventListener = EventListener.getNoOpInstance();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	The current instance.
	 */
	LoaderConfiguration withEventListener(final EventListener eventListener){
		this.eventListener = JavaHelper.nonNullOrDefault(eventListener, EventListener.getNoOpInstance());

		return this;
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	void loadConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		eventListener.loadingConfigurations(basePackageClasses);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		/** extract all classes annotated with {@link MessageHeader}. */
		final List<Class<?>> annotatedClasses = reflectiveClassLoader.extractClassesWithAnnotation(ConfigurationHeader.class);
		final Map<String, ConfigurationMessage<?>> configurations = extractConfigurations(annotatedClasses);
		addConfigurationsInner(configurations);

		eventListener.loadedConfigurations(configurations.size());
	}

	/**
	 * Loads the specified configuration class annotated with {@link ConfigurationHeader}.
	 *
	 * @param configurationClass	Configuration class.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 * @throws ConfigurationException	If a configuration is not well formatted.
	 */
	void loadConfiguration(final Class<?> configurationClass) throws AnnotationException, ConfigurationException{
		eventListener.loadingConfiguration(configurationClass);

		final Map<String, ConfigurationMessage<?>> configurations = extractConfigurations(Collections.singletonList(configurationClass));
		addConfigurationsInner(configurations);

		eventListener.loadedConfiguration();
	}


	private Map<String, ConfigurationMessage<?>> extractConfigurations(final List<Class<?>> annotatedClasses)
			throws AnnotationException, ConfigurationException{
		final int size = annotatedClasses.size();
		final Map<String, ConfigurationMessage<?>> configurations = new ConcurrentHashMap<>(size);
		for(int i = 0; i < size; i ++){
			final Class<?> type = annotatedClasses.get(i);
			//for each extracted class, try to parse it, extracting all the information needed for the configuration of a message
			final ConfigurationMessage<?> from = createConfiguration(type);
			if(from.canBeCoded()){
				//if the configuration is valid, add it to the list of templates...
				final ConfigurationHeader header = from.getHeader();
				configurations.put(header.start(), from);
			}
			else
				//... otherwise throw exception
				throw ConfigurationException.create("Cannot create a raw message from data: cannot scan configuration for {}",
					type.getSimpleName());
		}
		return configurations;
	}

	/**
	 * Constructs a new {@link ConfigurationMessage}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link ConfigurationMessage}.
	 * @param type	The class of the object to be returned as a {@link ConfigurationMessage}.
	 * @return	The {@link ConfigurationMessage} for the given type.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 */
	@SuppressWarnings("unchecked")
	private <T> ConfigurationMessage<T> createConfiguration(final Class<T> type) throws AnnotationException{
		return (ConfigurationMessage<T>)configurationStore.apply(type);
	}

	private void addConfigurationsInner(final Map<String, ConfigurationMessage<?>> configurations){
		//load each configuration into the available configurations list
		for(final ConfigurationMessage<?> configuration : configurations.values())
			if(configuration != null && configuration.canBeCoded())
				addConfigurationInner(configuration);
	}

	/**
	 * For each valid configuration, add it to the map of configurations indexed by starting message bytes.
	 *
	 * @param configuration	The configuration to add to the list of available configurations.
	 */
	private void addConfigurationInner(final ConfigurationMessage<?> configuration){
		try{
			final ConfigurationHeader header = configuration.getHeader();
			final String start = header.start();
			loadConfigurationInner(start, configuration);
		}
		catch(final Exception e){
			eventListener.cannotLoadConfiguration(configuration.getType().getName(), e);
		}
	}

	private void loadConfigurationInner(final String headerStart, final ConfigurationMessage<?> configuration) throws ConfigurationException{
		if(configurations.containsKey(headerStart))
			throw ConfigurationException.create("Duplicated key `{}` found for class {}", headerStart,
				configuration.getType().getName());

		configurations.put(headerStart, configuration);
	}


	/**
	 * Get a list of configuration messages.
	 *
	 * @return	The list of configuration messages.
	 */
	List<ConfigurationMessage<?>> getConfigurations(){
		return List.copyOf(configurations.values());
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
	 */
	static Object getConfigurationWithDefaults(final ConfigurationMessage<?> configuration, final Map<String, Object> data,
			final Version protocol) throws EncodeException, CodecException{
		final Object configurationObject = ConstructorHelper.getCreator(configuration.getType())
			.get();

		//fill in default values
		final List<ConfigField> configurableFields = configuration.getConfigurationFields();
		fillDefaultValues(configurationObject, configurableFields, protocol);


		//collect mandatory fields:
		final Collection<ConfigField> mandatoryFields = extractMandatoryFields(configurableFields, protocol);

		//load data into configuration based on protocol version:
		for(final Map.Entry<String, Object> entry : data.entrySet()){
			final String dataKey = entry.getKey();
			Object dataValue = entry.getValue();

			//find field in `configuration` that matches `dataKey` and `protocol`
			final ConfigField foundField = findField(configurableFields, dataKey, protocol);
			final Annotation foundFieldAnnotation = foundField.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(foundFieldAnnotation);
			final Class<?> fieldType = foundField.getFieldType();
			manager.validateValue(dataKey, dataValue, fieldType);
			dataValue = manager.convertValue(dataKey, dataValue, foundField.getField(), protocol);
			ReflectionHelper.setValue(configurationObject, foundField.getField(), dataValue);

			if(dataValue instanceof String && !((String)dataValue).isEmpty() || dataValue != null)
				mandatoryFields.remove(foundField);
		}

		//check mandatory fields
		validateMandatoryFields(mandatoryFields);

		return configurationObject;
	}

	/**
	 * Retrieve the configuration by header start parameter.
	 *
	 * @param configurationType	The header start of a configuration.
	 * @return	The configuration.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	ConfigurationMessage<?> getConfiguration(final String configurationType) throws EncodeException{
		final ConfigurationMessage<?> configuration = configurations.get(configurationType);
		if(configuration == null)
			throw EncodeException.create("Cannot find any configuration for given class type");

		return configuration;
	}

	private static void fillDefaultValues(final Object configurationObject, final List<ConfigField> fields, final Version protocol)
			throws EncodeException, CodecException{
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
			Object dataValue = manager.getDefaultValue(field.getField(), protocol);
			dataValue = manager.convertValue(manager.getShortDescription(), dataValue, field.getField(), protocol);
			ReflectionHelper.setValue(configurationObject, field.getField(), dataValue);
		}
	}

	private static Collection<ConfigField> extractMandatoryFields(final List<ConfigField> fields, final Version protocol){
		final int size = fields.size();
		final Collection<ConfigField> mandatoryFields = new HashSet<>(size);
		for(int i = 0; i < size; i ++){
			final ConfigField field = fields.get(i);
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.annotationToBeProcessed(protocol);
			if(manager.isMandatory(annotation))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private static ConfigField findField(final List<ConfigField> fields, final String key, final Version protocol) throws EncodeException{
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.annotationToBeProcessed(protocol);
			if(annotation.annotationType() != Annotation.class && manager.getShortDescription().equals(key))
				return field;
		}
		throw EncodeException.create("Cannot find any field to set for data key {}", key);
	}

	private static void validateMandatoryFields(final Collection<ConfigField> mandatoryFields) throws EncodeException{
		if(!mandatoryFields.isEmpty()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final ConfigField mandatoryField : mandatoryFields){
				final Annotation annotation = mandatoryField.getBinding();
				final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
				final String shortDescription = manager.getShortDescription();
				sj.add(shortDescription);
			}
			throw EncodeException.create("Mandatory fields missing: {}", sj.toString());
		}
	}

}
