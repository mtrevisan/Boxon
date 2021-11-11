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
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.Memoizer;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.ThrowingFunction;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


final class LoaderConfiguration{

	private static final String CONFIGURATION_COMPOSITE_FIELDS = "fields";

	private static final String EMPTY_STRING = "";


	static final class ConfigurationPair{
		private final Configuration<?> configuration;
		private final Object configurationData;

		static ConfigurationPair of(final Configuration<?> configuration, final Object configurationData){
			return new ConfigurationPair(configuration, configurationData);
		}

		private ConfigurationPair(final Configuration<?> configuration, final Object configurationData){
			this.configuration = configuration;
			this.configurationData = configurationData;
		}

		Configuration<?> getConfiguration(){
			return configuration;
		}

		Object getConfigurationData(){
			return configurationData;
		}
	}


	@InjectEventListener
	private final EventListener eventListener;

	private final ThrowingFunction<Class<?>, Configuration<?>, AnnotationException> configurationStore
		= Memoizer.throwingMemoize(Configuration::new);

	private final Map<String, Configuration<?>> configurations = new TreeMap<>(Comparator.comparingInt(String::length).reversed()
		.thenComparing(String::compareTo));


	LoaderConfiguration(final EventListener eventListener){
		this.eventListener = eventListener;
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	void loadDefaultConfigurations() throws AnnotationException, ConfigurationException{
		loadConfigurations(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 */
	void loadConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
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
				loadConfigurationInner(header.start(), from);
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
	List<String> getProtocolVersionBoundaries(){
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
	List<Map<String, Object>> getConfigurations() throws ConfigurationException{
		final Version currentProtocol = Version.of(EMPTY_STRING);

		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues){
			final ConfigurationHeader header = configuration.getHeader();
			if(!ConfigurationValidatorHelper.shouldBeExtracted(currentProtocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> headerMap = extractMap(currentProtocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(currentProtocol, configuration);
			response.add(Map.of(
				"header", headerMap,
				"fields", fieldsMap,
				"protocolVersionBoundaries", configuration.getProtocolVersionBoundaries()
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
	List<Map<String, Object>> getConfigurations(final String protocol) throws ConfigurationException{
		if(JavaHelper.isBlank(protocol))
			throw new IllegalArgumentException(JavaHelper.format("Invalid protocol: {}", protocol));

		final Version currentProtocol = Version.of(protocol);

		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues){
			final ConfigurationHeader header = configuration.getHeader();
			if(!ConfigurationValidatorHelper.shouldBeExtracted(currentProtocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> headerMap = extractMap(currentProtocol, header);
			final Map<String, Object> fieldsMap = extractFieldsMap(currentProtocol, configuration);
			response.add(Map.of(
				"header", headerMap,
				"fields", fieldsMap
			));
		}
		return Collections.unmodifiableList(response);
	}

	private static Map<String, Object> extractMap(final Version protocol, final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		putIfNotEmpty(map, "shortDescription", header.shortDescription());
		putIfNotEmpty(map, "longDescription", header.longDescription());
		if(protocol.isEmpty()){
			putIfNotEmpty(map, "minProtocol", header.minProtocol());
			putIfNotEmpty(map, "maxProtocol", header.maxProtocol());
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
	ConfigurationPair getConfigurationWithDefaults(final String configurationType, final Map<String, Object> data, final Version protocol)
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
			if(ConfigurationField.class.isInstance(foundFieldAnnotation)){
				final ConfigurationField foundBinding = (ConfigurationField)foundFieldAnnotation;

				ConfigurationValidatorHelper.validateValue(foundBinding, dataKey, dataValue, foundField);

				setValue(configurationObject, foundBinding.enumeration(), dataKey, dataValue, foundField);
			}
			else if(CompositeConfigurationField.class.isInstance(foundFieldAnnotation)){
				final CompositeConfigurationField foundBinding = (CompositeConfigurationField)foundFieldAnnotation;

				ConfigurationValidatorHelper.validateValue(foundBinding, dataKey, dataValue);

				//compose outer field value
				final String composition = foundBinding.composition();
				final CompositeSubField[] fields = foundBinding.value();
				@SuppressWarnings("unchecked")
				final String outerValue = ConfigurationValidatorHelper.replace(composition, (Map<String, Object>)dataValue, fields);
				setValue(configurationObject, outerValue, foundField);
			}
			else if(AlternativeConfigurationField.class.isInstance(foundFieldAnnotation)){
				final AlternativeConfigurationField binding = (AlternativeConfigurationField)foundFieldAnnotation;

				if(ConfigurationValidatorHelper.shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol())){
					final AlternativeSubField fieldBinding = extractField(binding, protocol);
					if(fieldBinding != null){
						ConfigurationValidatorHelper.validateValue(fieldBinding, dataKey, dataValue, foundField);

						setValue(configurationObject, binding.enumeration(), dataKey, dataValue, foundField);
					}
				}
			}

			if(String.class.isInstance(dataValue) && !((String)dataValue).isEmpty() || dataValue != null)
				mandatoryFields.remove(foundField);
		}

		//check mandatory fields
		ConfigurationValidatorHelper.validateMandatoryFields(mandatoryFields);

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

	private static void fillDefaultValues(final Object object, final List<ConfigField> fields, final Version protocol)
			throws EncodeException{
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			if(ConfigurationField.class.isAssignableFrom(field.getBinding().annotationType())){
				final ConfigurationField binding = (ConfigurationField)field.getBinding();
				if(!JavaHelper.isBlank(binding.defaultValue())
						&& ConfigurationValidatorHelper.shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
					setValue(object, binding.enumeration(), binding.shortDescription(), binding.defaultValue(), field);
			}
		}
	}

	private static Collection<ConfigField> extractMandatoryFields(final List<ConfigField> fields, final Version protocol){
		final Collection<ConfigField> mandatoryFields = new HashSet<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			boolean mandatory = false;
			String minProtocol = null;
			String maxProtocol = null;
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			if(ConfigurationField.class.isInstance(annotation)){
				final ConfigurationField binding = (ConfigurationField)annotation;
				mandatory = JavaHelper.isBlank(binding.defaultValue());
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(CompositeConfigurationField.class.isInstance(annotation)){
				final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;
				final CompositeSubField[] compositeFields = binding.value();
				for(int j = 0; !mandatory && j < compositeFields.length; j ++)
					mandatory = JavaHelper.isBlank(compositeFields[j].defaultValue());
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(AlternativeConfigurationField.class.isInstance(annotation)){
				final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;

				final AlternativeSubField fieldBinding = extractField(binding, protocol);
				if(fieldBinding != null){
					mandatory = JavaHelper.isBlank(fieldBinding.defaultValue());
					minProtocol = binding.minProtocol();
					maxProtocol = binding.maxProtocol();
				}
			}
			if(mandatory && ConfigurationValidatorHelper.shouldBeExtracted(protocol, minProtocol, maxProtocol))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private static ConfigField findField(final List<ConfigField> fields, final String key, final Version protocol) throws EncodeException{
		ConfigField foundField = null;
		for(int i = 0; foundField == null && i < fields.size(); i ++){
			String shortDescription = null;
			String minProtocol = null;
			String maxProtocol = null;
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			if(ConfigurationField.class.isInstance(annotation)){
				final ConfigurationField binding = (ConfigurationField)annotation;
				shortDescription = binding.shortDescription();
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(CompositeConfigurationField.class.isInstance(annotation)){
				final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;
				shortDescription = binding.shortDescription();
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(AlternativeConfigurationField.class.isInstance(annotation)){
				final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;
				shortDescription = binding.shortDescription();

				final AlternativeSubField fieldBinding = extractField(binding, protocol);
				if(fieldBinding != null){
					minProtocol = binding.minProtocol();
					maxProtocol = binding.maxProtocol();
				}
			}
			if(shortDescription.equals(key) && ConfigurationValidatorHelper.shouldBeExtracted(protocol, minProtocol, maxProtocol))
				foundField = field;
		}
		if(foundField == null)
			throw EncodeException.create("Cannot find any field to set for data key {}", key);

		return foundField;
	}

	private static void setValue(final Object object, final Class<? extends Enum<?>> enumeration, final String key, final Object value,
			final ConfigField field) throws EncodeException{
		if(enumeration != NullEnum.class){
			if(!String.class.isInstance(value))
				throw EncodeException.create("Data value incompatible with field type {}; found {}, expected String.class",
					key, value.getClass());

			final Object dataEnum;
			final Enum<?>[] enumConstants = enumeration.getEnumConstants();
			if(field.getFieldType().isArray()){
				final String[] defaultValues = JavaHelper.split((String)value, '|', -1);
				dataEnum = Array.newInstance(enumeration, defaultValues.length);
				for(int i = 0; i < defaultValues.length; i ++)
					Array.set(dataEnum, i, JavaHelper.extractEnum(enumConstants, defaultValues[i]));
			}
			else
				dataEnum = enumeration
					.cast(JavaHelper.extractEnum(enumConstants, (String)value));
			field.setFieldValue(object, dataEnum);
		}
		else if(String.class.isInstance(value)){
			final Object val = JavaHelper.getValue(field.getFieldType(), (String)value);
			field.setFieldValue(object, val);
		}
		else
			field.setFieldValue(object, value);
	}

	private static void setValue(final Object object, final Object value, final ConfigField field) throws EncodeException{
		setValue(object, NullEnum.class, null, value, field);
	}

	private static Map<String, Object> extractFieldsMap(final Version protocol, final Configuration<?> configuration)
			throws ConfigurationException{
		final List<ConfigField> fields = configuration.getConfigurationFields();
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final Annotation annotation = field.getBinding();
			if(ConfigurationField.class.isInstance(annotation)){
				final ConfigurationField binding = (ConfigurationField)annotation;
				final Map<String, Object> fieldMap = extractFieldMap(field, binding, protocol);
				if(fieldMap != null)
					fieldsMap.put(binding.shortDescription(), fieldMap);
			}
			else if(CompositeConfigurationField.class.isInstance(annotation)){
				final CompositeConfigurationField compositeBinding = (CompositeConfigurationField)annotation;
				final Map<String, Object> compositeMap = extractCompositeFieldMap(field, compositeBinding, protocol);
				if(compositeMap != null)
					fieldsMap.put(compositeBinding.shortDescription(), compositeMap);
			}
			else if(AlternativeConfigurationField.class.isInstance(annotation)){
				final AlternativeConfigurationField alternativeBinding = (AlternativeConfigurationField)annotation;
				final Map<String, Object> alternativeMap = extractAlternativeField(field, alternativeBinding, protocol);
				if(alternativeMap != null)
					fieldsMap.put(alternativeBinding.shortDescription(), alternativeMap);
			}
		}
		return fieldsMap;
	}

	private static Map<String, Object> extractFieldMap(final ConfigField field, final ConfigurationField binding,
			final Version protocol) throws ConfigurationException{
		if(!ConfigurationValidatorHelper.shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
			return null;

		final Class<?> fieldType = field.getFieldType();
		final Map<String, Object> fieldMap = extractMap(binding, fieldType);

		if(protocol.isEmpty()){
			putIfNotEmpty(fieldMap, "minProtocol", binding.minProtocol());
			putIfNotEmpty(fieldMap, "maxProtocol", binding.maxProtocol());
		}
		return fieldMap;
	}

	private static Map<String, Object> extractMap(final ConfigurationField binding, final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, "minValue", JavaHelper.getValue(fieldType, binding.minValue()));
		putIfNotEmpty(map, "maxValue", JavaHelper.getValue(fieldType, binding.maxValue()));
		putIfNotEmpty(map, "pattern", binding.pattern());
		if(binding.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = binding.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, "enumeration", enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, "mutuallyExclusive", true);
		}

		putValueIfNotEmpty(map, "defaultValue", fieldType, binding.enumeration(), binding.defaultValue());
		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, "charset", binding.charset());

		return map;
	}

	private static Map<String, Object> extractCompositeFieldMap(final ConfigField field, final CompositeConfigurationField binding,
			final Version protocol) throws ConfigurationException{
		if(!ConfigurationValidatorHelper.shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
			return null;

		final Class<?> fieldType = field.getFieldType();
		final Map<String, Object> compositeMap = extractMap(binding);
		final CompositeSubField[] bindings = binding.value();
		final Map<String, Object> compositeFieldsMap = new HashMap<>(bindings.length);
		for(int j = 0; j < bindings.length; j ++){
			final Map<String, Object> fieldMap = extractMap(bindings[j], fieldType);

			compositeFieldsMap.put(bindings[j].shortDescription(), fieldMap);
		}
		compositeMap.put(CONFIGURATION_COMPOSITE_FIELDS, compositeFieldsMap);

		if(protocol.isEmpty()){
			putIfNotEmpty(compositeMap, "minProtocol", binding.minProtocol());
			putIfNotEmpty(compositeMap, "maxProtocol", binding.maxProtocol());
		}
		return compositeMap;
	}

	private static Map<String, Object> extractMap(final CompositeSubField binding, final Class<?> fieldType)
		throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		putIfNotEmpty(map, "pattern", binding.pattern());
		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());

		putValueIfNotEmpty(map, "defaultValue", fieldType, NullEnum.class, binding.defaultValue());

		return map;
	}

	private static Map<String, Object> extractAlternativeField(final ConfigField field, final AlternativeConfigurationField binding,
			final Version protocol) throws ConfigurationException{
		if(!ConfigurationValidatorHelper.shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
			return null;

		final Class<?> fieldType = field.getFieldType();
		final Map<String, Object> alternativeMap = extractMap(binding, fieldType);

		Map<String, Object> alternativesMap = null;
		if(protocol.isEmpty()){
			//extract all the alternatives, because it was requested all the configurations regardless of protocol:
			final AlternativeSubField[] alternativeFields = binding.value();
			final Collection<Map<String, Object>> alternatives = new ArrayList<>(alternativeFields.length);
			for(int j = 0; j < alternativeFields.length; j ++){
				final AlternativeSubField alternativeField = alternativeFields[j];

				final Map<String, Object> fieldMap = extractMap(alternativeField, fieldType);

				putIfNotEmpty(fieldMap, "minProtocol", alternativeField.minProtocol());
				putIfNotEmpty(fieldMap, "maxProtocol", alternativeField.maxProtocol());
				putValueIfNotEmpty(fieldMap, "defaultValue", fieldType, binding.enumeration(), alternativeField.defaultValue());

				fieldMap.putAll(alternativeMap);

				alternatives.add(fieldMap);
			}
			alternativesMap = new HashMap<>(3);
			alternativesMap.put("alternatives", alternatives);
			putIfNotEmpty(alternativesMap, "minProtocol", binding.minProtocol());
			putIfNotEmpty(alternativesMap, "maxProtocol", binding.maxProtocol());
		}
		else{
			//extract the specific alternative, because it was requested the configuration of a particular protocol:
			final AlternativeSubField fieldBinding = extractField(binding, protocol);
			if(fieldBinding != null){
				alternativesMap = extractMap(fieldBinding, fieldType);

				putValueIfNotEmpty(alternativesMap, "defaultValue", fieldType, binding.enumeration(), fieldBinding.defaultValue());

				alternativesMap.putAll(alternativeMap);

			}
		}
		return alternativesMap;
	}

	private static Map<String, Object> extractMap(final AlternativeConfigurationField binding, final Class<?> fieldType)
		throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		if(binding.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = binding.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, "enumeration", enumValues);
			if(fieldType.isEnum())
				putIfNotEmpty(map, "mutuallyExclusive", true);
		}

		return map;
	}

	private static Map<String, Object> extractMap(final AlternativeSubField binding, final Class<?> fieldType)
		throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		if(!fieldType.isEnum() && !fieldType.isArray())
			putIfNotEmpty(map, "fieldType", ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());
		putIfNotEmpty(map, "minValue", JavaHelper.getValue(fieldType, binding.minValue()));
		putIfNotEmpty(map, "maxValue", JavaHelper.getValue(fieldType, binding.maxValue()));
		putIfNotEmpty(map, "pattern", binding.pattern());

		if(String.class.isAssignableFrom(fieldType))
			putIfNotEmpty(map, "charset", binding.charset());

		return map;
	}

	private static AlternativeSubField extractField(final AlternativeConfigurationField binding, final Version protocol){
		final AlternativeSubField[] alternativeFields = binding.value();
		for(int i = 0; i < alternativeFields.length; i ++){
			final AlternativeSubField fieldBinding = alternativeFields[i];
			if(ConfigurationValidatorHelper.shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				return fieldBinding;
		}
		return null;
	}

	private static Map<String, Object> extractMap(final CompositeConfigurationField binding) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "pattern", binding.pattern());
		putIfNotEmpty(map, "charset", binding.charset());

		return map;
	}

	private static void putIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Object value) throws ConfigurationException{
		if(value != null && (!String.class.isInstance(value) || !JavaHelper.isBlank((CharSequence)value)))
			if(map.put(key, value) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
	}

	private static void putValueIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Class<?> fieldType, final Class<? extends Enum<?>> enumeration, final String value) throws ConfigurationException{
		if(!JavaHelper.isBlank(value)){
			Object val = value;
			if(enumeration != NullEnum.class && fieldType.isArray())
				val = JavaHelper.split(value, '|', -1);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			if(map.put(key, val) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

}
