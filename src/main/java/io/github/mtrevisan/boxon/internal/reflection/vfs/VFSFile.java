package io.github.mtrevisan.boxon.internal.reflection.vfs;

import java.io.IOException;
import java.io.InputStream;


public interface VFSFile{

	String getName();

	String getRelativePath();

	InputStream openInputStream() throws IOException;

}
