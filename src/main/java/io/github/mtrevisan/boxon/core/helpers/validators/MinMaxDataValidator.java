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
package io.github.mtrevisan.boxon.core.helpers.validators;

import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.math.BigDecimal;


/**
 * A collection of convenience methods for working with validations.
 */
final class MinMaxDataValidator{

	private static final String VALUE_TYPE_DATA = "Data";
	private static final String VALUE_TYPE_DEFAULT = "Default";


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

		validateArrayFieldType(configData.getFieldType());

		final BigDecimal min = validateMinValue(configData);
		final BigDecimal max = validateMaxValue(configData);
		validateMinMaxValues(min, max, configData);

		validateDefaultAgainstMinAndMax(min, max, configData);

		validateDataAgainstMinAndMax(min, max, dataValue, configData);
	}

	private static boolean isMinAndMaxValuesBlank(final ConfigFieldData configData){
		return (StringHelper.isBlank(configData.getMinValue()) && StringHelper.isBlank(configData.getMaxValue()));
	}

	private static void validateArrayFieldType(final Class<?> fieldType) throws AnnotationException{
		if(fieldType.isArray())
			throw AnnotationException.create("Array field should not have `minValue` or `maxValue`");
	}

	private static void validateDefaultAgainstMinAndMax(final BigDecimal min, final BigDecimal max, final ConfigFieldData configData)
			throws AnnotationException{
		final BigDecimal def = JavaHelper.convertToBigDecimal(configData.getDefaultValue());

		checkMinimumCompatibility(def, min, configData, VALUE_TYPE_DEFAULT);
		checkMaximumCompatibility(def, max, configData, VALUE_TYPE_DEFAULT);
	}

	private static void validateDataAgainstMinAndMax(final BigDecimal min, final BigDecimal max, final Object dataValue,
			final ConfigFieldData configData) throws AnnotationException{
		if(isStringAssignableFrom(dataValue.getClass()) && !StringHelper.isBlank((String)dataValue)){
			final BigDecimal val = JavaHelper.convertToBigDecimal((String)dataValue);

			checkMinimumCompatibility(val, min, configData, VALUE_TYPE_DATA);
			checkMaximumCompatibility(val, max, configData, VALUE_TYPE_DATA);
		}
	}

	private static void checkMinimumCompatibility(final BigDecimal val, final BigDecimal min, final ConfigFieldData configData,
			final String valueType) throws AnnotationException{
		if(min != null && isValueLessThanMinimum(val, min))
			//`dataValue` compatible with `minValue`
			throw AnnotationException.create(valueType + " value incompatible with minimum value in {}; expected {} >= {}",
				configData.getAnnotationName(), configData.getDefaultValue(), configData.getMinValue().getClass().getSimpleName());
	}

	private static void checkMaximumCompatibility(final BigDecimal val, final BigDecimal max, final ConfigFieldData configData,
			final String valueType) throws AnnotationException{
		if(max != null && isValueGreaterThanMaximum(val, max))
			//`dataValue` compatible with `maxValue`
			throw AnnotationException.create(valueType + " value incompatible with maximum value in {}; expected {} <= {}",
				configData.getAnnotationName(), configData.getDefaultValue(), configData.getMaxValue().getClass().getSimpleName());
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

	private static boolean isValueLessThanMinimum(final Comparable<BigDecimal> val, final BigDecimal min){
		return (val != null && val.compareTo(min) < 0);
	}

	private static boolean isValueGreaterThanMaximum(final Comparable<BigDecimal> val, final BigDecimal max){
		return (val != null && val.compareTo(max) > 0);
	}


	private static boolean isStringAssignableFrom(final Class<?> cls){
		return String.class.isAssignableFrom(cls);
	}

}
