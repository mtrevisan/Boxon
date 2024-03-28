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

import io.github.mtrevisan.boxon.exceptions.DataException;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A collection of convenience methods for working with reflection.
 *
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final Class<?> PARENT_CLASS_LIMIT = Object.class;


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
		catch(final IllegalAccessException ignored){
			//should never happen
			return null;
		}
	}

	/**
	 * Sets the field represented by this {@code Field} object on the specified object argument to the specified value.
	 * <p>The new value is automatically unwrapped if the underlying field has a primitive type.</p>
	 * @param obj	The object whose field should be modified.
	 * @param field	The field.
	 * @param value	The value for the field being modified.
	 * @return	The (possibly new) object on witch the value was set.
	 * @throws DataException	If the value cannot be set to the field.
	 */
	public static Object withValue(final Object obj, final Field field, final Object value){
		try{
			return (isRecordClass(obj)
				? constructRecordWithUpdatedField(obj, field.getName(), value)
				: updateField(obj, field, value)
			);
		}
		catch(final IllegalArgumentException | ReflectiveOperationException e){
			throw DataException.create("Can not set {} field to {}",
				field.getType().getSimpleName(), value.getClass().getSimpleName(), e);
		}
	}

	private static boolean isRecordClass(final Object obj){
		return obj.getClass()
			.isRecord();
	}

	private static <T> T constructRecordWithUpdatedField(final T obj, final String fieldName, final Object value)
			throws ReflectiveOperationException{
		@SuppressWarnings("unchecked")
		final Class<T> objClass = (Class<T>)obj.getClass();
		final RecordComponent[] recordComponents = objClass.getRecordComponents();

		//extract the current field values from the record class
		final Object[] recordValues = extractCurrentFieldValues(obj, recordComponents);

		//find the index of the field to update
		setFieldValue(fieldName, value, recordComponents, recordValues);

		return ConstructorHelper.createRecordInstance(recordComponents, objClass, recordValues);
	}

	private static <T> Object[] extractCurrentFieldValues(final T obj, final RecordComponent[] components)
			throws IllegalAccessException, InvocationTargetException{
		final int length = components.length;
		final Object[] recordValues = new Object[length];
		for(int i = 0; i < length; i ++){
			final RecordComponent recordComponent = components[i];

			final Method accessor = recordComponent.getAccessor();
			makeAccessible(accessor);
			recordValues[i] = accessor.invoke(obj);
		}
		return recordValues;
	}

	private static void setFieldValue(final String fieldName, final Object value, final RecordComponent[] recordComponents,
			final Object[] recordValues){
		for(int i = 0, length = recordComponents.length; i < length; i ++)
			if(fieldName.equals(recordComponents[i].getName())){
				recordValues[i] = value;
				break;
			}
	}

	private static Object updateField(final Object obj, final Field field, final Object value) throws IllegalAccessException{
		field.set(obj, value);
		return obj;
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
			for(int i = 0, length = fields.size(); i < length; i ++)
				fields.get(i)
					.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}


	/**
	 * Maps the fields of an object to a Map, where the keys are the field names and the values are the field values.
	 *
	 * @param object	The object whose fields should be mapped.
	 * @return	A Map containing the field names as keys and the field values as values.
	 */
	public static Map<String, Object> mapObject(final Object object){
		if(object == null)
			return null;

		final List<Field> fields = getAccessibleFields(object.getClass());
		final int size = fields.size();
		final Map<String, Object> map = new HashMap<>(size);
		for(int i = 0; i < size; i ++){
			final Field field = fields.get(i);

			final String key = field.getName();
			final Object value = getValue(object, field);
			map.put(key, value);
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
	 * @param fieldType	The class of the fields to be extracted (for injection purposes).
	 * @return	An array of all the fields of the given class.
	 */
	private static List<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final List<Field> allFields = new ArrayList<>(0);

		final ArrayList<Field> childFields = new ArrayList<>(0);
		while(cls != null && cls != PARENT_CLASS_LIMIT){
			final Field[] rawChildFields = cls.getDeclaredFields();
			childFields.clear();
			childFields.ensureCapacity(rawChildFields.length);
			if(fieldType == null)
				extractChildFields(rawChildFields, childFields);
			else
				extractInjectableChildFields(rawChildFields, fieldType, childFields);

			if(!childFields.isEmpty())
				//place parent's fields before all the child's fields
				allFields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		makeFieldsAccessible(allFields);

		return allFields;
	}

	/**
	 * Extracts the child fields from the given array of fields and adds them to the provided collection, keeping those fields that have
	 * the {@link Injected} annotation and are of the given (field) type.
	 *
	 * @param rawFields	The array of fields to extract child fields from.
	 * @param fieldType	The class of the fields to be extracted.
	 * @param fields	The collection to which the child fields will be added.
	 */
	private static void extractInjectableChildFields(final Field[] rawFields, final Class<?> fieldType, final Collection<Field> fields){
		for(int i = 0, length = rawFields.length; i < length; i ++){
			final Field rawField = rawFields[i];

			//an injection should be performed
			if(rawField.isAnnotationPresent(Injected.class) && fieldType.isAssignableFrom(rawField.getType()))
				fields.add(rawField);
		}
	}

	/**
	 * Extracts the child fields from the given array of fields and adds them to the provided collection, keeping those fields that have
	 * an annotation and are not static.
	 *
	 * @param rawFields	The array of fields to extract child fields from.
	 * @param fields	The collection to which the child fields will be added.
	 */
	private static void extractChildFields(final Field[] rawFields, final Collection<Field> fields){
		for(int i = 0, length = rawFields.length; i < length; i ++){
			final Field rawField = rawFields[i];

			if(rawField.getAnnotations().length > 0 && !isStatic(rawField))
				fields.add(rawField);
		}
	}

	static boolean isStatic(final Field field){
		return Modifier.isStatic(field.getModifiers());
	}

	private static void makeFieldsAccessible(final List<Field> fields){
		for(int i = 0, length = fields.size(); i < length; i ++)
			makeAccessible(fields.get(i));
	}

	/**
	 * Makes the given {@code AccessibleObject} accessible, allowing it to be accessed or invoked regardless of the usual access
	 * restrictions.
	 *
	 * @param obj	The {@code AccessibleObject} to make accessible.
	 */
	public static void makeAccessible(final AccessibleObject obj){
		obj.setAccessible(true);
	}

}
