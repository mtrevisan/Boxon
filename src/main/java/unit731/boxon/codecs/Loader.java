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
package unit731.boxon.codecs;

import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.utils.ByteHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Loader{

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class.getName());

	private static final String POINT = ".";
	private static final String SCHEMA_FILE = "file:";
	private static final String EXTENSION_CLASS = ".class";
	private static final String BOOT_INF_CLASSES = "BOOT-INF.classes.";

	private final Map<String, Codec<?>> codecs = new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(String::compareTo));

	private final AtomicBoolean initialized = new AtomicBoolean(false);


	public Loader(){}

	/** This method should be called from a method inside a class that lies on a parent of all the decoders */
	public synchronized void init(){
		try{
			final String callerClassName = Thread.currentThread().getStackTrace()[3].getClassName();
			init(Class.forName(callerClassName));
		}
		catch(final ClassNotFoundException ignored){}
	}

	/**
	 * This method should be called before instantiating a {@link Parser}
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes
	 */
	public synchronized void init(final Class<?>... basePackageClasses){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from package {}",
				Arrays.toString(Arrays.stream(basePackageClasses).map(Class::getName)
					.map(name -> name.substring(0, name.lastIndexOf('.'))).toArray(String[]::new)));

			final Collection<Codec<?>> codecs = extractCodecs(basePackageClasses);
			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	/**
	 * This method should be called before instantiating a {@link Parser}
	 *
	 * @param codecs	The list of codecs to be loaded
	 */
	public synchronized void init(final Collection<Codec<?>> codecs){
		if(!initialized.get()){
			LOGGER.info("Load parsing classes from method");

			loadCodecs(codecs);

			LOGGER.trace("Codecs loaded are {}", codecs.size());

			initialized.set(true);
		}
	}

	public synchronized boolean isInitialized(){
		return initialized.get();
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package
	 *
	 * @param basePackageClasses	A list of classes that resides in a base package(s)
	 * @return	The classes
	 */
	private Collection<Codec<?>> extractCodecs(final Class<?>... basePackageClasses){
		final Set<Codec<?>> codecs = new HashSet<>();

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for(final Class<?> basePackageClass : basePackageClasses){
			try{
				final String basePackageName = basePackageClass.getName().substring(0, basePackageClass.getName().lastIndexOf('.'));
				final String path = basePackageName.replace('.', '/');
				final Enumeration<URL> resources = classLoader.getResources(path);
				while(resources.hasMoreElements()){
					final URL resource = resources.nextElement();
					final String directory = resource.getFile();
					final int exclamationMarkIndex = directory.indexOf('!');
					if(exclamationMarkIndex >= 0){
						final String libraryName = directory.substring(SCHEMA_FILE.length(), exclamationMarkIndex);
						codecs.addAll(findCodecsFromLibrary(libraryName));
					}
					else
						codecs.addAll(findCodecs(new File(directory), basePackageName));
				}
			}
			catch(final NoSuchFileException e){
				LOGGER.error("Are you sure you are not running this library from a OneDrive folder?", e);
			}
			catch(final IOException ignored){}
		}

		return codecs;
	}

	private static final class ClassElem{
		final File file;
		final String packageName;

		ClassElem(final File file, final String packageName){
			this.file = file;
			this.packageName = packageName;
		}
	}

	/**
	 * Extract all classes from a given directory
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 */
	private Set<Codec<?>> findCodecs(final File directory, final String packageName){
		final Set<Codec<?>> codecs = new HashSet<>();

		final Stack<ClassElem> stack = new Stack<>();
		stack.push(new ClassElem(directory, packageName));
		while(!stack.isEmpty()){
			final ClassElem elem = stack.pop();
			final File[] files = Optional.ofNullable(elem.file.listFiles())
				.orElse(new File[0]);
			for(final File file : files){
				if(file.isDirectory())
					stack.push(new ClassElem(file, elem.packageName + POINT + file.getName()));
				else if(file.getName().endsWith(EXTENSION_CLASS)){
					try{
						final String className = elem.packageName + POINT
							+ file.getName().substring(0, file.getName().length() - EXTENSION_CLASS.length());
						final Class<?> cls = Class.forName(className);
						final Codec<?> codec = Codec.createFrom(cls);
						if(codec.canBeDecoded())
							codecs.add(codec);
					}
					catch(final ClassNotFoundException ignored){}
				}
			}
		}

		return codecs;
	}

	/**
	 * Scans all classes accessible from a library which belong to the given package
	 *
	 * @param libraryName The name of the library to load the classes from
	 * @return The classes
	 */
	private Set<Codec<?>> findCodecsFromLibrary(final String libraryName){
		final Set<Codec<?>> codecs = new HashSet<>();

		try{
			final JarFile jarFile = new JarFile(libraryName);
			final Enumeration<JarEntry> resources = jarFile.entries();
			while(resources.hasMoreElements()){
				final JarEntry resource = resources.nextElement();
				final String resourceName = resource.getName();
				if(!resource.isDirectory() && resourceName.endsWith(EXTENSION_CLASS)){
					try{
						final String className = resourceName.substring(0, resourceName.length() - EXTENSION_CLASS.length())
							.replace('/', '.');
						final Class<?> cls = Class.forName(className.startsWith(BOOT_INF_CLASSES)?
							className.substring(BOOT_INF_CLASSES.length()): className);
						final Codec<?> codec = Codec.createFrom(cls);
						if(codec.canBeDecoded())
							codecs.add(codec);
					}
					catch(final ClassNotFoundException ignored){}
				}
			}
		}
		catch(final IOException ignored){}

		return codecs;
	}

	private void loadCodecs(final Collection<Codec<?>> codecs){
		for(final Codec<?> codec : codecs){
			try{
				final MessageHeader header = codec.getHeader();
				final Charset charset = Charset.forName(header.charset());
				for(final String headerStart : header.start()){
					//calculate key
					final String key = ByteHelper.byteArrayToHexString(headerStart.getBytes(charset));
					if(this.codecs.containsKey(key))
						throw new IllegalArgumentException("Duplicate key `" + headerStart + "` found for class "
							+ codec.getType().getSimpleName());

					this.codecs.put(key, codec);
				}
			}
			catch(final Exception e){
				LOGGER.error("Cannot load class {}", codec.getType().getSimpleName(), e);
			}
		}
	}

	Codec<?> getCodec(final BitBuffer reader){
		final int index = reader.positionAsBits() / Byte.SIZE;

		Codec<?> codec = null;
		for(final Map.Entry<String, Codec<?>> codecElem : codecs.entrySet()){
			final String header = codecElem.getKey();

			final byte[] codecHeader = ByteHelper.hexStringToByteArray(header);
			final byte[] messageHeader = Arrays.copyOfRange(reader.array(), index, index + codecHeader.length);

			//verify if it's a valid message header
			if(Arrays.equals(messageHeader, codecHeader)){
				codec = codecElem.getValue();
				break;
			}
		}
		if(codec == null)
			throw new IllegalArgumentException("Cannot find any codec for message");

		return codec;
	}

	int findNextMessageIndex(final BitBuffer reader){
		int minOffset = -1;
		for(final Codec<?> codec : codecs.values()){
			final MessageHeader header = codec.getHeader();
			final Charset charset = Charset.forName(header.charset());
			final String[] messageStarts = header.start();
			for(final String messageStart : messageStarts){
				final int offset = searchNextSequence(reader, messageStart.getBytes(charset));
				if(offset >= 0 && (minOffset < 0 || offset < minOffset))
					minOffset = offset;
			}
		}
		return minOffset;
	}

	private int searchNextSequence(final BitBuffer reader, final byte[] startMessageSequence){
		final int[] boundarySequenceLps = ByteHelper.indexOfComputeLPS(startMessageSequence);

		final byte[] message = reader.array();
		//search inside message:
		final int startIndex = reader.positionAsBits() / Byte.SIZE;
		final int index = ByteHelper.indexOf(message, startMessageSequence, startIndex + 1, boundarySequenceLps);
		return (index >= startIndex? index: -1);
	}

}
