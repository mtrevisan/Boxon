package org.reflections.scanners;

import org.reflections.ClassStore;


/**
 * Scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes.
 */
public class SubTypesScanner extends AbstractScanner{

	@SuppressWarnings("unchecked")
	public void scan(final Object cls, final ClassStore classStore){
		final String className = metadataAdapter.getClassName(cls);
		final String superclass = metadataAdapter.getSuperclassName(cls);

		put(classStore, superclass, className);

		final String[] interfacesNames = metadataAdapter.getInterfacesNames(cls);
		for(int i = 0; i < interfacesNames.length; i ++)
			put(classStore, interfacesNames[i], className);
	}

}
