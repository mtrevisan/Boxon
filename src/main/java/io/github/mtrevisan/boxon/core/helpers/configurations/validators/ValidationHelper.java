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
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.math.BigDecimal;
import java.util.regex.Pattern;


/**
 * A collection of convenience methods for working with validations.
 */
public final class ValidationHelper{

	private static final ThrowingFunction<String, Pattern, Exception> PATTERN_STORE = Memoizer.throwingMemoize(Pattern::compile);


	private ValidationHelper(){}


	/**
	 * Validate the minimum and maximum values.
	 *
	 * @param dataValue  The value to check against.
	 * @param configData The configuration field data.
	 * @throws AnnotationException If a validation error occurs.
	 */
	static void validateMinMaxDataValues(final Object dataValue, final ConfigFieldData configData) throws AnnotationException{
		if(StringHelper.isBlank(configData.getMinValue()) && StringHelper.isBlank(configData.getMaxValue()))
			return;

		validateArrayFieldType(configData);

		final BigDecimal min = validateMinValue(configData);
		final BigDecimal max = validateMaxValue(configData);
		validateDefaultAgainstMinAndMax(min, max, configData);

		validateDataAgainstMinAndMax(min, max, dataValue, configData);
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

	private static boolean isMinimumGraterThanMaximum(final BigDecimal min, final BigDecimal max){
		return (min != null && max != null && min.compareTo(max) > 0);
	}

	private static boolean isDefaultLessThanMinimum(final BigDecimal def, final BigDecimal min){
		return (def != null && def.compareTo(min) < 0);
	}

	private static boolean isDefaultGreaterThanMaximum(final BigDecimal def, final BigDecimal max){
		return (def != null && def.compareTo(max) > 0);
	}


	/**
	 * Validate the default value.
	 *
	 * @param configData	The configuration field data.
	 * @throws AnnotationException	If a validation error occurs.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	static void validateDefaultValue(final ConfigFieldData configData) throws AnnotationException, CodecException{
		final Class<?> fieldType = configData.getFieldType();

		final String defaultValue = configData.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue)){
			//`defaultValue` compatible with variable type
			final boolean hasEnumeration = configData.hasEnumeration();
			if(!hasEnumeration && ParserDataType.getValue(fieldType, defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					configData.getAnnotationName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
			if(hasEnumeration && !fieldType.isArray()
					&& StringHelper.contains(defaultValue, EnumerationValidator.MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR))
				throw AnnotationException.create("Incompatible default value in {}, field {}, found '{}', expected mutually exclusive value",
					configData.getAnnotationName(), defaultValue.getClass().getSimpleName(), defaultValue);
		}
		//if `defaultValue` is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				configData.getAnnotationName(), fieldType.getSimpleName(), fieldType.getSimpleName());
	}

	private static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final ConfigFieldData configData)
			throws AnnotationException{
		//`defaultValue` compatible with `pattern`
		if(!matchesOrBlank(configData.getDefaultValue(), formatPattern))
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				configData.getAnnotationName(), configData.getDefaultValue(), formatPattern.pattern());
		//`minValue` compatible with `pattern`
		if(!matchesOrBlank(configData.getMinValue(), formatPattern))
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				configData.getAnnotationName(), configData.getMinValue(), formatPattern.pattern());
		//`maxValue` compatible with `pattern`
		if(!matchesOrBlank(configData.getMaxValue(), formatPattern))
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				configData.getAnnotationName(), configData.getMaxValue(), formatPattern.pattern());
	}

	private static boolean matchesOrBlank(final String text, final Pattern pattern){
		return (StringHelper.isBlank(text) || matches(text, pattern));
	}


	/**
	 * Validate the pattern.
	 *
	 * @param defaultValue  The field (default) value to check against.
	 * @param configData The configuration field data.
	 * @throws AnnotationException If a validation error occurs.
	 */
	static void validatePattern(final Object defaultValue, final ConfigFieldData configData) throws AnnotationException{
		//valid pattern
		final String pattern = configData.getPattern();
		if(StringHelper.isBlank(pattern))
			return;

		final Pattern formatPattern = extractPattern(pattern, configData);

		//`defaultValue` compatible with field type
		if(!isStringAssignableFrom(configData.getFieldType())
				|| defaultValue != null && isStringAssignableFrom(defaultValue.getClass()) && !((String)defaultValue).isEmpty()
				&& !matches((String)defaultValue, formatPattern))
			throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected complying with {}",
				configData.getAnnotationName(), configData.getFieldType(), pattern);

		validateMinMaxDefaultValuesToPattern(formatPattern, configData);
	}

	private static Pattern extractPattern(final String pattern, final ConfigFieldData configData) throws AnnotationException{
		try{
			return PATTERN_STORE.apply(pattern);
		}
		catch(final Exception e){
			throw AnnotationException.create("Invalid pattern in {} in field {}", configData.getAnnotationName(), configData.getFieldName(),
				e);
		}
	}

	private static boolean isStringAssignableFrom(final Class<?> cls){
		return String.class.isAssignableFrom(cls);
	}

	public static boolean matches(final CharSequence text, final Pattern pattern){
		return pattern.matcher(text)
			.matches();
	}


	/**
	 * Validate the radix.
	 *
	 * @param radix	The radix.
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validateRadix(final int radix) throws AnnotationException{
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
			throw AnnotationException.create("Radix must be in [{}, {}]", Character.MIN_RADIX, Character.MAX_RADIX);
	}

}
