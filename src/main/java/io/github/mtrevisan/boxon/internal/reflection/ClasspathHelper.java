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

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Helper methods for working with the classpath.
 */
public final class ClasspathHelper{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(ClasspathHelper.class);

	private static final String SLASH = "/";
	private static final String BACKSLASH = "\\";
	private static final String DOT = ".";
	private static final String DOT_CLASS = ".class";


	private ClasspathHelper(){}

	/**
	 * Gets the current thread context class loader.
	 * {@code Thread.currentThread().getContextClassLoader()}.
	 *
	 * @return	The context class loader, may be null
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
	public static ClassLoader staticClassLoader(){
		return ClasspathHelper.class.getClassLoader();
	}

	/**
	 * Returns an array of class Loaders initialized from the specified array.
	 * <p>
	 * If the input is null or empty, it defaults to both {@link #contextClassLoader()} and {@link #staticClassLoader()}
	 *
	 * @param classLoaders	The class loaders.
	 * @return	The array of class loaders, not {@code null}.
	 */
	public static ClassLoader[] classLoaders(final ClassLoader... classLoaders){
		if(classLoaders != null && classLoaders.length != 0)
			return classLoaders;

		final ClassLoader contextClassLoader = contextClassLoader();
		final ClassLoader staticClassLoader = staticClassLoader();
		return (contextClassLoader != null?
			(staticClassLoader != null && contextClassLoader != staticClassLoader? new ClassLoader[]{contextClassLoader, staticClassLoader}: new ClassLoader[]{contextClassLoader}):
			new ClassLoader[]{});
	}


	/**
	 * Returns the URL that contains a {@code Class}.
	 * <p>
	 * This searches for the class using {@link ClassLoader#getResource(String)}.
	 * <p>
	 * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
	 * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
	 *
	 * @param cls	The class.
	 * @param classLoaders	The class loaders.
	 * @return	The URL containing the class, {@code null} if not found.
	 */
	public static URL forClass(final Class<?> cls, final ClassLoader... classLoaders){
		final ClassLoader[] loaders = classLoaders(classLoaders);
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
	 * <p>
	 * This searches for the package name as a resource, using {@link ClassLoader#getResources(String)}.
	 * For example, {@code forPackage(org.reflections)} effectively returns URLs from the
	 * classpath containing packages starting with {@code org.reflections}.
	 * <p>
	 * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
	 * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
	 * <p>
	 * The returned URLs retains the order of the given {@code classLoaders}.
	 *
	 * @param name	The package name.
	 * @param classLoaders	The class loaders.
	 * @return	The collection of URLs, not {@code null}.
	 */
	public static Collection<URL> forPackage(final String name, final ClassLoader... classLoaders){
		return forResource(resourceName(name), classLoaders);
	}

	/**
	 * Returns a distinct collection of URLs based on a resource.
	 * <p>
	 * This searches for the resource name, using {@link ClassLoader#getResources(String)}.
	 * For example, {@code forResource(test.properties)} effectively returns URLs from the
	 * classpath containing files of that name.
	 * <p>
	 * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
	 * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
	 * <p>
	 * The returned URLs retains the order of the given {@code classLoaders}.
	 *
	 * @param resourceName	The resource name.
	 * @param classLoaders	The class loaders.
	 * @return	The collection of URLs, not {@code null}.
	 */
	private static Collection<URL> forResource(final String resourceName, final ClassLoader... classLoaders){
		final List<URL> result = new ArrayList<>();
		final ClassLoader[] loaders = classLoaders(classLoaders);
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

	//http://michaelscharf.blogspot.co.il/2006/11/javaneturlequals-and-hashcode-make.html
	private static Collection<URL> distinctUrls(final Collection<URL> urls){
		final Map<String, URL> distinct = new HashMap<>(urls.size());
		for(final URL url : urls)
			distinct.put(url.toExternalForm(), url);
		return distinct.values();
	}

}

