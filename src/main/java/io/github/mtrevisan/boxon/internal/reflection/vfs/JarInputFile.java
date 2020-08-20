/**
 * Copyright (c) 2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.boxon.internal.reflection.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;


public class JarInputFile implements VFSFile{

	private static final String SLASH = "/";

	private final ZipEntry entry;
	private final JarInputDirectory jarInputDir;
	private final long fromIndex;
	private final long endIndex;


	JarInputFile(final ZipEntry entry, final JarInputDirectory jarInputDir, final long cursor, final long nextCursor){
		this.entry = entry;
		this.jarInputDir = jarInputDir;
		fromIndex = cursor;
		endIndex = nextCursor;
	}

	@Override
	public String getName(){
		final String name = getRelativePath();
		return name.substring(name.lastIndexOf(SLASH) + 1);
	}

	@Override
	public String getRelativePath(){
		return entry.getName();
	}

	@Override
	public InputStream openInputStream(){
		return new InputStream(){
			@Override
			public int read() throws IOException{
				if(jarInputDir.cursor >= fromIndex && jarInputDir.cursor <= endIndex){
					final int read = jarInputDir.jarInputStream.read();
					jarInputDir.cursor++;
					return read;
				}
				else
					return -1;
			}
		};
	}

}
