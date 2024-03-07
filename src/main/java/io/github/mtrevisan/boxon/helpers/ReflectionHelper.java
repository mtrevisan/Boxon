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
package io.github.mtrevisan.boxon.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;


/**
 * A collection of convenience methods for working with reflection.
 *
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private ReflectionHelper(){}


	/**
	 * Returns the value of the field represented by this {@code Field}, on the specified object.
	 *
	 * @param obj	Object from which the represented field's value is to be extracted.
	 * @param field	The field whose value is to be extracted.
	 * @param <T>	The value class type.
	 * @return	The value.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValue(final Object obj, final Field field){
		try{
			return (T)field.get(obj);
		}
		catch(final IllegalAccessException e){
			//should never happen
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Sets the field represented by this {@code Field} object on the specified object argument to the specified value.
	 * <p>The new value is automatically unwrapped if the underlying field has a primitive type.</p>
	 * @param obj	The object whose field should be modified.
	 * @param field	The field.
	 * @param value	The value for the field being modified.
	 */
	public static void setValue(final Object obj, final Field field, final Object value){
		try{
			field.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException e){
			throw new IllegalArgumentException("Can not set " + field.getType().getSimpleName() + " field to "
				+ value.getClass().getSimpleName(), e);
		}
	}


	/**
	 * Injects the given value of given field type in the given object.
	 *
	 * @param obj	The object whose field should be modified.
	 * @param fieldType	The field class.
	 * @param value	The value for the field being modified.
	 * @param <T>	The value class type.
	 */
	public static <T> void injectValue(final Object obj, final Class<T> fieldType, final T value){
		final Class<?> type = obj.getClass();
		injectValue(type, obj, fieldType, value);
	}

	/**
	 * Static injects the given value of given field type in the given class.
	 *
	 * @param type	The object class whose static field should be modified.
	 * @param fieldType	The field class.
	 * @param value	The value for the field being modified.
	 * @param <T>	The value class type.
	 */
	public static <T> void injectStaticValue(final Class<?> type, final Class<T> fieldType, final T value){
		injectValue(type, null, fieldType, value);
	}

	private static <T> void injectValue(final Class<?> objType, final Object obj, final Class<T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(objType, fieldType);
			for(int i = 0; i < fields.size(); i ++)
				fields.get(i)
					.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}


	/**
	 * Maps the fields of an object to a Map, where the keys are the field names and the values are the field values.
	 *
	 * @param template	The object whose fields should be mapped.
	 * @return	A Map containing the field names as keys and the field values as values.
	 */
	public static Map<String, Object> mapObject(final Object template){
		final List<Field> fields = getAccessibleFields(template.getClass());
		final Map<String, Object> map = new HashMap<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final Field field = fields.get(i);

			map.put(field.getName(), getValue(template, field));
		}
		return map;
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @return	An array of all the fields of the given class.
	 */
	public static List<Field> getAccessibleFields(final Class<?> cls){
		return getAccessibleFields(cls, null);
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param fieldType	The class of the fields to be extracted.
	 * @return	An array of all the fields of the given class.
	 */
	private static List<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final List<Field> allFields = new ArrayList<>(0);

		//recurse classes:
		final BiConsumer<Collection<Field>, Field[]> extractChildFields = getExtractChildFieldsMethod(fieldType);
		final ArrayList<Field> childFields = new ArrayList<>(0);
		while(cls != null && cls != Object.class){
			final Field[] rawChildFields = cls.getDeclaredFields();
			childFields.clear();
			childFields.ensureCapacity(rawChildFields.length);
			extractChildFields.accept(childFields, rawChildFields);

			//place parent's fields before all the child's fields
			allFields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		makeFieldsAccessible(allFields);

		return allFields;
	}

	private static BiConsumer<Collection<Field>, Field[]> getExtractChildFieldsMethod(final Class<?> fieldType){
		return (fieldType == null
			? ReflectionHelper::extractChildFields
			: (fields, rawFields) -> extractChildFields(fields, rawFields, fieldType)
		);
	}

	private static void extractChildFields(final Collection<Field> fields, final Field[] rawFields){
		for(int i = 0; i < rawFields.length; i ++)
			fields.add(rawFields[i]);
	}

	//an injection must be performed
	private static void extractChildFields(final Collection<Field> fields, final Field[] rawFields, final Class<?> fieldType){
		for(int i = 0; i < rawFields.length; i ++){
			final Field rawSubField = rawFields[i];
			if(rawSubField.isAnnotationPresent(Injected.class) && fieldType.isAssignableFrom(rawSubField.getType()))
				fields.add(rawSubField);
		}
	}

	private static void makeFieldsAccessible(final List<Field> fields){
		for(int i = 0; i < fields.size(); i ++)
			fields.get(i).setAccessible(true);
	}


	/**
	 * Invokes the underlying method represented by the given {@code Method} object, on the specified object.
	 *
	 * @param obj	The object the underlying method is invoked from.
	 * @param method	The method to be called on the given object.
	 * @param defaultValue	The default value should the method not exists, or returns an error.
	 * @param <T>	The class type of the default value and the returned value.
	 * @return	The value returned by the given method, or the default value if an exception occurs.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethodOrDefault(final Object obj, final Method method, final T defaultValue){
		T result = defaultValue;
		try{
			result = (method != null? (T)method.invoke(obj): defaultValue);
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){}
		return result;
	}

	/**
	 * Get an accessible method defined in the given class (or one of its parents), with the given name, return type, and parameters' types.
	 *
	 * @param cls	The class from which to extract the method.
	 * @param methodName	The method name.
	 * @param returnType	The method return type.
	 * @param parameterTypes	The method parameters' types.
	 * @return	The method, or {@code null} if not found.
	 */
	public static Method getAccessibleMethod(Class<?> cls, final String methodName, final Class<?> returnType,
			final Class<?>... parameterTypes){
		Method method = null;
		while(method == null && cls != null && cls != Object.class){
			method = getMethod(cls, methodName, returnType, parameterTypes);

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return method;
	}

	private static Method getMethod(final Class<?> cls, final String methodName, final Class<?> returnType,
			final Class<?>... parameterTypes){
		Method method = null;
		try{
			method = cls.getDeclaredMethod(methodName, parameterTypes);
			if(returnType == null || method.getReturnType() == returnType)
				method.setAccessible(true);
		}
		catch(final NoSuchMethodException | SecurityException | InaccessibleObjectException ignored){}
		return method;
	}

}
