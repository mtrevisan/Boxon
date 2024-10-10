/*
 * Copyright (c) 2024 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.core.helpers.generators.ClassCreator;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Provides methods to generate class templates and configurations based on descriptions.
 * <p>
 * It integrates with an event listener and uses several methods to handle metadata and enumerations.
 * </p>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class Generator{

	private final EventListener eventListener;


	/**
	 * Create a generator.
	 *
	 * @param core	The core of the parser (used to retrieve the event listener).
	 * @return	A generator.
	 */
	public static Generator create(final Core core){
		return new Generator(core.getEventListener());
	}


	private Generator(final EventListener eventListener){
		this.eventListener = (eventListener != null? eventListener: EventListener.getNoOpInstance());
	}


	/**
	 * Generates a class template based on the given description (see {@link Describer}).
	 * <p>
	 * Note that the context will be ignored.
	 * </p>
	 *
	 * @param description	The description of the template.
	 * @return	The generated class template.
	 * @throws ClassNotFoundException	If the class specified in the description cannot be found.
	 */
	public Class<?> generateTemplate(final Map<String, Object> description) throws ClassNotFoundException{
		return generateWithMetadata(description, DescriberKey.TEMPLATE, TemplateHeader.class);
	}

	/**
	 * Generates a class configuration based on the given description (see {@link Describer}).
	 * <p>
	 * Note that the context will be ignored.
	 * </p>
	 *
	 * @param description	The description of the configuration.
	 * @return	The generated class configuration.
	 * @throws ClassNotFoundException	If the class specified in the description cannot be found.
	 */
	public Class<?> generateConfiguration(final Map<String, Object> description) throws ClassNotFoundException{
		//enumerations: array of strings, each string is a pair `<name>(<value>)`
		final List<Map<String, Object>> enumerations = (List<Map<String, Object>>)description.get(DescriberKey.ENUMERATIONS.toString());
		loadEnumerations(enumerations);

		return generateWithMetadata(description, DescriberKey.CONFIGURATION, ConfigurationHeader.class);
	}

	private Class<?> generateWithMetadata(final Map<String, Object> metadata, final DescriberKey key,
			final Class<? extends Annotation> templateType) throws ClassNotFoundException{
		final String className = (String)metadata.get(key.toString());
		final Map<String, Object> header = getHeaderFromMetadata(metadata);
		final Annotation headerAnnotation = AnnotationCreator.createAnnotation(templateType, header);

		return generate(metadata, className, headerAnnotation);
	}

	private Class<?> generate(final Map<String, Object> metadata, final String className, final Annotation header)
			throws ClassNotFoundException{
		final List<Map<String, Object>> fields = getFieldsFromMetadata(metadata);
		final List<Map<String, Object>> evaluatedFields = getEvaluatedFieldsFromMetadata(metadata);
		final List<Map<String, Object>> postProcessedFields = getPostProcessedFieldsFromMetadata(metadata);

		try{
			return ClassCreator.generateClass(className, header, fields, evaluatedFields, postProcessedFields);
		}
		catch(final IllegalStateException ise){
			eventListener.alreadyGeneratedClass(className, ise.getMessage());

			return null;
		}
		catch(final ClassNotFoundException cnfe){
			eventListener.generatorException(className, cnfe.getMessage());

			throw cnfe;
		}
	}

	private void loadEnumerations(final List<Map<String, Object>> enumerations){
		final int length = enumerations.size();
		final ArrayList<String> elementNames = new ArrayList<>(0);
		final ArrayList<BigInteger> elementValues = new ArrayList<>(0);
		for(int i = 0; i < length; i ++){
			final Map<String, Object> enumeration = enumerations.get(i);

			final String enumName = (String)enumeration.get(DescriberKey.ENUMERATION_NAME.toString());
			final String[] enumValues = (String[])enumeration.get(DescriberKey.ENUMERATION_VALUES.toString());
			fillElements(enumValues, elementNames, elementValues);

			try{
				ClassCreator.loadEnumeration(enumName, elementNames, elementValues);
			}
			catch(final IllegalStateException ise){
				eventListener.alreadyGeneratedEnum(enumName, ise.getMessage());
			}

			elementNames.clear();
			elementValues.clear();
		}
	}

	private static void fillElements(final String[] enumValues, final ArrayList<String> elementNames,
			final ArrayList<BigInteger> elementValues){
		final int count = enumValues.length;
		elementNames.ensureCapacity(count);
		elementValues.ensureCapacity(count);
		for(int j = 0; j < count; j ++){
			final String enumValue = enumValues[j];

			String elementName = enumValue;
			BigInteger elementValue = null;
			final int index = enumValue.indexOf('(');
			if(index > 0){
				elementName = enumValue.substring(0, index);
				elementValue = new BigInteger(enumValue.substring(index + 1, enumValue.length() - 1));
			}
			elementNames.add(elementName);
			elementValues.add(elementValue);
		}
	}

	private static Map<String, Object> getHeaderFromMetadata(final Map<String, Object> metadata){
		return (Map<String, Object>)metadata.get(DescriberKey.HEADER.toString());
	}

	private static List<Map<String, Object>> getFieldsFromMetadata(final Map<String, Object> metadata){
		return (List<Map<String, Object>>)metadata.get(DescriberKey.FIELDS.toString());
	}

	private static List<Map<String, Object>> getEvaluatedFieldsFromMetadata(final Map<String, Object> metadata){
		return (List<Map<String, Object>>)metadata.get(DescriberKey.EVALUATED_FIELDS.toString());
	}

	private static List<Map<String, Object>> getPostProcessedFieldsFromMetadata(final Map<String, Object> metadata){
		return (List<Map<String, Object>>)metadata.get(DescriberKey.POST_PROCESSED_FIELDS.toString());
	}

}
