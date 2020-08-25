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


/**
 * An implementation of {@link VFSFile} for a {@link File}.
 */
class SystemFile implements VFSFile{

	private static final char SLASH = '/';
	private static final char BACKSLASH = '\\';


	private final String rootPath;
	final File entry;


	SystemFile(final String rootPath, final File entry){
		this.rootPath = rootPath;
		this.entry = entry;
	}

	@Override
	public String getName(){
		return entry.getName();
	}

	@Override
	public String getRelativePath(){
		final String filepath = entry.getPath().replace(BACKSLASH, SLASH);
		return (filepath.startsWith(rootPath)? filepath.substring(rootPath.length() + 1): null);
	}

	@Override
	public String toString(){
		return entry.toString();
	}

}
