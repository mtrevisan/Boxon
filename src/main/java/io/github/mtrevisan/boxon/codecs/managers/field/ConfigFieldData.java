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
package io.github.mtrevisan.boxon.codecs.managers.field;

import io.github.mtrevisan.boxon.external.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;


/** Data associated to an annotated field. */
public final class ConfigFieldData<T extends Annotation>{

	public Field field;

	public String minProtocol;
	public String maxProtocol;

	public String minValue;
	public String maxValue;

	public String pattern;
	public Class<? extends ConfigurationEnum> enumeration;

	public String defaultValue;

	public String charset;

	public int radix;

	public Class<T> annotation;


	public static <T extends Annotation> ConfigFieldData<T> create(){
		return new ConfigFieldData<>();
	}

	private ConfigFieldData(){}

	public Class<?> getFieldType(){
		return field.getType();
	}

	public boolean hasEnumeration(){
		return hasEnumeration(enumeration);
	}

	public static boolean hasEnumeration(final Class<? extends ConfigurationEnum> enumeration){
		return (enumeration != NullEnum.class);
	}

}
