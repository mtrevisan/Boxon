package org.reflections.vfs;

import org.reflections.Reflections;

import java.io.IOException;
import java.util.jar.JarFile;


/**
 * an implementation of {@link Vfs.Directory} for {@link java.util.zip.ZipFile}
 */
public class ZipDirectory implements Vfs.Directory{
	final java.util.zip.ZipFile jarFile;

	public ZipDirectory(final JarFile jarFile){
		this.jarFile = jarFile;
	}

	public String getPath(){
		return jarFile.getName();
	}

	public Iterable<Vfs.File> getFiles(){
		return () -> jarFile.stream().filter(entry -> !entry.isDirectory()).map(entry -> (Vfs.File) new ZipFile(ZipDirectory.this, entry)).iterator();
	}

	public void close(){
		try{
			jarFile.close();
		}catch(final IOException e){
			if(Reflections.LOGGER != null){
				Reflections.LOGGER.warn("Could not close JarFile", e);
			}
		}
	}

	@Override
	public String toString(){
		return jarFile.getName();
	}
}
