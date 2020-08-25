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
package io.github.mtrevisan.boxon.internal.reflection.scanners;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapterBuilder;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapterInterface;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSDirectory;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSException;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VFSFile;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public abstract class AbstractScanner implements ScannerInterface{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(AbstractScanner.class);

	@SuppressWarnings("rawtypes")
	protected static final MetadataAdapterInterface METADATA_ADAPTER = MetadataAdapterBuilder.getMetadataAdapter();

	private final Map<String, Collection<String>> metadataStore = new ConcurrentHashMap<>(0);


	public static Object createClassObject(final VFSDirectory root, final VFSFile file){
		try{
			return METADATA_ADAPTER.createClassObject(root, file);
		}
		catch(final Exception e){
			throw new VFSException("Could not create class object from file {}", file.getRelativePath())
				.withCause(e);
		}
	}


	/**
	 * Get the values stored for the given {@code scannerClass} and {@code key}.
	 *
	 * @param key	The key.
	 * @return	The classes.
	 */
	public Set<String> get(final String key){
		return get(Collections.singletonList(key));
	}

	/**
	 * Get the values stored for this scanner and {@code keys}.
	 *
	 * @param keys	The keys.
	 * @return	The classes.
	 */
	public Set<String> get(final Collection<String> keys){
		final Set<String> result = new HashSet<>();
		for(final String key : keys){
			final Collection<String> values = metadataStore.get(key);
			if(values != null)
				result.addAll(values);
		}
		return result;
	}

	/**
	 * Recursively get the values stored for the given {@code index} and {@code keys}, not including keys
	 *
	 * @param key	The key.
	 * @return	The classes.
	 */
	public Set<String> getAll(final String key){
		return getAllIncludingKeys(get(key));
	}

	/**
	 * Recursively get the values stored for this scanner and {@code keys}, including keys.
	 *
	 * @param keys	The keys.
	 * @return	The classes, including the keys.
	 */
	public Set<String> getAllIncludingKeys(final Collection<String> keys){
		final List<String> workKeys = new ArrayList<>(keys);

		final Set<String> result = new HashSet<>(keys.size());
		for(int i = 0; i < workKeys.size(); i ++){
			final String key = workKeys.get(i);
			if(result.add(key))
				workKeys.addAll(metadataStore.getOrDefault(key, Collections.emptyList()));
		}
		return result;
	}

	public Set<String> keys(){
		return new HashSet<>(metadataStore.keySet());
	}

	public Set<String> values(){
		final Set<String> result = new HashSet<>();
		for(final Collection<String> value : metadataStore.values())
			result.addAll(value);
		return result;
	}

	public final boolean put(final String key, final String value){
		if(LOGGER != null)
			LOGGER.trace("Add {} > {} to metadata store", key, value);

		return metadataStore.computeIfAbsent(key, s -> new ArrayList<>(1))
			.add(value);
	}

}
