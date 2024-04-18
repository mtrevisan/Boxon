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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;
import io.github.mtrevisan.boxon.logs.EventListener;
import io.github.mtrevisan.boxon.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;


public final class LoaderConfiguration{

	private final ThrowingFunction<Class<?>, ConfigurationMessage<?>, AnnotationException> configurationStore
		= Memoizer.throwingMemoize(ConfigurationMessage::create);

	private final Map<String, ConfigurationMessage<?>> configurations = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));

	private EventListener eventListener;


	/**
	 * Create a configuration loader.
	 *
	 * @return	A configuration parser.
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
	 * @return	This instance, used for chaining.
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
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	void loadConfigurationsFrom(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		eventListener.loadingConfigurationsFrom(basePackageClasses);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		/** extract all classes annotated with {@link TemplateHeader}. */
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
	 * @throws ConfigurationException	If a configuration error occurs.
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

			extractConfigurationFromType(type, configurations);
		}
		return configurations;
	}

	private void extractConfigurationFromType(final Class<?> type, final Map<String, ConfigurationMessage<?>> configurations)
			throws AnnotationException, ConfigurationException{
		//for each extracted class, try to parse it, extracting all the information needed for the configuration of a message
		final ConfigurationMessage<?> from = createConfiguration(type);
		if(from.canBeCoded()){
			//if the configuration is valid, add it to the list of configurations...
			final ConfigurationHeader header = from.getHeader();
			configurations.put(header.shortDescription(), from);
		}
		else
			//... otherwise throw exception
			throw ConfigurationException.create("Cannot create a configuration message from data: cannot scan configuration for {}",
				type.getSimpleName());
	}

	/**
	 * Extract a configuration for the given class.
	 *
	 * @param type	The class type.
	 * @return	A configuration.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws ConfigurationException	If a configuration error occurs.
	 */
	public ConfigurationMessage<?> extractConfiguration(final Class<?> type) throws AnnotationException, ConfigurationException{
		final ConfigurationMessage<?> from = createConfiguration(type);
		if(!from.canBeCoded())
			throw ConfigurationException.create("Cannot create a configuration message from data: cannot scan configuration for {}",
				type.getSimpleName());

		return from;
	}

	/**
	 * Constructs a new {@link ConfigurationMessage}.
	 *
	 * @param type	The class of the object to be returned as a {@link ConfigurationMessage}.
	 * @param <T>	The type of the object to be returned as a {@link ConfigurationMessage}.
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
			final String shortDescription = header.shortDescription();
			loadConfigurationInner(shortDescription, configuration);
		}
		catch(final Exception e){
			eventListener.cannotLoadConfiguration(configuration.getType().getName(), e);
		}
	}

	private void loadConfigurationInner(final String shortDescription, final ConfigurationMessage<?> configuration)
			throws ConfigurationException{
		if(configurations.containsKey(shortDescription))
			throw ConfigurationException.create("Duplicated key `{}` found for class {}", shortDescription,
				configuration.getType().getName());

		configurations.put(shortDescription, configuration);
	}


	/**
	 * Get a list of configuration messages.
	 *
	 * @return	The list of configuration messages.
	 */
	public List<ConfigurationMessage<?>> getConfigurations(){
		return List.copyOf(configurations.values());
	}

	/**
	 * Retrieve the configuration by class, filled with data, and considering the protocol version.
	 *
	 * @param configuration	The configuration message.
	 * @param data	The data to load into the configuration.
	 * @param protocol	The protocol the data refers to.
	 * @return	The configuration data.
	 * @throws AnnotationException	If a configuration annotation is invalid, or no annotation was found.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 * @throws EncodeException	If a placeholder cannot be substituted.
	 */
	static Object getConfigurationWithDefaults(final ConfigurationMessage<?> configuration, final Map<String, Object> data,
			final Version protocol) throws AnnotationException, CodecException, EncodeException{
		Object configurationObject = ConstructorHelper.getEmptyCreator(configuration.getType())
			.get();

		//fill in default values
		final List<ConfigurationField> configurableFields = configuration.getConfigurationFields();
		configurationObject = fillDefaultValues(configurationObject, configurableFields, protocol);


		//collect mandatory fields:
		final Collection<ConfigurationField> mandatoryFields = extractMandatoryFields(configurableFields, protocol);

		//load data into configuration based on protocol version:
		for(final Map.Entry<String, Object> entry : data.entrySet()){
			final String dataKey = entry.getKey();
			Object dataValue = entry.getValue();

			//find field in `configuration` that matches `dataKey` and `protocol`
			final ConfigurationField foundField = findField(configurableFields, dataKey, protocol);
			final Annotation foundFieldAnnotation = foundField.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(foundFieldAnnotation);
			manager.validateValue(foundField.getField(), dataKey, dataValue);
			dataValue = manager.convertValue(foundField.getField(), dataKey, dataValue, protocol);
			configurationObject = FieldAccessor.setFieldValue(configurationObject, foundField.getField(), dataValue);

			if(dataValue != null)
				mandatoryFields.remove(foundField);
		}

		//check mandatory fields
		validateMandatoryFields(mandatoryFields);

		return configurationObject;
	}

	/**
	 * Retrieve the configuration by header start parameter.
	 *
	 * @param shortDescription	The short description identifying a message, see {@link ConfigurationHeader#shortDescription()}.
	 * @return	The configuration.
	 * @throws EncodeException	If a configuration cannot be retrieved.
	 */
	public ConfigurationMessage<?> getConfiguration(final String shortDescription) throws EncodeException{
		final ConfigurationMessage<?> configuration = configurations.get(shortDescription);
		if(configuration == null)
			throw EncodeException.create("No configuration could be found for the specified class type");

		return configuration;
	}

	private static Object fillDefaultValues(Object configurationObject, final List<ConfigurationField> fields, final Version protocol)
			throws AnnotationException, CodecException, EncodeException{
		for(int i = 0, length = fields.size(); i < length; i ++){
			final ConfigurationField field = fields.get(i);

			final Annotation binding = field.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(binding);
			final Field f = field.getField();
			Object dataValue = manager.getDefaultValue(f.getType(), protocol);
			dataValue = manager.convertValue(f, manager.getShortDescription(), dataValue, protocol);
			configurationObject = FieldAccessor.setFieldValue(configurationObject, f, dataValue);
		}
		return configurationObject;
	}

	private static Collection<ConfigurationField> extractMandatoryFields(final List<ConfigurationField> fields, final Version protocol){
		final int length = fields.size();
		final Collection<ConfigurationField> mandatoryFields = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigurationField field = fields.get(i);

			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.annotationToBeProcessed(protocol);
			if(manager.isMandatory(annotation))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private static ConfigurationField findField(final List<ConfigurationField> fields, final String key, final Version protocol)
			throws EncodeException{
		for(int i = 0, length = fields.size(); i < length; i ++){
			final ConfigurationField field = fields.get(i);

			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.annotationToBeProcessed(protocol);
			if(annotation.annotationType() != Annotation.class && manager.getShortDescription().equals(key))
				return field;
		}
		throw EncodeException.create("Could not find fields to set for data key {}", key);
	}

	private static void validateMandatoryFields(final Collection<ConfigurationField> mandatoryFields) throws EncodeException{
		if(!mandatoryFields.isEmpty()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final ConfigurationField mandatoryField : mandatoryFields){
				final Annotation annotation = mandatoryField.getBinding();
				final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
				final String shortDescription = manager.getShortDescription();
				sj.add(shortDescription);
			}
			throw EncodeException.create("Mandatory fields missing: {}", sj.toString());
		}
	}

}
