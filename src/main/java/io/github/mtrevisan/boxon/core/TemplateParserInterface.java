package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitWriter;


interface TemplateParserInterface{

	<T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException;

	<T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
		throws FieldException;

}
