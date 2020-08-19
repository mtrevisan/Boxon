package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;


/**
 * scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes
 */
public class SubTypesScanner extends AbstractScanner{

	@SuppressWarnings({"unchecked"})
	public void scan(final Object cls, final Store store){
		final String className = getMetadataAdapter().getClassName(cls);
		final String superclass = getMetadataAdapter().getSuperclassName(cls);

		put(store, superclass, className);

		final List<String> interfacesNames = getMetadataAdapter().getInterfacesNames(cls);
		for(final String interfaceName : interfacesNames)
			put(store, interfaceName, className);
	}

}
