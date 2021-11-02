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
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationMessage;
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

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
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
import java.util.regex.Pattern;


final class LoaderConfiguration{

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

		public Object getConfigurationData(){
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
	 * Loads all the configuration classes annotated with {@link ConfigurationMessage}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	void loadDefaultConfigurations() throws AnnotationException, ConfigurationException{
		loadConfigurations(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationMessage}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 */
	void loadConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		eventListener.loadingConfigurations(basePackageClasses);

		/** extract all classes annotated with {@link MessageHeader}. */
		final Collection<Class<?>> annotatedClasses = LoaderHelper.extractClasses(ConfigurationMessage.class, basePackageClasses);
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
				final ConfigurationMessage header = from.getHeader();
				final String start = header.start();
				final Charset charset = Charset.forName(header.charset());
				final String k = LoaderHelper.calculateKey(start, charset);
				configurations.put(k, from);
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
			final ConfigurationMessage header = configuration.getHeader();
			final String start = header.start();
			final Charset charset = Charset.forName(header.charset());
			loadConfigurationInner(configuration, start, charset);
		}
		catch(final Exception e){
			eventListener.cannotLoadConfiguration(configuration.getType().getName(), e);
		}
	}

	private void loadConfigurationInner(final Configuration<?> configuration, final String headerStart, final Charset charset)
			throws ConfigurationException{
		final String key = LoaderHelper.calculateKey(headerStart, charset);
		if(configurations.containsKey(key))
			throw ConfigurationException.create("Duplicated key `{}` found for class {}", headerStart,
				configuration.getType().getName());

		configurations.put(key, configuration);
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

		final Version currentProtocol = new Version(protocol);

		final Collection<Configuration<?>> configurationValues = configurations.values();
		final List<Map<String, Object>> response = new ArrayList<>(configurationValues.size());
		for(final Configuration<?> configuration : configurationValues){
			final ConfigurationMessage header = configuration.getHeader();
			if(!shouldBeExtracted(currentProtocol, header.minProtocol(), header.maxProtocol()))
				continue;

			final Map<String, Object> headerMap = extractMap(header);
			final Map<String, Object> fieldsMap = extractFieldsMap(currentProtocol, configuration);
			response.add(Map.of(
				"header", headerMap,
				"fields", fieldsMap
			));
		}
		return Collections.unmodifiableList(response);
	}

	/**
	 * Retrieve the configuration by class, filled with data, and considering the protocol version.
	 *
	 * @param data   The data to load into the configuration.
	 * @param protocol   The protocol the data refers to.
	 * @return	The configuration.
	 */
	ConfigurationPair getConfiguration(final Map<String, Object> data, final Version protocol) throws EncodeException{
		final Configuration<?> configuration = getConfiguration(data);
		final Object configurationObject = ReflectionHelper.getCreator(configuration.getType())
			.get();

		//fill in default values
		final List<ConfigurationField> configurableFields = configuration.getConfigurationFields();
		fillDefaultValues(configurationObject, configurableFields, protocol);


		//collect mandatory fields:
		final Collection<ConfigurationField> mandatoryFields = extractMandatoryFields(configurableFields, protocol);

		//load data into configuration based on protocol version:
		for(final Map.Entry<String, Object> entry : data.entrySet()){
			final String dataKey = entry.getKey();
			final Object dataValue = entry.getValue();

			//find field in `configuration` that matches `dataKey` and `protocol`
			final ConfigurationField foundField = findField(configurableFields, protocol, dataKey);
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField foundBinding
				= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)foundField.getBinding();

			validateValue(foundBinding, dataKey, dataValue, foundField);

			setValue(configurationObject, foundBinding, dataKey, dataValue, foundField);

			if(foundBinding.mandatory())
				mandatoryFields.remove(foundField);
		}

		//check mandatory fields
		validateMandatoryFields(mandatoryFields);

		return ConfigurationPair.of(configuration, configurationObject);
	}

	/**
	 * Retrieve the configuration by class.
	 *
	 * @param data   The data to load into the configuration.
	 * @return	The configuration.
	 */
	private Configuration<?> getConfiguration(final Map<String, Object> data) throws EncodeException{
		final String headerStart = (String)data.remove("__type__");
		final Object charsetField = data.remove("__charset__");
		final Charset charset;
		try{
			charset = Charset.forName((String)charsetField);
		}
		catch(final ClassCastException | UnsupportedCharsetException e){
			throw EncodeException.create("Missing, or not recognized, mandatory field on data: `__charset__`, found: {}",
				charsetField);
		}
		if(JavaHelper.isBlank(headerStart))
			throw EncodeException.create("Missing mandatory field on data: `__type__`");

		final String key = LoaderHelper.calculateKey(headerStart, charset);
		final Configuration<?> configuration = configurations.get(key);
		if(configuration == null)
			throw EncodeException.create("Cannot find any configuration for given class type");

		return configuration;
	}

	private void fillDefaultValues(final Object object, final List<ConfigurationField> fields, final Version protocol)
			throws EncodeException{
		for(int i = 0; i < fields.size(); i ++){
			final ConfigurationField field = fields.get(i);
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding
				= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)field.getBinding();
			if(!JavaHelper.isBlank(binding.defaultValue()) && shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
				setValue(object, binding, binding.shortDescription(), binding.defaultValue(), field);
		}
	}

	private Collection<ConfigurationField> extractMandatoryFields(final List<ConfigurationField> fields, final Version protocol){
		final Collection<ConfigurationField> mandatoryFields = new HashSet<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigurationField field = fields.get(i);
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding
				= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)field.getBinding();
			if(binding.mandatory() && shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private ConfigurationField findField(final List<ConfigurationField> fields, final Version protocol, final String key)
			throws EncodeException{
		ConfigurationField foundField = null;
		for(int i = 0; foundField == null && i < fields.size(); i ++){
			final ConfigurationField field = fields.get(i);
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding
				= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)field.getBinding();
			if(binding.shortDescription().equals(key) && shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
				foundField = field;
		}
		if(foundField == null)
			throw EncodeException.create("Cannot find any field to set for data key {}", key);

		return foundField;
	}

	private void validateValue(final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding,
			final String key, final Object value, final ConfigurationField field) throws EncodeException{
		//check format
		final String format = binding.format();
		if(!format.isEmpty()){
			final Pattern formatPattern = Pattern.compile(format);

			//value compatible with data type and format
			if(!String.class.isAssignableFrom(value.getClass()) || !formatPattern.matcher((CharSequence)value).matches())
				throw EncodeException.create("Data value not compatible with `format` for data key {}; found {}, expected {}",
					key, value, format);
		}
		//check minValue
		final String minValue = binding.minValue();
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(field.getFieldType(), minValue);
			if(Number.class.isAssignableFrom(value.getClass()) && ((Number)value).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}",
					key, value, minValue.getClass().getSimpleName());
		}
		//check maxValue
		final String maxValue = binding.maxValue();
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(field.getFieldType(), maxValue);
			if(Number.class.isAssignableFrom(value.getClass()) && ((Number)value).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}",
					key, value, maxValue.getClass().getSimpleName());
		}
	}

	private void setValue(final Object object,
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding, final String key, final Object value,
			final ConfigurationField field) throws EncodeException{
		final Class<? extends Enum<?>> foundEnumeration = binding.enumeration();
		if(foundEnumeration != NullEnum.class){
			if(!String.class.isAssignableFrom(value.getClass()))
				throw EncodeException.create("Data value incompatible with field type {}; found {}, expected String.class",
					key, value.getClass());

			final Object dataEnum;
			final Enum<?>[] enumConstants = foundEnumeration.getEnumConstants();
			if(!binding.mutuallyExclusive()){
				final String[] defaultValues = JavaHelper.split((String)value, "|", -1);
				dataEnum = Array.newInstance(foundEnumeration, defaultValues.length);
				for(int i = 0; i < defaultValues.length; i ++)
					Array.set(dataEnum, i, JavaHelper.extractEnum(enumConstants, defaultValues[i]));
			}
			else
				dataEnum = foundEnumeration
					.cast(JavaHelper.extractEnum(enumConstants, (String)value));
			field.setFieldValue(object, dataEnum);
		}
		else if(String.class.isAssignableFrom(value.getClass())){
			final Object val = JavaHelper.getValue(field.getFieldType(), (String)value);
			field.setFieldValue(object, val);
		}
		else
			field.setFieldValue(object, value);
	}

	private void validateMandatoryFields(final Collection<ConfigurationField> mandatoryFields) throws EncodeException{
		if(!mandatoryFields.isEmpty()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final ConfigurationField mandatoryField : mandatoryFields){
				final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField foundBinding
					= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)mandatoryField.getBinding();
				sj.add(foundBinding.shortDescription());
			}
			throw EncodeException.create("Mandatory fields missing: {}", sj.toString());
		}
	}

	private Map<String, Object> extractFieldsMap(final Version protocol, final Configuration<?> configuration) throws ConfigurationException{
		final List<ConfigurationField> fields = configuration.getConfigurationFields();
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigurationField field = fields.get(i);
			final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField fieldBinding
				= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)field.getBinding();
			if(!shouldBeExtracted(protocol, fieldBinding.minProtocol(), fieldBinding.maxProtocol()))
				continue;

			final Class<?> fieldType = field.getFieldType();
			final Map<String, Object> fieldMap = extractMap(fieldBinding, fieldType);

			fieldsMap.put(fieldBinding.shortDescription(), fieldMap);
		}
		return fieldsMap;
	}

	private Map<String, Object> extractMap(final ConfigurationMessage header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		putIfNotEmpty(map, "shortDescription", header.shortDescription());
		putIfNotEmpty(map, "longDescription", header.longDescription());
		putIfNotEmpty(map, "charset", header.charset());
		putIfNotEmpty(map, "minProtocol", header.minProtocol());
		putIfNotEmpty(map, "maxProtocol", header.maxProtocol());
		return map;
	}

	private Map<String, Object> extractMap(final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding,
			final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);
		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());
		putIfNotEmpty(map, "minValue", JavaHelper.getValue(fieldType, binding.minValue()));
		putIfNotEmpty(map, "maxValue", JavaHelper.getValue(fieldType, binding.maxValue()));
		putIfNotEmpty(map, "format", binding.format());
		if(binding.enumeration() != NullEnum.class){
			final Enum<?>[] enumConstants = binding.enumeration().getEnumConstants();
			final String[] enumValues = new String[enumConstants.length];
			for(int j = 0; j < enumConstants.length; j ++)
				enumValues[j] = enumConstants[j].name();
			putIfNotEmpty(map, "enumeration", enumValues);
			putIfNotEmpty(map, "mutuallyExclusive", binding.mutuallyExclusive());
		}
		putIfNotEmpty(map, "mandatory", binding.mandatory());
		putValueIfNotEmpty(map, "defaultValue", fieldType, binding.enumeration(), binding.defaultValue());
		putIfNotEmpty(map, "writable", binding.writable());
		return map;
	}

	private void putIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key, final Object value)
			throws ConfigurationException{
		if(value != null && (!(value instanceof String) || !JavaHelper.isBlank((CharSequence)value))){
			final Object previousValue = map.put(key, value);
			if(previousValue != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	private void putValueIfNotEmpty(@SuppressWarnings("BoundedWildcard") final Map<String, Object> map, final String key,
			final Class<?> fieldType, final Class<? extends Enum<?>> enumeration, final String value) throws ConfigurationException{
		if(!JavaHelper.isBlank(value)){
			Object val = value;
			if(enumeration != NullEnum.class)
				val = JavaHelper.split(value, "|", -1);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			final Object previousValue = map.put(key, val);
			if(previousValue != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	public static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		final Version min = new Version(minProtocol);
		final Version max = new Version(maxProtocol);
		return (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min)) && (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
	}

}
