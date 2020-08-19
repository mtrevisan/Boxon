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
import java.util.jar.JarFile;


/**
 * an implementation of {@link Directory} for {@link java.util.zip.ZipFile}
 */
public class ZipDirectory implements Directory{

	public static final Logger LOGGER = JavaHelper.getLoggerFor(ZipDirectory.class);


	final java.util.zip.ZipFile jarFile;


	public ZipDirectory(final JarFile jarFile){
		this.jarFile = jarFile;
	}

	public String getPath(){
		return jarFile.getName();
	}

	public Iterable<File> getFiles(){
		return () -> jarFile.stream()
			.filter(entry -> !entry.isDirectory())
			.map(entry -> (File)new ZipFile(ZipDirectory.this, entry))
			.iterator();
	}

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
