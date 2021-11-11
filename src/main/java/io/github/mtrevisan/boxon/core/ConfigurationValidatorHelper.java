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
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationFields;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSubField;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;


final class ConfigurationValidatorHelper{

	private static final String NOTIFICATION_TEMPLATE = "compositeTemplate";
	private static final freemarker.template.Configuration FREEMARKER_CONFIGURATION
		= new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_31);
	static{
		FREEMARKER_CONFIGURATION.setDefaultEncoding(StandardCharsets.UTF_8.name());
		FREEMARKER_CONFIGURATION.setLocale(Locale.US);
		FREEMARKER_CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}


	private ConfigurationValidatorHelper(){}

	static boolean shouldBeExtracted(final Version protocol, final String minProtocol, final String maxProtocol){
		if(protocol.isEmpty())
			return true;

		final Version min = Version.of(minProtocol);
		final Version max = Version.of(maxProtocol);
		final boolean validMinimum = (min.isEmpty() || protocol.isGreaterThanOrEqualTo(min));
		final boolean validMaximum = (max.isEmpty() || protocol.isLessThanOrEqualTo(max));
		return (validMinimum && validMaximum);
	}

	static void validateValue(final ConfigurationField binding, final String key, final Object value, final ConfigField field)
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

	static void validateValue(final CompositeConfigurationField binding, final String key, final Object value)
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

	static String replace(final String text, final Map<String, Object> replacements, final ConfigurationSubField[] fields)
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

	static void validateValue(final AlternativeConfigurationField binding, final String key, final Object value,
			final ConfigField field) throws EncodeException{
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

	static void validateMandatoryFields(final Collection<ConfigField> mandatoryFields) throws EncodeException{
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
				else if(AlternativeConfigurationFields.class.isInstance(mandatoryField.getBinding())){
					final AlternativeConfigurationFields foundBinding = (AlternativeConfigurationFields)mandatoryField.getBinding();
					shortDescription = foundBinding.shortDescription();
				}
				sj.add(shortDescription);
			}
			throw EncodeException.create("Mandatory fields missing: {}", sj.toString());
		}
	}

}
