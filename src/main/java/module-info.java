module io.github.mtrevisan.boxon {
	requires org.slf4j;
	requires spring.expression;
	requires spring.core;

	exports io.github.mtrevisan.boxon.annotations;
	exports io.github.mtrevisan.boxon.annotations.checksummers;
	exports io.github.mtrevisan.boxon.annotations.converters;
	exports io.github.mtrevisan.boxon.annotations.validators;
	exports io.github.mtrevisan.boxon.codecs;
	exports io.github.mtrevisan.boxon.exceptions;
	exports io.github.mtrevisan.boxon.external;
}