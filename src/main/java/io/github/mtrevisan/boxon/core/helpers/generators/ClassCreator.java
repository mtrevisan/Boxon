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
package io.github.mtrevisan.boxon.core.helpers.generators;

import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataType;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class ClassCreator{

	private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

	private static final String KEY_SEPARATOR = "|";

	private static final String REPEATABLE_VALUE = "value";

	private static final String METHOD_NAME_GET_CODE = "getCode";


	private ClassCreator(){}


	public static Class<?> generateClass(final String className, final Annotation header, final List<Map<String, Object>> fields,
			final List<Map<String, Object>> evaluatedFields, final List<Map<String, Object>> postProcessedFields)
			throws ClassNotFoundException{
		final Map<String, Map<String, Object>> postProcessedNavigableFields = extractPostProcessedNavigableFields(postProcessedFields);

		DynamicType.Builder<Object> builder = BYTE_BUDDY.subclass(Object.class)
			.name(className)
			.annotateType(header);
		builder = annotateFields(builder, fields, postProcessedNavigableFields);
		builder = annotateEvaluatedFields(builder, evaluatedFields);
		return loadClass(builder);
	}

	private static DynamicType.Builder<Object> annotateFields(DynamicType.Builder<Object> builder, final List<Map<String, Object>> fields,
		final Map<String, Map<String, Object>> postProcessedNavigableFields) throws ClassNotFoundException{
		final List<Annotation> additionalAnnotations = new ArrayList<>(0);
		for(int i = 0, length = JavaHelper.sizeOrZero(fields); i < length; i ++){
			final Map<String, Object> fieldValues = fields.get(i);

			final String name = (String)fieldValues.get(DescriberKey.FIELD_NAME.toString());
			final String annotationType = (String)fieldValues.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Annotation annotation = AnnotationCreator.createAnnotation(annotationType, fieldValues);
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
			return AnnotationCreator.createAnnotation(collectionFieldType, collectionFields);
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
			? AnnotationCreator.createAnnotation(PostProcess.class, postProcessedField)
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
			final Annotation repeatableAnnotation = AnnotationCreator.createAnnotation(repeatable.value(), Map.of(REPEATABLE_VALUE, array));

			simpleAnnotations.add(repeatableAnnotation);
		}
		return simpleAnnotations;
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

	private static DynamicType.Builder<Object> annotateEvaluatedFields(DynamicType.Builder<Object> builder,
		final List<Map<String, Object>> evaluatedFields) throws ClassNotFoundException{
		for(int i = 0, length = JavaHelper.sizeOrZero(evaluatedFields); i < length; i ++){
			final Map<String, Object> field = evaluatedFields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
			final Class<?> type = DataType.toTypeOrSelf(fieldType);

			final String evaluatedFieldType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Annotation annotation = AnnotationCreator.createAnnotation(evaluatedFieldType, field);

			builder = builder.defineField(name, type, Visibility.PACKAGE_PRIVATE)
				.annotateField(annotation);
		}
		return builder;
	}


	public static <E extends ConfigurationEnum<?>> Class<E> loadEnumeration(final String enumName, final List<String> elementNames,
			final List<BigInteger> elementValues){
		final DynamicType.Builder<? extends Enum<?>> builder = BYTE_BUDDY.makeEnumeration(elementNames)
			.implement(ConfigurationEnum.class)
			.name(enumName)
			.defineMethod(METHOD_NAME_GET_CODE, BigInteger.class)
			.intercept(MethodDelegation.to(new EnumCodeInterceptor(elementNames, elementValues)));
		return (Class<E>)loadClass(builder);
	 }

	private static Class<?> loadClass(final DynamicType.Builder<?> builder){
		try(final DynamicType.Unloaded<?> unloaded = builder.make()){
			return unloaded.load(ClassCreator.class.getClassLoader()).getLoaded();
		}
	}


	private static final class EnumCodeInterceptor{
		private final Map<String, BigInteger> elementCodeMap;

		private EnumCodeInterceptor(final List<String> elementNames, final List<BigInteger> elementValues){
			final int length = elementNames.size();
			elementCodeMap = new HashMap<>(length);
			for(int i = 0; i < length; i ++)
				elementCodeMap.put(elementNames.get(i), elementValues.get(i));
		}

		@RuntimeType
		public BigInteger getCode(@This final Enum<?> enumInstance){
			return elementCodeMap.get(enumInstance.name());
		}
	}

}
