module io.github.mtrevisan.boxon{
	requires freemarker;
	requires io.github.classgraph;
	requires org.slf4j;
	requires spring.core;
	requires spring.expression;

	opens io.github.mtrevisan.boxon.codecs to spring.core, spring.expression;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.bindings;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.configurations;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.core;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.external.semanticversioning;
	exports io.github.mtrevisan.boxon.external.configurations;
	exports io.github.mtrevisan.boxon.external.codecs;
	exports io.github.mtrevisan.boxon.external.logs;
}