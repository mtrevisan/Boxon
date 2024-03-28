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
package io.github.mtrevisan.boxon.core.helpers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.helpers.ThrowingFunction;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.semanticversioning.Version;
import io.github.mtrevisan.boxon.semanticversioning.VersionException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Pattern;


/**
 * A collection of convenience methods for working with validations.
 */
final class ValidationHelper{

	private static final char MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR = '|';

	private static final ThrowingFunction<String, Pattern, Exception> PATTERN_STORE = Memoizer.throwingMemoize(Pattern::compile);


	private ValidationHelper(){}


	/**
	 * Validate the protocol.
	 *
	 * @param field	The configuration field data.
	 * @param minProtocolVersion	The minimum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @param maxProtocolVersion	The maximum protocol version (should follow <a href="https://semver.org/">Semantic Versioning</a>).
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validateProtocol(final ConfigFieldData field, final Version minProtocolVersion, final Version maxProtocolVersion)
			throws AnnotationException{
		final Version minimum = validateProtocol(field.getMinProtocol(), field,
			"Invalid minimum protocol version in {}; found {}");
		final Version maximum = validateProtocol(field.getMaxProtocol(), field,
			"Invalid maximum protocol version in {}; found {}");

		//`maxProtocol` must be after or equal to `minProtocol`
		if(minimum != null && maximum != null && maximum.isLessThan(minimum))
			throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {} >= {}",
				field.getAnnotationName(), field.getMinProtocol(), field.getMaxProtocol());

		//`minProtocol` must be after or equal to `minProtocolVersion`
		//NOTE: `minimum.isLessThan(minProtocolVersion)` return false if `minProtocolVersion` is empty
		if(minimum != null && minimum.isLessThan(minProtocolVersion))
			throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; expected {} >= {}",
				field.getAnnotationName(), minimum, minProtocolVersion);
		//`maxProtocol` must be before or equal to `maxProtocolVersion`
		if(maximum != null && !maxProtocolVersion.isEmpty() && maxProtocolVersion.isLessThan(maximum))
			throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; expected {} <= {}",
				field.getAnnotationName(), maximum, maxProtocolVersion);
	}

	private static Version validateProtocol(final String protocolVersion, final ConfigFieldData field, final String errorMessage)
			throws AnnotationException{
		Version protocol = null;
		if(!StringHelper.isBlank(protocolVersion)){
			try{
				protocol = Version.of(protocolVersion);
			}
			catch(final VersionException ve){
				throw AnnotationException.create(ve, errorMessage, field.getAnnotationName(), protocolVersion);
			}
		}
		return protocol;
	}


	/**
	 * Validate the minimum and maximum values.
	 *
	 * @param field	The configuration field data.
	 * @param dataValue	The value to check against.
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validateMinMaxDataValues(final ConfigFieldData field, final Object dataValue) throws AnnotationException{
		if(StringHelper.isBlank(field.getMinValue()) && StringHelper.isBlank(field.getMaxValue()))
			return;

		final Class<?> fieldType = field.getFieldType();
		if(fieldType.isArray())
			throw AnnotationException.create("Array field should not have `minValue` or `maxValue`");

		final BigDecimal def = JavaHelper.convertToBigDecimal(field.getDefaultValue());
		validateMinMaxValues(field, def);

		if(dataValue != null && String.class.isAssignableFrom(dataValue.getClass())){
			final BigDecimal val = JavaHelper.convertToBigDecimal((String)dataValue);
			validateMinValue(field, val);
			validateMaxValue(field, val);
		}
	}

	private static void validateMinMaxValues(final ConfigFieldData field, final BigDecimal def) throws AnnotationException{
		final BigDecimal min = validateMinValue(field, def);
		final BigDecimal max = validateMaxValue(field, def);

		if(min != null && max != null && min.compareTo(max) > 0)
			//`maxValue` after or equal to `minValue`
			throw AnnotationException.create("Minimum value should be less than or equal to maximum value in {}; expected {} <= {}",
				field.getAnnotationName(), field.getMinValue(), field.getMaxValue());
	}

	private static BigDecimal validateMinValue(final ConfigFieldData field, final BigDecimal def) throws AnnotationException{
		BigDecimal min = null;
		final String minValue = field.getMinValue();
		if(!StringHelper.isBlank(minValue)){
			min = JavaHelper.convertToBigDecimal(minValue);
			//`minValue` compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected a valid number",
					field.getAnnotationName(), minValue);

			if(def != null && def.compareTo(min) < 0)
				//`defaultValue` compatible with `minValue`
				throw AnnotationException.create("Default value incompatible with minimum value in {}; expected {} >= {}",
					field.getAnnotationName(), field.getDefaultValue(), minValue.getClass().getSimpleName());
		}
		return min;
	}

	private static BigDecimal validateMaxValue(final ConfigFieldData field, final BigDecimal def) throws AnnotationException{
		BigDecimal max = null;
		final String maxValue = field.getMaxValue();
		if(!StringHelper.isBlank(maxValue)){
			max = JavaHelper.convertToBigDecimal(maxValue);
			//`maxValue` compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected a valid number",
					field.getAnnotationName(), maxValue);

			if(def != null && def.compareTo(max) > 0)
				//`defaultValue` compatible with `maxValue`
				throw AnnotationException.create("Default value incompatible with maximum value in {}; expected {} <= {}",
					field.getAnnotationName(), field.getDefaultValue(), maxValue.getClass().getSimpleName());
		}
		return max;
	}


	/**
	 * Validate the default value.
	 *
	 * @param field	The configuration field data.
	 * @throws AnnotationException	If a validation error occurs.
	 * @throws CodecException	If the value cannot be interpreted as primitive or objective.
	 */
	static void validateDefaultValue(final ConfigFieldData field) throws AnnotationException, CodecException{
		final Class<?> fieldType = field.getFieldType();

		final String defaultValue = field.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue)){
			//`defaultValue` compatible with variable type
			final boolean hasEnumeration = field.hasEnumeration();
			if(!hasEnumeration && ParserDataType.getValue(fieldType, defaultValue) == null)
				throw AnnotationException.create("Incompatible enum in {}, found {}, expected {}",
					field.getAnnotationName(), defaultValue.getClass().getSimpleName(), fieldType.toString());
			if(hasEnumeration && !fieldType.isArray()
					&& StringHelper.contains(defaultValue, MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR))
				throw AnnotationException.create("Incompatible default value in {}, field {}, found '{}', expected mutually exclusive value",
					field.getAnnotationName(), defaultValue.getClass().getSimpleName(), defaultValue);
		}
		//if `defaultValue` is not present, then field type must be an object
		else if(ParserDataType.isPrimitive(fieldType))
			throw AnnotationException.create("Default must be present for primitive type in {}, found {}, expected {}",
				field.getAnnotationName(), fieldType.getSimpleName(), fieldType.getSimpleName());
	}

	private static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final ConfigFieldData field)
			throws AnnotationException{
		//`defaultValue` compatible with `pattern`
		if(!matchesOrBlank(field.getDefaultValue(), formatPattern))
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getDefaultValue(), formatPattern.pattern());
		//`minValue` compatible with `pattern`
		if(!matchesOrBlank(field.getMinValue(), formatPattern))
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getMinValue(), formatPattern.pattern());
		//`maxValue` compatible with `pattern`
		if(!matchesOrBlank(field.getMaxValue(), formatPattern))
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				field.getAnnotationName(), field.getMaxValue(), formatPattern.pattern());
	}

	private static boolean matchesOrBlank(final String text, final Pattern pattern){
		return (StringHelper.isBlank(text) || matches(text, pattern));
	}

	static boolean matches(final CharSequence text, final Pattern pattern){
		return pattern.matcher(text)
			.matches();
	}


	/**
	 * Validate the pattern.
	 *
	 * @param field	The configuration field data.
	 * @param dataValue	The value to check against.
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validatePattern(final ConfigFieldData field, final Object dataValue) throws AnnotationException{
		//valid pattern
		final String pattern = field.getPattern();
		if(StringHelper.isBlank(pattern))
			return;

		final Pattern formatPattern = extractPattern(pattern, field);

		//`defaultValue` compatible with field type
		if(!String.class.isAssignableFrom(field.getFieldType())
				|| dataValue != null && String.class.isAssignableFrom(dataValue.getClass()) && !matches((String)dataValue, formatPattern))
			throw AnnotationException.create("Data type not compatible with `pattern` in {}; found {}.class, expected complying with {}",
				field.getAnnotationName(), field.getFieldType(), pattern);

		validateMinMaxDefaultValuesToPattern(formatPattern, field);
	}

	private static Pattern extractPattern(final String pattern, final ConfigFieldData field) throws AnnotationException{
		try{
			return PATTERN_STORE.apply(pattern);
		}
		catch(final Exception e){
			throw AnnotationException.create("Invalid pattern in {} in field {}", field.getAnnotationName(), field.getFieldName(),
				e);
		}
	}


	/**
	 * Validate the enumeration.
	 *
	 * @param field	The configuration field data.
	 * @throws AnnotationException	If a validation error occurs.
	 */
	static void validateEnumeration(final ConfigFieldData field) throws AnnotationException{
		if(!field.hasEnumeration())
			return;

		//enumeration can be encoded
		final Class<? extends ConfigurationEnum> enumeration = field.getEnumeration();
		if(!ConfigurationEnum.class.isAssignableFrom(enumeration))
			throw AnnotationException.create("Enumeration must implement interface {} in {} in field {}",
				ConfigurationEnum.class.getSimpleName(), field.getAnnotationName(), field.getFieldName());

		//non-empty enumeration
		final ConfigurationEnum[] enumConstants = enumeration.getEnumConstants();
		if(enumConstants.length == 0)
			throw AnnotationException.create("Empty enum in {} in field {}", field.getAnnotationName(), field.getFieldName());

		final Class<?> fieldType = field.getFieldType();
		if(fieldType.isArray())
			validateEnumerationMultipleValues(field, enumConstants);
		else
			validateEnumerationMutuallyExclusive(field, enumConstants);
	}

	private static void validateEnumerationMultipleValues(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		final Class<?> fieldType = field.getFieldType();
		if(!fieldType.getComponentType().isAssignableFrom(field.getEnumeration()))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.getEnumeration().getSimpleName(), fieldType.toString());

		//default value(s) compatible with enumeration
		validateEnumerationCompatibility(field, enumConstants);
	}

	private static void validateEnumerationCompatibility(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		final String[] defaultValues = StringHelper.split(field.getDefaultValue(), MUTUALLY_EXCLUSIVE_ENUMERATION_SEPARATOR);
		for(int i = 0, length = defaultValues.length; i < length; i ++){
			final ConfigurationEnum enumValue = ConfigurationEnum.extractEnum(enumConstants, defaultValues[i]);
			if(enumValue == null)
				throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
					field.getAnnotationName(), defaultValues[i], Arrays.toString(enumConstants));
		}
	}

	private static void validateEnumerationMutuallyExclusive(final ConfigFieldData field, final ConfigurationEnum[] enumConstants)
			throws AnnotationException{
		//enumeration compatible with variable type
		final Class<?> fieldType = field.getFieldType();
		if(!fieldType.isAssignableFrom(field.getEnumeration()))
			throw AnnotationException.create("Incompatible enum in {}; found {}, expected {}",
				field.getAnnotationName(), field.getEnumeration().getSimpleName(), fieldType.toString());

		final String defaultValue = field.getDefaultValue();
		if(!StringHelper.isBlank(defaultValue) && ConfigurationEnum.extractEnum(enumConstants, defaultValue) == null)
			throw AnnotationException.create("Default value not compatible with `enumeration` in {}; found {}, expected one of {}",
				field.getAnnotationName(), defaultValue, Arrays.toString(enumConstants));
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
