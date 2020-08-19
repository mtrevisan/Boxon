package org.reflections;

import org.reflections.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * stores metadata information in multimaps
 * <p>use the different query methods (getXXX) to query the metadata
 * <p>the query methods are string based, and does not cause the class loader to define the types
 * <p>use {@link Reflections#getStore()} to access this store
 */
public class Store{

	private final Map<String, Map<String, Collection<String>>> map = new ConcurrentHashMap<>();


	Store(){}

	/**
	 * Get the values stored for the given {@code scannerClass} and {@code key}.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param key	The key.
	 */
	public Set<String> get(final Class<?> scannerClass, final String key){
		return get(scannerClass, Collections.singletonList(key));
	}

	/**
	 * Get the values stored for the given {@code scannerName} and {@code keys}.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param keys	The keys.
	 */
	public Set<String> get(final Class<?> scannerClass, final Collection<String> keys){
		final String scannerName = Utils.index(scannerClass);
		final Map<String, Collection<String>> scannerMap = get(scannerName);
		final Set<String> result = new HashSet<>();
		for(final String key : keys){
			final Collection<String> values = scannerMap.get(key);
			if(values != null)
				result.addAll(values);
		}
		return result;
	}

	/**
	 * Recursively get the values stored for the given {@code index} and {@code keys}, not including keys
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param key	The key.
	 */
	public Set<String> getAll(final Class<?> scannerClass, final String key){
		return getAllIncludingKeys(scannerClass, get(scannerClass, key));
	}

	/**
	 * Recursively get the values stored for the given {@code scannerName} and {@code keys}, including keys.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param keys	The keys.
	 */
	public Set<String> getAllIncludingKeys(final Class<?> scannerClass, final Collection<String> keys){
		final String scannerName = Utils.index(scannerClass);
		final Map<String, Collection<String>> scannerMap = get(scannerName);
		final List<String> workKeys = new ArrayList<>(keys);

		final Set<String> result = new HashSet<>();
		for(int i = 0; i < workKeys.size(); i++){
			final String key = workKeys.get(i);
			if(result.add(key)){
				final Collection<String> values = scannerMap.get(key);
				if(values != null)
					workKeys.addAll(values);
			}
		}
		return result;
	}

	/**
	 * get the MAP for the given {@code scannerName}, otherwise throws a {@link ReflectionsException}
	 *
	 * @param scannerName	The name of the scanner.
	 */
	private Map<String, Collection<String>> get(final String scannerName){
		final Map<String, Collection<String>> scannerMap = map.get(scannerName);
		if(scannerMap == null)
			throw new ReflectionsException("Scanner " + scannerName + " was not configured");

		return scannerMap;
	}

	public Set<String> keys(final String index){
		final Map<String, Collection<String>> scannerMap = map.get(index);
		return (scannerMap != null? new HashSet<>(scannerMap.keySet()): Collections.emptySet());
	}

	public Set<String> values(final String index){
		final Map<String, Collection<String>> scannerMap = map.get(index);
		return (scannerMap != null? scannerMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()): Collections.emptySet());
	}

	public boolean put(final Class<?> scannerClass, final String key, final String value){
		return put(Utils.index(scannerClass), key, value);
	}

	public boolean put(final String index, final String key, final String value){
		return map.computeIfAbsent(index, s -> new ConcurrentHashMap<>())
			.computeIfAbsent(key, s -> new ArrayList<>())
			.add(value);
	}

}
