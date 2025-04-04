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

import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;

import java.lang.annotation.Annotation;


/**
 * Implements the {@link FieldExtractor} interface for extracting information from a {@link TemplateField} object.
 * <p>
 * This strategy provides methods to retrieve various metadata associated with a field in a template, such as skips, bindings, collection
 * bindings, field name, and field type.
 * </p>
 */
public final class FieldExtractorStrategy implements FieldExtractor<TemplateField>{

	@Override
	public SkipParams[] getSkips(final TemplateField field){
		return field.getSkips();
	}

	@Override
	public Annotation getBinding(final TemplateField field){
		return field.getBinding();
	}

	@Override
	public Annotation getCollectionBinding(final TemplateField field){
		return field.getCollectionBinding();
	}

	@Override
	public String getFieldName(final TemplateField field){
		return field.getFieldName();
	}

	@Override
	public Class<?> getFieldType(final TemplateField field){
		return field.getFieldType();
	}

}
