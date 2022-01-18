/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs.managers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ContextHelper{

	/** The name of the current object being scanner (used for referencing variables from SpEL). */
	public static final String CONTEXT_SELF = "self";
	/** The name of the prefix for the alternative (used for referencing variables from SpEL). */
	public static final String CONTEXT_CHOICE_PREFIX = "prefix";

	private static final Matcher CONTEXT_PREFIXED_CHOICE_PREFIX = Pattern.compile("#" + CONTEXT_CHOICE_PREFIX + "[^a-zA-Z]")
		.matcher("");


	private ContextHelper(){}

	public static boolean containsPrefixReference(final CharSequence condition){
		return CONTEXT_PREFIXED_CHOICE_PREFIX.reset(condition).find();
	}

}
