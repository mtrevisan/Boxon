/*
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.helpers.extractors;

import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;

import java.util.Collection;
import java.util.List;


/**
 * Abstract class to extract information from messages.
 *
 * @param <M>	The type of the message.
 * @param <H>	The type of the header.
 * @param <F>	The type of the fields.
 */
public abstract class MessageExtractor<M, H, F>{

	/**
	 * Returns the type name of a given message.
	 *
	 * @param message	the message for which to retrieve the type name.
	 * @return	The type name of the given message.
	 */
	public abstract String getTypeName(M message);

	/**
	 * Retrieves the header of a given message.
	 *
	 * @param message	The message for which to retrieve the header.
	 * @return	The header of the given message.
	 */
	public abstract H getHeader(M message);

	/**
	 * Retrieves the list of {@link io.github.mtrevisan.boxon.core.helpers.templates.TemplateField TemplateField}s from a given
	 * {@link io.github.mtrevisan.boxon.core.helpers.templates.Template Template} message, or the list of
	 * {@link io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField ConfigurationField}s from a given
	 * {@link io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage Configuration} message.
	 *
	 * @param message	The {@link io.github.mtrevisan.boxon.core.helpers.templates.Template Template} message from which to retrieve the
	 * 	{@link io.github.mtrevisan.boxon.core.helpers.templates.TemplateField TemplateField}s, or the
	 *    {@link io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage Configuration} message from which to retrieve the
	 * 	{@link io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField ConfigurationField}s.
	 * @return	The list of {@link io.github.mtrevisan.boxon.core.helpers.templates.TemplateField TemplateField}s or
	 * 	{@link io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField ConfigurationField}s.
	 */
	public abstract List<F> getFields(M message);

	/**
	 * Retrieves the evaluated fields from a given message template.
	 *
	 * @param message	The message template from which to retrieve the evaluated fields.
	 * @return	The list of evaluated fields in the message template.
	 */
	public List<EvaluatedField<Evaluate>> getEvaluatedFields(final M message){
		return null;
	}

	/**
	 * Retrieves the post-processed fields from a given message template.
	 *
	 * @param message	The message template from which to retrieve the post-processed fields.
	 * @return	The list of post-processed fields in the message template.
	 */
	public List<EvaluatedField<PostProcess>> getPostProcessedFields(final M message){
		return null;
	}

	/**
	 * Retrieves the fields with an enumeration type from a given message configuration.
	 *
	 * @param message	The message configuration from which to retrieve the fields with enumeration type.
	 * @return	The list of fields with an enumeration type in the message configuration.
	 */
	public Collection<F> getEnumerations(final M message){
		return null;
	}

}
