package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;


/**
 * scans for class's annotations, where @Retention(RetentionPolicy.RUNTIME)
 */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner{

	public void scan(final Object cls, Store store){
		final String className = getMetadataAdapter().getClassName(cls);

		for(String annotationType : (List<String>) getMetadataAdapter().getClassAnnotationNames(cls))
			put(store, annotationType, className);
	}

}
