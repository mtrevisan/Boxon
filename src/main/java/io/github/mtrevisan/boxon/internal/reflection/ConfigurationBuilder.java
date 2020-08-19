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

import io.github.mtrevisan.boxon.internal.reflection.adapters.JavaReflectionAdapter;
import io.github.mtrevisan.boxon.internal.reflection.adapters.JavassistAdapter;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapter;
import io.github.mtrevisan.boxon.internal.reflection.util.ClasspathHelper;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * a fluent builder for {@link Configuration}, to be used for constructing a {@link Reflections} instance
 * <p>usage:
 * <pre><code>
 *      new Reflections(
 *          new ConfigurationBuilder()
 *              .filterInputsBy(new FilterBuilder().include("your project's common package prefix here..."))
 *              .withUrls(ClasspathHelper.forClassLoader()));
 * </code></pre>
 */
public class ConfigurationBuilder implements Configuration{

	private final Set<URL> urls;
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
				if(Reflections.LOGGER != null)
					Reflections.LOGGER.warn("could not create JavassistAdapter, using JavaReflectionAdapter", e);

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
