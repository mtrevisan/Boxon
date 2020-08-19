package io.github.mtrevisan.boxon.internal.reflection.vfs;

import io.github.mtrevisan.boxon.internal.reflection.ReflectionsException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;


/*
 * An implementation of {@link io.github.mtrevisan.boxon.internal.reflection.vfs.Vfs.Dir} for directory {@link java.io.File}.
 */
public class SystemDirectory implements Vfs.Directory{
	private final File file;

	public SystemDirectory(final File file){
		if(file != null && (!file.isDirectory() || !file.canRead())){
			throw new RuntimeException("cannot use dir " + file);
		}

		this.file = file;
	}

	public String getPath(){
		if(file == null){
			return "/NO-SUCH-DIRECTORY/";
		}
		return file.getPath().replace("\\", "/");
	}

	public Iterable<Vfs.File> getFiles(){
		if(file == null || !file.exists()){
			return Collections.emptyList();
		}
		return () -> {
			try{
				return Files.walk(file.toPath()).filter(Files::isRegularFile).map(path -> (Vfs.File) new SystemFile(SystemDirectory.this, path.toFile())).iterator();
			}catch(final IOException e){
				throw new ReflectionsException("could not get files for " + file, e);
			}
		};
	}

	@Override
	public String toString(){
		return getPath();
	}
}
