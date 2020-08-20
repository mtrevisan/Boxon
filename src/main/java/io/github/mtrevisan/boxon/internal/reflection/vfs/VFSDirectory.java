package io.github.mtrevisan.boxon.internal.reflection.vfs;


public interface VFSDirectory{

	String getPath();

	Iterable<VFSFile> getFiles();

}
