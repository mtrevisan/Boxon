/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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

module io.github.mtrevisan.boxon{
	requires freemarker;
	requires io.github.classgraph;
	requires org.slf4j;
	requires spring.core;
	requires spring.expression;

	opens io.github.mtrevisan.boxon.core.codecs to spring.core, spring.expression;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.bindings;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.configurations;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.core;
	exports io.github.mtrevisan.boxon.core.keys;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.io;
	exports io.github.mtrevisan.boxon.logs;
	exports io.github.mtrevisan.boxon.semanticversioning;
}