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
package io.github.mtrevisan.boxon.external.codecs;

import io.github.mtrevisan.boxon.exceptions.FieldException;

import java.lang.annotation.Annotation;


/**
 * The interface every codec should implement.
 *
 * @param <B>	The bind annotation associated with this codec.
 */
public interface CodecInterface<B extends Annotation>{

	/**
	 * Decode the next field of a message.
	 *
	 * @param reader	The reader that holds the raw data of the message (to be interpreted), positioned at a certain index.
	 * @param annotation	The annotation that links what have to be read and the variable of the POJO.
	 * @param rootObject	The parent object that holds what have been read so far.
	 * @return	The object with the new value read and interpreted.
	 * @throws FieldException	If something bad happened while reading, validating, or converting the raw value.
	 */
	Object decode(BitReader reader, Annotation annotation, Object rootObject) throws FieldException;

	/**
	 * Encode the next field of a message.
	 *
	 * @param writer	The writer, positioned at a certain index, in which the value will be put.
	 * @param annotation	The annotation that links what have to be read and the variable of the POJO.
	 * @param rootObject	The parent object that holds what have been read so far.
	 * @param value	The value that have to be encoded.
	 * @throws FieldException	If something bad happened while converting, validating, or writing the value.
	 */
	void encode(BitWriterInterface writer, Annotation annotation, Object rootObject, Object value) throws FieldException;


	/**
	 * Interpret the annotation as the data type indicated in the generic of this codec.
	 *
	 * @param annotation	The generic annotation to be interpreted.
	 * @return	The casted annotation.
	 */
	@SuppressWarnings("unchecked")
	default B extractBinding(final Annotation annotation){
		return (B)annotation;
	}

}
