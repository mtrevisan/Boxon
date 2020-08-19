package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.vfs.Vfs;


public abstract class AbstractScanner implements Scanner{

	private Configuration configuration;


	public void setConfiguration(final Configuration configuration){
		this.configuration = configuration;
	}

	public boolean acceptsInput(final String file){
		return getMetadataAdapter().acceptsInput(file);
	}

	public Object scan(final Vfs.File file, Object classObject, final Store store){
		if(classObject == null){
			try{
				classObject = configuration.getMetadataAdapter()
					.getOrCreateClassObject(file);
			}
			catch(final Exception e){
				throw new ReflectionsException("Could not create class object from file " + file.getRelativePath(), e);
			}
		}

		scan(classObject, store);

		return classObject;
	}

	public abstract void scan(final Object cls, final Store store);

	protected void put(final Store store, final String key, final String value){
		store.put(getClass(), key, value);
	}

	@SuppressWarnings("rawtypes")
	protected MetadataAdapter getMetadataAdapter(){
		return configuration.getMetadataAdapter();
	}

}
