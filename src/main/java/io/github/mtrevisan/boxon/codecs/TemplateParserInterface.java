package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.core.BitReader;
import io.github.mtrevisan.boxon.core.BitWriter;


public interface TemplateParserInterface{

	<T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException;

	<T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
		throws FieldException;

}
