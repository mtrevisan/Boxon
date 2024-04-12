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
package io.github.mtrevisan.boxon.core.helpers.configurations.validators;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.math.BigDecimal;


/**
 * A collection of convenience methods for working with validations.
 */
final class MinMaxDataValidator{

	private MinMaxDataValidator(){}


	/**
	 * Validate the minimum and maximum values.
	 *
	 * @param dataValue	The value to check against.
	 * @param configData	The configuration field data.
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validateMinMaxDataValues(final Object dataValue, final ConfigFieldData configData) throws AnnotationException{
		if(isMinAndMaxValuesBlank(configData))
			return;

		validateArrayFieldType(configData);

		final BigDecimal min = validateMinValue(configData);
		final BigDecimal max = validateMaxValue(configData);
		validateDefaultAgainstMinAndMax(min, max, configData);

		validateDataAgainstMinAndMax(min, max, dataValue, configData);
	}

	private static boolean isMinAndMaxValuesBlank(final ConfigFieldData configData){
		return (StringHelper.isBlank(configData.getMinValue()) && StringHelper.isBlank(configData.getMaxValue()));
	}

	private static void validateArrayFieldType(final ConfigFieldData configData) throws AnnotationException{
		final Class<?> fieldType = configData.getFieldType();
		if(fieldType.isArray())
			throw AnnotationException.create("Array field should not have `minValue` or `maxValue`");
	}

	private static void validateDefaultAgainstMinAndMax(final BigDecimal min, final BigDecimal max, final ConfigFieldData configData)
			throws AnnotationException{
		final BigDecimal def = JavaHelper.convertToBigDecimal(configData.getDefaultValue());
		validateMinMaxValues(min, max, configData);

		if(min != null && isDefaultLessThanMinimum(def, min))
			//`defaultValue` compatible with `minValue`
			throw AnnotationException.create("Default value incompatible with minimum value in {}; expected {} >= {}",
				configData.getAnnotationName(), configData.getDefaultValue(), configData.getMinValue().getClass().getSimpleName());
		if(max != null && isDefaultGreaterThanMaximum(def, max))
			//`defaultValue` compatible with `maxValue`
			throw AnnotationException.create("Default value incompatible with maximum value in {}; expected {} <= {}",
				configData.getAnnotationName(), configData.getDefaultValue(), configData.getMaxValue().getClass().getSimpleName());
	}

	private static void validateDataAgainstMinAndMax(final BigDecimal min, final BigDecimal max, final Object dataValue,
			final ConfigFieldData configData) throws AnnotationException{
		if(isStringAssignableFrom(dataValue.getClass()) && !StringHelper.isBlank((String)dataValue)){
			final BigDecimal val = JavaHelper.convertToBigDecimal((String)dataValue);

			if(min != null && isDefaultLessThanMinimum(val, min))
				//`dataValue` compatible with `minValue`
				throw AnnotationException.create("Data value incompatible with minimum value in {}; expected {} >= {}",
					configData.getAnnotationName(), configData.getDefaultValue(), configData.getMinValue().getClass().getSimpleName());
			if(max != null && isDefaultGreaterThanMaximum(val, max))
				//`dataValue` compatible with `maxValue`
				throw AnnotationException.create("Data value incompatible with maximum value in {}; expected {} <= {}",
					configData.getAnnotationName(), configData.getDefaultValue(), configData.getMaxValue().getClass().getSimpleName());
		}
	}

	private static void validateMinMaxValues(final BigDecimal min, final BigDecimal max, final ConfigFieldData configData)
			throws AnnotationException{
		if(isMinimumGraterThanMaximum(min, max))
			//`maxValue` after or equal to `minValue`
			throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; expected {} <= {}",
				configData.getAnnotationName(), configData.getMinValue(), configData.getMaxValue());
	}

	private static BigDecimal validateMinValue(final ConfigFieldData configData) throws AnnotationException{
		BigDecimal min = null;
		final String minValue = configData.getMinValue();
		if(!StringHelper.isBlank(minValue)){
			min = JavaHelper.convertToBigDecimal(minValue);
			//`minValue` compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected a valid number",
					configData.getAnnotationName(), minValue);
		}
		return min;
	}

	private static BigDecimal validateMaxValue(final ConfigFieldData configData) throws AnnotationException{
		BigDecimal max = null;
		final String maxValue = configData.getMaxValue();
		if(!StringHelper.isBlank(maxValue)){
			max = JavaHelper.convertToBigDecimal(maxValue);
			//`maxValue` compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected a valid number",
					configData.getAnnotationName(), maxValue);
		}
		return max;
	}

	private static boolean isMinimumGraterThanMaximum(final Comparable<BigDecimal> min, final BigDecimal max){
		return (min != null && max != null && min.compareTo(max) > 0);
	}

	private static boolean isDefaultLessThanMinimum(final Comparable<BigDecimal> def, final BigDecimal min){
		return (def != null && def.compareTo(min) < 0);
	}

	private static boolean isDefaultGreaterThanMaximum(final Comparable<BigDecimal> def, final BigDecimal max){
		return (def != null && def.compareTo(max) > 0);
	}


	private static boolean isStringAssignableFrom(final Class<?> cls){
		return String.class.isAssignableFrom(cls);
	}

}
