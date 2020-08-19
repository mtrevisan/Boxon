package io.github.mtrevisan.boxon.internal.reflection.vfs;

import io.github.mtrevisan.boxon.internal.reflection.ReflectionsException;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;


public class JarInputDirectory implements VirtualFileSystem.Directory{
	private final URL url;
	JarInputStream jarInputStream;
	long cursor = 0;
	long nextCursor = 0;

	public JarInputDirectory(final URL url){
		this.url = url;
	}

	public String getPath(){
		return url.getPath();
	}

	public Iterable<VirtualFileSystem.File> getFiles(){
		return () -> new Iterator<>(){
			{
				try{
					jarInputStream = new JarInputStream(url.openConnection().getInputStream());
				}
				catch(final Exception e){
					throw new ReflectionsException("Could not open url connection", e);
				}
			}

			VirtualFileSystem.File entry = null;

			@Override
			public boolean hasNext(){
				return entry != null || (entry = computeNext()) != null;
			}

			@Override
			public VirtualFileSystem.File next(){
				final VirtualFileSystem.File next = entry;
				entry = null;
				return next;
			}

			private VirtualFileSystem.File computeNext(){
				while(true){
					try{
						final ZipEntry entry = jarInputStream.getNextJarEntry();
						if(entry == null){
							return null;
						}

						long size = entry.getSize();
						if(size < 0)
							size = 0xffffffffl + size; //JDK-6916399
						nextCursor += size;
						if(!entry.isDirectory()){
							return new JarInputFile(entry, JarInputDirectory.this, cursor, nextCursor);
						}
					}catch(final IOException e){
						throw new ReflectionsException("could not get next zip entry", e);
					}
				}
			}
		};
	}

}
