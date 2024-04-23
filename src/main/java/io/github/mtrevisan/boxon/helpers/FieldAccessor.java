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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A collection of convenience methods for working with reflection.
 *
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class FieldAccessor{

	private static final Class<?> PARENT_CLASS_LIMIT = Object.class;


	private FieldAccessor(){}


	public static Class<?> extractFieldType(final Class<?> fieldType){
		return (fieldType.isArray()
			? fieldType.getComponentType()
			: fieldType
		);
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
	public static <T> T setFieldValue(final T obj, final Field field, final Object value){
		try{
			return updateObjectFieldValue(obj, field, value);
		}
		catch(final IllegalArgumentException | ReflectiveOperationException e){
			throw DataException.create("Can not set {} field to {}",
				field.getType().getSimpleName(), value.getClass().getSimpleName(), e);
		}
	}

	//FIXME ugliness (set & create... also a cycle between classes...)
	private static <T> T updateObjectFieldValue(final T obj, final Field field, final Object value) throws IllegalArgumentException,
			ReflectiveOperationException{
		return (isRecordClass(obj)
			? ConstructorHelper.constructRecordWithUpdatedField(obj, field.getName(), value)
			: updateField(obj, field, value)
		);
	}

	private static <T> T updateField(final T obj, final Field field, final Object value) throws IllegalAccessException{
		field.set(obj, value);
		return obj;
	}

	private static boolean isRecordClass(final Object obj){
		return obj.getClass()
			.isRecord();
	}

	static Object[] retrieveCurrentFieldValues(final Object obj, final RecordComponent[] components) throws ReflectiveOperationException{
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

	static void updateFieldValue(final String fieldName, final Object value, final RecordComponent[] recordComponents,
			final Object[] recordValues){
		for(int i = 0, length = recordComponents.length; i < length; i ++)
			if(fieldName.equals(recordComponents[i].getName())){
				recordValues[i] = value;
				break;
			}
	}


	/**
	 * Injects the given value of given field type in the given object.
	 *
	 * @param obj	The object whose field should be modified.
	 * @param values	The value for the field being modified.
	 */
	public static void injectValues(final Object obj, final Object... values){
		injectValues(obj.getClass(), obj, values);
	}

	/**
	 * Static injects the given value of given field type in the given class.
	 *
	 * @param objClass	The object class whose static field should be modified.
	 * @param values	The value for the field being modified.
	 */
	public static void injectStaticValues(final Class<?> objClass, final Object... values){
		injectValues(objClass, null, values);
	}

	private static void injectValues(final Class<?> objClass, final Object obj, final Object... values){
		for(int i = 0, length = values.length; i < length; i ++){
			final Object value = values[i];

			final Type fieldType = extractFieldType(value);
			if(fieldType instanceof final Class<?> fieldClass && fieldClass.isAssignableFrom(value.getClass()))
				injectValue(objClass, obj, fieldClass, value);
		}
	}

	private static Type extractFieldType(final Object value){
		return GenericHelper.resolveGenericTypes(value.getClass(), Object.class)
			.getFirst();
	}

	private static <T> void injectValue(final Class<?> objType, final Object obj, final Class<? extends T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(objType, fieldType);
			for(int i = 0, length = fields.size(); i < length; i ++)
				fields.get(i)
					.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
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
		final List<Field> allFields = new ArrayList<>();
		while(cls != null && cls != PARENT_CLASS_LIMIT){
			final Field[] rawChildFields = cls.getDeclaredFields();
			final List<Field> childFields = extractChildFields(rawChildFields, fieldType);

			if(!childFields.isEmpty())
				//place parent's fields before all the child's fields
				allFields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}
		makeFieldsAccessible(allFields);
		return allFields;
	}

	private static List<Field> extractChildFields(final Field[] rawChildFields, final Class<?> fieldType){
		final List<Field> childFields = new ArrayList<>();
		if(fieldType != null)
			extractInjectableChildFields(rawChildFields, fieldType, childFields);
		else
			extractChildFields(rawChildFields, childFields);
		return childFields;
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

	static boolean isStatic(final Member field){
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
