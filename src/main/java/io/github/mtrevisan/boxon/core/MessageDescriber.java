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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.MessageExtractor;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.ContextHelper;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Class for describing messages and entities.
 */
class MessageDescriber{

	private final Core core;


	/**
	 * Create a describer.
	 *
	 * @param core	The parser core.
	 * @return	A message describer.
	 */
	static MessageDescriber create(final Core core){
		return new MessageDescriber(core);
	}


	private MessageDescriber(final Core core){
		this.core = core;
	}


	/**
	 * Returns a description of a message.
	 *
	 * @param message	The message to describe.
	 * @param messageExtractor	The message extractor to use.
	 * @param fieldExtractor	The field extractor to use.
	 * @return	A map containing the description of the message.
	 * @param <M>	The type of the message.
	 * @param <F>	The type of the message.
	 */
	<M, F> Map<String, Object> describeMessage(final M message, final MessageExtractor<M, ? extends Annotation, F> messageExtractor,
			final FieldExtractor<F> fieldExtractor){
		final Map<String, Object> description = new HashMap<>(6);

		describeContext(description);

		final Annotation header = messageExtractor.getHeader(message);
		FieldDescriber.extractObjectParameters(header, header.annotationType(), description, DescriberKey.HEADER.toString());

		FieldDescriber.describeRawMessage(message, messageExtractor, fieldExtractor, description);
		return Collections.unmodifiableMap(description);
	}

	/**
	 * Describes the context by populating the given map with the context information.
	 *
	 * @param description	The map where the context description will be populated.
	 */
	private void describeContext(final Map<String, Object> description){
		final Map<String, Object> ctx = new HashMap<>(core.getContext());
		ctx.remove(ContextHelper.CONTEXT_SELF);
		ctx.remove(ContextHelper.CONTEXT_CHOICE_PREFIX);
		FieldDescriber.putIfNotEmpty(DescriberKey.CONTEXT, Collections.unmodifiableMap(ctx), description);
	}

}
