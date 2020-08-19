package io.github.mtrevisan.boxon.internal.reflection.vfs;

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.jar.JarFile;


/**
 * an implementation of {@link VirtualFileSystem.Directory} for {@link java.util.zip.ZipFile}
 */
public class ZipDirectory implements VirtualFileSystem.Directory{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(ZipDirectory.class);


	final java.util.zip.ZipFile jarFile;


	public ZipDirectory(final JarFile jarFile){
		this.jarFile = jarFile;
	}

	public String getPath(){
		return jarFile.getName();
	}

	public Iterable<VirtualFileSystem.File> getFiles(){
		return () -> jarFile.stream().filter(entry -> !entry.isDirectory()).map(entry -> (VirtualFileSystem.File) new ZipFile(ZipDirectory.this, entry)).iterator();
	}

	public void close(){
		try{
			jarFile.close();
		}
		catch(final IOException e){
			if(LOGGER != null)
				LOGGER.warn("Could not close JarFile", e);
		}
	}

	@Override
	public String toString(){
		return jarFile.getName();
	}
}
