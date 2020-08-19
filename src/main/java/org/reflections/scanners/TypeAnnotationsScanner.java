package org.reflections.scanners;

import org.reflections.Store;

import java.util.List;


/**
 * scans for class's annotations, where @Retention(RetentionPolicy.RUNTIME)
 */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner{

	public void scan(final Object cls, final Store store){
		final String className = getMetadataAdapter().getClassName(cls);

		final List<String> classAnnotationNames = getMetadataAdapter().getClassAnnotationNames(cls);
		for(final String classAnnotationName : classAnnotationNames)
			put(store, classAnnotationName, className);
	}

}
