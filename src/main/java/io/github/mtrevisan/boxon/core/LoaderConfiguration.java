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

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSubField;
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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Pattern;


final class LoaderConfiguration{

	private static final String CONFIGURATION_COMPOSITE_FIELDS = "fields";

	private static final String NOTIFICATION_TEMPLATE = "compositeTemplate";
	private static final freemarker.template.Configuration FREEMARKER_CONFIGURATION
		= new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
	static{
		FREEMARKER_CONFIGURATION.setDefaultEncoding(StandardCharsets.UTF_8.name());
		FREEMARKER_CONFIGURATION.setLocale(Locale.US);
		FREEMARKER_CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}


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
		final Collection<Class<?>> annotatedClasses = LoaderHelper.extractClasses(ConfigurationHeader.class, basePackageClasses);
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
			loadConfigurationInner(configuration, start);
		}
		catch(final Exception e){
			eventListener.cannotLoadConfiguration(configuration.getType().getName(), e);
		}
	}

	private void loadConfigurationInner(final Configuration<?> configuration, final String headerStart) throws ConfigurationException{
		if(configurations.containsKey(headerStart))
			throw ConfigurationException.create("Duplicated key `{}` found for class {}", headerStart,
				configuration.getType().getName());

		configurations.put(headerStart, configuration);
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
	 * @param configurationType	The configuration message type.
	 * @param data   The data to load into the configuration.
	 * @param protocol   The protocol the data refers to.
	 * @return	The configuration.
	 */
	ConfigurationPair getConfiguration(final String configurationType, final Map<String, Object> data, final Version protocol)
			throws EncodeException{
		final Configuration<?> configuration = getConfiguration(configurationType, data);
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
			final ConfigField foundField = findField(configurableFields, protocol, dataKey);
			if(ConfigurationField.class.isInstance(foundField.getBinding())){
				final ConfigurationField foundBinding = (ConfigurationField)foundField.getBinding();

				validateValue(foundBinding, dataKey, dataValue, foundField);

				setValue(configurationObject, foundBinding.enumeration(), dataKey, dataValue, foundField);

				if(JavaHelper.isBlank(foundBinding.defaultValue()))
					mandatoryFields.remove(foundField);
			}
			else if(CompositeConfigurationField.class.isInstance(foundField.getBinding())){
				final CompositeConfigurationField foundBinding = (CompositeConfigurationField)foundField.getBinding();

				validateValue(foundBinding, dataKey, dataValue);

				//compose outer field value
				final String composition = foundBinding.composition();
				final ConfigurationSubField[] fields = foundBinding.value();
				@SuppressWarnings("unchecked")
				final String outerValue = replace(composition, (Map<String, Object>)dataValue, fields);
				setValue(configurationObject, outerValue, foundField);
			}
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
	private Configuration<?> getConfiguration(final String configurationType, final Map<String, Object> data) throws EncodeException{
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
				if(!JavaHelper.isBlank(binding.defaultValue()) && shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
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
			if(ConfigurationField.class.isAssignableFrom(field.getBinding().annotationType())){
				final ConfigurationField binding = (ConfigurationField)field.getBinding();
				mandatory = JavaHelper.isBlank(binding.defaultValue());
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(CompositeConfigurationField.class.isAssignableFrom(field.getBinding().annotationType())){
				final CompositeConfigurationField binding = (CompositeConfigurationField)field.getBinding();
				final ConfigurationSubField[] compositeFields = binding.value();
				for(int j = 0; j < compositeFields.length; j ++)
					mandatory |= !JavaHelper.isBlank(compositeFields[j].defaultValue());
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			if(mandatory && shouldBeExtracted(protocol, minProtocol, maxProtocol))
				mandatoryFields.add(field);
		}
		return mandatoryFields;
	}

	private static ConfigField findField(final List<ConfigField> fields, final Version protocol, final String key) throws EncodeException{
		ConfigField foundField = null;
		for(int i = 0; foundField == null && i < fields.size(); i ++){
			String shortDescription = null;
			String minProtocol = null;
			String maxProtocol = null;
			final ConfigField field = fields.get(i);
			if(ConfigurationField.class.isAssignableFrom(field.getBinding().annotationType())){
				final ConfigurationField binding = (ConfigurationField)field.getBinding();
				shortDescription = binding.shortDescription();
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(CompositeConfigurationField.class.isAssignableFrom(field.getBinding().annotationType())){
				final CompositeConfigurationField binding = (CompositeConfigurationField)field.getBinding();
				shortDescription = binding.shortDescription();
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			if(shortDescription.equals(key) && shouldBeExtracted(protocol, minProtocol, maxProtocol))
				foundField = field;
		}
		if(foundField == null)
			throw EncodeException.create("Cannot find any field to set for data key {}", key);

		return foundField;
	}

	private static void validateValue(final ConfigurationField binding, final String key, final Object value, final ConfigField field)
			throws EncodeException{
		//check pattern
		final String pattern = binding.pattern();
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//value compatible with data type and pattern
			if(!String.class.isInstance(value) || !formatPattern.matcher((CharSequence)value).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					key, value, pattern);
		}
		//check minValue
		final String minValue = binding.minValue();
		if(!minValue.isEmpty()){
			final Object min = JavaHelper.getValue(field.getFieldType(), minValue);
			if(Number.class.isInstance(value) && ((Number)value).doubleValue() < ((Number)min).doubleValue())
				throw EncodeException.create("Data value incompatible with minimum value for data key {}; found {}, expected greater than or equals to {}",
					key, value, minValue.getClass().getSimpleName());
		}
		//check maxValue
		final String maxValue = binding.maxValue();
		if(!maxValue.isEmpty()){
			final Object max = JavaHelper.getValue(field.getFieldType(), maxValue);
			if(Number.class.isInstance(value) && ((Number)value).doubleValue() > ((Number)max).doubleValue())
				throw EncodeException.create("Data value incompatible with maximum value for data key {}; found {}, expected greater than or equals to {}",
					key, value, maxValue.getClass().getSimpleName());
		}
	}

	private static void validateValue(final CompositeConfigurationField binding, final String key, final Object value)
			throws EncodeException{
		//check pattern
		final String pattern = binding.pattern();
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//compose outer field value
			final String composition = binding.composition();
			final ConfigurationSubField[] fields = binding.value();
			@SuppressWarnings("unchecked")
			final String outerValue = replace(composition, (Map<String, Object>)value, fields);

			//value compatible with data type and format
			if(!formatPattern.matcher(outerValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					key, outerValue, pattern);
		}
	}

	private static String replace(final String text, final Map<String, Object> replacements, final ConfigurationSubField[] fields)
			throws EncodeException{
		final Map<String, Object> trueReplacements = new HashMap<>(fields.length);
		for(int i = 0; i < fields.length; i ++){
			final String key = fields[i].shortDescription();
			trueReplacements.put(key, replacements.get(key));
		}
		return substitutePlaceholders(text, trueReplacements);
	}

	private static String substitutePlaceholders(final String text, final Map<String, Object> dataModel) throws EncodeException{
		if(dataModel != null){
			try{
				final Writer writer = new StringWriter();
				final Template template = new Template(NOTIFICATION_TEMPLATE, new StringReader(text), FREEMARKER_CONFIGURATION);

				//create a processing environment
				final Environment mainTemplateEnvironment = template.createProcessingEnvironment(dataModel, writer);

				//process everything
				mainTemplateEnvironment.process();

				return writer.toString();
			}
			catch(final IOException | TemplateException e){
				throw EncodeException.create(e);
			}
		}
		return text;
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
				final String[] defaultValues = JavaHelper.split((String)value, "|", -1);
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

	private static void validateMandatoryFields(final Collection<ConfigField> mandatoryFields) throws EncodeException{
		if(!mandatoryFields.isEmpty()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final ConfigField mandatoryField : mandatoryFields){
				String shortDescription = null;
				if(ConfigurationField.class.isInstance(mandatoryField.getBinding())){
					final ConfigurationField foundBinding = (ConfigurationField)mandatoryField.getBinding();
					shortDescription = foundBinding.shortDescription();
				}
				else if(CompositeConfigurationField.class.isInstance(mandatoryField.getBinding())){
					final CompositeConfigurationField foundBinding = (CompositeConfigurationField)mandatoryField.getBinding();
					shortDescription = foundBinding.shortDescription();
				}
				sj.add(shortDescription);
			}
			throw EncodeException.create("Mandatory fields missing: {}", sj.toString());
		}
	}

	private static Map<String, Object> extractFieldsMap(final Version protocol, final Configuration<?> configuration)
			throws ConfigurationException{
		final List<ConfigField> fields = configuration.getConfigurationFields();
		final Map<String, Object> fieldsMap = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);
			final Annotation fieldBinding = field.getBinding();
			if(ConfigurationField.class.isAssignableFrom(fieldBinding.annotationType())){
				final ConfigurationField binding = (ConfigurationField)fieldBinding;
				if(!shouldBeExtracted(protocol, binding.minProtocol(), binding.maxProtocol()))
					continue;

				final Class<?> fieldType = field.getFieldType();
				final Map<String, Object> fieldMap = extractMap(binding, fieldType);

				fieldsMap.put(binding.shortDescription(), fieldMap);
			}
			else if(CompositeConfigurationField.class.isAssignableFrom(fieldBinding.annotationType())){
				final CompositeConfigurationField compositeBinding = (CompositeConfigurationField)fieldBinding;
				if(!shouldBeExtracted(protocol, compositeBinding.minProtocol(), compositeBinding.maxProtocol()))
					continue;

				final Class<?> fieldType = field.getFieldType();
				final Map<String, Object> compositeMap = extractMap(compositeBinding);
				final ConfigurationSubField[] bindings = compositeBinding.value();
				final Map<String, Object> compositeFieldsMap = new HashMap<>(bindings.length);
				for(int j = 0; j < bindings.length; j ++){
					final Map<String, Object> fieldMap = extractMap(bindings[j], fieldType);

					compositeFieldsMap.put(bindings[j].shortDescription(), fieldMap);
				}
				compositeMap.put(CONFIGURATION_COMPOSITE_FIELDS, compositeFieldsMap);
				fieldsMap.put(compositeBinding.shortDescription(), compositeMap);
			}
		}
		return fieldsMap;
	}

	private static Map<String, Object> extractMap(final ConfigurationHeader header) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(3);
		putIfNotEmpty(map, "shortDescription", header.shortDescription());
		putIfNotEmpty(map, "longDescription", header.longDescription());
		return map;
	}

	private static Map<String, Object> extractMap(final ConfigurationField binding, final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

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

	private static Map<String, Object> extractMap(final CompositeConfigurationField binding) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "pattern", binding.pattern());
		putIfNotEmpty(map, "charset", binding.charset());

		return map;
	}

	private static Map<String, Object> extractMap(final ConfigurationSubField binding, final Class<?> fieldType)
			throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		putIfNotEmpty(map, "longDescription", binding.longDescription());
		putIfNotEmpty(map, "unitOfMeasure", binding.unitOfMeasure());

		putIfNotEmpty(map, "pattern", binding.pattern());

		putValueIfNotEmpty(map, "defaultValue", fieldType, NullEnum.class, binding.defaultValue());

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
				val = JavaHelper.split(value, "|", -1);
			else if(Number.class.isAssignableFrom(ParserDataType.toObjectiveTypeOrSelf(fieldType)))
				val = JavaHelper.getValue(fieldType, value);
			if(map.put(key, val) != null)
				throw ConfigurationException.create("Duplicated short description: {}", key);
		}
	}

	public static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = min.isEmpty() || protocol.isGreaterThanOrEqualTo(min);
		final boolean validMaximum = max.isEmpty() || protocol.isLessThanOrEqualTo(max);
		return (validMinimum && validMaximum);
	}

}
