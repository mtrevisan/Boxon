package org.reflections.vfs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * an implementation of {@link Vfs.File} for a directory {@link java.io.File}
 */
public class SystemFile implements Vfs.File{
	private final SystemDir root;
	private final java.io.File file;

	public SystemFile(final SystemDir root, final java.io.File file){
		this.root = root;
		this.file = file;
	}

	public String getName(){
		return file.getName();
	}

	public String getRelativePath(){
		final String filepath = file.getPath().replace("\\", "/");
		if(filepath.startsWith(root.getPath())){
			return filepath.substring(root.getPath().length() + 1);
		}

		return null; //should not get here
	}

	public InputStream openInputStream(){
		try{
			return new FileInputStream(file);
		}catch(final FileNotFoundException e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString(){
		return file.toString();
	}
}
