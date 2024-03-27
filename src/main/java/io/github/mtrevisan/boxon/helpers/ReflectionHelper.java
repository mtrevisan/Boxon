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
import org.springframework.objenesis.Objenesis;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A collection of convenience methods for working with reflection.
 *
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private static final Function<Class<?>, Supplier<?>> EMPTY_CREATORS = Memoizer.memoize(ReflectionHelper::getEmptyCreatorInner);
	private static final Function<NonEmptyConstructorTuple<?>, Function<Object[], ?>> NON_EMPTY_CREATORS
		= Memoizer.memoize(ReflectionHelper::getNonEmptyCreatorInner);

	private static final Objenesis OBJENESIS = new ObjenesisStd();


	private ReflectionHelper(){}


	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Supplier<T> getEmptyCreator(final Class<T> type){
		return (Supplier<T>)EMPTY_CREATORS.apply(type);
	}

	/**
	 * Gets the creator function for the given class.
	 *
	 * @param type	The class to extract the creator for.
	 * @param constructorClasses	Array of types of constructor parameters.
	 * @param <T>	The parameter identifying the class.
	 * @return	A method that construct the given class.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Function<Object[], T> getNonEmptyCreator(final Class<T> type, final Class<?>[] constructorClasses){
		return (Function<Object[], T>)NON_EMPTY_CREATORS.apply(new NonEmptyConstructorTuple<>(type, constructorClasses));
	}

	private static <T> Supplier<T> getEmptyCreatorInner(final Class<T> type){
		ObjectInstantiator<T> instantiator;
		try{
			final Constructor<T> constructor = type.getDeclaredConstructor();
			ReflectionHelper.makeAccessible(constructor);

			//try creating an instance
			constructor.newInstance();

			instantiator = () -> {
				try{
					return constructor.newInstance();
				}
				catch(final Exception e){
					throw new ObjenesisException(e);
				}
			};
		}
		catch(final Exception ignored){
			instantiator = OBJENESIS.getInstantiatorOf(type);
		}
		return instantiator::newInstance;
	}

	private static <T> Function<Object[], T> getNonEmptyCreatorInner(final NonEmptyConstructorTuple<T> tuple){
		Function<Object[], T> instantiator = null;
		try{
			final Class<T> type = tuple.type;
			final Class<?>[] constructorClasses = tuple.constructorClasses;
			final Constructor<T> constructor = type.getDeclaredConstructor(constructorClasses);
			ReflectionHelper.makeAccessible(constructor);

			instantiator = (final Object[] constructorValues) -> {
				try{
					return constructor.newInstance(constructorValues);
				}
				catch(final Exception e){
					throw new ObjenesisException(e);
				}
			};
		}
		catch(final Exception ignored){}
		return instantiator;
	}


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

		return createRecordInstance(recordComponents, objClass, recordValues);
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

	private static Object[] setFieldValue(final String fieldName, final Object value, final RecordComponent[] recordComponents,
			final Object[] recordValues){
		for(int i = 0, length = recordComponents.length; i < length; i ++)
			if(fieldName.equals(recordComponents[i].getName())){
				recordValues[i] = value;
				break;
			}
		return recordValues;
	}

	private static <T> T createRecordInstance(final RecordComponent[] recordComponents, final Class<T> objClass,
			final Object[] recordValues){
		//extract the field types from the record class
		final Class<?>[] constructorClasses = extractFieldTypes(recordComponents);

		//creates a new instance of the record class with the updated values
		return getNonEmptyCreator(objClass, constructorClasses)
			.apply(recordValues);
	}

	private static Class<?>[] extractFieldTypes(final RecordComponent[] components){
		final int length = components.length;
		final Class<?>[] constructorClasses = new Class<?>[length];
		for(int i = 0; i < length; i ++)
			constructorClasses[i] = components[i].getType();
		return constructorClasses;
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
	 * @param fieldType	The class of the fields to be extracted.
	 * @return	An array of all the fields of the given class.
	 */
	private static List<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final List<Field> allFields = new ArrayList<>(0);

		final ArrayList<Field> childFields = new ArrayList<>(0);
		while(cls != null && cls != Object.class){
			final Field[] rawChildFields = cls.getDeclaredFields();
			childFields.clear();
			childFields.ensureCapacity(rawChildFields.length);
			if(fieldType == null)
				extractChildFields(rawChildFields, childFields);
			else
				extractChildFields2(rawChildFields, fieldType, childFields);

			//place parent's fields before all the child's fields
			allFields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		makeFieldsAccessible(allFields);

		return allFields;
	}

	private static void extractChildFields2(final Field[] rawFields, final Class<?> fieldType, final Collection<Field> fields){
		for(int i = 0, length = rawFields.length; i < length; i ++){
			final Field rawSubField = rawFields[i];

			//an injection must be performed
			if(rawSubField.isAnnotationPresent(Injected.class) && fieldType.isAssignableFrom(rawSubField.getType()))
				fields.add(rawSubField);
		}
	}

	private static void extractChildFields(final Field[] rawFields, final Collection<Field> fields){
		for(int i = 0, length = rawFields.length; i < length; i ++)
			fields.add(rawFields[i]);
	}

	private static void makeFieldsAccessible(final List<Field> fields){
		for(int i = 0, length = fields.size(); i < length; i ++)
			makeAccessible(fields.get(i));
	}

	public static void makeAccessible(final AccessibleObject obj){
		obj.setAccessible(true);
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
	public static <T> T invokeMethod(final Object obj, final Method method, final T defaultValue){
		T result = defaultValue;
		try{
			if(method != null)
				result = (T)method.invoke(obj);
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
				makeAccessible(method);
		}
		catch(final NoSuchMethodException | SecurityException | InaccessibleObjectException ignored){}
		return method;
	}


	private record NonEmptyConstructorTuple<T>(Class<T> type, Class<?>[] constructorClasses){
		@Override
		public boolean equals(final Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;
			final NonEmptyConstructorTuple<?> other = (NonEmptyConstructorTuple<?>)obj;
			return (Objects.equals(type, other.type) && Arrays.equals(constructorClasses, other.constructorClasses));
		}

		@Override
		public int hashCode(){
			return 31 * Objects.hash(type) + Arrays.hashCode(constructorClasses);
		}
	}

}
