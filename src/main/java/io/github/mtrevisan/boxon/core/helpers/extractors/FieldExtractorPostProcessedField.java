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

import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;

import java.lang.annotation.Annotation;


/**
 * Implements methods to extract field information from an {@link EvaluatedField<PostProcess>} object.
 */
public final class FieldExtractorPostProcessedField implements FieldExtractor<EvaluatedField<PostProcess>>{

	@Override
	public SkipParams[] getSkips(final EvaluatedField<PostProcess> field){
		return null;
	}

	@Override
	public Annotation getBinding(final EvaluatedField<PostProcess> field){
		return field.getBinding();
	}

	@Override
	public Annotation getCollectionBinding(final EvaluatedField<PostProcess> field){
		return null;
	}

	@Override
	public String getFieldName(final EvaluatedField<PostProcess> field){
		return field.getFieldName();
	}

	@Override
	public Class<?> getFieldType(final EvaluatedField<PostProcess> field){
		return field.getFieldType();
	}

}
