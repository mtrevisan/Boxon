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
package io.github.mtrevisan.boxon.core.helpers.validators;

import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.util.List;


/**
 * Implements {@link ValidationStrategy} to validate the types for fields and converters ensuring compatibility.
 */
class NullConverterValidationStrategy implements ValidationStrategy{

	@Override
	public final void validate(Class<?> fieldType, final Class<? extends Converter<?, ?>> converter, final Class<?> bindingType)
			throws AnnotationException{
		fieldType = (fieldType != List.class
			? FieldAccessor.extractFieldType(fieldType)
			: bindingType
		);
		if(!ValidationHelper.validateTypes(fieldType, bindingType))
			throw AnnotationException.create("Type mismatch between annotation output ({}) and field type ({})",
				bindingType.getSimpleName(), fieldType.getSimpleName());
	}

}
