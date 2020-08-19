package org.reflections;

import org.reflections.adapters.MetadataAdapter;
import org.reflections.util.ConfigurationBuilder;

import java.net.URL;
import java.util.Set;


/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link ConfigurationBuilder}
 */
public interface Configuration{

	/**
	 * the urls to be scanned
	 */
	Set<URL> getUrls();

	/**
	 * the metadata adapter used to fetch metadata from classes
	 */
	@SuppressWarnings({"RawUseOfParameterizedType"})
	MetadataAdapter getMetadataAdapter();

	/**
	 * get class loaders, might be used for resolving methods/fields
	 */
	ClassLoader[] getClassLoaders();

	/**
	 * if true (default), expand super types after scanning, for super types that were not scanned.
	 * <p>see {@link Reflections#expandSuperTypes()}
	 */
	boolean shouldExpandSuperTypes();

}
