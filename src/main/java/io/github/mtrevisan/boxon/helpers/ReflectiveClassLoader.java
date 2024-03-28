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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @see <a href="https://github.com/classgraph/classgraph">ClassGraph</a>
 */
public final class ReflectiveClassLoader{

	private final Map<Class<?>, Collection<Class<?>>> metadataStore = new ConcurrentHashMap<>(0);

	private final ClassGraph classGraph;


	/**
	 * @param packageClasses	List of packages to scan into.
	 * @return	The loader instance for the given packages.
	 */
	public static ReflectiveClassLoader createFrom(final Class<?>... packageClasses){
		return new ReflectiveClassLoader(packageClasses);
	}


	private ReflectiveClassLoader(final Class<?>... packageClasses){
		Objects.requireNonNull(packageClasses, "Packages list cannot be null");

		final int length = packageClasses.length;
		if(length == 0)
			throw new IllegalArgumentException("Packages list cannot be empty");

		final String[] packageNames = new String[length];
		for(int i = 0; i < length; i ++)
			packageNames[i] = packageClasses[i].getPackageName();
		classGraph = new ClassGraph()
			.ignoreClassVisibility()
			.enableAnnotationInfo()
			.acceptPackages(packageNames);
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
	public List<Class<?>> extractClassesWithAnnotation(final Class<? extends Annotation> annotation){
		return extractClassesWithInfo(annotation, scanResult -> scanResult.getClassesWithAnnotation(annotation));
	}

	/**
	 * Gets all classes implementing a give interface.
	 *
	 * @param type	The interface to search the implementation for.
	 * @return	The collection of classes implementing the given interface.
	 */
	public List<Class<?>> extractClassesImplementing(final Class<?> type){
		return extractClassesWithInfo(type, scanResult -> scanResult.getClassesImplementing(type));
	}

	/**
	 * Scan the given packages for classes that have a field with the given annotation.
	 *
	 * @param type	Annotation that must be extended or implemented.
	 * @return	The classes.
	 */
	public List<Class<?>> extractClassesWithFieldAnnotation(final Class<? extends Annotation> type){
		return extractClassesWithInfo(type, scanResult -> scanResult.getClassesWithFieldAnnotation(type));
	}

	private List<Class<?>> extractClassesWithInfo(final Class<?> type, final Function<ScanResult, ClassInfoList> filter){
		final List<Class<?>> loadedClasses = getStoredClasses(type);

		if(loadedClasses.isEmpty()){
			try(final ScanResult scanResult = classGraph.scan()){
				final ClassInfoList classInfo = filter.apply(scanResult);
				final List<Class<?>> list = classInfo.loadClasses();
				storeClasses(type, list);
				loadedClasses.addAll(list);
			}
		}
		return Collections.unmodifiableList(loadedClasses);
	}

	private List<Class<?>> getStoredClasses(final Class<?> type){
		return (List<Class<?>>)metadataStore.getOrDefault(type, new ArrayList<>(0));
	}

	private void storeClasses(final Class<?> type, final Collection<Class<?>> list){
		metadataStore.put(type, list);
	}


	/**
	 * Extracts the two direct callers of this method.
	 *
	 * @return	An array with the two most direct callers of this method.
	 */
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

}
