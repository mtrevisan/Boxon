package org.reflections.scanners;

import org.reflections.Store;
import org.reflections.adapters.MetadataAdapter;


/**
 * Scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes.
 */
public class SubTypesScanner extends AbstractScanner{

	@SuppressWarnings({"unchecked"})
	public void scan(final Object cls, final Store store){
		@SuppressWarnings("rawtypes")
		final MetadataAdapter metadataAdapter = getMetadataAdapter();
		final String className = metadataAdapter.getClassName(cls);
		final String superclass = metadataAdapter.getSuperclassName(cls);

		put(store, superclass, className);

		final String[] interfacesNames = metadataAdapter.getInterfacesNames(cls);
		for(int i = 0; i < interfacesNames.length; i ++)
			put(store, interfacesNames[i], className);
	}

}
