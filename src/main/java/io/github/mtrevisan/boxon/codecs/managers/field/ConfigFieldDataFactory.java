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

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;

import java.lang.reflect.Field;


public final class ConfigFieldDataFactory{

	private ConfigFieldDataFactory(){}

	public static ConfigFieldData<ConfigurationField> buildData(final Field field, final ConfigurationField annotation){
		final ConfigFieldData<ConfigurationField> data = new ConfigFieldData<>();
		data.field = field;
		data.minProtocol = annotation.minProtocol();
		data.maxProtocol = annotation.maxProtocol();
		data.minValue = annotation.minValue();
		data.maxValue = annotation.maxValue();
		data.pattern = annotation.pattern();
		data.enumeration = annotation.enumeration();
		data.defaultValue = annotation.defaultValue();
		data.charset = annotation.charset();
		data.radix = annotation.radix();
		data.annotation = ConfigurationField.class;
		return data;
	}


	public static ConfigFieldData<CompositeConfigurationField> buildData(final Field field, final CompositeConfigurationField annotation){
		final ConfigFieldData<CompositeConfigurationField> data = new ConfigFieldData<>();
		data.field = field;
		data.minProtocol = annotation.minProtocol();
		data.maxProtocol = annotation.maxProtocol();
		data.pattern = annotation.pattern();
		data.charset = annotation.charset();
		data.annotation = CompositeConfigurationField.class;
		return data;
	}

	public static ConfigFieldData<CompositeSubField> buildData(final Field field, final CompositeSubField annotation){
		final ConfigFieldData<CompositeSubField> data = new ConfigFieldData<>();
		data.field = field;
		data.pattern = annotation.pattern();
		data.defaultValue = annotation.defaultValue();
		data.annotation = CompositeSubField.class;
		return data;
	}


	public static ConfigFieldData<AlternativeConfigurationField> buildData(final Field field,
			final AlternativeConfigurationField annotation){
		final ConfigFieldData<AlternativeConfigurationField> data = new ConfigFieldData<>();
		data.field = field;
		data.minProtocol = annotation.minProtocol();
		data.maxProtocol = annotation.maxProtocol();
		data.enumeration = annotation.enumeration();
		data.annotation = AlternativeConfigurationField.class;
		return data;
	}

	public static ConfigFieldData<AlternativeSubField> buildData(final Field field, final AlternativeSubField annotation){
		final ConfigFieldData<AlternativeSubField> data = new ConfigFieldData<>();
		data.field = field;
		data.minProtocol = annotation.minProtocol();
		data.maxProtocol = annotation.maxProtocol();
		data.minValue = annotation.minValue();
		data.maxValue = annotation.maxValue();
		data.pattern = annotation.pattern();
		data.defaultValue = annotation.defaultValue();
		data.charset = annotation.charset();
		data.radix = annotation.radix();
		data.annotation = AlternativeSubField.class;
		return data;
	}

}
