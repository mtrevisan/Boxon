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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;


public interface TemplateParserInterface{

	/**
	 * Decode the template using the given reader with the parent object.
	 *
	 * @param template	The template to decode.
	 * @param reader	The reader that holds the decoded template.
	 * @param parentObject	The parent object (for condition evaluation and field evaluation purposes).
	 * @return	The data read.
	 * @throws BoxonException	If a codec is not found.
	 */
	Object decode(Template<?> template, BitReaderInterface reader, Object parentObject) throws BoxonException;

	/**
	 * Encode the template using the given writer with the given object that contains the values.
	 *
	 * @param template	The template to encode.
	 * @param writer	The writer that holds the encoded template.
	 * @param parentObject	The parent object (for condition evaluation and field evaluation purposes).
	 * @param currentObject	The current object that holds the values.
	 * @throws BoxonException	If a codec is not found.
	 */
	void encode(Template<?> template, BitWriterInterface writer, Object parentObject, Object currentObject) throws BoxonException;

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation has validation problems.
	 */
	<T> Template<T> createTemplate(Class<T> type) throws AnnotationException;

}
