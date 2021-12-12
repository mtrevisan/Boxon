/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.external.logs;

import io.github.mtrevisan.boxon.internal.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.StringJoiner;


public final class EventLogger extends EventListener{

	private static final String EMPTY_STRING = "";

	static{
		try{
			//check whether an optional SLF4J binding is available
			Class.forName("org.slf4j.impl.StaticLoggerBinder");
		}
		catch(final LinkageError | ClassNotFoundException ignored){
			System.out.println("[WARN] SLF4J: No logger is defined, NO LOG will be printed!");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(EventLogger.class);


	private static class SingletonHelper{
		private static final EventLogger INSTANCE = new EventLogger();
	}


	public static EventLogger getInstance(){
		return EventLogger.SingletonHelper.INSTANCE;
	}

	private EventLogger(){}

	@Override
	public void loadingCodecs(final Class<?>[] basePackageClasses){
		if(LOGGER.isInfoEnabled())
			info("Load codecs from package(s) {}", joinPackageNames(basePackageClasses));
	}

	@Override
	public void loadingCodec(final Class<?>[] codecClasses){
		if(LOGGER.isInfoEnabled()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(int i = 0; i < codecClasses.length; i ++)
				sj.add(codecClasses[i].getSimpleName());

			info("Loading codecs: {}", sj.toString());
		}
	}

	@Override
	public void loadedCodecs(final int count){
		trace("Codecs loaded are {}", count);
	}

	@Override
	public void cannotCreateCodec(final String codecClassName){
		warn("Cannot create an instance of codec {}", codecClassName);
	}


	@Override
	public void loadingTemplates(final Class<?>[] basePackageClasses){
		if(LOGGER.isInfoEnabled())
			info("Load templates from package(s) {}", joinPackageNames(basePackageClasses));
	}

	private static StringJoiner joinPackageNames(final Class<?>[] basePackageClasses){
		final StringJoiner sj = new StringJoiner(", ", "[", "]");
		for(int i = 0; i < basePackageClasses.length; i ++)
			sj.add(basePackageClasses[i].getPackageName());
		return sj;
	}

	@Override
	public void loadingTemplate(final Class<?> templateClass){
		if(LOGGER.isInfoEnabled())
			info("Load template {}", templateClass.getName());
	}

	@Override
	public void loadedTemplates(final int count){
		trace("Templates loaded are {}", count);
	}

	@Override
	public void cannotLoadTemplate(final String templateClassName, final Exception exception){
		LOGGER.error("Cannot load class {}", templateClassName, exception);
	}


	@Override
	public void loadingConfigurations(final Class<?>[] basePackageClasses){
		if(LOGGER.isInfoEnabled())
			info("Load configurations from package(s) {}", joinPackageNames(basePackageClasses));
	}

	@Override
	public void loadedConfigurations(final int count){
		trace("Configurations loaded are {}", count);
	}

	@Override
	public void cannotLoadConfiguration(final String configurationClassName, final Exception exception){
		LOGGER.error("Cannot load class {}", configurationClassName, exception);
	}


	@Override
	public void processingAlternative(final Exception exception){
		trace("Error while processing alternative", exception);
		warn(exception.getMessage() != null? exception.getMessage(): exception.getClass().getSimpleName());
	}


	@Override
	public void readingField(final String templateName, final String fieldName, final String bindingTypeName){
		trace("reading {}.{} with bind {}", templateName, fieldName, bindingTypeName);
	}

	@Override
	public void readField(final String templateName, final String fieldName, final Object value){
		trace("read {}.{} = {}", templateName, fieldName, value);
	}

	@Override
	public void evaluatingField(final String templateName, final String fieldName){
		trace("evaluating {}.{}", templateName, fieldName);
	}

	@Override
	public void evaluatedField(final String templateName, final String fieldName, final Object value){
		trace("wrote {}.{} = {}", templateName, fieldName, value);
	}

	@Override
	public void writingField(final String templateName, final String fieldName, final String bindingTypeName){
		trace("writing {}.{} with bind {}", templateName, fieldName, bindingTypeName);
	}

	@Override
	public void writtenField(final String templateName, final String fieldName, final Object value){
		trace("written {}.{} = {}", templateName, fieldName, value);
	}


	private static void trace(final String message, final Exception exception){
		LOGGER.trace(composeMessage(message), exception);
	}

	private static void trace(final String message, final Object... parameters){
		LOGGER.trace(composeMessage(message, parameters));
	}

	private static void warn(final String message, final Object... parameters){
		LOGGER.warn(composeMessage(message, parameters));
	}

	private static void info(final String message, final Object... parameters){
		LOGGER.info(composeMessage(message, parameters));
	}

	private static String composeMessage(final String message, final Object... parameters){
		String outputMessage = EMPTY_STRING;
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final Class<?> callerClass = Class.forName(stackTrace[3].getClassName());
			final int callerLineNumber = stackTrace[3].getLineNumber();

			outputMessage += "(" + callerClass.getSimpleName();
			if(callerLineNumber >= 0)
				outputMessage += ":" + callerLineNumber;
			outputMessage += ") ";
		}
		catch(final ClassNotFoundException ignored){}

		if(!message.isEmpty())
			outputMessage += message;

		return StringHelper.format(outputMessage, extractParameters(parameters));
	}

	private static Object[] extractParameters(final Object[] parameters){
		if(parameters instanceof Class<?>[]){
			final Collection<String> packages = collectPackages(parameters);

			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final String pkg : packages)
				sj.add(pkg);

			return new Object[]{sj.toString()};
		}
		return parameters;
	}

	private static Collection<String> collectPackages(final Object[] parameters){
		final Collection<String> packages = new HashSet<>(parameters.length);
		for(int i = 0; i < parameters.length; i ++)
			packages.add(((Class<?>)parameters[i]).getPackageName());
		return packages;
	}

}
