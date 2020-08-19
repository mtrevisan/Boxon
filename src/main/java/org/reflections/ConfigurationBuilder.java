package org.reflections;

import org.reflections.adapters.JavaReflectionAdapter;
import org.reflections.adapters.JavassistAdapter;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.util.ClasspathHelper;

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
 *              .withUrls(ClasspathHelper.forClassLoader()));
 * </pre>
 */
public class ConfigurationBuilder implements Configuration{

	private final Set<URL> urls;
	//lazy
	protected MetadataAdapter<?> metadataAdapter;
	private boolean expandSuperTypes = true;


	public ConfigurationBuilder(){
		urls = new HashSet<>();
	}

	public ConfigurationBuilder withPackages(final String... packages){
		for(final String pkg : packages)
			withUrls(ClasspathHelper.forPackage(pkg));
		return this;
	}

	public Set<URL> getUrls(){
		return urls;
	}

	/**
	 * Add urls to be scanned.
	 * <p>use {@link ClasspathHelper} convenient methods to get the relevant URLs</p>
	 *
	 * @param urls	The URLs.
	 * @return	The builder, for easy concatenation.
	 */
	public ConfigurationBuilder withUrls(final Collection<URL> urls){
		this.urls.addAll(urls);
		return this;
	}

	/**
	 * Add urls to be scanned.
	 * <p>use {@link ClasspathHelper} convenient methods to get the relevant URLs.</p>
	 *
	 * @param urls	The URLs.
	 * @return	The builder, for easy concatenation.
	 */
	public ConfigurationBuilder withUrls(final URL... urls){
		this.urls.addAll(new HashSet<>(Arrays.asList(urls)));
		return this;
	}

	/**
	 * returns the metadata adapter.
	 * if javassist library exists in the classpath, this method returns {@link JavassistAdapter} otherwise defaults to {@link JavaReflectionAdapter}.
	 * <p>the {@link JavassistAdapter} is preferred in terms of performance and class loading.
	 *
	 * @return	The metadata adapter.
	 */
	public MetadataAdapter<?> getMetadataAdapter(){
		if(metadataAdapter != null)
			return metadataAdapter;
		else{
			try{
				return (metadataAdapter = new JavassistAdapter());
			}
			catch(final Throwable e){
				if(Reflections.log != null)
					Reflections.log.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);

				return (metadataAdapter = new JavaReflectionAdapter());
			}
		}
	}

	@Override
	public boolean shouldExpandSuperTypes(){
		return expandSuperTypes;
	}

	/**
	 * if set to true, Reflections will expand super types after scanning.
	 * <p>see {@link Reflections#expandSuperTypes()}.</p>
	 *
	 * @param expandSuperTypes	Whether to expand super types.
	 * @return	The builder, for easy concatenation.
	 */
	public ConfigurationBuilder withExpandSuperTypes(final boolean expandSuperTypes){
		this.expandSuperTypes = expandSuperTypes;
		return this;
	}

}
