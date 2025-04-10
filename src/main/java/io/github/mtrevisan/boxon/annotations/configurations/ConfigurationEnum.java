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
package io.github.mtrevisan.boxon.annotations.configurations;


/**
 * Interface every enumeration used in a configuration MUST implement.
 *
 * @param <T>	The type of the code associated with the enumeration.
 */
public interface ConfigurationEnum<T>{

	/**
	 * The code associated with this numeration value.
	 *
	 * @return	The code for this enumeration.
	 */
	T getCode();

	/**
	 * Name of the value.
	 * <p>
	 * NOTE: already implemented in an enum! Normally, it is not necessary to extend it.
	 * </p>
	 *
	 * @return	The name of the value.
	 */
	String name();


	/**
	 * Convert a text value into an enumeration constant.
	 *
	 * @param enumConstants	The array of possible constants.
	 * @param value	The value to be converted.
	 * @return	The enumeration constant that matches the value.
	 */
	static ConfigurationEnum<?> extractEnum(final ConfigurationEnum<?>[] enumConstants, final String value){
		for(int i = 0, length = enumConstants.length; i < length; i ++){
			final ConfigurationEnum<?> enumConstant = enumConstants[i];
			if(enumConstant.name().equals(value))
				return enumConstant;
		}
		return null;
	}

}
