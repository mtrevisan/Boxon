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
package io.github.mtrevisan.boxon.core.helpers.extractors;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationEnum;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.core.helpers.configurations.ConfigurationMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;


public final class MessageExtractorConfiguration extends MessageExtractor<ConfigurationMessage<?>, ConfigurationHeader,
		ConfigurationField>{

	@Override
	public String getTypeName(final ConfigurationMessage<?> message){
		return message.getType()
			.getName();
	}

	@Override
	public ConfigurationHeader getHeader(final ConfigurationMessage<?> message){
		return message.getHeader();
	}

	@Override
	public List<ConfigurationField> getFields(final ConfigurationMessage<?> message){
		return message.getConfigurationFields();
	}

	@Override
	public Collection<ConfigurationField> getEnumerations(final ConfigurationMessage<?> message){
		final List<ConfigurationField> configurationFields = message.getConfigurationFields();
		final int length = configurationFields.size();
		final Collection<ConfigurationField> enumerations = new HashSet<>(length);
		for(int i = 0; i < length; i ++){
			final ConfigurationField configurationField = configurationFields.get(i);

			final Class<?> fieldType = FieldAccessor.extractFieldType(configurationField.getFieldType());
			if(fieldType.isEnum() && ConfigurationEnum.class.isAssignableFrom(fieldType))
				enumerations.add(configurationField);
		}
		return enumerations;
	}

}
