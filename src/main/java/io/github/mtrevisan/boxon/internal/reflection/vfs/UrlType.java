package io.github.mtrevisan.boxon.internal.reflection.vfs;

import java.net.URL;


/** A matcher and factory for a URL. */
public interface UrlType{

	boolean matches(URL url);

	VFSDirectory createDir(URL url) throws Exception;

}
