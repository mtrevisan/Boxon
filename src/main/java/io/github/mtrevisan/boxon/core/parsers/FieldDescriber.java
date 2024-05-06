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

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorConfiguration;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorEvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorPostProcessedField;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractorStrategy;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorBasicStrategy;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorConfiguration;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractorFullStrategy;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Class for describing messages and entities.
 */
public final class FieldDescriber{

	static final MessageExtractorBasicStrategy MESSAGE_EXTRACTOR_BASIC_STRATEGY = new MessageExtractorBasicStrategy();
	static final MessageExtractorBasicStrategy MESSAGE_EXTRACTOR_FULL_STRATEGY = new MessageExtractorFullStrategy();
	static final MessageExtractorConfiguration MESSAGE_EXTRACTOR_CONFIGURATION = new MessageExtractorConfiguration();
	static final FieldExtractorStrategy FIELD_EXTRACTOR_STRATEGY = new FieldExtractorStrategy();
	private static final FieldExtractorEvaluatedField FIELD_EXTRACTOR_EVALUATED_FIELD = new FieldExtractorEvaluatedField();
	private static final FieldExtractorPostProcessedField FIELD_EXTRACTOR_POST_PROCESSED_FIELD = new FieldExtractorPostProcessedField();
	static final FieldExtractorConfiguration FIELD_EXTRACTOR_CONFIGURATION = new FieldExtractorConfiguration();

	private static final Map<Class<?>, ArrayHandler> PROCESSOR_MAP = new HashMap<>(5);
	static{
		PROCESSOR_MAP.put(String.class, FieldDescriber::describeString);
		PROCESSOR_MAP.put(ObjectChoices.ObjectChoice.class, FieldDescriber::describeAlternatives);
		PROCESSOR_MAP.put(ConverterChoices.ConverterChoice.class, FieldDescriber::describeAlternatives);
		PROCESSOR_MAP.put(AlternativeSubField.class, FieldDescriber::describeAlternatives);
		PROCESSOR_MAP.put(CompositeSubField.class, FieldDescriber::describeAlternatives);
	}

	private interface ArrayHandler{
		void handle(String key, Object value, Map<String, Object> description);
	}


	private FieldDescriber(){}


	private static void extractObjectParameters(final Object obj, final Class<?> objType, final Map<String, Object> rootDescription){
		final Method[] methods = objType.getDeclaredMethods();

		extractObjectParameters(obj, methods, rootDescription);
	}

	static void extractObjectParameters(final Object obj, final Class<?> objType, final Map<String, Object> rootDescription,
			final String key){
		final Method[] methods = objType.getDeclaredMethods();

		final Map<String, Object> description = new LinkedHashMap<>(methods.length);
		extractObjectParameters(obj, methods, description);
		if(!description.isEmpty())
			rootDescription.put(key, Collections.unmodifiableMap(description));
	}

	private static void extractObjectParameters(final Object obj, final Method[] methods, final Map<String, Object> rootDescription){
		for(int i = 0, length = methods.length; i < length; i ++){
			final Method method = methods[i];
			if(method.getParameters().length > 0)
				continue;

			try{
				final Object value = method.invoke(obj);

				putIfNotEmpty(method.getName(), value, rootDescription);
			}
			catch(final Exception ignored){
				//cannot happen
			}
		}
	}

	private static void extractCollectionParameters(final Annotation collectionBinding, final Map<String, Object> fieldDescription){
		if(collectionBinding != null){
			fieldDescription.put(DescriberKey.COLLECTION_TYPE.toString(), collectionBinding.annotationType().getName());
			if(collectionBinding.annotationType() == BindAsArray.class)
				fieldDescription.put(DescriberKey.COLLECTION_ARRAY_SIZE.toString(), ((BindAsArray)collectionBinding).size());
		}
	}

	/**
	 * Description of a single annotated class.
	 *
	 * @param boundClass	Generic bound class to be described.
	 * @return	The description.
	 */
	public static Map<String, Object> describeRawMessage(final Class<?> boundClass){
		final Map<String, Object> description = new LinkedHashMap<>(6);
		try{
			final Template<?> entity = Template.create(boundClass);
			describeRawMessage(entity, MESSAGE_EXTRACTOR_BASIC_STRATEGY, FIELD_EXTRACTOR_STRATEGY, description);
		}
		catch(final AnnotationException ignored){
			description.put(DescriberKey.TEMPLATE.toString(), boundClass.getName());
		}
		return Collections.unmodifiableMap(description);
	}

	/**
	 * Describes a raw message by extracting various information from it and creating a description map.
	 * <p>The description includes the message type name, fields, evaluated fields, and post-processed fields.</p>
	 *
	 * @param message	The message to describe.
	 * @param messageExtractor	The message extractor to use.
	 * @param fieldExtractor	The field extractor to use.
	 * @param rootDescription	The map where the description will be populated.
	 * @param <M>	The type of the message.
	 * @param <F>	The type of the fields.
	 */
	static <M, F> void describeRawMessage(final M message, final MessageExtractor<M, ?, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor, final Map<String, Object> rootDescription){
		final DescriberKey messageKey = selectMessageKey(messageExtractor);
		putIfNotEmpty(messageKey, messageExtractor.getTypeName(message), rootDescription);

		putIfNotEmpty(DescriberKey.FIELDS, describeFields(messageExtractor.getFields(message), fieldExtractor),
			rootDescription);
		putIfNotEmpty(DescriberKey.EVALUATED_FIELDS, describeFields(messageExtractor.getEvaluatedFields(message),
			FIELD_EXTRACTOR_EVALUATED_FIELD), rootDescription);
		putIfNotEmpty(DescriberKey.POST_PROCESSED_FIELDS, describeFields(
			messageExtractor.getPostProcessedFields(message), FIELD_EXTRACTOR_POST_PROCESSED_FIELD), rootDescription);
	}

	private static <M, F> DescriberKey selectMessageKey(final MessageExtractor<M, ?, F> messageExtractor){
		return (messageExtractor instanceof MessageExtractorBasicStrategy
			? DescriberKey.TEMPLATE
			: DescriberKey.CONFIGURATION
		);
	}

	private static <F> Collection<Map<String, Object>> describeFields(final List<F> fields, final FieldExtractor<F> fieldExtractor){
		final int length = JavaHelper.sizeOrZero(fields);
		final Collection<Map<String, Object>> fieldsDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final F field = fields.get(i);

			extractAnnotationParameters(field, fieldExtractor, fieldsDescription);
		}
		return Collections.unmodifiableCollection(fieldsDescription);
	}

	private static <F> void extractAnnotationParameters(final F field, final FieldExtractor<F> fieldExtractor,
			final Collection<Map<String, Object>> fieldsDescription){
		final Annotation binding = fieldExtractor.getBinding(field);
		final Annotation collectionBinding = fieldExtractor.getCollectionBinding(field);
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final String fieldName = fieldExtractor.getFieldName(field);
		final Class<?> fieldType = fieldExtractor.getFieldType(field);

		final Map<String, Object> fieldDescription = new LinkedHashMap<>(3);
		putIfNotEmpty(DescriberKey.FIELD_NAME, fieldName, fieldDescription);
		putIfNotEmpty(DescriberKey.FIELD_TYPE, JavaHelper.prettyPrintClassName(fieldType), fieldDescription);
		putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, annotationType.getName(), fieldDescription);

		extractSkipParameters(field, fieldExtractor, fieldsDescription);

		extractObjectParameters(binding, annotationType, fieldDescription);
		extractCollectionParameters(collectionBinding, fieldDescription);

		fieldsDescription.add(Collections.unmodifiableMap(fieldDescription));
	}

	private static <F> void extractSkipParameters(final F field, final FieldExtractor<F> fieldExtractor,
			final Collection<Map<String, Object>> fieldsDescription){
		final SkipParams[] skips = fieldExtractor.getSkips(field);
		for(int i = 0, length = JavaHelper.sizeOrZero(skips); i < length; i ++){
			final SkipParams skip = skips[i];

			final Map<String, Object> skipDescription = new LinkedHashMap<>(4);
			extractObjectParameters(skip, skip.getClass(), skipDescription);
			fieldsDescription.add(Collections.unmodifiableMap(skipDescription));
		}
	}


	/**
	 * Put the pair key-value into the given map if the value is not {@code null} or empty string.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param rootDescription	The map in which to load the key-value pair.
	 */
	private static void putIfNotEmpty(final String key, final Object value, final Map<String, Object> rootDescription){
		if(value == null)
			return;

		switch(value){
			case final Class<?> cls -> handleClassValue(key, cls, rootDescription);
			case final ObjectChoices choices -> extractObjectParameters(choices, choices.annotationType(), rootDescription, key);
			case final ObjectChoicesList choices -> extractObjectParameters(choices, choices.annotationType(), rootDescription, key);
			case final ConverterChoices converter -> extractObjectParameters(converter, converter.annotationType(), rootDescription, key);
			default -> {
				if(value.getClass().isArray())
					handleArrayValue(key, value, rootDescription);
				else if(!StringHelper.isEmptyStringOrCollectionOrVoid(value))
					rootDescription.put(key, value);
			}
		}
	}

	private static void handleClassValue(final String key, final Class<?> cls, final Map<String, Object> rootDescription){
		if(Validator.class.isAssignableFrom(cls)){
			if(cls != NullValidator.class)
				rootDescription.put(key, cls.getName());
		}
		else if(Converter.class.isAssignableFrom(cls)){
			if(cls != NullConverter.class)
				rootDescription.put(key, cls.getName());
		}
		else if(!cls.isEnum() || cls != NullEnum.class)
			rootDescription.put(key, JavaHelper.prettyPrintClassName(cls));
	}

	private static void handleArrayValue(final String key, final Object value, final Map<String, Object> rootDescription){
		final Class<?> componentType = value.getClass()
			.getComponentType();

		final ArrayHandler processor = PROCESSOR_MAP.get(componentType);
		processor.handle(key, value, rootDescription);
	}

	private static void describeString(final String key, final Object value, final Map<String, Object> rootDescription){
		rootDescription.put(key, value);
	}

	private static void describeAlternatives(final String key, final Object value, final Map<String, Object> rootDescription){
		final Annotation[] annotations = (Annotation[])value;
		final int length = annotations.length;
		if(length == 0)
			return;

		final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation alternative = annotations[i];

			final Map<String, Object> alternativeDescription = describeAlternative(alternative);

			alternativesDescription.add(alternativeDescription);
		}
		putIfNotEmpty(key, alternativesDescription, rootDescription);
	}

	private static Map<String, Object> describeAlternative(final Annotation alternative){
		final Map<String, Object> alternativeDescription = new LinkedHashMap<>(2);

		extractObjectParameters(alternative, alternative.annotationType(), alternativeDescription);
		if(alternative instanceof final ObjectChoices.ObjectChoice choice)
			describeChoiceType(choice.type(), alternativeDescription);

		return Collections.unmodifiableMap(alternativeDescription);
	}

	private static void describeChoiceType(final Class<?> type, final Map<String, Object> rootDescription){
		if(JavaHelper.isUserDefinedClass(type)){
			final List<Map<String, Object>> typeDescription = new ArrayList<>(1);
			final Collection<Class<?>> processedTypes = new HashSet<>(1);
			Class<?> parent = type;
			while(parent != null && parent != Object.class && !processedTypes.contains(parent)){
				typeDescription.addFirst(describeRawMessage(parent));

				processedTypes.add(parent);

				//go up to parent class
				parent = parent.getSuperclass();
			}
			putIfNotEmpty(DescriberKey.BIND_SUBTYPES, typeDescription, rootDescription);
		}
	}

	/**
	 * Put the pair key-value into the given map if the value is not {@code null} or empty string.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 */
	static void putIfNotEmpty(final Enum<?> key, final Object value, final Map<String, Object> map){
		if(value != null
			&& !(value instanceof final String v && StringHelper.isBlank(v))
			&& !(value instanceof final Collection<?> c && c.isEmpty())
		)
			map.put(key.toString(), (value instanceof final Class<?> cls? JavaHelper.prettyPrintClassName(cls): value));
	}

}
