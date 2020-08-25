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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;


/**
 * An implementation of {@link VFSDirectory} for a {@link File}.
 */
class SystemDirectory implements VFSDirectory{

	private static final char SLASH = '/';
	private static final char BACKSLASH = '\\';


	private final File file;


	SystemDirectory(final File file){
		if(file != null && (!file.isDirectory() || !file.canRead()))
			throw new RuntimeException("Cannot use directory " + file);

		this.file = file;
	}

	@Override
	public String getPath(){
		return (file != null? file.getPath().replace(BACKSLASH, SLASH): "/NO-SUCH-DIRECTORY/");
	}

	@Override
	public Iterable<VFSFile> getFiles(){
		if(file == null || !file.exists())
			return Collections.emptyList();

		return () -> {
			try{
				return Files.walk(file.toPath())
					.filter(Files::isRegularFile)
					.map(path -> (VFSFile)new SystemFile(SystemDirectory.this.getPath(), path.toFile()))
					.iterator();
			}
			catch(final IOException e){
				throw new VFSException("Could not get files for {}", file)
					.withCause(e);
			}
		};
	}

	@Override
	public InputStream openInputStream(final VFSFile file){
		try{
			return new FileInputStream(((SystemFile)file).entry);
		}
		catch(final FileNotFoundException e){
			throw new RuntimeException(e);
		}
	}

	@Override
	@SuppressWarnings("RedundantThrows")
	public void close() throws Exception{}

	@Override
	public String toString(){
		return getPath();
	}

}
