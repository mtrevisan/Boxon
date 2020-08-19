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
	 * The URLs to be scanned.
	 *
	 * @return	The URLs to be scanned.
	 */
	Set<URL> getUrls();

	/**
	 * The metadata adapter used to fetch metadata from classes.
	 *
	 * @return	The metadata adapter used to fetch metadata from classes.
	 */
	@SuppressWarnings({"RawUseOfParameterizedType"})
	MetadataAdapter<?> getMetadataAdapter();

	/**
	 * Whether to expand super types after scanning, for super types that were not scanned.
	 * <p>see {@link Reflections#expandSuperTypes()}.</p>
	 *
	 * @return	Whether to expand super types after scanning.
	 */
	boolean shouldExpandSuperTypes();

}
