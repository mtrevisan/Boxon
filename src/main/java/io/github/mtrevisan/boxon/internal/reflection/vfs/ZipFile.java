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


/**
 * an implementation of {@link File} for {@link ZipEntry}
 */
public class ZipFile implements File{

	private final ZipDirectory root;
	private final ZipEntry entry;


	public ZipFile(final ZipDirectory root, final ZipEntry entry){
		this.root = root;
		this.entry = entry;
	}

	public String getName(){
		final String name = entry.getName();
		return name.substring(name.lastIndexOf("/") + 1);
	}

	public String getRelativePath(){
		return entry.getName();
	}

	public InputStream openInputStream() throws IOException{
		return root.jarFile.getInputStream(entry);
	}

	@Override
	public String toString(){
		return root.getPath() + "!" + java.io.File.separatorChar + entry.toString();
	}

}
