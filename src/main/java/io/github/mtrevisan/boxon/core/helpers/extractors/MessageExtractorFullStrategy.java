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
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;


/**
 * A strategy for extracting full information from a {@link Template} message.
 * <p>
 * This class extends {@link MessageExtractorBasicStrategy} to provide additional methods for retrieving evaluated and post-processed
 * fields, as well as enumeration fields.
 * </p>
 */
public final class MessageExtractorFullStrategy extends MessageExtractorBasicStrategy{

	@Override
	public List<EvaluatedField<Evaluate>> getEvaluatedFields(final Template<?> message){
		return message.getEvaluatedFields();
	}

	@Override
	public List<EvaluatedField<PostProcess>> getPostProcessedFields(final Template<?> message){
		return message.getPostProcessedFields();
	}

	@Override
	public Collection<TemplateField> getEnumerations(final Template<?> message){
		final List<TemplateField> templateFields = message.getTemplateFields();
		final int length = templateFields.size();
		final Collection<TemplateField> enumerations = new HashSet<>(length);
		for(int i = 0; i < length; i ++){
			final TemplateField templateField = templateFields.get(i);

			final Class<?> fieldType = FieldAccessor.extractFieldType(templateField.getFieldType());
			if(fieldType.isEnum() && ConfigurationEnum.class.isAssignableFrom(fieldType))
				enumerations.add(templateField);
		}
		return enumerations;
	}

}
