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
import io.github.mtrevisan.boxon.helpers.GenericHelper;

import java.lang.reflect.Type;
import java.util.List;


/**
 * A validation strategy that ensures the converter types and field types are non-null and appropriately matched.
 * <p>
 * This class implements the {@link ValidationStrategy} interface and overrides the validate method to perform type checks between the
 * field type, the converter's input and output types, and the binding type.
 * </p>
 */
class NonNullConverterValidationStrategy implements ValidationStrategy{

	@Override
	public final void validate(final Class<?> fieldType, final Class<? extends Converter<?, ?>> converter, final Class<?> bindingType)
		throws AnnotationException{
		final List<Type> inOutTypes = GenericHelper.resolveGenericTypes(converter);
		final Class<?> inputType = FieldAccessor.extractFieldType((Class<?>)inOutTypes.getFirst());
		final Class<?> outputType = (Class<?>)inOutTypes.getLast();

		if(!ValidationHelper.validateTypes(inputType, bindingType))
			throw AnnotationException.create("Type mismatch between annotation output ({}) and converter input ({})",
				bindingType.getSimpleName(), inputType.getSimpleName());

		if(!ValidationHelper.validateTypes(fieldType, outputType))
			throw AnnotationException.create("Type mismatch between converter output ({}) and field type ({})",
				outputType.getSimpleName(), fieldType.getSimpleName());
	}

}
