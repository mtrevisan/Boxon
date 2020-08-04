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
package io.github.mtrevisan.boxon.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public final class AnnotationHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationHelper.class);

	private enum BucketType{DIRECTORY, FILE}


	private static final String SCHEMA_FILE = "file:";
	private static final String EXTENSION_CLASS = ".class";
	private static final String BOOT_INF_CLASSES = "BOOT-INF.classes.";
	private static final String POINT = ".";

	private static final class ClassDescriptor{
		final File file;
		final String packageName;

		ClassDescriptor(final File file, final String packageName){
			this.file = file;
			this.packageName = packageName;
		}
	}


	private AnnotationHelper(){}

	/**
	 * Retrieving fields list of specified class.
	 *
	 * @param cls	The class from which to extract the declared fields
	 * @param recursively	If {@code true}, it retrieves fields from all class hierarchy
	 * @return	An array of all the fields of the given class
	 */
	public static Field[] getDeclaredFields(final Class<?> cls, final boolean recursively){
		final Field[] result;
		if(recursively){
			final SimpleDynamicArray<Field> fields = SimpleDynamicArray.create(Field.class);
			Class<?> currentType = cls;
			while(currentType != Object.class){
				final Field[] subfields = currentType.getDeclaredFields();
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
			result = fields.extractCopy();
		}
		else
			result = cls.getDeclaredFields();
		return result;
	}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package
	 *
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes
	 */
	public static Collection<Class<?>> extractClasses(final Object type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for(int i = 0; i < basePackageClasses.length; i ++){
			final String basePackageClassName = basePackageClasses[i].getName();
			final String basePackageName = basePackageClassName.substring(0, basePackageClassName.lastIndexOf('.'));
			final String path = packageToUri(basePackageName);
			try{
				final Enumeration<URL> resources = classLoader.getResources(path);
				classes.addAll(extractClasses(resources, type, basePackageName));
			}
			catch(final NoSuchFileException e){
				LOGGER.error("Are you sure you are not running this library from a OneDrive folder?", e);
			}
			catch(final IOException e){
				LOGGER.error("Cannot load classes from {}", path, e);
			}
		}

		return classes;
	}

	private static Collection<Class<?>> extractClasses(final Enumeration<URL> resources, final Object type, final String basePackageName) throws IOException{
		final Collection<Class<?>> classes = new HashSet<>(0);
		while(resources.hasMoreElements()){
			final URL resource = resources.nextElement();
			final String directory = resource.getFile();
			final int exclamationMarkIndex = directory.indexOf('!');
			final Collection<Class<?>> subClasses;
			if(exclamationMarkIndex >= 0){
				final String libraryName = directory.substring(SCHEMA_FILE.length(), exclamationMarkIndex);
				subClasses = extractClassesFromLibrary(type, libraryName);
			}
			else
				subClasses = extractClasses(type, new File(directory), basePackageName);
			classes.addAll(subClasses);
		}
		return classes;
	}

	/**
	 * Scans all classes accessible from a library which belong to the given package
	 *
	 * @param libraryName The name of the library to load the classes from
	 * @return The classes
	 */
	private static Collection<Class<?>> extractClassesFromLibrary(final Object type, final String libraryName) throws IOException{
		final Collection<Class<?>> classes = new HashSet<>(0);

		final JarFile jarFile = new JarFile(libraryName);
		final Enumeration<JarEntry> resources = jarFile.entries();
		while(resources.hasMoreElements()){
			final JarEntry resource = resources.nextElement();
			final Class<?> cls = getClassFromResource(resource);
			addIf(classes, cls, type);
		}

		return classes;
	}

	/**
	 * Extract all classes from a given directory
	 *
	 * @param directory	The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 */
	private static Collection<Class<?>> extractClasses(final Object type, final File directory, final String packageName){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final Stack<ClassDescriptor> stack = new Stack<>();
		stack.push(new ClassDescriptor(directory, packageName));
		while(!stack.isEmpty()){
			final ClassDescriptor elem = stack.pop();

			final File[] files = elem.file.listFiles();
			final Map<BucketType, SimpleDynamicArray<File>> bucket = bucketByFileType(files);
			final SimpleDynamicArray<File> bucketDirectory = bucket.get(BucketType.DIRECTORY);
			for(int i = 0; i < bucketDirectory.limit; i ++){
				final File dir = bucketDirectory.data[i];
				stack.add(new ClassDescriptor(dir, elem.packageName + POINT + dir.getName()));
			}
			final SimpleDynamicArray<File> bucketFile = bucket.get(BucketType.FILE);
			for(int i = 0; i < bucketFile.limit; i ++){
				final File file = bucketFile.data[i];
				final Class<?> cls = getClassFromFilename(elem.packageName, file.getName());
				addIf(classes, cls, type);
			}
		}

		return classes;
	}

	private static Map<BucketType, SimpleDynamicArray<File>> bucketByFileType(final File[] files){
		final Map<BucketType, SimpleDynamicArray<File>> bucket = new EnumMap<>(BucketType.class);
		bucket.put(BucketType.DIRECTORY, SimpleDynamicArray.create(File.class));
		bucket.put(BucketType.FILE, SimpleDynamicArray.create(File.class, files.length));
		if(files != null)
			for(int i = 0; i < files.length; i ++){
				final File file = files[i];
				bucket.get(file.isDirectory()? BucketType.DIRECTORY: BucketType.FILE)
					.add(file);
			}
		return bucket;
	}

	private static Class<?> getClassFromResource(final JarEntry resource){
		Class<?> cls = null;
		final String resourceName = resource.getName();
		if(!resource.isDirectory() && resourceName.endsWith(EXTENSION_CLASS)){
			final String className = resourceName.substring(
				(resourceName.startsWith(BOOT_INF_CLASSES)? BOOT_INF_CLASSES.length(): 0),
				resourceName.length() - EXTENSION_CLASS.length());
			cls = getClassFromName(uriToPackage(className));
		}
		return cls;
	}

	private static Class<?> getClassFromFilename(final String packageName, final String filename){
		Class<?> cls = null;
		if(filename.endsWith(EXTENSION_CLASS)){
			final String className = filename.substring(0, filename.length() - EXTENSION_CLASS.length());
			cls = getClassFromName(packageName + POINT + className);
		}
		return cls;
	}

	private static Class<?> getClassFromName(final String className){
		Class<?> type = null;
		try{
			type = Class.forName(className);
		}
		catch(final ClassNotFoundException ignored){}
		return type;
	}

	@SuppressWarnings("unchecked")
	private static void addIf(final Collection<Class<?>> classes, final Class<?> cls, final Object type){
		if(cls != null && !cls.isInterface() && (cls.isAnnotationPresent((Class<? extends Annotation>)type) || ((Class<?>)type).isAssignableFrom(cls)))
			classes.add(cls);
	}

	private static String packageToUri(String packageName){
		return packageName.replace('.', '/');
	}

	private static String uriToPackage(final String uri){
		return uri.replace('/', '.');
	}

}
