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
package unit731.boxon.helpers;

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
		final StringBuffer sb = new StringBuffer(composeExceptionMessage(t, includeLineNumber));
		Throwable cause = t.getCause();
		while(cause != null){
			sb.append(System.lineSeparator())
				.append(composeExceptionMessage(cause, includeLineNumber));

			cause = cause.getCause();
		}
		return sb.toString();
	}

	private static String composeExceptionMessage(final Throwable t, final boolean includeLineNumber){
		final StringBuffer sb = new StringBuffer();
		if(includeLineNumber)
			sb.append(extractExceptionName(t))
				.append(" at ")
				.append(extractExceptionPosition(t))
				.append(' ');
		final String msg = t.getMessage();
		if(msg != null)
			sb.append(msg);
		return sb.toString();
	}

	private static String extractExceptionPosition(final Throwable t){
		final StackTraceElement stackTrace = extractOwnCodeStackTrace(t);
		String filename = stackTrace.getFileName();
		assert filename != null;
		filename = filename.substring(0, filename.lastIndexOf('.'));
		return filename + "." + stackTrace.getMethodName() + ":" + stackTrace.getLineNumber();
	}

	private static StackTraceElement extractOwnCodeStackTrace(final Throwable t){
		final StackTraceElement[] stackTrace = t.getStackTrace();
		StackTraceElement stackTrace0 = null;
		if(stackTrace.length > 0){
			final String className = ExceptionHelper.class.getName();
			final String classPackage = className.substring(0, className.indexOf('.') + 1);
			stackTrace0 = match(stackTrace, trace -> trace.getClassName().startsWith(classPackage));
			if(stackTrace0 == null)
				stackTrace0 = stackTrace[0];
		}
		return stackTrace0;
	}

	private static <T> T match(final T[] array, final Predicate<T> condition){
		for(final T elem : array)
			if(condition.test(elem))
				return elem;
		return null;
	}

	private static String extractExceptionName(final Throwable t){
		return t.getClass().getSimpleName();
	}

}
