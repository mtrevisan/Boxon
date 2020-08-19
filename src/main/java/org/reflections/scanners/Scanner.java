package org.reflections.scanners;

import org.reflections.Configuration;
import org.reflections.Store;
import org.reflections.vfs.Vfs;


public interface Scanner{

	void setConfiguration(Configuration configuration);

	boolean acceptsInput(String file);

	Object scan(Vfs.File file, Object classObject, Store store);

}
