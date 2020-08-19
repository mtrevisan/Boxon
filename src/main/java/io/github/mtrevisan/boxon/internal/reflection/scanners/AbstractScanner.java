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

import io.github.mtrevisan.boxon.internal.reflection.ClassStore;
import io.github.mtrevisan.boxon.internal.reflection.ReflectionsException;
import io.github.mtrevisan.boxon.internal.reflection.adapters.MetadataAdapter;
import io.github.mtrevisan.boxon.internal.reflection.vfs.VirtualFileSystem;


public abstract class AbstractScanner implements Scanner{

	@SuppressWarnings("rawtypes")
	protected MetadataAdapter metadataAdapter;


	@SuppressWarnings("rawtypes")
	public void setMetadataAdapter(final MetadataAdapter metadataAdapter){
		this.metadataAdapter = metadataAdapter;
	}

	public boolean acceptsInput(final String file){
		return metadataAdapter.acceptsInput(file);
	}

	public Object scan(final VirtualFileSystem.File file, Object classObject, final ClassStore classStore){
		if(classObject == null){
			try{
				classObject = metadataAdapter.getOrCreateClassObject(file);
			}
			catch(final Exception e){
				throw new ReflectionsException("Could not create class object from file " + file.getRelativePath(), e);
			}
		}

		scan(classObject, classStore);

		return classObject;
	}

	protected abstract void scan(final Object cls, final ClassStore classStore);

	protected void put(final ClassStore classStore, final String key, final String value){
		classStore.put(getClass(), key, value);
	}

}
