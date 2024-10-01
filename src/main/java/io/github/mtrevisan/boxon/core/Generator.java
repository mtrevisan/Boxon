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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataType;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.logs.EventListener;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unused", "WeakerAccess"})
public final class Generator{

	private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

	private static final String KEY_SEPARATOR = "|";

	private static final String REPEATABLE_VALUE = "value";
	private static final String METHOD_NAME_GET_CODE = "getCode";


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


	//TODO manage context
	public Class<?> generateTemplate(final Map<String, Object> description) throws ClassNotFoundException{
		return generateWithMetadata(description, DescriberKey.TEMPLATE, TemplateHeader.class);
	}

	//TODO manage context
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
		final Annotation headerAnnotation = createAnnotation(templateType, header);

		return generate(metadata, className, headerAnnotation);
	}

	private Class<?> generate(final Map<String, Object> metadata, final String className, final Annotation header)
			throws ClassNotFoundException{
		final List<Map<String, Object>> fields = getFieldsFromMetadata(metadata);
		final List<Map<String, Object>> evaluatedFields = getEvaluatedFieldsFromMetadata(metadata);
		final List<Map<String, Object>> postProcessedFields = getPostProcessedFieldsFromMetadata(metadata);
		final Map<String, Map<String, Object>> postProcessedNavigableFields = extractPostProcessedNavigableFields(postProcessedFields);

		DynamicType.Builder<Object> builder = BYTE_BUDDY.subclass(Object.class)
			.name(className)
			.annotateType(header);
		builder = annotateFields(builder, fields, postProcessedNavigableFields);
		builder = annotateEvaluatedFields(builder, evaluatedFields);
		try{
			return loadClass(builder);
		}
		catch(final IllegalStateException ignored){
			eventListener.alreadyGeneratedClass(className);

			return null;
		}
	}

	private void loadEnumerations(final List<Map<String, Object>> enumerations){
		final int length = enumerations.size();
		for(int i = 0; i < length; i ++){
			final Map<String, Object> enumeration = enumerations.get(i);

			final String enumName = (String)enumeration.get(DescriberKey.ENUMERATION_NAME.toString());
			final String[] enumValues = (String[])enumeration.get(DescriberKey.ENUMERATION_VALUES.toString());
			final int count = enumValues.length;
			final String[] elementNames = new String[count];
			final BigInteger[] elementValues = new BigInteger[count];
			for(int j = 0; j < count; j ++){
				final String enumValue = enumValues[j];

				String elementName = enumValue;
				BigInteger elementValue = null;
				final int index = enumValue.indexOf('(');
				if(index > 0){
					elementName = enumValue.substring(0, index);
					elementValue = new BigInteger(enumValue.substring(index + 1, enumValue.length() - 1));
				}
				elementNames[j] = elementName;
				elementValues[j] = elementValue;
			}

			//create enum
			final DynamicType.Builder<? extends Enum<?>> builder = BYTE_BUDDY.makeEnumeration(elementNames)
				.name(enumName)
				.defineMethod(METHOD_NAME_GET_CODE, BigInteger.class)
				.intercept(MethodDelegation.to(new EnumCodeInterceptor(elementNames, elementValues)));
			try{
				loadClass(builder);
			}
			catch(final IllegalStateException ignored){
				eventListener.alreadyGeneratedEnum(enumName);
			}
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

	private Class<?> loadClass(final DynamicType.Builder<?> builder){
		try(final DynamicType.Unloaded<?> unloaded = builder.make()){
			return unloaded.load(getClass().getClassLoader()).getLoaded();
		}
	}

	private static Map<String, Map<String, Object>> extractPostProcessedNavigableFields(final List<Map<String, Object>> postProcessedFields){
		final int length = JavaHelper.sizeOrZero(postProcessedFields);
		final Map<String, Map<String, Object>> postProcessedNavigableFields = new HashMap<>(length);
		for(int i = 0; i < length; i ++){
			final Map<String, Object> postProcessedField = postProcessedFields.get(i);

			final String name = (String)postProcessedField.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)postProcessedField.get(DescriberKey.FIELD_TYPE.toString());
			final String key = composeKey(name, fieldType);
			postProcessedNavigableFields.put(key, postProcessedField);
		}
		return postProcessedNavigableFields;
	}

	private static DynamicType.Builder<Object> annotateFields(DynamicType.Builder<Object> builder, final List<Map<String, Object>> fields,
			final Map<String, Map<String, Object>> postProcessedNavigableFields) throws ClassNotFoundException{
		final List<Annotation> additionalAnnotations = new ArrayList<>(0);
		for(int i = 0, length = JavaHelper.sizeOrZero(fields); i < length; i ++){
			final Map<String, Object> fieldValues = fields.get(i);

			final String name = (String)fieldValues.get(DescriberKey.FIELD_NAME.toString());
			final String annotationType = (String)fieldValues.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Class<? extends Annotation> annotationClass = getAnnotationClass(annotationType);
			final Annotation annotation = createAnnotation(annotationClass, fieldValues);
			additionalAnnotations.add(annotation);
			if(name != null){
				final Class<?> type = extractFieldType(fieldValues);
				final Annotation collectionAnnotation = getCollectionAnnotation(fieldValues);
				final Annotation postProcessedAnnotation = getPostProcessedAnnotation(postProcessedNavigableFields, name, type);

				if(collectionAnnotation != null)
					additionalAnnotations.add(collectionAnnotation);
				if(postProcessedAnnotation != null)
					additionalAnnotations.add(postProcessedAnnotation);
				builder = addAnnotations(builder, name, type, additionalAnnotations);

				additionalAnnotations.clear();
			}
		}
		return builder;
	}

	private static Class<?> extractFieldType(final Map<String, Object> values) throws ClassNotFoundException{
		String fieldType = (String)values.get(DescriberKey.FIELD_TYPE.toString());
		final int arraysCount = GenericHelper.countArrays(fieldType);
		if(arraysCount > 0)
			fieldType = fieldType.substring(0, fieldType.length() - JavaHelper.ARRAY_VARIABLE.length() * arraysCount);
		Class<?> type = DataType.toTypeOrSelf(fieldType);
		if(arraysCount > 0)
			type = GenericHelper.addArrayToType(type, arraysCount);
		return type;
	}

	private static Annotation getCollectionAnnotation(final Map<String, Object> values) throws ClassNotFoundException{
		final String collectionFieldType = (String)values.get(DescriberKey.COLLECTION_TYPE.toString());
		if(collectionFieldType != null){
			final String collectionArraySize = (String)values.get(DescriberKey.COLLECTION_ARRAY_SIZE.toString());
			final Map<String, Object> collectionFields = getFieldMap(collectionArraySize);
			final Class<? extends Annotation> annotationType = getAnnotationClass(collectionFieldType);
			return createAnnotation(annotationType, collectionFields);
		}
		return null;
	}

	private static Map<String, Object> getFieldMap(final String collectionArraySize){
		return (collectionArraySize != null
			? Map.of(DescriberKey.COLLECTION_ARRAY_SIZE.toString(), collectionArraySize)
			: Collections.emptyMap());
	}

	private static Annotation getPostProcessedAnnotation(final Map<String, Map<String, Object>> postProcessedFields, final String name,
			final Class<?> fieldType){
		final String key = composeKey(name, fieldType);
		final Map<String, Object> postProcessedField = postProcessedFields.get(key);
		return (postProcessedField != null
			? createAnnotation(PostProcess.class, postProcessedField)
			: null);
	}

	private static String composeKey(final String name, final Class<?> fieldType){
		return composeKey(name, JavaHelper.prettyPrintClassName(fieldType));
	}

	private static String composeKey(final String name, final String fieldType){
		return name + KEY_SEPARATOR + fieldType;
	}

	private static DynamicType.Builder<Object> addAnnotations(final DynamicType.Builder<Object> builder, final String name,
			final Class<?> type, final List<Annotation> additionalAnnotations){
		final Map<Class<?>, List<Annotation>> groupedAnnotations = groupSimilarAnnotations(additionalAnnotations);

		final List<Annotation> simpleAnnotations = reduceSimilarAnnotations(groupedAnnotations);

		return builder.defineField(name, type, Visibility.PACKAGE_PRIVATE)
			.annotateField(simpleAnnotations);
	}

	private static Map<Class<?>, List<Annotation>> groupSimilarAnnotations(final List<Annotation> annotations){
		final int length = annotations.size();
		final Map<Class<?>, List<Annotation>> groupedAnnotations = new LinkedHashMap<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations.get(i);

			groupedAnnotations.computeIfAbsent(annotation.annotationType(), k -> new ArrayList<>(1))
				.add(annotation);
		}
		return groupedAnnotations;
	}

	private static List<Annotation> reduceSimilarAnnotations(final Map<Class<?>, List<Annotation>> groupedAnnotations){
		final List<Annotation> simpleAnnotations = new ArrayList<>(groupedAnnotations.size());
		for(final Map.Entry<Class<?>, List<Annotation>> entry : groupedAnnotations.entrySet()){
			final List<Annotation> annotations = entry.getValue();
			if(annotations.size() == 1){
				simpleAnnotations.add(annotations.getFirst());

				continue;
			}

			final Class<?> annotationType = entry.getKey();

			final Object array = CodecHelper.createArray(annotationType, annotations.size());
			for(int i = 0, count = annotations.size(); i < count; i ++)
				Array.set(array, i, annotations.get(i));
			final Repeatable repeatable = annotationType.getAnnotation(Repeatable.class);
			final Annotation repeatableAnnotation = createAnnotation(repeatable.value(), Map.of(REPEATABLE_VALUE, array));

			simpleAnnotations.add(repeatableAnnotation);
		}
		return simpleAnnotations;
	}

	private static DynamicType.Builder<Object> annotateEvaluatedFields(DynamicType.Builder<Object> builder,
			final List<Map<String, Object>> evaluatedFields) throws ClassNotFoundException{
		for(int i = 0, length = JavaHelper.sizeOrZero(evaluatedFields); i < length; i ++){
			final Map<String, Object> field = evaluatedFields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
			final Class<?> type = DataType.toTypeOrSelf(fieldType);

			final String evaluatedFieldType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Class<? extends Annotation> annotationType = getAnnotationClass(evaluatedFieldType);
			final Annotation annotation = createAnnotation(annotationType, field);

			builder = builder.defineField(name, type, Visibility.PACKAGE_PRIVATE)
				.annotateField(annotation);
		}
		return builder;
	}


	private static Class<? extends Annotation> getAnnotationClass(final String annotationType) throws ClassNotFoundException{
		return (Class<? extends Annotation>)Class.forName(annotationType);
	}

	public static <A extends Annotation> A createAnnotation(final Class<A> annotationType, final Map<String, Object> values){
		return (A)Proxy.newProxyInstance(
			annotationType.getClassLoader(),
			new Class[]{annotationType},
			new DynamicAnnotationInvocationHandler(annotationType, values));
	}


	private static final class DynamicAnnotationInvocationHandler implements InvocationHandler{
		private final Class<? extends Annotation> annotationType;
		private final Map<String, Object> values;

		private DynamicAnnotationInvocationHandler(final Class<? extends Annotation> annotationType, final Map<String, Object> values){
			this.annotationType = annotationType;
			this.values = new HashMap<>(values);
		}

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args){
			final String methodName = method.getName();
			Object value = (methodName.equals(DescriberKey.ANNOTATION_TYPE.toString())
				? annotationType
				: values.getOrDefault(methodName, method.getDefaultValue()));

			try{
				final Class<?> returnType = method.getReturnType();
				value = extractValueBasedOnReturnType(returnType, value);
			}
			catch(final Exception ignored){}
			return value;
		}

		private static Object extractValueBasedOnReturnType(final Class<?> returnType, Object value) throws ClassNotFoundException{
			if(returnType.equals(Class.class) && value instanceof final String v)
				value = DataType.toTypeOrSelf(v);
			else if(returnType.isArray() && value instanceof final List<?> subValues){
				final Class<?> elementType = returnType.getComponentType();
				value = invokeArrayValues(elementType, (List<Map<String, Object>>)subValues);
			}
			else if(Annotation.class.isAssignableFrom(returnType) && value instanceof final Map<?, ?> subValues){
				//manage `ConverterChoices`, `ObjectChoices`, and `ObjectChoicesList`
				final Class<? extends Annotation> annotationType = (Class<? extends Annotation>)returnType;
				value = createAnnotation(annotationType, (Map<String, Object>)subValues);
			}
			return value;
		}

		private static Object invokeArrayValues(final Class<?> elementType, final List<Map<String, Object>> values)
				throws ClassNotFoundException{
			final int length = JavaHelper.sizeOrZero(values);
			if(length == 0)
				return CodecHelper.createArray(elementType, 0);

			final Object result = CodecHelper.createArray(elementType, length);
			for(int i = 0; i < length; i ++){
				final Map<String, Object> subs = values.get(i);

				final String subAnnotationType = (String)subs.get(DescriberKey.ANNOTATION_TYPE.toString());
				final Class<? extends Annotation> annotationType = getAnnotationClass(subAnnotationType);
				Array.set(result, i, createAnnotation(annotationType, subs));
			}
			return result;
		}
	}

	public static class EnumCodeInterceptor{
		private final Map<String, BigInteger> elementCodeMap;

		public EnumCodeInterceptor(final String[] elementNames, final BigInteger[] elementValues){
			elementCodeMap = new HashMap<>(elementNames.length);
			for(int i = 0, length = elementNames.length; i < length; i ++)
				elementCodeMap.put(elementNames[i], elementValues[i]);
		}

		@RuntimeType
		public BigInteger getCode(@This final Enum<?> enumInstance){
			return elementCodeMap.get(enumInstance.name());
		}
	}

}
