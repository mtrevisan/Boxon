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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;

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
		final int length = fields.length;
		final Map<String, Object> dataValue = new HashMap<>(length);
		for(int i = 0; i < length; i ++)
			dataValue.put(fields[i].shortDescription(), fields[i].defaultValue());
		return replace(composition, dataValue, fields);
	}

	@Override
	public void addProtocolVersionBoundaries(final Collection<String> boundaries){
		boundaries.add(annotation.minProtocol());
		boundaries.add(annotation.maxProtocol());
	}

	@Override
	public Annotation annotationToBeProcessed(final Version protocol){
		final boolean shouldBeExtracted = ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol());
		return (shouldBeExtracted? annotation: PlainManager.EMPTY_ANNOTATION);
	}

	//at least one field is mandatory
	@Override
	public boolean isMandatory(final Annotation annotation){
		boolean mandatory = false;
		final CompositeSubField[] compositeFields = this.annotation.value();
		for(int j = 0, length = compositeFields.length; !mandatory && j < length; j ++)
			mandatory = StringHelper.isBlank(compositeFields[j].defaultValue());
		return mandatory;
	}

	@Override
	public Map<String, Object> extractConfigurationMap(final Class<?> fieldType, final Version protocol) throws ConfigurationException,
			CodecException{
		if(!ConfigurationHelper.shouldBeExtracted(protocol, annotation.minProtocol(), annotation.maxProtocol()))
			return Collections.emptyMap();

		final Map<String, Object> compositeMap = extractMap();
		final CompositeSubField[] bindings = annotation.value();
		final int length = bindings.length;
		final Map<String, Object> compositeFieldsMap = new HashMap<>(length);
		for(int j = 0; j < length; j ++){
			final Map<String, Object> fieldMap = extractMap(bindings[j], fieldType);

			compositeFieldsMap.put(bindings[j].shortDescription(), fieldMap);
		}
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.COMPOSITE_FIELDS, compositeFieldsMap, compositeMap);

		if(protocol.isEmpty())
			ConfigurationHelper.extractMinMaxProtocol(annotation.minProtocol(), annotation.maxProtocol(), compositeMap);

		return Collections.unmodifiableMap(compositeMap);
	}

	private Map<String, Object> extractMap() throws ConfigurationException{
		final Map<String, Object> map = new HashMap<>(6);

		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, annotation.longDescription(), map);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.PATTERN, annotation.pattern(), map);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.CHARSET, annotation.charset(), map);

		return map;
	}

	private static Map<String, Object> extractMap(final CompositeSubField binding, final Class<?> fieldType) throws ConfigurationException,
			CodecException{
		final Map<String, Object> map = new HashMap<>(10);

		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), map);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, binding.unitOfMeasure(), map);

		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.PATTERN, binding.pattern(), map);
		if(!fieldType.isEnum() && !fieldType.isArray())
			ConfigurationHelper.putIfNotEmpty(ConfigurationKey.FIELD_TYPE, ParserDataType.toPrimitiveTypeOrSelf(fieldType).getSimpleName(),
				map);

		final Object defaultValue = ConfigurationHelper.convertValue(binding.defaultValue(), fieldType, NullEnum.class);
		ConfigurationHelper.putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, defaultValue, map);

		return map;
	}

	@Override
	public void validateValue(final Field field, final String dataKey, final Object dataValue) throws EncodeException{
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
	public Object convertValue(final Field field, final String dataKey, Object dataValue, final Version protocol) throws EncodeException{
		//compose field value
		final String composition = annotation.composition();
		final CompositeSubField[] fields = annotation.value();
		if(dataValue instanceof Map)
			dataValue = replace(composition, (Map<String, Object>)dataValue, fields);
		return dataValue;
	}

	private static String replace(final String text, final Map<String, Object> replacements, final CompositeSubField[] fields)
			throws EncodeException{
		final int length = fields.length;
		final Map<String, Object> trueReplacements = new HashMap<>(length);
		for(int i = 0; i < length; i ++){
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
