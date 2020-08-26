module boxon{
	requires org.slf4j;
	requires io.github.classgraph;
	requires spring.core;
	requires spring.expression;

	opens io.github.mtrevisan.boxon.codecs to spring.core, spring.expression;
//	opens io.github.mtrevisan.boxon.codecs.queclink to spring.core, spring.expression;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.codecs;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.external;
}