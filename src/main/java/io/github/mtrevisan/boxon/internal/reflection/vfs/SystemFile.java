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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


/**
 * An implementation of {@link VFSFile} for a {@link File}.
 */
public class SystemFile implements VFSFile{

	private static final String SLASH = "/";
	private static final String BACKSLASH = "\\";


	private final SystemDirectory root;
	private final File file;


	SystemFile(final SystemDirectory root, final File file){
		this.root = root;
		this.file = file;
	}

	@Override
	public String getName(){
		return file.getName();
	}

	@Override
	public String getRelativePath(){
		final String filepath = file.getPath().replace(BACKSLASH, SLASH);
		return (filepath.startsWith(root.getPath())? filepath.substring(root.getPath().length() + 1): null);
	}

	@Override
	public InputStream openInputStream(){
		try{
			return new FileInputStream(file);
		}
		catch(final FileNotFoundException e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString(){
		return file.toString();
	}

}
