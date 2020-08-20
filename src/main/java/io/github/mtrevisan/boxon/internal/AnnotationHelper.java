/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.internal;

import io.github.mtrevisan.boxon.internal.reflection.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * @see <a href="https://bill.burkecentral.com/2008/01/14/scanning-java-annotations-at-runtime/">Scanning Java Annotations at Runtime</a>
 */
public final class AnnotationHelper{

	private AnnotationHelper(){}

	/**
	 * Retrieving fields list of specified class.
	 *
	 * @param cls	The class from which to extract the declared fields.
	 * @param recursively	If {@code true}, it retrieves fields from all class hierarchy.
	 * @return	An array of all the fields of the given class.
	 */
	public static DynamicArray<Field> getDeclaredFields(final Class<?> cls, final boolean recursively){
		final DynamicArray<Field> fields;
		if(recursively){
			fields = DynamicArray.create(Field.class);
			Class<?> currentType = cls;
			while(currentType != Object.class){
				final Field[] subfields = currentType.getDeclaredFields();
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
		}
		else
			fields = DynamicArray.createFrom(cls.getDeclaredFields());
		return fields;
	}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes.
	 */
	public static Collection<Class<?>> extractClasses(final Object type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final Reflections reflections = Reflections.create(basePackageClasses);
		@SuppressWarnings("unchecked")
		final Set<Class<?>> modules = reflections.getSubTypesOf((Class<Object>)type);
		@SuppressWarnings("unchecked")
		final Set<Class<?>> singletons = reflections.getTypesAnnotatedWith((Class<? extends Annotation>)type);
		classes.addAll(modules);
		classes.addAll(singletons);
		return classes;
	}

}
