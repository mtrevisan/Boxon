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

import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


/**
 * An implementation of {@link VFSDirectory} for {@link java.util.zip.ZipFile ZipFile}.
 */
class ZipDirectory implements VFSDirectory{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(ZipDirectory.class);


	private final java.util.zip.ZipFile jarFile;


	ZipDirectory(final JarFile jarFile){
		this.jarFile = jarFile;
	}

	@Override
	public String getPath(){
		return jarFile.getName();
	}

	@Override
	public Iterable<VFSFile> getFiles(){
		return () -> jarFile.stream()
			.filter(Predicate.not(ZipEntry::isDirectory))
			.map(entry -> (VFSFile)new ZipFile(ZipDirectory.this.getPath(), entry))
			.iterator();
	}

	@Override
	public InputStream openInputStream(final VFSFile file){
		try{
			return jarFile.getInputStream(((ZipFile)file).entry);
		}
		catch(final IOException e){
			//cannot happen
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void close(){
		try{
			jarFile.close();
		}
		catch(final IOException e){
			if(LOGGER != null)
				LOGGER.warn("Could not close JarFile", e);
		}
	}

	@Override
	public String toString(){
		return jarFile.getName();
	}

}
