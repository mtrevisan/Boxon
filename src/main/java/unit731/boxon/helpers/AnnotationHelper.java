package unit731.boxon.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public final class AnnotationHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationHelper.class.getName());

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
	public static Field[] getDeclaredFields(final Class<?> cls, @SuppressWarnings("SameParameterValue") final boolean recursively){
		final Field[] result;
		if(recursively){
			final List<Field> fields = new ArrayList<>(0);
			Class<?> currentType = cls;
			while(currentType != null){
				final Collection<Field> subfields = Arrays.asList(currentType.getDeclaredFields());
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
			result = fields.toArray(Field[]::new);
		}
		else
			result = cls.getDeclaredFields();
		return result;
	}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package
	 *
	 * @param <T>	The class type of type.
	 * @param type	Whether a class or an interface (for example).
	 * @param basePackageClasses	A list of classes that resides in a base package(s).
	 * @return	The classes
	 */
	public static <T> Collection<Class<?>> extractClasses(final T type, final Class<?>... basePackageClasses){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for(final Class<?> basePackageClass : basePackageClasses){
			final String basePackageName = basePackageClass.getName().substring(0, basePackageClass.getName().lastIndexOf('.'));
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

	private static <T> Collection<Class<?>> extractClasses(final Enumeration<URL> resources, final T type, final String basePackageName) throws IOException{
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
	private static <T> Collection<Class<?>> extractClassesFromLibrary(final T type, final String libraryName) throws IOException{
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
	private static <T> Collection<Class<?>> extractClasses(final T type, final File directory, final String packageName){
		final Collection<Class<?>> classes = new HashSet<>(0);

		final Stack<ClassDescriptor> stack = new Stack<>();
		stack.push(new ClassDescriptor(directory, packageName));
		while(!stack.isEmpty()){
			final ClassDescriptor elem = stack.pop();

			final File[] files = Optional.ofNullable(elem.file.listFiles())
				.orElse(new File[0]);
			final Map<BucketType, Collection<File>> bucket = bucketByFileType(files);
			bucket.get(BucketType.DIRECTORY).stream()
				.map(file -> new ClassDescriptor(file, elem.packageName + POINT + file.getName()))
				.forEach(stack::add);
			bucket.get(BucketType.FILE).stream()
				.map(file -> getClassFromFilename(elem.packageName, file.getName()))
				.forEach(cls -> addIf(classes, cls, type));
		}

		return classes;
	}

	private static Map<BucketType, Collection<File>> bucketByFileType(final File[] files){
		final Map<BucketType, Collection<File>> bucket = new EnumMap<>(BucketType.class);
		bucket.put(BucketType.DIRECTORY, new ArrayList<>());
		bucket.put(BucketType.FILE, new ArrayList<>());
		for(final File file : files)
			bucket.get(file.isDirectory()? BucketType.DIRECTORY: BucketType.FILE)
				.add(file);
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
	private static <T> void addIf(final Collection<Class<?>> classes, final Class<?> cls, final Object type){
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
