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

import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.DataType;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;


public final class AnnotationCreator{

	private AnnotationCreator(){}


	public static <A extends Annotation> A createAnnotation(final Class<A> annotationType, final Map<String, Object> values){
		return (A)Proxy.newProxyInstance(
			annotationType.getClassLoader(),
			new Class[]{annotationType},
			new DynamicAnnotationInvocationHandler(annotationType, values));
	}

	public static <A extends Annotation> A createAnnotation(final String annotationTypeName, final Map<String, Object> values)
			throws ClassNotFoundException{
		final Class<? extends A> annotationType = (Class<? extends A>)getAnnotationClass(annotationTypeName);
		return createAnnotation(annotationType, values);
	}

	private static Class<? extends Annotation> getAnnotationClass(final String annotationType) throws ClassNotFoundException{
		return (Class<? extends Annotation>)Class.forName(annotationType);
	}


	private static final class DynamicAnnotationInvocationHandler implements InvocationHandler{
		private final Class<? extends Annotation> annotationType;
		private final Map<String, Object> values;

		private DynamicAnnotationInvocationHandler(final Class<? extends Annotation> annotationType, final Map<String, Object> values){
			this.annotationType = annotationType;
			this.values = Map.copyOf(values);
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
			else if(returnType.isArray() && value instanceof List<?>){
				final Class<?> elementType = returnType.getComponentType();
				value = invokeArrayValues(elementType, (List<Map<String, Object>>)value);
			}
			else if(Annotation.class.isAssignableFrom(returnType) && value instanceof Map<?, ?>){
				//manage `ConverterChoices`, `ObjectChoices`, and `ObjectChoicesList`
				final Class<? extends Annotation> annotationType = (Class<? extends Annotation>)returnType;
				value = createAnnotation(annotationType, (Map<String, Object>)value);
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

}
