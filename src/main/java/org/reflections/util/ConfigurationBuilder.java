package org.reflections.util;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.adapters.JavaReflectionAdapter;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.adapters.MetadataAdapter;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * a fluent builder for {@link Configuration}, to be used for constructing a {@link Reflections} instance
 * <p>usage:
 * <pre>
 *      new Reflections(
 *          new ConfigurationBuilder()
 *              .filterInputsBy(new FilterBuilder().include("your project's common package prefix here..."))
 *              .setUrls(ClasspathHelper.forClassLoader())
 *              .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner().filterResultsBy(myClassAnnotationsFilter)));
 * </pre>
 */
public class ConfigurationBuilder implements Configuration{

	private Set<URL> urls;
	/*lazy*/ protected MetadataAdapter metadataAdapter;
	private ClassLoader[] classLoaders;
	private boolean expandSuperTypes = true;


	public ConfigurationBuilder(){
		urls = new HashSet<>();
	}

	public ConfigurationBuilder forPackages(String... packages){
		for(String pkg : packages){
			addUrls(ClasspathHelper.forPackage(pkg));
		}
		return this;
	}

	public Set<URL> getUrls(){
		return urls;
	}

	/**
	 * add urls to be scanned
	 * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
	 */
	public ConfigurationBuilder addUrls(final Collection<URL> urls){
		this.urls.addAll(urls);
		return this;
	}

	/**
	 * add urls to be scanned
	 * <p>use {@link ClasspathHelper} convenient methods to get the relevant urls
	 */
	public ConfigurationBuilder addUrls(final URL... urls){
		this.urls.addAll(new HashSet<>(Arrays.asList(urls)));
		return this;
	}

	/**
	 * returns the metadata adapter.
	 * if javassist library exists in the classpath, this method returns {@link JavassistAdapter} otherwise defaults to {@link JavaReflectionAdapter}.
	 * <p>the {@link JavassistAdapter} is preferred in terms of performance and class loading.
	 */
	public MetadataAdapter getMetadataAdapter(){
		if(metadataAdapter != null)
			return metadataAdapter;
		else{
			try{
				return (metadataAdapter = new JavassistAdapter());
			}catch(Throwable e){
				if(Reflections.log != null)
					Reflections.log.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);
				return (metadataAdapter = new JavaReflectionAdapter());
			}
		}
	}

	/**
	 * get class loader, might be used for scanning or resolving methods/fields
	 */
	public ClassLoader[] getClassLoaders(){
		return classLoaders;
	}

	@Override
	public boolean shouldExpandSuperTypes(){
		return expandSuperTypes;
	}

	/**
	 * if set to true, Reflections will expand super types after scanning.
	 * <p>see {@link Reflections#expandSuperTypes()}
	 */
	public ConfigurationBuilder setExpandSuperTypes(boolean expandSuperTypes){
		this.expandSuperTypes = expandSuperTypes;
		return this;
	}

	/**
	 * set class loader, might be used for resolving methods/fields
	 */
	public void setClassLoaders(ClassLoader[] classLoaders){
		this.classLoaders = classLoaders;
	}

}
