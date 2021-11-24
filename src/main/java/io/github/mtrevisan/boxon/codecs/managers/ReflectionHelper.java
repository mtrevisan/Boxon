/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs.managers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class ReflectionHelper{

	private ReflectionHelper(){}


	public static Class<?>[] extractCallerClasses(){
		final StackWalker walker = StackWalker.getInstance();
		final List<String> classNames = walker.walk(frames -> frames.skip(1)
			.limit(2)
			.map(StackWalker.StackFrame::getClassName)
			.collect(Collectors.toList()));

		final Class<?>[] classes = new Class[classNames.size()];
		try{
			for(int i = 0; i < classNames.size(); i ++)
				classes[i] = Class.forName(classNames.get(i));
		}
		catch(final ClassNotFoundException ignored){}
		return classes;
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes.
	 */
	public static Collection<Class<?>> extractClasses(final Class<?> type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		reflectiveClassLoader.scan(type);
		final Collection<Class<?>> modules = reflectiveClassLoader.getImplementationsOf(type);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> singletons = reflectiveClassLoader.getTypesAnnotatedWith((Class<? extends Annotation>)type);
		classes.addAll(modules);
		classes.addAll(singletons);
		return classes;
	}


	@SuppressWarnings("unchecked")
	static <T> T getFieldValue(final Field field, final Object obj){
		try{
			return (T)field.get(obj);
		}
		catch(final IllegalAccessException e){
			//should never happen
			throw new IllegalArgumentException(e);
		}
	}

	public static void setFieldValue(final Field field, final Object obj, final Object value){
		try{
			field.set(obj, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException e){
			throw new IllegalArgumentException("Can not set " + field.getType().getSimpleName() + " field to "
				+ value.getClass().getSimpleName(), e);
		}
	}

	public static <T> void setFieldValue(final Object obj, final Class<T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(obj.getClass(), fieldType);
			for(int i = 0; i < fields.size(); i ++){
				final Field field = fields.get(i);
				field.set(obj, value);
			}
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}

	public static <T> void setStaticFieldValue(final Class<?> cl, final Class<T> fieldType, final T value){
		try{
			final List<Field> fields = getAccessibleFields(cl, fieldType);
			for(int i = 0; i < fields.size(); i ++)
				fields.get(i).set(null, value);
		}
		catch(final IllegalArgumentException | IllegalAccessException ignored){}
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @return	An array of all the fields of the given class.
	 */
	static List<Field> getAccessibleFields(final Class<?> cls){
		return getAccessibleFields(cls, null);
	}

	/**
	 * Retrieve all declared fields in the current class AND in the parent classes.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param fieldType	The class for which to extract all the fields.
	 * @return	An array of all the fields of the given class.
	 */
	private static List<Field> getAccessibleFields(Class<?> cls, final Class<?> fieldType){
		final List<Field> fields = new ArrayList<>(0);

		//recurse classes:
		final ArrayList<Field> childFields = new ArrayList<>(0);
		while(cls != null && cls != Object.class){
			final Field[] rawSubfields = cls.getDeclaredFields();
			extractChildFields(childFields, rawSubfields, fieldType);
			//place parent's fields before all the child's fields
			fields.addAll(0, childFields);

			//go up to parent class
			cls = cls.getSuperclass();
		}

		makeFieldsAccessible(fields);

		return fields;
	}

	private static void extractChildFields(final ArrayList<Field> childFields, final Field[] rawSubfields, final Class<?> fieldType){
		childFields.clear();
		childFields.ensureCapacity(rawSubfields.length);
		//apply filter on field type if needed
		for(int i = 0; i < rawSubfields.length; i ++)
			if(fieldType == null || rawSubfields[i].getType() == fieldType)
				childFields.add(rawSubfields[i]);
	}

	private static void makeFieldsAccessible(final List<Field> fields){
		for(int i = 0; i < fields.size(); i ++)
			fields.get(i).setAccessible(true);
	}


	@SuppressWarnings("unchecked")
	static <T> T invokeMethod(final Object obj, final Method method, final T defaultValue){
		T result = defaultValue;
		try{
			result = (method != null? (T)method.invoke(obj): defaultValue);
		}
		catch(final IllegalAccessException | InvocationTargetException ignored){}
		return result;
	}

	static Method getAccessibleMethod(Class<?> cls, final String methodName, final Class<?> returnType, final Class<?>... parameterTypes){
		Method method = null;
		while(method == null && cls != null && cls != Object.class){
			method = makeMethodAccessible(cls, methodName, returnType, parameterTypes);

			//go up to parent class
			cls = cls.getSuperclass();
		}
		return method;
	}

	private static Method makeMethodAccessible(final Class<?> cls, final String methodName, final Class<?> returnType,
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
