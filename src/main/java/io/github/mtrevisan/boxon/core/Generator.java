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
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unused", "WeakerAccess"})
public final class Generator{

	private static final String KEY_SEPARATOR = "|";

	private static final String PRIMITIVE_CLASS_NAME_BOOLEAN = "boolean";
	private static final String PRIMITIVE_CLASS_NAME_BYTE = "byte";
	private static final String PRIMITIVE_CLASS_NAME_CHAR = "char";
	private static final String PRIMITIVE_CLASS_NAME_SHORT = "short";
	private static final String PRIMITIVE_CLASS_NAME_INT = "int";
	private static final String PRIMITIVE_CLASS_NAME_LONG = "long";
	private static final String PRIMITIVE_CLASS_NAME_FLOAT = "float";
	private static final String PRIMITIVE_CLASS_NAME_DOUBLE = "double";
	private static final String CLASS_NAME_STRING = "java.lang.String";


	/**
	 * Create a generator.
	 *
	 * @return	A generator.
	 */
	public static Generator create(){
		return new Generator();
	}


	private Generator(){}


	//TODO manage context
	public Class<?> generateTemplate(final Map<String, Object> description) throws ClassNotFoundException, InvocationTargetException,
			IllegalAccessException{
		final String template = (String)description.get(DescriberKey.TEMPLATE.toString());
		final Map<String, Object> header = (Map<String, Object>)description.get(DescriberKey.HEADER.toString());
		final List<Map<String, Object>> fields = (List<Map<String, Object>>)description.get(DescriberKey.FIELDS.toString());
		final List<Map<String, Object>> evaluatedFields = (List<Map<String, Object>>)description.get(
			DescriberKey.EVALUATED_FIELDS.toString());
		final List<Map<String, Object>> postProcessedFields = (List<Map<String, Object>>)description.get(
			DescriberKey.POST_PROCESSED_FIELDS.toString());
		final Map<String, Map<String, Object>> postProcessedNavigableFields = extractPostProcessedNavigableFields(postProcessedFields);

		final Annotation templateHeader = createAnnotation(TemplateHeader.class, header);

		final ByteBuddy byteBuddy = new ByteBuddy();
		DynamicType.Builder<Object> builder = byteBuddy.subclass(Object.class)
			.name(template);
		builder = builder.annotateType(convertAnnotationToDescription(templateHeader));
		builder = annotateFields(builder, fields, postProcessedNavigableFields);
		builder = annotateEvaluatedFields(builder, evaluatedFields);
		try(final DynamicType.Unloaded<Object> unloaded = builder.make()){
			return unloaded.load(getClass().getClassLoader())
				.getLoaded();
		}
	}

	//TODO manage context, enumerations
	public Class<?> generateConfiguration(final Map<String, Object> description) throws ClassNotFoundException, InvocationTargetException,
			IllegalAccessException{
		final String configuration = (String)description.get(DescriberKey.CONFIGURATION.toString());
		final Map<String, Object> header = (Map<String, Object>)description.get(DescriberKey.HEADER.toString());
		final List<Map<String, Object>> fields = (List<Map<String, Object>>)description.get(DescriberKey.FIELDS.toString());

		final Annotation configurationHeader = createAnnotation(ConfigurationHeader.class, header);

		final ByteBuddy byteBuddy = new ByteBuddy();
		final DynamicType.Builder<Object> builder = byteBuddy.subclass(Object.class)
			.name(configuration);
		builder.annotateType(convertAnnotationToDescription(configurationHeader));
		annotateFields(builder, fields, Collections.emptyMap());
		try(final DynamicType.Unloaded<Object> unloaded = builder.make()){
			return unloaded.load(getClass().getClassLoader())
				.getLoaded();
		}
	}

	private static Map<String, Map<String, Object>> extractPostProcessedNavigableFields(final List<Map<String, Object>> postProcessedFields){
		final int length = JavaHelper.sizeOrZero(postProcessedFields);
		final Map<String, Map<String, Object>> postProcessedNavigableFields = new HashMap<>(length);
		for(int i = 0; i < length; i ++){
			final Map<String, Object> postProcessedField = postProcessedFields.get(i);

			final String name = (String)postProcessedField.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)postProcessedField.get(DescriberKey.FIELD_TYPE.toString());
			postProcessedNavigableFields.put(name + KEY_SEPARATOR + fieldType, postProcessedField);
		}
		return postProcessedNavigableFields;
	}

	private static DynamicType.Builder<Object> annotateFields(DynamicType.Builder<Object> builder, final List<Map<String, Object>> fields,
			final Map<String, Map<String, Object>> postProcessedNavigableFields) throws ClassNotFoundException, InvocationTargetException,
			IllegalAccessException{
		final List<Annotation> additionalAnnotations = new ArrayList<>(0);
		for(int i = 0, length = JavaHelper.sizeOrZero(fields); i < length; i ++){
			final Map<String, Object> field = fields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			if(name == null){
				//skip-like annotation, collect generate annotation until a true field is found, then attach to it
				final String annotationType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
				final Annotation annotation = createAnnotation((Class<? extends Annotation>)Class.forName(annotationType), field);
				additionalAnnotations.add(annotation);
			}
			else{
				String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
				final int arraysCount = GenericHelper.countArrays(fieldType);
				if(arraysCount > 0)
					fieldType = fieldType.substring(0, fieldType.length() - JavaHelper.ARRAY_VARIABLE.length() * arraysCount);
				Class<?> type = DataType.toTypeOrSelf(fieldType);
				if(arraysCount > 0)
					type = GenericHelper.addArrayToType(type, arraysCount);

				final String collectionFieldType = (String)field.get(DescriberKey.COLLECTION_TYPE.toString());
				final Class<?> collectionType = (collectionFieldType != null
					? DataType.toTypeOrSelf(collectionFieldType)
					: null);
				final String collectionArraySize = (String)field.get(DescriberKey.COLLECTION_ARRAY_SIZE.toString());

				final String annotationType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
				final Annotation annotation = createAnnotation((Class<? extends Annotation>)Class.forName(annotationType), field);
				final Map<String, Object> collectionFields = (collectionArraySize != null
					? Map.of(DescriberKey.COLLECTION_ARRAY_SIZE.toString(), collectionArraySize)
					: Collections.emptyMap());
				final Annotation collectionAnnotation = (collectionType != null
					? createAnnotation((Class<? extends Annotation>)Class.forName(collectionFieldType), collectionFields)
					: null);

				final String key = name + KEY_SEPARATOR + fieldType;
				final Map<String, Object> postProcessedField = postProcessedNavigableFields.get(key);
				final Annotation postProcessedAnnotation = (postProcessedField != null
					? createAnnotation(PostProcess.class, postProcessedField)
					: null);

				final DynamicType.Builder.FieldDefinition.Optional.Valuable<Object> fieldBuilder
					= builder.defineField(name, type, Visibility.PACKAGE_PRIVATE);
				for(int j = 0, count = additionalAnnotations.size(); j < count; j ++)
					fieldBuilder.annotateField(convertAnnotationToDescription(additionalAnnotations.get(j)));
				additionalAnnotations.clear();
				builder = fieldBuilder.annotateField(convertAnnotationToDescription(annotation));
				if(collectionAnnotation != null)
					builder = fieldBuilder.annotateField(convertAnnotationToDescription(collectionAnnotation));
				if(postProcessedAnnotation != null)
					builder = fieldBuilder.annotateField(convertAnnotationToDescription(postProcessedAnnotation));
			}
		}
		return builder;
	}

	private static DynamicType.Builder<Object> annotateEvaluatedFields(DynamicType.Builder<Object> builder,
			final List<Map<String, Object>> evaluatedFields) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException{
		for(int i = 0, length = JavaHelper.sizeOrZero(evaluatedFields); i < length; i ++){
			final Map<String, Object> field = evaluatedFields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
			final Class<?> type = DataType.toTypeOrSelf(fieldType);

			final String annotationType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Annotation annotation = createAnnotation((Class<? extends Annotation>)Class.forName(annotationType), field);

			builder = builder.defineField(name, type, Visibility.PACKAGE_PRIVATE)
				.annotateField(convertAnnotationToDescription(annotation));
		}
		return builder;
	}


	public static <A extends Annotation> A createAnnotation(final Class<A> annotationType, final Map<String, Object> values){
		return (A)Proxy.newProxyInstance(
			annotationType.getClassLoader(),
			new Class[]{annotationType},
			new DynamicAnnotationInvocationHandler(annotationType, values));
	}

	public static AnnotationDescription convertAnnotationToDescription(final Annotation annotation) throws InvocationTargetException,
			IllegalAccessException{
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		AnnotationDescription.Builder builder = AnnotationDescription.Builder.ofType(annotationType);
		final Method[] declaredMethods = annotationType.getDeclaredMethods();
		for(int i = 0, length = declaredMethods.length; i < length; i ++){
			final Method method = declaredMethods[i];
			final Object value = method.invoke(annotation);

			final String key = method.getName();
			final Class<?> valueClass = value.getClass();
			if(valueClass.isArray()){
				final Class<?> componentType = valueClass.getComponentType();
				builder = switch(componentType.getName()){
					case PRIMITIVE_CLASS_NAME_BOOLEAN -> builder.defineArray(key, (boolean[])value);
					case PRIMITIVE_CLASS_NAME_BYTE -> builder.defineArray(key, (byte[])value);
					case PRIMITIVE_CLASS_NAME_CHAR -> builder.defineArray(key, (char[])value);
					case PRIMITIVE_CLASS_NAME_SHORT -> builder.defineArray(key, (short[])value);
					case PRIMITIVE_CLASS_NAME_INT -> builder.defineArray(key, (int[])value);
					case PRIMITIVE_CLASS_NAME_LONG -> builder.defineArray(key, (long[])value);
					case PRIMITIVE_CLASS_NAME_FLOAT -> builder.defineArray(key, (float[])value);
					case PRIMITIVE_CLASS_NAME_DOUBLE -> builder.defineArray(key, (double[])value);
					case CLASS_NAME_STRING -> builder.defineArray(key, (String[])value);
					default -> throw new IllegalArgumentException("Unsupported array type: " + componentType);
				};
			}
			else
				switch(value){
					case final Boolean b -> builder = builder.define(key, b);
					case final Byte b -> builder = builder.define(key, b);
					case final Character c -> builder = builder.define(key, c);
					case final Short n -> builder = builder.define(key, n);
					case final Integer n -> builder = builder.define(key, n);
					case final Long l -> builder = builder.define(key, l);
					case final Float v -> builder = builder.define(key, v);
					case final Double v -> builder = builder.define(key, v);
					case final String s -> builder = builder.define(key, s);
					case final Enum<?> e -> builder = builder.define(key, e);
					case final Class<?> c -> builder = builder.define(key, c);
					case final Annotation a -> builder = builder.define(key, a);
					default -> throw new IllegalArgumentException("Unsupported type: " + value.getClass().getName());
				}
		}
		return builder.build();
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
				if(returnType.equals(Class.class) && value instanceof final String v)
					value = DataType.toTypeOrSelf(v);
				else if(returnType.isArray() && value instanceof final List<?> subValues){
					final int length = subValues.size();
					if(length > 0){
						final Object result = CodecHelper.createArray(returnType.getComponentType(), length);
						for(int i = 0; i < length; i ++){
							final Map<String, Object> subs = (Map<String, Object>)subValues.get(i);

							final String subAnnotationType = (String)subs.get(DescriberKey.ANNOTATION_TYPE.toString());
							Array.set(result, i, createAnnotation((Class<? extends Annotation>)Class.forName(subAnnotationType), subs));
						}
						value = result;
					}
					else
						value = CodecHelper.createArray(returnType.getComponentType(), 0);
				}
				else if(Annotation.class.isAssignableFrom(returnType) && value instanceof final Map<?, ?> subValues)
					//manage ConverterChoices, ConverterChoice, ObjectChoices, ObjectChoice, ObjectChoicesList
					value = createAnnotation((Class<? extends Annotation>)returnType, (Map<String, Object>)subValues);
			}
			catch(final Exception ignored){}
			return value;
		}
	}

}
