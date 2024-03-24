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


public class Key implements Poolable{

	private final KeyPool pool;
//	@Synthetic
	int size;
	private Class<?> arrayClass;


	Key(final KeyPool pool){
		this.pool = pool;
	}

	void init(final int size, final Class<?> arrayClass){
		this.size = size;
		this.arrayClass = arrayClass;
	}

	@Override
	public boolean equals(final Object o){
		if(o instanceof final Key other)
			return (size == other.size && arrayClass == other.arrayClass);
		return false;
	}

	@Override
	public String toString(){
		return "Key{" + "size=" + size + "array=" + arrayClass + '}';
	}

	@Override
	public void offer(){
		pool.offer(this);
	}

	@Override
	public int hashCode(){
		int result = size;
		result = 31 * result + (arrayClass != null? arrayClass.hashCode(): 0);
		return result;
	}

}
