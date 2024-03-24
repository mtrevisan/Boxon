/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.helpers.pools;


/**
 * Interface for an array pool that pools arrays of different types.
 */
public interface ArrayPool{

	/**
	 * Level for {@link #trimMemory(int)}: the process is nearing the end of the background LRU list, and if more memory isn't found soon
	 * it will be killed.
	 */
	int TRIM_MEMORY_COMPLETE = 80;
	/**
	 * Level for {@link #trimMemory(int)}: the process is around the middle of the background LRU list; freeing memory can help the system
	 * keep other processes running later in the list for better overall performance.
	 */
	int TRIM_MEMORY_MODERATE = 60;
	/**
	 * Level for {@link #trimMemory(int)}: the process has gone on to the LRU list. This is a good opportunity to clean up resources that
	 * can efficiently and quickly be re-built if the user returns to the app.
	 */
	int TRIM_MEMORY_BACKGROUND = 40;
	/**
	 * Level for {@link #trimMemory(int)}: the process had been showing a user interface, and is no longer doing so. Large allocations
	 * with the UI should be released at this point to allow memory to be better managed.
	 */
	int TRIM_MEMORY_UI_HIDDEN = 20;
	/**
	 * Level for {@link #trimMemory(int)}: the process is not an expendable background process, but the device is running extremely low
	 * on memory and is about to not be able to keep any background processes running.
	 * Your running process should free up as many non-critical resources as it can to allow that memory to be used elsewhere.
	 */
	int TRIM_MEMORY_RUNNING_CRITICAL = 15;
	/**
	 * Level for {@link #trimMemory(int)}: the process is not an expendable background process, but the device is running low on memory.
	 * Your running process should free up unneeded resources to allow that memory to be used elsewhere.
	 */
	int TRIM_MEMORY_RUNNING_LOW = 10;
	/**
	 * Level for {@link #trimMemory(int)}: the process is not an expendable background process, but the device is running moderately low
	 * on memory.
	 * Your running process may want to release some unneeded resources for use elsewhere.
	 */
	int TRIM_MEMORY_RUNNING_MODERATE = 5;


	/**
	 * Optionally adds the given array of the given type to the pool.
	 *
	 * <p>Arrays may be ignored, for example if the array is larger than the maximum size of the pool.
	 */
	<T> void put(T array);

	/**
	 * Returns a non-null array of the given type with a length {@code >=} to the given size.
	 *
	 * <p>If an array of the given size isn't in the pool, a new one will be allocated.
	 * <p>This class makes no guarantees about the contents of the returned array.
	 *
	 * @see #getExact(int, Class)
	 */
	<T> T get(int size, Class<T> arrayClass);

	/**
	 * Returns a non-null array of the given type with a length exactly equal to the given size.
	 *
	 * <p>If an array of the given size isn't in the pool, a new one will be allocated.
	 * <p>This class makes no guarantees about the contents of the returned array.
	 *
	 * @see #get(int, Class)
	 */
	<T> T getExact(int size, Class<T> arrayClass);

	/** Clears all arrays from the pool. */
	void clearMemory();

	/**
	 * Trims the size to the appropriate level.
	 *
	 * @param level	A trim.
	 */
	void trimMemory(int level);

}
