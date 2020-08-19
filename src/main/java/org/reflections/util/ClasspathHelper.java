package org.reflections.util;

import org.reflections.Reflections;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//import javax.servlet.ServletContext;


/**
 * Helper methods for working with the classpath.
 */
public abstract class ClasspathHelper{

	/**
	 * Gets the current thread context class loader.
	 * {@code Thread.currentThread().getContextClassLoader()}.
	 *
	 * @return the context class loader, may be null
	 */
	public static ClassLoader contextClassLoader(){
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Gets the class loader of this library.
	 * {@code Reflections.class.getClassLoader()}.
	 *
	 * @return the static library class loader, may be null
	 */
	public static ClassLoader staticClassLoader(){
		return Reflections.class.getClassLoader();
	}

	/**
	 * Returns an array of class Loaders initialized from the specified array.
	 * <p>
	 * If the input is null or empty, it defaults to both {@link #contextClassLoader()} and {@link #staticClassLoader()}
	 *
	 * @return the array of class loaders, not null
	 */
	public static ClassLoader[] classLoaders(ClassLoader... classLoaders){
		if(classLoaders != null && classLoaders.length != 0){
			return classLoaders;
		}
		else{
			ClassLoader contextClassLoader = contextClassLoader(), staticClassLoader = staticClassLoader();
			return contextClassLoader != null? staticClassLoader != null && contextClassLoader != staticClassLoader? new ClassLoader[]{contextClassLoader, staticClassLoader}: new ClassLoader[]{contextClassLoader}: new ClassLoader[]{};

		}
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
	 * The returned URLs retainsthe order of the given {@code classLoaders}.
	 *
	 * @return the collection of URLs, not null
	 */
	public static Collection<URL> forPackage(String name, ClassLoader... classLoaders){
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
	 * @return the collection of URLs, not null
	 */
	public static Collection<URL> forResource(String resourceName, ClassLoader... classLoaders){
		final List<URL> result = new ArrayList<>();
		final ClassLoader[] loaders = classLoaders(classLoaders);
		for(ClassLoader classLoader : loaders){
			try{
				final Enumeration<URL> urls = classLoader.getResources(resourceName);
				while(urls.hasMoreElements()){
					final URL url = urls.nextElement();
					int index = url.toExternalForm().lastIndexOf(resourceName);
					if(index != -1){
						// Add old url as contextUrl to support exotic url handlers
						result.add(new URL(url, url.toExternalForm().substring(0, index)));
					}
					else{
						result.add(url);
					}
				}
			}catch(IOException e){
				if(Reflections.log != null){
					Reflections.log.error("error getting resources for " + resourceName, e);
				}
			}
		}
		return distinctUrls(result);
	}

	/**
	 * Returns the URL that contains a {@code Class}.
	 * <p>
	 * This searches for the class using {@link ClassLoader#getResource(String)}.
	 * <p>
	 * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
	 * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
	 *
	 * @return the URL containing the class, null if not found
	 */
	public static URL forClass(Class<?> aClass, ClassLoader... classLoaders){
		final ClassLoader[] loaders = classLoaders(classLoaders);
		final String resourceName = aClass.getName().replace(".", "/") + ".class";
		for(ClassLoader classLoader : loaders){
			try{
				final URL url = classLoader.getResource(resourceName);
				if(url != null){
					final String normalizedUrl = url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf(aClass.getPackage().getName().replace(".", "/")));
					return new URL(normalizedUrl);
				}
			}catch(MalformedURLException e){
				if(Reflections.log != null){
					Reflections.log.warn("Could not get URL", e);
				}
			}
		}
		return null;
	}

	/**
	 * Returns a distinct collection of URLs based on URLs derived from class loaders.
	 * <p>
	 * This finds the URLs using {@link URLClassLoader#getURLs()} using both
	 * {@link #contextClassLoader()} and {@link #staticClassLoader()}.
	 * <p>
	 * The returned URLs retains the order of the given {@code classLoaders}.
	 *
	 * @return the collection of URLs, not null
	 */
	public static Collection<URL> forClassLoader(){
		return forClassLoader(classLoaders());
	}

	/**
	 * Returns a distinct collection of URLs based on URLs derived from class loaders.
	 * <p>
	 * This finds the URLs using {@link URLClassLoader#getURLs()} using the specified
	 * class loader, searching up the parent hierarchy.
	 * <p>
	 * If the optional {@link ClassLoader}s are not specified, then both {@link #contextClassLoader()}
	 * and {@link #staticClassLoader()} are used for {@link ClassLoader#getResources(String)}.
	 * <p>
	 * The returned URLs retains the order of the given {@code classLoaders}.
	 *
	 * @return the collection of URLs, not null
	 */
	public static Collection<URL> forClassLoader(ClassLoader... classLoaders){
		final Collection<URL> result = new ArrayList<>();
		final ClassLoader[] loaders = classLoaders(classLoaders);
		for(ClassLoader classLoader : loaders){
			while(classLoader != null){
				if(classLoader instanceof URLClassLoader){
					URL[] urls = ((URLClassLoader) classLoader).getURLs();
					if(urls != null){
						result.addAll(Arrays.asList(urls));
					}
				}
				classLoader = classLoader.getParent();
			}
		}
		return distinctUrls(result);
	}

	/**
	 * Returns a distinct collection of URLs based on the {@code WEB-INF/lib} folder.
	 * <p>
	 * This finds the URLs using the {@link ServletContext}.
	 * <p>
	 * The returned URLs retains the order of the given {@code classLoaders}.
	 *
	 * @return the collection of URLs, not null
	 */
	//    public static Collection<URL> forWebInfLib(final ServletContext servletContext) {
	//        final Collection<URL> urls = new ArrayList<>();
	//        Set<?> resourcePaths = servletContext.getResourcePaths("/WEB-INF/lib");
	//        if (resourcePaths == null) {
	//            return urls;
	//        }
	//        for (Object urlString : resourcePaths) {
	//            try {
	//                urls.add(servletContext.getResource((String) urlString));
	//            } catch (MalformedURLException e) { /*fuck off*/ }
	//        }
	//        return distinctUrls(urls);
	//    }

	/**
	 * Returns the URL of the {@code WEB-INF/classes} folder.
	 * <p>
	 * This finds the URLs using the {@ link ServletContext}.
     * 
     * @return the collection of URLs, not null
	 */
	//    public static URL forWebInfClasses(final ServletContext servletContext) {
	//        try {
	//            final String path = servletContext.getRealPath("/WEB-INF/classes");
	//            if (path != null) {
	//                final File file = new File(path);
	//                if (file.exists())
	//                    return file.toURL();
	//            } else {
	//                return servletContext.getResource("/WEB-INF/classes");
	//            }
	//        } catch (MalformedURLException e) { /*fuck off*/ }
	//        return null;
	//    }

	private static String resourceName(String name){
		if(name != null){
			String resourceName = name.replace(".", "/");
			resourceName = resourceName.replace("\\", "/");
			if(resourceName.startsWith("/")){
				resourceName = resourceName.substring(1);
			}
			return resourceName;
		}
		return null;
	}

	//http://michaelscharf.blogspot.co.il/2006/11/javaneturlequals-and-hashcode-make.html
	private static Collection<URL> distinctUrls(Collection<URL> urls){
		Map<String, URL> distinct = new LinkedHashMap<>(urls.size());
		for(URL url : urls){
			distinct.put(url.toExternalForm(), url);
		}
		return distinct.values();
	}

}

