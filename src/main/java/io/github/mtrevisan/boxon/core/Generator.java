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
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataType;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.GenericHelper;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unused", "WeakerAccess"})
public final class Generator{

	private static final String KEY_SEPARATOR = "|";


	/**
	 * Create a generator.
	 *
	 * @return	A generator.
	 */
	public static Generator create(){
		return new Generator();
	}


	private Generator(){}


	public Class<?> generateClass(final Map<String, Object> descriptor) throws ClassNotFoundException{
		final String template = (String)descriptor.get(DescriberKey.TEMPLATE.toString());
		final Map<String, Object> header = (Map<String, Object>)descriptor.get(DescriberKey.HEADER.toString());
		final List<Map<String, Object>> fields = (List<Map<String, Object>>)descriptor.get(DescriberKey.FIELDS.toString());
		final List<Map<String, Object>> evaluatedFields = (List<Map<String, Object>>)descriptor.get(
			DescriberKey.EVALUATED_FIELDS.toString());
		final List<Map<String, Object>> postProcessedFields = (List<Map<String, Object>>)descriptor.get(
			DescriberKey.POST_PROCESSED_FIELDS.toString());
		final Map<String, Map<String, Object>> postProcessedNavigableFields = new HashMap<>(postProcessedFields.size());
		for(int i = 0, length = postProcessedFields.size(); i < length; i ++){
			final Map<String, Object> postProcessedField = postProcessedFields.get(i);

			final String name = (String)postProcessedField.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)postProcessedField.get(DescriberKey.FIELD_TYPE.toString());
			postProcessedNavigableFields.put(name + KEY_SEPARATOR + fieldType, postProcessedField);
		}

		final Annotation templateHeader = createAnnotation(TemplateHeader.class, header);

		final ByteBuddy byteBuddy = new ByteBuddy();
		final DynamicType.Builder<Object> builder = byteBuddy.subclass(Object.class)
			.name(template);
		builder.annotateType(templateHeader);
		annotateFields(builder, fields, postProcessedNavigableFields);
		annotateEvaluatedFields(builder, evaluatedFields);
		try(final DynamicType.Unloaded<Object> make = builder.make()){
			return make.load(getClass().getClassLoader())
				.getLoaded();
		}
	}

	private static void annotateFields(final DynamicType.Builder<Object> builder, final List<Map<String, Object>> fields,
			final Map<String, Map<String, Object>> postProcessedNavigableFields) throws ClassNotFoundException{
		final List<Annotation> additionalAnnotations = new ArrayList<>(0);
		for(int i = 0, length = fields.size(); i < length; i ++){
			final Map<String, Object> field = fields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			if(name == null){
				//skip-like annotation, collect generate annotation until a true field is found, then attach to it
				final String annotationType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
				final Annotation annotation = createAnnotation((Class<? extends Annotation>)Class.forName(annotationType), field);
				additionalAnnotations.add(annotation);
			}
			else{
				final String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
				final Class<?> type = DataType.toTypeOrSelf(fieldType);
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
					fieldBuilder.annotateField(additionalAnnotations.get(j));
				additionalAnnotations.clear();
				fieldBuilder.annotateField(annotation);
				if(collectionAnnotation != null)
					fieldBuilder.annotateField(collectionAnnotation);
				if(postProcessedAnnotation != null)
					fieldBuilder.annotateField(postProcessedAnnotation);
			}
		}
	}

	private static void annotateEvaluatedFields(final DynamicType.Builder<Object> builder, final List<Map<String, Object>> evaluatedFields)
			throws ClassNotFoundException{
		for(int i = 0, length = evaluatedFields.size(); i < length; i ++){
			final Map<String, Object> field = evaluatedFields.get(i);

			final String name = (String)field.get(DescriberKey.FIELD_NAME.toString());
			final String fieldType = (String)field.get(DescriberKey.FIELD_TYPE.toString());
			final Class<?> type = DataType.toTypeOrSelf(fieldType);

			final String annotationType = (String)field.get(DescriberKey.ANNOTATION_TYPE.toString());
			final Annotation annotation = createAnnotation((Class<? extends Annotation>)Class.forName(annotationType), field);

			builder.defineField(name, type, Visibility.PACKAGE_PRIVATE)
				.annotateField(annotation);
		}
	}


	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A createAnnotation(final Class<A> annotationType, final Map<String, Object> values){
		return (A)Proxy.newProxyInstance(
			annotationType.getClassLoader(),
			new Class[]{annotationType},
			new DynamicAnnotationInvocationHandler(annotationType, values));
	}


	private static class DynamicAnnotationInvocationHandler implements InvocationHandler{
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

			final Class<?> returnType = method.getReturnType();
			if(returnType.equals(Class.class)){
				try{
					return DataType.toTypeOrSelf((String)value);
				}
				catch(final Exception ignored){}
			}
			else if(returnType.isArray() && value instanceof final List<?> subValues){
				final int length = subValues.size();
				if(length > 0){
					//TODO
					final Annotation[] result = new Annotation[length];
					final Annotation subAnnotationType = (Annotation)GenericHelper.resolveGenericTypes(subValues.getClass(), Object.class);
					for(int i = 0; i < length; i ++)
						result[i] = createAnnotation(subAnnotationType.getClass(), (Map<String, Object>)subValues.get(i));
					value = result;
				}
				else
					value = CodecHelper.createArray(returnType.getComponentType(), 0);
			}
			else if(Annotation.class.isAssignableFrom(returnType) && value instanceof final Map<?, ?> subValues)
				//manage ConverterChoices, ConverterChoice, ObjectChoices, ObjectChoice, ObjectChoicesList
				value = createAnnotation((Class<? extends Annotation>)returnType, (Map<String, Object>)subValues);
			return value;
		}
	}

}
