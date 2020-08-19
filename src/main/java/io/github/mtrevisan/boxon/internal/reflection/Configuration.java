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

import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapter;

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
	MetadataAdapter<?> getMetadataAdapter();

	/**
	 * Whether to expand super types after scanning, for super types that were not scanned.
	 * <p>see {@link Reflections#expandSuperTypes()}.</p>
	 *
	 * @return	Whether to expand super types after scanning.
	 */
	boolean shouldExpandSuperTypes();

}
