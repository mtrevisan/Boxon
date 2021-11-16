/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.regex.Pattern;


final class ValidatorHelper{

	private ValidatorHelper(){}

	static void assertValidCharset(final String charsetName) throws AnnotationException{
		try{
			Charset.forName(charsetName);
		}
		catch(final IllegalArgumentException ignored){
			throw AnnotationException.create("Invalid charset: '{}'", charsetName);
		}
	}


	static void validateProtocol(final String minProtocol, final String maxProtocol, final Version minMessageProtocol,
			final Version maxMessageProtocol, final Class<? extends Annotation> binding) throws AnnotationException{
		if(!minProtocol.isEmpty() || !maxProtocol.isEmpty()){
			//minProtocol/maxProtocol are valid
			final Version minimum = validateMinMaxProtocol(minProtocol, binding, "Invalid minimum protocol version in {}; found {}");
			final Version maximum = validateMinMaxProtocol(maxProtocol, binding, "Invalid maximum protocol version in {}; found {}");
			//maxProtocol after or equal to minProtocol
			if(minimum != null && maximum != null && maximum.isLessThan(minimum))
				throw AnnotationException.create("Minimum protocol version is greater than maximum protocol version in {}; found {}",
					binding.getSimpleName(), maxProtocol);

			//minProtocol after or equal to minMessageProtocol
			if(minimum != null && !minMessageProtocol.isEmpty() && minimum.isLessThan(minMessageProtocol))
				throw AnnotationException.create("Minimum protocol version is less than whole message minimum protocol version in {}; found {}",
					binding.getSimpleName(), maxMessageProtocol);
			//maxProtocol before or equal to maxMessageProtocol
			if(maximum != null && !maxMessageProtocol.isEmpty() && maxMessageProtocol.isLessThan(maximum))
				throw AnnotationException.create("Maximum protocol version is greater than whole message maximum protocol version in {}; found {}",
					binding.getSimpleName(), maxMessageProtocol);
		}
	}

	private static Version validateMinMaxProtocol(final String protocolVersion, final Class<? extends Annotation> binding,
			final String errorMessage) throws AnnotationException{
		Version protocol = null;
		if(!protocolVersion.isEmpty()){
			try{
				protocol = Version.of(protocolVersion);
			}catch(final IllegalArgumentException iae){
				throw AnnotationException.create(iae, errorMessage, binding.getSimpleName(), protocolVersion);
			}
		}
		return protocol;
	}

	static Object validateMinValue(final Class<?> fieldType, final String minValue, final String defaultValue, final Object def)
			throws AnnotationException{
		Object min = null;
		if(!minValue.isEmpty()){
			min = JavaHelper.getValue(fieldType, minValue);
			//minValue compatible with variable type
			if(min == null)
				throw AnnotationException.create("Incompatible minimum value in {}; found {}, expected {}",
					ConfigurationField.class.getSimpleName(), minValue.getClass().getSimpleName(), fieldType.toString());

			if(def != null && ((Number)def).doubleValue() < ((Number)min).doubleValue())
				//defaultValue compatible with minValue
				throw AnnotationException.create("Default value incompatible with minimum value in {}; found {}, expected greater than or equals to {}",
					ConfigurationField.class.getSimpleName(), defaultValue, minValue.getClass().getSimpleName());
		}
		return min;
	}

	static Object validateMaxValue(final Class<?> fieldType, final String maxValue, final String defaultValue, final Object def)
			throws AnnotationException{
		Object max = null;
		if(!maxValue.isEmpty()){
			max = JavaHelper.getValue(fieldType, maxValue);
			//maxValue compatible with variable type
			if(max == null)
				throw AnnotationException.create("Incompatible maximum value in {}; found {}, expected {}",
					ConfigurationField.class.getSimpleName(), maxValue.getClass().getSimpleName(), fieldType.toString());

			if(StringHelper.isNumeric(defaultValue) && def != null && ((Number)def).doubleValue() > ((Number)max).doubleValue())
				//defaultValue compatible with maxValue
				throw AnnotationException.create("Default value incompatible with maximum value in {}; found {}, expected less than or equals to {}",
					ConfigurationField.class.getSimpleName(), defaultValue, maxValue.getClass().getSimpleName());
		}
		return max;
	}

	static void validateMinMaxDefaultValuesToPattern(final Pattern formatPattern, final String minValue, final String maxValue,
			final String defaultValue) throws AnnotationException{
		//defaultValue compatible with pattern
		if(!defaultValue.isEmpty() && !formatPattern.matcher(defaultValue).matches())
			throw AnnotationException.create("Default value not compatible with `pattern` in {}; found {}, expected {}",
				ConfigurationField.class.getSimpleName(), defaultValue, formatPattern.pattern());
		//minValue compatible with pattern
		if(!minValue.isEmpty() && !formatPattern.matcher(minValue).matches())
			throw AnnotationException.create("Minimum value not compatible with `pattern` in {}; found {}, expected {}",
				ConfigurationField.class.getSimpleName(), minValue, formatPattern.pattern());
		//maxValue compatible with pattern
		if(!maxValue.isEmpty() && !formatPattern.matcher(maxValue).matches())
			throw AnnotationException.create("Maximum value not compatible with `pattern` in {}; found {}, expected {}",
				ConfigurationField.class.getSimpleName(), maxValue, formatPattern.pattern());
	}

}
