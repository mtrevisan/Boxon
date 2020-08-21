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
package io.github.mtrevisan.boxon.internal.reflection;

import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Helper methods for working with the classpath.
 */
public final class ClasspathHelper{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(ClasspathHelper.class);

	private static final String EMPTY_STRING = "";

	private static final List<String> PRIMITIVE_NAMES;
	private static final List<Class<?>> PRIMITIVE_TYPES;
	private static final List<String> PRIMITIVE_DESCRIPTORS;
	static{
		PRIMITIVE_NAMES = Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
		PRIMITIVE_TYPES = Arrays.asList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
		PRIMITIVE_DESCRIPTORS = Arrays.asList("Z", "C", "B", "S", "I", "J", "F", "D", "V");
	}

	private static final String SLASH = "/";
	private static final String BACKSLASH = "\\";
	private static final String DOT = ".";
	private static final String DOT_CLASS = ".class";


	private ClasspathHelper(){}

	/**
	 * Returns an array of class Loaders initialized from the specified array.
	 * <p>It defaults to both {@link #contextClassLoader()} and {@link #staticClassLoader()}</p>
	 *
	 * @return	The array of class loaders.
	 */
	private static ClassLoader[] classLoaders(){
		final ClassLoader contextClassLoader = contextClassLoader();
		final ClassLoader staticClassLoader = staticClassLoader();
		final DynamicArray<ClassLoader> loaders = DynamicArray.create(ClassLoader.class, 2);
		if(contextClassLoader != null)
			loaders.add(contextClassLoader);
		if(staticClassLoader != null && contextClassLoader != staticClassLoader)
			loaders.add(staticClassLoader);
		return loaders.extractCopy();
	}

	/**
	 * Gets the current thread context class loader.
	 *
	 * @return	The context class loader, may be {@code null}.
	 */
	public static ClassLoader contextClassLoader(){
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Gets the class loader of this library.
	 * {@code Reflections.class.getClassLoader()}.
	 *
	 * @return	The static library class loader, may be null
	 */
	private static ClassLoader staticClassLoader(){
		return ClasspathHelper.class.getClassLoader();
	}


	/**
	 * Returns the URL that contains a {@code Class}.
	 * <p>This searches for the class using {@link ClassLoader#getResource(String)}.</p>
	 *
	 * @param cls	The class.
	 * @return	The URL containing the class, {@code null} if not found.
	 */
	public static URL forClass(final Class<?> cls){
		final ClassLoader[] loaders = classLoaders();
		final String resourceName = cls.getName().replace(DOT, SLASH) + DOT_CLASS;
		for(final ClassLoader classLoader : loaders){
			try{
				final URL url = classLoader.getResource(resourceName);
				if(url != null){
					final String normalizedUrl = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf(cls.getPackage().getName().replace(DOT, SLASH)));
					return new URL(normalizedUrl);
				}
			}
			catch(final MalformedURLException e){
				if(LOGGER != null)
					LOGGER.warn("Could not get URL", e);
			}
		}
		return null;
	}

	/**
	 * Returns a distinct collection of URLs based on a package name.
	 * <p>This searches for the package name as a resource, using {@link ClassLoader#getResources(String)}.
	 * For example, {@code forPackage(org.reflections)} effectively returns URLs from the
	 * classpath containing packages starting with {@code org.reflections}.</p>
	 * <p>The returned URLs retains the order of the given {@code classLoaders}.</p>
	 *
	 * @param name	The package name.
	 * @return	The collection of URLs.
	 */
	public static Collection<URL> forPackage(final String name){
		return forResource(resourceName(name));
	}

	/**
	 * Returns a distinct collection of URLs based on a resource.
	 * <p>
	 * This searches for the resource name, using {@link ClassLoader#getResources(String)}.
	 * For example, {@code forResource(test.properties)} effectively returns URLs from the
	 * classpath containing files of that name.
	 * <p>The returned URLs retains the order of the given {@code classLoaders}.</p>
	 *
	 * @param resourceName	The resource name.
	 * @return	The collection of URLs.
	 */
	private static Collection<URL> forResource(final String resourceName){
		final List<URL> result = new ArrayList<>();
		final ClassLoader[] loaders = classLoaders();
		for(final ClassLoader loader : loaders){
			try{
				final Enumeration<URL> urls = loader.getResources(resourceName);
				while(urls.hasMoreElements()){
					final URL url = urls.nextElement();
					final int index = url.toExternalForm().lastIndexOf(resourceName);
					if(index != -1)
						//add old url as contextUrl to support exotic url handlers
						result.add(new URL(url, url.toExternalForm().substring(0, index)));
					else
						result.add(url);
				}
			}
			catch(final IOException e){
				if(LOGGER != null)
					LOGGER.error("error getting resources for " + resourceName, e);
			}
		}
		return distinctUrls(result);
	}

	private static String resourceName(final String name){
		if(name != null){
			String resourceName = name.replace(DOT, SLASH);
			resourceName = resourceName.replace(BACKSLASH, SLASH);
			if(resourceName.startsWith(SLASH))
				resourceName = resourceName.substring(1);
			return resourceName;
		}
		return null;
	}

	/**
	 * Sometimes simple calls have unexpected side effects. I wanted to update some plugins, but the update manager was hanging my UI. Looking at the stack trace reveals:
	 * <pre><code>
	 * at java.net.Inet4AddressImpl.lookupAllHostAddr(Native Method)
	 * at java.net.InetAddress$1.lookupAllHostAddr(Unknown Source)
	 * at java.net.InetAddress.getAddressFromNameService(Unknown Source)
	 * at java.net.InetAddress.getAllByName0(Unknown Source)
	 * at java.net.InetAddress.getAllByName0(Unknown Source)
	 * at java.net.InetAddress.getAllByName(Unknown Source)
	 * at java.net.InetAddress.getByName(Unknown Source)
	 * at java.net.URLStreamHandler.getHostAddress(Unknown Source)
	 * - locked <0x15ce1280> (a sun.net.www.protocol.http.Handler)
	 * at java.net.URLStreamHandler.hashCode(Unknown Source)
	 * at java.net.URL.hashCode(Unknown Source)
	 * - locked <0x1a3100d0> (a java.net.URL)
	 *</code></pre>
	 * <p>Hmm, I must say that it is very dangerous that {@link URL#hashCode()} and {@link URL#equals(Object)} makes an Internet connection. {@link URL} has the worst
	 * equals/hasCode implementation I have ever seen: equality DEPENDS on the state of the Internet.</p>
	 * <p>Do not put {@link URL} into collections unless you can live with the fact that comparing makes calls to the Internet. Use {@link java.net.URI} instead.</p>
	 * <p>URL is an aggressive beast that can slow down and hang your application by making unexpected network traffic.</p>
	 *
	 * @see <a href="http://michaelscharf.blogspot.co.il/2006/11/javaneturlequals-and-hashcode-make.html">java.net.URL.equals and hashCode make (blocking) Internet connections</a>
	 */
	private static Collection<URL> distinctUrls(final Collection<URL> urls){
		final Map<String, URL> distinct = new HashMap<>(urls.size());
		for(final URL url : urls)
			distinct.put(url.toExternalForm(), url);
		return distinct.values();
	}


	public static List<String> getClassNames(final Collection<Class<?>> types){
		final List<String> list = new ArrayList<>(types.size());
		for(final Class<?> type : types)
			list.add(getClassName(type));
		return list;
	}

	private static String getClassName(Class<?> type){
		if(!type.isArray())
			return type.getName();
		else{
			int dim = 0;
			while(type.isArray()){
				dim ++;
				type = type.getComponentType();
			}
			return type.getName() + "[]".repeat(dim);
		}
	}

	/**
	 * Try to resolve all given string representation of types to a list of java types
	 *
	 * @param classNames	The class names.
	 * @return	The classes.
	 */
	public static Set<Class<?>> getClassesFromNames(final Collection<String> classNames){
		final Set<Class<?>> result = new HashSet<>(classNames.size());
		for(final String className : classNames){
			final Class<?> classFromName = getClassFromName(className);
			if(classFromName != null)
				result.add(classFromName);
		}
		return result;
	}

	public static Class<?> getClassFromName(final String typeName){
		int index = PRIMITIVE_NAMES.indexOf(typeName);
		if(index >= 0)
			return PRIMITIVE_TYPES.get(index);
		else{
			String type = typeName;
			index = typeName.indexOf("[");
			if(index >= 0){
				final String array = typeName.substring(index).replace("]", EMPTY_STRING);

				type = typeName.substring(0, index);
				index = PRIMITIVE_NAMES.indexOf(type);
				type = array + (index >= 0? PRIMITIVE_DESCRIPTORS.get(index): "L" + type + ";");
			}


			final List<ReflectionsException> reflectionsExceptions = new ArrayList<>();
			for(final ClassLoader classLoader : ClasspathHelper.classLoaders()){
				if(type.contains("[")){
					try{
						return Class.forName(type, false, classLoader);
					}
					catch(final Throwable e){
						reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
					}
				}
				try{
					return classLoader.loadClass(type);
				}
				catch(final Throwable e){
					reflectionsExceptions.add(new ReflectionsException("could not get type for name " + typeName, e));
				}
			}

			if(LOGGER != null)
				for(final ReflectionsException reflectionsException : reflectionsExceptions)
					LOGGER.warn("could not get type for name " + typeName + " from any class loader", reflectionsException);

			return null;
		}
	}

}

