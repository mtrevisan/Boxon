package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;


@SuppressWarnings({"RawUseOfParameterizedType"})
public abstract class AbstractScanner implements Scanner{

	private Configuration configuration;


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
				throw new ReflectionsException("could not create class object from file " + file.getRelativePath(), e);
			}
		}

		scan(classObject, store);
		return classObject;
	}

	public abstract void scan(final Object cls, final Store store);

	protected void put(final Store store, final String key, final String value){
		store.put(Utils.index(getClass()), key, value);
	}

	public void setConfiguration(final Configuration configuration){
		this.configuration = configuration;
	}

	@SuppressWarnings("rawtypes")
	protected MetadataAdapter getMetadataAdapter(){
		return configuration.getMetadataAdapter();
	}

	@Override
	public boolean equals(final Object o){
		return (this == o || o != null && getClass() == o.getClass());
	}

	@Override
	public int hashCode(){
		return getClass().hashCode();
	}

}
