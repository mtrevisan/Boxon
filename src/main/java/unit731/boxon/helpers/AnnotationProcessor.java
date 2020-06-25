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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class AnnotationProcessor{

	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationProcessor.class.getName());


	private static final String SCHEMA_FILE = "file:";
	private static final String EXTENSION_CLASS = ".class";
	private static final String BOOT_INF_CLASSES = "BOOT-INF.classes.";
	private static final String POINT = ".";

	private static final class ClassElem{
		final File file;
		final String packageName;

		ClassElem(final File file, final String packageName){
			this.file = file;
			this.packageName = packageName;
		}
	}


	private AnnotationProcessor(){}

	/**
	 * Retrieving fields list of specified class
	 * If `recursively` is {@code true}, retrieving fields from all class hierarchy
	 */
	public static Field[] getDeclaredFields(final Class<?> cls, @SuppressWarnings("SameParameterValue") final boolean recursively){
		if(recursively){
			final List<Field> fields = new ArrayList<>();
			Class<?> currentType = cls;
			while(currentType != null){
				final List<Field> subfields = Arrays.asList(currentType.getDeclaredFields());
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
			return fields.toArray(Field[]::new);
		}
		else
			return cls.getDeclaredFields();
	}


	/**
	 * Scans all classes accessible from the context class loader which belong to the given package
	 *
	 * @param basePackageClasses	A list of classes that resides in a base package(s)
	 * @return	The classes
	 */
	public static Collection<Class<?>> extractAnnotatedClasses(final Class<? extends Annotation> annotation, final Class<?>... basePackageClasses){
		final Set<Class<?>> codecs = new HashSet<>();

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
						codecs.addAll(extractAnnotatedClassesFromLibrary(annotation, libraryName));
					}
					else
						codecs.addAll(extractAnnotatedClasses(annotation, new File(directory), basePackageName));
				}
			}
			catch(final NoSuchFileException e){
				LOGGER.error("Are you sure you are not running this library from a OneDrive folder?", e);
			}
			catch(final IOException ignored){}
		}

		return codecs;
	}

	/**
	 * Scans all classes accessible from a library which belong to the given package
	 *
	 * @param libraryName The name of the library to load the classes from
	 * @return The classes
	 */
	private static Set<Class<?>> extractAnnotatedClassesFromLibrary(final Class<? extends Annotation> annotation, final String libraryName){
		final Set<Class<?>> classes = new HashSet<>();

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
						if(cls.isAnnotationPresent(annotation))
							classes.add(cls);
					}
					catch(final ClassNotFoundException ignored){}
				}
			}
		}
		catch(final IOException ignored){}

		return classes;
	}

	/**
	 * Extract all classes from a given directory
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 */
	private static Set<Class<?>> extractAnnotatedClasses(final Class<? extends Annotation> annotation, final File directory, final String packageName){
		final Set<Class<?>> codecs = new HashSet<>();

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
						if(cls.isAnnotationPresent(annotation))
							codecs.add(cls);
					}
					catch(final ClassNotFoundException ignored){}
				}
			}
		}

		return codecs;
	}

}
