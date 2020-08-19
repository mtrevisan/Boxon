package org.reflections.util;

import org.reflections.Reflections;

import java.io.IOException;
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
public abstract class ClasspathHelper{

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
		return Reflections.class.getClassLoader();
	}

	/**
	 * Returns an array of class Loaders initialized from the specified array.
	 * <p>
	 * If the input is null or empty, it defaults to both {@link #contextClassLoader()} and {@link #staticClassLoader()}
	 *
	 * @param classLoaders	The class loaders.
	 * @return	The array of class loaders, not null.
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
	 * @return	The collection of URLs, not null.
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
	 * @return	The collection of URLs, not null.
	 */
	private static Collection<URL> forResource(final String resourceName, final ClassLoader... classLoaders){
		final List<URL> result = new ArrayList<>();
		final ClassLoader[] loaders = classLoaders(classLoaders);
		for(final ClassLoader classLoader : loaders){
			try{
				final Enumeration<URL> urls = classLoader.getResources(resourceName);
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
				if(Reflections.log != null)
					Reflections.log.error("error getting resources for " + resourceName, e);
			}
		}
		return distinctUrls(result);
	}

	private static String resourceName(final String name){
		if(name != null){
			String resourceName = name.replace(".", "/");
			resourceName = resourceName.replace("\\", "/");
			if(resourceName.startsWith("/"))
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

