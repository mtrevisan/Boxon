package org.reflections.scanners;

import org.reflections.ClassStore;
import org.reflections.ReflectionsException;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.vfs.Vfs;


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

	public Object scan(final Vfs.File file, Object classObject, final ClassStore classStore){
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
