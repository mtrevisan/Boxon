package io.github.mtrevisan.boxon.internal.reflection.vfs;


public interface Directory{

	String getPath();

	Iterable<File> getFiles();

}
