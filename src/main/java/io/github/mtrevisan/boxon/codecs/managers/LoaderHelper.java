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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


public final class LoaderHelper{

	private LoaderHelper(){}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package.
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes.
	 */
	public static Collection<Class<?>> extractClasses(final Class<?> type, final Class<?>... basePackageClasses){
		final ReflectiveClassLoader reflectiveClassLoader = ReflectiveClassLoader.createFrom(basePackageClasses);
		reflectiveClassLoader.scan(type);
		final Collection<Class<?>> modules = reflectiveClassLoader.getImplementationsOf(type);
		@SuppressWarnings("unchecked")
		final Collection<Class<?>> singletons = reflectiveClassLoader.getTypesAnnotatedWith((Class<? extends Annotation>)type);
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

}
