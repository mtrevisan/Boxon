package org.reflections.scanners;

import org.reflections.ClassStore;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.vfs.Vfs;


public interface Scanner{

	@SuppressWarnings("rawtypes")
	void setMetadataAdapter(final MetadataAdapter metadataAdapter);

	boolean acceptsInput(final String file);

	Object scan(final Vfs.File file, final Object classObject,final ClassStore classStore);

}
