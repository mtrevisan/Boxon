package io.github.mtrevisan.boxon.core.codecs;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.core.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


final class CompositeManager implements ConfigurationManagerInterface{

	private static final String NOTIFICATION_TEMPLATE = "compositeTemplate";
	private static final Configuration FREEMARKER_CONFIGURATION = new Configuration(Configuration.VERSION_2_3_31);

	static{
		FREEMARKER_CONFIGURATION.setDefaultEncoding(StandardCharsets.UTF_8.name());
		FREEMARKER_CONFIGURATION.setLocale(Locale.US);
		FREEMARKER_CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}


	private final CompositeConfigurationField annotation;


	CompositeManager(final CompositeConfigurationField annotation){
		this.annotation = annotation;
	}

	@Override
	public String getShortDescription(){
		return annotation.shortDescription();
	}

	@Override
	public Object getDefaultValue(final Field field, final Version protocol) throws EncodeException{
		//compose field value
		final String composition = annotation.composition();
		final CompositeSubField[] fields = annotation.value();
		final Map<String, Object> dataValue = new HashMap<>(fields.length);
		for(int i = 0; i < fields.length; i ++)
			dataValue.put(fields[i].shortDescription(), fields[i].defaultValue());
		return replace(composition, dataValue, fields);
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> protocolVersionBoundaries){
		protocolVersionBoundaries.add(annotation.minProtocol());
		protocolVersionBoundaries.add(annotation.maxProtocol());
	}

	@Override
	public Annotation shouldBeExtracted(final Version protocol){
		final boolean shouldBeExtracted = ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: PlainManager.EMPTY_ANNOTATION);
	}

	//at least one field is mandatory
	@Override
	public boolean isMandatory(final Annotation annotation){
		boolean mandatory = false;
		final CompositeSubField[] compositeFields = this.annotation.value();
		for(int j = 0; !mandatory && j < compositeFields.length; j ++)
			mandatory = StringHelper.isBlank(compositeFields[j].defaultValue());
		return mandatory;
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException{
		if(!ManagerHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> compositeMap = extractMap();
		final CompositeSubField[] bindings = annotation.value();
		final Map<String, Object> compositeFieldsMap = new HashMap<>(bindings.length);
		for(int j = 0; j < bindings.length; j ++){
			final Map<String, Object> fieldMap = extractMap(bindings[j], fieldType);

			compositeFieldsMap.put(bindings[j].shortDescription(), fieldMap);
		}
		compositeMap.put(LoaderConfiguration.KEY_CONFIGURATION_COMPOSITE_FIELDS, compositeFieldsMap);

		if(protocol.isEmpty()){
			ManagerHelper.putIfNotEmpty(compositeMap, LoaderConfiguration.KEY_MIN_PROTOCOL, annotation.minProtocol());
			ManagerHelper.putIfNotEmpty(compositeMap, LoaderConfiguration.KEY_MAX_PROTOCOL, annotation.maxProtocol());
		}
		return compositeMap;
	}

	private Map<String, Object> extractMap() throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_LONG_DESCRIPTION, annotation.longDescription());
		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_PATTERN, annotation.pattern());
		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_CHARSET, annotation.charset());

		return map;
	}

	private static Map<String, Object> extractMap(final CompositeSubField binding, final Class<?> fieldType) throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(10);

		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_LONG_DESCRIPTION, binding.longDescription());
		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_UNIT_OF_MEASURE, binding.unitOfMeasure());

		ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_PATTERN, binding.pattern());
		if(!fieldType.isEnum() && !fieldType.isArray())
			ManagerHelper.putIfNotEmpty(map, LoaderConfiguration.KEY_FIELD_TYPE,
				ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName());

		ManagerHelper.putValueIfNotEmpty(map, LoaderConfiguration.KEY_DEFAULT_VALUE, fieldType, NullEnum.class, binding.defaultValue());

		return map;
	}

	@Override
	public void validateValue(final String dataKey, final Object dataValue, final Class<?> fieldType) throws EncodeException{
		//check pattern
		final String pattern = annotation.pattern();
		if(!pattern.isEmpty()){
			final Pattern formatPattern = Pattern.compile(pattern);

			//compose outer field value
			final String composition = annotation.composition();
			final CompositeSubField[] fields = annotation.value();
			@SuppressWarnings("unchecked")
			final String outerValue = replace(composition, (Map<String, Object>)dataValue, fields);

			//value compatible with data type and format
			if(!formatPattern.matcher(outerValue).matches())
				throw EncodeException.create("Data value not compatible with `pattern` for data key {}; found {}, expected {}",
					dataKey, outerValue, pattern);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setValue(final Object configurationObject, final String dataKey, Object dataValue, final Field field, final Version protocol)
			throws EncodeException{
		//compose field value
		final String composition = annotation.composition();
		final CompositeSubField[] fields = annotation.value();
		if(Map.class.isInstance(dataValue))
			dataValue = replace(composition, (Map<String, Object>)dataValue, fields);
		ManagerHelper.setValue(field, configurationObject, dataValue);
	}

	private static String replace(final String text, final Map<String, Object> replacements, final CompositeSubField[] fields)
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

}
