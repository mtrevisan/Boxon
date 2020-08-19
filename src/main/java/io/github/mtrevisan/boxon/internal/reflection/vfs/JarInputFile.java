package io.github.mtrevisan.boxon.internal.reflection.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;


/**
 *
 */
public class JarInputFile implements Vfs.File{
	private final ZipEntry entry;
	private final JarInputDirectory jarInputDir;
	private final long fromIndex;
	private final long endIndex;

	public JarInputFile(final ZipEntry entry, final JarInputDirectory jarInputDir, final long cursor, final long nextCursor){
		this.entry = entry;
		this.jarInputDir = jarInputDir;
		fromIndex = cursor;
		endIndex = nextCursor;
	}

	public String getName(){
		final String name = entry.getName();
		return name.substring(name.lastIndexOf("/") + 1);
	}

	public String getRelativePath(){
		return entry.getName();
	}

	public InputStream openInputStream(){
		return new InputStream(){
			@Override
			public int read() throws IOException{
				if(jarInputDir.cursor >= fromIndex && jarInputDir.cursor <= endIndex){
					final int read = jarInputDir.jarInputStream.read();
					jarInputDir.cursor++;
					return read;
				}
				else{
					return -1;
				}
			}
		};
	}
}
