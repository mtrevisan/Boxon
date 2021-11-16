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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.core.EventListener;
import io.github.mtrevisan.boxon.core.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.Memoizer;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;
import io.github.mtrevisan.boxon.internal.ThrowingFunction;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;


public final class LoaderConfiguration{

	private static final String EMPTY_STRING = "";

	static final String KEY_CONFIGURATION_HEADER = "header";
	static final String KEY_CONFIGURATION_FIELDS = "fields";
	static final String KEY_CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES = "protocolVersionBoundaries";
	static final String KEY_CONFIGURATION_COMPOSITE_FIELDS = "fields";

	static final String KEY_ALTERNATIVES = "alternatives";
	static final String KEY_FIELD_TYPE = "fieldType";
	private static final String KEY_SHORT_DESCRIPTION = "shortDescription";
	static final String KEY_LONG_DESCRIPTION = "longDescription";
	static final String KEY_UNIT_OF_MEASURE = "unitOfMeasure";
	static final String KEY_MIN_PROTOCOL = "minProtocol";
	static final String KEY_MAX_PROTOCOL = "maxProtocol";
	static final String KEY_MIN_VALUE = "minValue";
	static final String KEY_MAX_VALUE = "maxValue";
	static final String KEY_PATTERN = "pattern";
	static final String KEY_ENUMERATION = "enumeration";
	static final String KEY_MUTUALLY_EXCLUSIVE = "mutuallyExclusive";
	static final String KEY_DEFAULT_VALUE = "defaultValue";
	static final String KEY_CHARSET = "charset";


	public static final class ConfigurationPair{
		private final Configuration<?> configuration;
		private final Object configurationData;

		static ConfigurationPair of(final Configuration<?> configuration, final Object configurationData){
			return new ConfigurationPair(configuration, configurationData);
		}

		private ConfigurationPair(final Configuration<?> configuration, final Object configurationData){
			this.configuration = configuration;
			this.configurationData = configurationData;
		}

		public Configuration<?> getConfiguration(){
			return configuration;
		}

		public Object getConfigurationData(){
			return configurationData;
		}
	}


	@InjectEventListener
	private final EventListener eventListener;

	private final ThrowingFunction<Class<?>, Configuration<?>, AnnotationException> configurationStore
		= Memoizer.throwingMemoize(Configuration::create);

	private final Map<String, Configuration<?>> configurations = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));


	/**
	 * Create a configuration loader.
	 *
	 * @return	A template parser.
	 */
	static LoaderConfiguration create(){
		return new LoaderConfiguration(EventListener.getNoOpInstance());
	}

	/**
	 * Create a configuration loader.
	 *
	 * @param eventListener	The event listener.
	 * @return	A template parser.
	 */
	public static LoaderConfiguration create(final EventListener eventListener){
		return new LoaderConfiguration(eventListener != null? eventListener: EventListener.getNoOpInstance());
	}

	private LoaderConfiguration(final EventListener eventListener){
		this.eventListener = eventListener;
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	public void loadDefaultConfigurations() throws AnnotationException, ConfigurationException{
		loadConfigurations(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 */
	public void loadConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		eventListener.loadingConfigurations(basePackageClasses);

		/** extract all classes annotated with {@link MessageHeader}. */
		final Collection<Class<?>> annotatedClasses = ReflectionHelper.extractClasses(ConfigurationHeader.class, basePackageClasses);
		final Map<String, Configuration<?>> configurations = extractConfigurations(annotatedClasses);
		addConfigurationsInner(configurations);

		eventListener.loadedConfigurations(configurations.size());
	}

	private Map<String, Configuration<?>> extractConfigurations(final Collection<Class<?>> annotatedClasses) throws AnnotationException,
			ConfigurationException{
		final Map<String, Configuration<?>> configurations = new HashMap<>(annotatedClasses.size());
		for(final Class<?> type : annotatedClasses){
			//for each extracted class, try to parse it, extracting all the information needed for the configuration of a message
			final Configuration<?> from = createConfiguration(type);
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
	 * Constructs a new {@link Configuration}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Configuration}.
	 * @param type	The class of the object to be returned as a {@link Configuration}.
	 * @return	The {@link Configuration} for the given type.
	 */
	@SuppressWarnings("unchecked")
	private <T> Configuration<T> createConfiguration(final Class<T> type) throws AnnotationException{
		return (Configuration<T>)configurationStore.apply(type);
	}

	private void addConfigurationsInner(final Map<String, Configuration<?>> configurations){
		//load each configuration into the available configurations list
		for(final Configuration<?> configuration : configurations.values())
			if(configuration != null && configuration.canBeCoded())
				addConfigurationInner(configuration);
	}

	/**
	 * For each valid configuration, add it to the map of configurations indexed by starting message bytes.
	 *
	 * @param configuration	The configuration to add to the list of available configurations.
	 */
	private void addConfigurationInner(final Configuration<?> configuration){
		try{
			final ConfigurationHeader header = configuration.getHeader();
			final String start = header.start();
			loadConfigurationInner(start, configuration);
		}
		catch(final Exception e){
			eventListener.cannotLoadConfiguration(configuration.getType().getName(), e);
		}
	}

	private void loadConfigurationInner(final String headerStart, final Configuration<?> configuration) throws ConfigurationException{
		if(configurations.containsKey(headerStart))
			throw ConfigurationException.create("Duplicated key `{}` found for class {}", headerStart,
				configuration.getType().getName());

		configurations.put(headerStart, configuration);
	}


	/**
	 * Retrieve all the protocol version boundaries.
	 *
	 * @return	The protocol version boundaries.
	 */
	public List<String> getProtocolVersionBoundaries(){
		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<String> protocolVersionBoundaries = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues)
			protocolVersionBoundaries.addAll(configuration.getProtocolVersionBoundaries());
		return Collections.unmodifiableList(protocolVersionBoundaries);
	}

	/**
	 * Retrieve all the configuration regardless the protocol version.
	 *
	 * @return	The configuration messages regardless the protocol version.
	 */
	@SuppressWarnings("ObjectAllocationInLoop")
	public List<Map<String, Object>> getConfigurations() throws ConfigurationException{
		final Version currentProtocol = Version.of(EMPTY_STRING);

		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues){
			final ConfigurationHeader header = configuration.getHeader();
			if(!ManagerHelper.shouldBeExtracted(currentProtocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> headerMap = extractMap(currentProtocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(currentProtocol, configuration);
			response.add(Map.of(
				KEY_CONFIGURATION_HEADER, headerMap,
				KEY_CONFIGURATION_FIELDS, fieldsMap,
				KEY_CONFIGURATION_PROTOCOL_VERSION_BOUNDARIES, configuration.getProtocolVersionBoundaries()
			));
		}
		return Collections.unmodifiableList(response);
	}

	/**
	 * Retrieve all the configuration given a protocol version.
	 *
	 * @param protocol	The protocol used to extract the configurations.
	 * @return	The configuration messages for a given protocol version.
	 */
	@SuppressWarnings("ObjectAllocationInLoop")
	public List<Map<String, Object>> getConfigurations(final String protocol) throws ConfigurationException{
		if(StringHelper.isBlank(protocol))
			throw new IllegalArgumentException(StringHelper.format("Invalid protocol: {}", protocol));

		final Version currentProtocol = Version.of(protocol);

		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues){
			final ConfigurationHeader header = configuration.getHeader();
			if(!ManagerHelper.shouldBeExtracted(currentProtocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> headerMap = extractMap(currentProtocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(currentProtocol, configuration);
			response.add(Map.of(
				KEY_CONFIGURATION_HEADER, headerMap,
				KEY_CONFIGURATION_FIELDS, fieldsMap
			));
		}
		return Collections.unmodifiableList(response);
	}

	private static Map<String, Object> extractMap(final Version protocol, final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		ManagerHelper.putIfNotEmpty(map, KEY_SHORT_DESCRIPTION, header.shortDescription());
		ManagerHelper.putIfNotEmpty(map, KEY_LONG_DESCRIPTION, header.longDescription());
		if(protocol.isEmpty()){
			ManagerHelper.putIfNotEmpty(map, KEY_MIN_PROTOCOL, header.minProtocol());
			ManagerHelper.putIfNotEmpty(map, KEY_MAX_PROTOCOL, header.maxProtocol());
		}
		return map;
	}

	/**
	 * Retrieve the configuration by class, filled with data, and considering the protocol version.
	 *
	 * @param configurationType	The configuration message type.
	 * @param data   The data to load into the configuration.
	 * @param protocol   The protocol the data refers to.
	 * @return	The configuration.
	 */
	public ConfigurationPair getConfigurationWithDefaults(final String configurationType, final Map<String, Object> data, final Version protocol)
			throws EncodeException{
		final Configuration<?> configuration = getConfiguration(configurationType);
		final Object configurationObject = ReflectionHelper.getCreator(configuration.getType())
			.get();

		//fill in default values
		final List<ConfigField> configurableFields = configuration.getConfigurationFields();
		fillDefaultValues(configurationObject, configurableFields, protocol);


		//collect mandatory fields:
		final Collection<ConfigField> mandatoryFields = extractMandatoryFields(configurableFields, protocol);

		//load data into configuration based on protocol version:
		for(final Map.Entry<String, Object> entry : data.entrySet()){
			final String dataKey = entry.getKey();
			final Object dataValue = entry.getValue();

			//find field in `configuration` that matches `dataKey` and `protocol`
			final ConfigField foundField = findField(configurableFields, dataKey, protocol);
			final Annotation foundFieldAnnotation = foundField.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(foundFieldAnnotation);
			final Class<?> fieldType = foundField.getFieldType();
			manager.validateValue(dataKey, dataValue, fieldType);
			manager.setValue(configurationObject, dataKey, dataValue, foundField.getField(), protocol);

			if(String.class.isInstance(dataValue) && !((String)dataValue).isEmpty() || dataValue != null)
				mandatoryFields.remove(foundField);
		}

		//check mandatory fields
		validateMandatoryFields(mandatoryFields);

		return ConfigurationPair.of(configuration, configurationObject);
	}

	/**
	 * Retrieve the configuration by class.
	 *
	 * @return	The configuration.
	 */
	private Configuration<?> getConfiguration(final String configurationType) throws EncodeException{
		final Configuration<?> configuration = configurations.get(configurationType);
		if(configuration == null)
			throw EncodeException.create("Cannot find any configuration for given class type");

		return configuration;
	}

	private static void fillDefaultValues(final Object configurationObject, final List<ConfigField> fields, final Version protocol)
			throws EncodeException{
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
			final Object dataValue = manager.getDefaultValue(field.getField(), protocol);
			manager.setValue(configurationObject, manager.getShortDescription(), dataValue, field.getField(), protocol);
		}
	}

	private static Collection<ConfigField> extractMandatoryFields(final List<ConfigField> fields, final Version protocol){
		final Collection<ConfigField> mandatoryFields = new HashSet<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.shouldBeExtracted(protocol);
			if(manager.isMandatory(annotation))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private static ConfigField findField(final List<ConfigField> fields, final String key, final Version protocol) throws EncodeException{
		ConfigField foundField = null;
		for(int i = 0; foundField == null && i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.shouldBeExtracted(protocol);
			if(annotation.annotationType() != Annotation.class && manager.getShortDescription().equals(key))
				foundField = field;
		}
		if(foundField == null)
			throw EncodeException.create("Cannot find any field to set for data key {}", key);

		return foundField;
	}

	private static Map<String, Object> extractFieldsMap(final Version protocol, final Configuration<?> configuration)
			throws ConfigurationException{
		final List<ConfigField> fields = configuration.getConfigurationFields();
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(annotation);
			final Map<String, Object> fieldMap = manager.extractConfigurationMap(field.getFieldType(), protocol);
			if(!fieldMap.isEmpty()){
				final String shortDescription = manager.getShortDescription();
				fieldsMap.put(shortDescription, fieldMap);
			}
		}
		return fieldsMap;
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
