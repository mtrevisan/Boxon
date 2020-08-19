package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.Store;
import org.reflections.vfs.Vfs;


public interface Scanner{

	void setConfiguration(final Configuration configuration);

	boolean acceptsInput(final String file);

	Object scan(final Vfs.File file, final Object classObject,final  Store store);

}
