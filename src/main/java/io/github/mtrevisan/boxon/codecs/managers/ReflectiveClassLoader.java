/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * @see <a href="https://github.com/classgraph/classgraph">ClassGraph</a>
 */
public final class ReflectiveClassLoader{

	private final Map<Class<?>, Collection<Class<?>>> metadataStore = new ConcurrentHashMap<>(0);

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

		final Collection<String> packageNames = new ArrayList<>(packageClasses.length);
		for(int i = 0; i < packageClasses.length; i ++)
			packageNames.add(packageClasses[i].getPackageName());
		classGraph = new ClassGraph()
			.ignoreClassVisibility()
			.enableAnnotationInfo()
			.acceptPackages(packageNames.toArray(String[]::new));
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @return	The classes.
	 */
	public Collection<Class<?>> extractClasses(final Class<?> type){
		scan(type);
		final Collection<Class<?>> modules = getImplementationsOf(type);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> singletons = getTypesAnnotatedWith((Class<? extends Annotation>)type);
		final Collection<Class<?>> classes = new HashSet<>(modules.size() + singletons.size());
		classes.addAll(modules);
		classes.addAll(singletons);
		return classes;
	}

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
	 * Scan the given packages for classes that extends or implements the given classes.
	 *
	 * @param classes	Classes that must be extended or implemented.
	 */
	private void scan(final Class<?>... classes){
		Objects.requireNonNull(classes, "Classes cannot be null");
		if(classes.length == 0)
			throw new IllegalArgumentException("Class list cannot be empty");

		try(final ScanResult scanResult = classGraph.scan()){
			for(int i = 0; i < classes.length; i ++)
				scan(scanResult, classes[i]);
		}
	}

	private void scan(final ScanResult scanResult, final Class<?> filteringClass){
		final ClassInfoList classInfo = (filteringClass.isAnnotation()
			? scanResult.getClassesWithAnnotation(filteringClass.getName())
			: scanResult.getClassesImplementing(filteringClass.getName()));
		final List<Class<?>> loadedClasses = classInfo.loadClasses();
		metadataStore.computeIfAbsent(filteringClass, classes -> new ArrayList<>(loadedClasses.size()))
			.addAll(loadedClasses);
	}

	/**
	 * Gets all classes implementing a give interface.
	 *
	 * @param type	The interface to search the implementation for.
	 * @return	The collection of classes implementing the given interface.
	 */
	private Collection<Class<?>> getImplementationsOf(final Class<?> type){
		return metadataStore.getOrDefault(type, Collections.emptyList());
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
	private Collection<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation){
		return metadataStore.getOrDefault(annotation, Collections.emptyList());
	}

}
