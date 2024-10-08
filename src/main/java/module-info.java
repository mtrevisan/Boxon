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

/**
 * This module is responsible for providing the functionality of the Boxon library.
 * <p>Boxon is a Java library that allows for easy serialization and deserialization of binary data.</p>
 * <p>It provides annotations and configurations to customize the serialization and deserialization process.</p>
 * <p>Boxon uses the `ClassGraph` library for runtime annotation processing, the `Freemarker` template engine for code generation, and the
 * `Spring <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression` library for interpreting
 * conditions and other user inputs.</p>
 * <br />
 * <p>This module has the following dependencies:
 * - freemarker: A template engine used for code generation.
 * - classgraph: A library used for runtime annotation processing.
 * - slf4j: A simple logging facade for various logging frameworks.
 * - spring.core: The core Spring framework library.
 * - spring.expression: The Spring expression language library.
 * </p>
 * <p>
 * It opens the 'io.github.mtrevisan.boxon.core.codecs' package to allow for introspection by the Spring framework.
 * </p>
 * <br />
 * <p>
 * It exports the following packages:
 * <ul>
 *    <li>io.github.mtrevisan.boxon.annotations: Contains annotations used to configure serialization and deserialization.</li>
 *   <li>io.github.mtrevisan.boxon.annotations.bindings: Contains annotations used to bind data fields to fields in a Java object.</li>
 *   <li>io.github.mtrevisan.boxon.annotations.checksummers: Contains annotations used to calculate checksums during serialization or
 * 	deserialization.</li>
 *   <li>io.github.mtrevisan.boxon.annotations.configurations: Contains annotations used to configure the behavior of the serialization and
 *   	deserialization process.</li>
 *   <li>io.github.mtrevisan.boxon.annotations.converters: Contains annotations used to convert data fields during serialization or
 *   	deserialization.</li>
 *   <li>io.github.mtrevisan.boxon.annotations.validators: Contains annotations used to validate data fields during serialization or
 *   	deserialization.</li>
 *   <li>io.github.mtrevisan.boxon.core: Contains the core functionality of the Boxon library.</li>
 *   <li>io.github.mtrevisan.boxon.core.keys: Contains classes used to define the keys to access data fields during serialization or
 *   	deserialization.</li>
 *   <li>io.github.mtrevisan.boxon.exceptions: Contains exceptions that can be thrown during the serialization or deserialization
 *   	process.</li>
 *   <li>io.github.mtrevisan.boxon.io: Contains classes for reading and writing binary data.</li>
 *   <li>io.github.mtrevisan.boxon.logs: Contains classes for logging in Boxon.</li>
 *   <li>io.github.mtrevisan.boxon.semanticversioning: Contains classes for representing and comparing semantic version numbers.</li>
 * </ul>
 */
module io.github.mtrevisan.boxon{
	requires freemarker;
	requires io.github.classgraph;
	requires org.slf4j;
	requires spring.core;
	requires spring.expression;
	requires net.bytebuddy;

	opens io.github.mtrevisan.boxon.core.codecs to spring.core, spring.expression;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.bindings;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.configurations;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.core;
	exports io.github.mtrevisan.boxon.core.detect;
	exports io.github.mtrevisan.boxon.core.keys;
	exports io.github.mtrevisan.boxon.core.similarity.distances;
	exports io.github.mtrevisan.boxon.core.similarity.metrics;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.io;
	exports io.github.mtrevisan.boxon.logs;
	exports io.github.mtrevisan.boxon.semanticversioning;
}
