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
package io.github.mtrevisan.boxon.core.helpers.descriptors;

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
import io.github.mtrevisan.boxon.core.Descriptor;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public final class AnnotationDescriptorHelper{

	private AnnotationDescriptorHelper(){}


	public static <F> Collection<Map<String, Object>> describeFields(final List<F> fields, final FieldExtractor<F> fieldExtractor){
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
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final String fieldName = fieldExtractor.getFieldName(field);
		final Class<?> fieldType = fieldExtractor.getFieldType(field);

		final Map<String, Object> fieldDescription = new HashMap<>(3);
		putIfNotEmpty(DescriberKey.FIELD_NAME, fieldName, fieldDescription);
		putIfNotEmpty(DescriberKey.FIELD_TYPE, JavaHelper.prettyPrintClassName(fieldType), fieldDescription);
		putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, annotationType.getName(), fieldDescription);

		extractSkipParameters(field, fieldExtractor, fieldsDescription);

		extractObjectParameters(binding, annotationType, fieldDescription);

		fieldsDescription.add(Collections.unmodifiableMap(fieldDescription));
	}

	private static <F> void extractSkipParameters(final F field, final FieldExtractor<F> fieldExtractor,
			final Collection<Map<String, Object>> fieldsDescription){
		final SkipParams[] skips = fieldExtractor.getSkips(field);
		for(int i = 0, length = JavaHelper.sizeOrZero(skips); i < length; i ++){
			final SkipParams skip = skips[i];

			final Map<String, Object> skipDescription = new HashMap<>(4);
			extractObjectParameters(skip, skip.getClass(), skipDescription);
			fieldsDescription.add(skipDescription);
		}
	}

	public static void extractObjectParameters(final Object obj, final Class<?> objType, final Map<String, Object> rootDescription,
			final String key){
		final Method[] methods = objType.getDeclaredMethods();

		final Map<String, Object> description = new HashMap<>(methods.length);
		extractObjectParameters(obj, methods, description);
		if(!description.isEmpty())
			rootDescription.put(key, Collections.unmodifiableMap(description));
	}

	private static void extractObjectParameters(final Object obj, final Class<?> objType, final Map<String, Object> rootDescription){
		final Method[] methods = objType.getDeclaredMethods();

		extractObjectParameters(obj, methods, rootDescription);
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
				else if(!isEmptyStringOrCollectionOrVoid(value))
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
		if(componentType == String.class)
			rootDescription.put(key, value);
		else if(componentType == ObjectChoices.ObjectChoice.class)
			describeAlternatives(key, (ObjectChoices.ObjectChoice[])value, rootDescription);
		else if(componentType == ConverterChoices.ConverterChoice.class || componentType == AlternativeSubField.class
				|| componentType == CompositeSubField.class)
			describeAlternatives(key, (Annotation[])value, rootDescription);
	}

	private static boolean isEmptyStringOrCollectionOrVoid(final Object value){
		return ((value instanceof final String v && StringHelper.isBlank(v))
			|| (value instanceof final Collection<?> c && c.isEmpty())
			|| value == void.class);
	}

	private static void describeAlternatives(final String key, final ObjectChoices.ObjectChoice[] alternatives,
			final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final ObjectChoices.ObjectChoice alternative : alternatives){
				final Map<String, Object> alternativeDescription = new HashMap<>(3);

				extractObjectParameters(alternative, alternative.annotationType(), alternativeDescription);
				describeType(alternative.type(), alternativeDescription);

				alternativesDescription.add(alternativeDescription);
			}
			putIfNotEmpty(key, alternativesDescription, rootDescription);
		}
	}

	private static void describeType(final Class<?> type, final Map<String, Object> rootDescription){
		if(isUserDefinedClass(type)){
			final List<Map<String, Object>> typeDescription = new ArrayList<>(1);
			final Collection<Class<?>> processedTypes = new HashSet<>(1);
			Class<?> parent = type;
			while(parent != null && parent != Object.class && !processedTypes.contains(parent)){
				typeDescription.addFirst(Descriptor.describeRawMessage(parent));

				processedTypes.add(parent);

				//go up to parent class
				parent = parent.getSuperclass();
			}
			putIfNotEmpty(DescriberKey.BIND_SUBTYPES, typeDescription, rootDescription);
		}
	}

	private static boolean isUserDefinedClass(final Class<?> cls){
		//check if the class is not an interface, an anonymous class, or a primitive data type
		return (!cls.isInterface() && !cls.isAnonymousClass() && !cls.isPrimitive());
	}

	private static <T extends Annotation> void describeAlternatives(final String key, final T[] alternatives,
			final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final T alternative : alternatives){
				final Map<String, Object> alternativeDescription = new HashMap<>(2);

				extractObjectParameters(alternative, alternative.annotationType(), alternativeDescription);

				alternativesDescription.add(alternativeDescription);
			}
			putIfNotEmpty(key, alternativesDescription, rootDescription);
		}
	}

	/**
	 * Put the pair key-value into the given map if the value is not {@code null} or empty string.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 */
	public static void putIfNotEmpty(final Enum<?> key, final Object value, final Map<String, Object> map){
		if(value != null
				&& !(value instanceof final String v && StringHelper.isBlank(v))
				&& !(value instanceof final Collection<?> c && c.isEmpty())
			)
			map.put(key.toString(), (value instanceof final Class<?> cls? JavaHelper.prettyPrintClassName(cls): value));
	}

}
