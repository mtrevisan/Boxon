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
package io.github.mtrevisan.boxon.external;

import io.github.mtrevisan.boxon.codecs.CodecInterface;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.StringJoiner;


public final class EventLogger extends EventListener{

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
	public void loadingCodec(final CodecInterface<?>[] codecs){
		if(LOGGER.isInfoEnabled()){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(int i = 0; i < codecs.length; i ++)
				sj.add(codecs[i].getClass().getSimpleName());

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

	private StringJoiner joinPackageNames(final Class<?>[] basePackageClasses){
		final StringJoiner sj = new StringJoiner(", ", "[", "]");
		for(int i = 0; i < basePackageClasses.length; i ++)
			sj.add(basePackageClasses[i].getPackageName());
		return sj;
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
	public void uselessAlternative(final String defaultAlternativeClassName){
		warn("Useless definition of default alternative ({}) due to no alternatives present on @BindArray or @BindObject",
			defaultAlternativeClassName);
	}

	@Override
	public void processingAlternative(final Exception exception){
		trace("Error while processing alternative", exception);
		warn(exception.getMessage() != null? exception.getMessage(): exception.getClass().getSimpleName());
	}


	@Override
	public void decodingField(final String templateName, final String fieldName, final String bindingTypeName){
		trace("reading {}.{} with bind {}", templateName, fieldName, bindingTypeName);
	}

	@Override
	public void decodedField(final String templateName, final String fieldName, final Object value){
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


	private void trace(final String message, final Exception exception){
		LOGGER.trace(composeMessage(message), exception);
	}

	private void trace(final String message, final Object... parameters){
		LOGGER.trace(composeMessage(message, parameters));
	}

	private void warn(final String message, final Object... parameters){
		LOGGER.warn(composeMessage(message, parameters));
	}

	private void info(final String message, final Object... parameters){
		LOGGER.info(composeMessage(message, parameters));
	}

	private String composeMessage(final String message, final Object... parameters){
		final StringBuilder sb = new StringBuilder();
		try{
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			final Class<?> callerClass = Class.forName(stackTrace[3].getClassName());
			final int callerLineNumber = stackTrace[3].getLineNumber();

			sb.append('(')
				.append(callerClass.getSimpleName());
			if(callerLineNumber >= 0)
				sb.append(':')
					.append(callerLineNumber);
			sb.append(')')
				.append(' ');
		}
		catch(final ClassNotFoundException ignored){}

		if(message != null)
			sb.append(message);

		return JavaHelper.format(sb.toString(), extractParameters(parameters));
	}

	private Object[] extractParameters(final Object[] parameters){
		if(parameters instanceof Class<?>[]){
			final Collection<String> packages = new LinkedHashSet<>(parameters.length);
			for(int i = 0; i < parameters.length; i ++)
				packages.add(((Class<?>)parameters[i]).getPackageName());

			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final String p : packages)
				sj.add(p);

			return new Object[]{sj.toString()};
		}
		return parameters;
	}

}
