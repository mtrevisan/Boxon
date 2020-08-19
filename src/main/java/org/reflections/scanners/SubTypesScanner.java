package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;


/**
 * scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes
 */
public class SubTypesScanner extends AbstractScanner{

	@SuppressWarnings({"unchecked"})
	public void scan(final Object cls, Store store){
		String className = getMetadataAdapter().getClassName(cls);
		String superclass = getMetadataAdapter().getSuperclassName(cls);

		put(store, superclass, className);

		for(String anInterface : (List<String>) getMetadataAdapter().getInterfacesNames(cls))
			put(store, anInterface, className);
	}

}
