module io.github.mtrevisan.boxon{
	requires org.slf4j;
	requires io.github.classgraph;
	requires spring.core;
	requires spring.expression;
	requires freemarker;

	opens io.github.mtrevisan.boxon.core to spring.core, spring.expression;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.bindings;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.core;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.external;
}