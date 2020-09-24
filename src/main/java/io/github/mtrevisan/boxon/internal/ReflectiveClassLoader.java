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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @see <a href="https://github.com/classgraph/classgraph">ClassGraph</a>
 */
public final class ReflectiveClassLoader{

	private static final Map<Class<?>, Collection<Class<?>>> METADATA_STORE = new ConcurrentHashMap<>(0);

	private final ClassGraph classGraph;


	/**
	 * @param packageClasses	List of packages to scan into.
	 */
	public static ReflectiveClassLoader createFrom(final Class<?>... packageClasses){
		return new ReflectiveClassLoader(packageClasses);
	}

	private ReflectiveClassLoader(final Class<?>... packageClasses){
		Objects.requireNonNull(packageClasses, "Packages list cannot be null");
		if(packageClasses.length == 0)
			throw new IllegalArgumentException("Packages list cannot be empty");

		final DynamicArray<String> packageNames = DynamicArray.create(String.class, packageClasses.length);
		for(final Class<?> packageClass : packageClasses)
			packageNames.add(packageClass.getPackageName());
		classGraph = new ClassGraph()
			.ignoreClassVisibility()
			.enableAnnotationInfo()
			.acceptPackages(packageNames.extractCopy());
	}

	/**
	 * Scan the given packages for classes that extends or implements the given classes.
	 *
	 * @param classes	Classes that must be extended or implemented.
	 */
	public void scan(final Class<?>... classes){
		Objects.requireNonNull(classes, "Classes cannot be null");
		if(classes.length == 0)
			throw new IllegalArgumentException("Class list cannot be empty");

		try(final ScanResult scanResult = classGraph.scan()){
			for(final Class<?> cls : classes)
				scan(scanResult, cls);
		}
	}

	private void scan(final ScanResult scanResult, final Class<?> filteringClass){
		final ClassInfoList classInfo = (filteringClass.isAnnotation()? scanResult.getClassesWithAnnotation(filteringClass.getName()):
			scanResult.getClassesImplementing(filteringClass.getName()));
		for(final Class<?> cls : classInfo.loadClasses())
			METADATA_STORE.computeIfAbsent(filteringClass, classes -> new ArrayList<>(1))
				.add(cls);
	}

	/**
	 * Gets all classes implementing a give interface.
	 *
	 * @param type	The interface to search the implementation for.
	 * @return	The collection of classes implementing the given interface.
	 */
	public Collection<Class<?>> getImplementationsOf(final Class<?> type){
		return METADATA_STORE.get(type);
	}

	/**
	 * Get types annotated with a given annotation, both classes and annotations.
	 * <p>{@link Inherited} is not honored by default.</p>
	 * <p>When honoring {@link Inherited}, meta-annotation should only effect annotated super classes and its sub types.</p>
	 * <p><i>Note that this ({@link Inherited}) meta-annotation type has no effect if the annotated type is used for
	 * anything other then a class. Also, this meta-annotation causes annotations to be inherited only from superclasses;
	 * annotations on implemented interfaces have no effect</i>.</p>
	 *
	 * @param annotation	The annotation to search for.
	 * @return	The collection of classes.
	 */
	public Collection<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation){
		return METADATA_STORE.get(annotation);
	}

}
