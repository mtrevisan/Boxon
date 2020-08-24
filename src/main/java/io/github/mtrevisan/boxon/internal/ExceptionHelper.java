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
package io.github.mtrevisan.boxon.internal;

import java.util.function.Predicate;


public final class ExceptionHelper{

	private ExceptionHelper(){}

	public static String getMessage(final Throwable t){
		return getMessage(t, true);
	}

	public static String getMessageNoLineNumber(final Throwable t){
		return getMessage(t, false);
	}

	private static String getMessage(final Throwable t, final boolean includeLineNumber){
		final StringBuilder sb = composeExceptionMessage(t, includeLineNumber);
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append(System.lineSeparator())
				.append(composeExceptionMessage(cause, includeLineNumber));

			cause = cause.getCause();
		}
		return sb.toString();
	}

	private static StringBuilder composeExceptionMessage(final Throwable t, final boolean includeLineNumber){
		final StringBuilder sb = new StringBuilder();
		if(includeLineNumber)
			includeLineNumber(t, sb);
		if(t.getClass() != RuntimeException.class)
			sb.append(t.getClass().getSimpleName())
				.append(':')
				.append(' ');
		final String msg = t.getMessage();
		if(msg != null)
			sb.append(msg);
		return sb;
	}

	private static void includeLineNumber(final Throwable t, final StringBuilder sb){
		sb.append(extractExceptionName(t))
			.append(" at ");
		final StackTraceElement stackTrace = extractOwnCodeStackTrace(t);
		final String filename = stackTrace.getFileName();
		if(filename != null)
			//append class name
			sb.append(filename, 0, filename.lastIndexOf('.'))
				.append('.');
		sb
			//append method name
			.append(stackTrace.getMethodName())
			.append(':')
			//append line number
			.append(stackTrace.getLineNumber())
			.append(' ');
	}

	private static StackTraceElement extractOwnCodeStackTrace(final Throwable t){
		final StackTraceElement[] stackTrace = t.getStackTrace();
		StackTraceElement stackTrace0 = null;
		if(stackTrace.length > 0){
			final String className = ExceptionHelper.class.getPackageName();
			//backtrack one package to get the base
			final String classPackage = className.substring(0, className.lastIndexOf('.') + 1);
			stackTrace0 = match(stackTrace, trace -> trace.getClassName().startsWith(classPackage));
			if(stackTrace0 == null)
				stackTrace0 = stackTrace[0];
		}
		return stackTrace0;
	}

	private static <T> T match(final T[] array, final Predicate<T> condition){
		for(final T elem : array){
			if(condition.test(elem))
				return elem;
		}
		return null;
	}

	private static String extractExceptionName(final Throwable t){
		return t.getClass().getSimpleName();
	}

}
