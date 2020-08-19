package io.github.mtrevisan.boxon.internal.reflection;

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
 */
public class ClassStore{

	private final Map<Class<?>, Map<String, Collection<String>>> map = new ConcurrentHashMap<>();


	ClassStore(){}

	/**
	 * Get the values stored for the given {@code scannerClass} and {@code key}.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param key	The key.
	 * @return	The classes.
	 */
	public Set<String> get(final Class<?> scannerClass, final String key){
		return get(scannerClass, Collections.singletonList(key));
	}

	/**
	 * Get the values stored for the given {@code scannerName} and {@code keys}.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param keys	The keys.
	 * @return	The classes.
	 */
	public Set<String> get(final Class<?> scannerClass, final Collection<String> keys){
		final Map<String, Collection<String>> scannerMap = get(scannerClass);
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
	 * @return	The classes.
	 */
	public Set<String> getAll(final Class<?> scannerClass, final String key){
		return getAllIncludingKeys(scannerClass, get(scannerClass, key));
	}

	/**
	 * Recursively get the values stored for the given {@code scannerName} and {@code keys}, including keys.
	 *
	 * @param scannerClass	The class of the scanner.
	 * @param keys	The keys.
	 * @return	The classes, including the keys.
	 */
	public Set<String> getAllIncludingKeys(final Class<?> scannerClass, final Collection<String> keys){
		final Map<String, Collection<String>> scannerMap = get(scannerClass);
		final List<String> workKeys = new ArrayList<>(keys);

		final Set<String> result = new HashSet<>();
		for(int i = 0; i < workKeys.size(); i ++){
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
	 * @param scannerClass	The class of the scanner.
	 */
	private Map<String, Collection<String>> get(final Class<?> scannerClass){
		final Map<String, Collection<String>> scannerMap = map.get(scannerClass);
		if(scannerMap == null)
			throw new ReflectionsException("Scanner " + scannerClass.getSimpleName() + " was not configured");

		return scannerMap;
	}

	public Set<String> keys(final Class<?> scannerClass){
		final Map<String, Collection<String>> scannerMap = map.get(scannerClass);
		return (scannerMap != null? new HashSet<>(scannerMap.keySet()): Collections.emptySet());
	}

	public Set<String> values(final Class<?> scannerClass){
		final Map<String, Collection<String>> scannerMap = map.get(scannerClass);
		return (scannerMap != null? scannerMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()): Collections.emptySet());
	}

	public boolean put(final Class<?> scannerClass, final String key, final String value){
		return map.computeIfAbsent(scannerClass, s -> new ConcurrentHashMap<>())
			.computeIfAbsent(key, s -> new ArrayList<>())
			.add(value);
	}

}
