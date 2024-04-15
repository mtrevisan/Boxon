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
package io.github.mtrevisan.boxon.core.helpers.descriptors;

import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.Descriptor;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public final class AnnotationDescriptorHelper{

	private AnnotationDescriptorHelper(){}


	static void describeChoices(final ObjectChoices choices, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_PREFIX_LENGTH, choices.prefixLength(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	static void describeChoices(final ObjectChoicesList choices, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_CHARSET, choices.charset(), rootDescription);
		putIfNotEmpty(DescriberKey.BIND_TERMINATOR, choices.terminator(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	private static void describeAlternatives(final ObjectChoices.ObjectChoice[] alternatives, final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final ObjectChoices.ObjectChoice alternative : alternatives)
				describeObjectChoicesAlternatives(alternative.condition(), alternative.prefix(), alternative.type(), alternativesDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_CONVERTER_FROM, alternativesDescription, rootDescription);
		}
	}

	private static void describeObjectChoicesAlternatives(final String condition, final Object prefix, final Class<?> type,
			final Collection<Map<String, Object>> alternativesDescription){
		final Map<String, Object> alternativeDescription = new HashMap<>(3);
		putIfNotEmpty(DescriberKey.BIND_CONDITION, condition, alternativeDescription);
		putIfNotEmpty(DescriberKey.BIND_PREFIX, prefix, alternativeDescription);
		describeType(type, alternativeDescription);
		alternativesDescription.add(alternativeDescription);
	}

	static void describeType(final Class<?> type, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_TYPE, type, rootDescription);

		if(isUserDefinedClass(type)){
			try{
				final List<Map<String, Object>> typeDescription = new ArrayList<>(1);
				final Collection<Class<?>> processedTypes = new HashSet<>(1);
				Class<?> parent = type;
				while(parent != null && parent != Object.class && !processedTypes.contains(parent)){
					typeDescription.addFirst(Descriptor.describeRawMessage(parent));

					processedTypes.add(parent);

					//go up to parent class
					parent = parent.getSuperclass();
				}
				putIfNotEmpty(DescriberKey.BIND_SUBTYPES, typeDescription, rootDescription);
			}
			catch(final FieldException fe){
				final Exception exception = ConfigurationException.create("Cannot handle this type of class: {}, please report to the developer",
					type.getSimpleName(), fe.getMessage());
				System.out.println("Boxon ERROR: " + exception.getMessage());
			}
		}
	}

	private static boolean isUserDefinedClass(final Class<?> cls){
		//check if the class is not an interface, an anonymous class, or a primitive data type
		return (!cls.isInterface() && !cls.isAnonymousClass() && !cls.isPrimitive());
	}

	static void describeValidator(final Class<? extends Validator<?>> validator, final Map<String, Object> rootDescription){
		if(validator != NullValidator.class)
			putIfNotEmpty(DescriberKey.BIND_VALIDATOR, validator, rootDescription);
	}

	static void describeConverter(final Class<? extends Converter<?, ?>> converter, final Map<String, Object> rootDescription){
		if(converter != NullConverter.class)
			putIfNotEmpty(DescriberKey.BIND_CONVERTER, converter, rootDescription);
	}

	static void describeAlternatives(final ConverterChoices.ConverterChoice[] alternatives, final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final ConverterChoices.ConverterChoice alternative : alternatives){
				final Map<String, Object> alternativeDescription = new HashMap<>(2);
				putIfNotEmpty(DescriberKey.BIND_CONDITION, alternative.condition(), alternativeDescription);
				describeConverter(alternative.converter(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			putIfNotEmpty(DescriberKey.BIND_SELECT_CONVERTER_FROM, alternativesDescription, rootDescription);
		}
	}

	static void describeAlternatives(final AlternativeSubField[] alternatives,
		final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final AlternativeSubField alternative : alternatives)
				describeObjectChoicesAlternatives(alternative, alternativesDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_CONVERTER_FROM, alternativesDescription, rootDescription);
		}
	}

	private static void describeObjectChoicesAlternatives(final AlternativeSubField alternative,
			final Collection<Map<String, Object>> alternativesDescription){
		final Map<String, Object> alternativeDescription = new HashMap<>(10);
		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, alternative.longDescription(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, alternative.unitOfMeasure(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, alternative.minProtocol(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, alternative.maxProtocol(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.MIN_VALUE, alternative.minValue(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.MAX_VALUE, alternative.maxValue(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.PATTERN, alternative.pattern(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, alternative.defaultValue(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.CHARSET, alternative.charset(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.RADIX, alternative.radix(), alternativeDescription);
		alternativesDescription.add(alternativeDescription);
	}

	static void describeComposite(final CompositeSubField[] composites, final Map<String, Object> rootDescription){
		final int length = composites.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(final CompositeSubField composite : composites)
				describeFieldComposite(composite, alternativesDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_CONVERTER_FROM, alternativesDescription, rootDescription);
		}
	}

	private static void describeFieldComposite(final CompositeSubField composite,
			final Collection<Map<String, Object>> alternativesDescription){
		final Map<String, Object> alternativeDescription = new HashMap<>(5);
		putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, composite.shortDescription(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, composite.longDescription(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, composite.unitOfMeasure(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.PATTERN, composite.pattern(), alternativeDescription);
		putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, composite.defaultValue(), alternativeDescription);
		alternativesDescription.add(alternativeDescription);
	}

	/**
	 * Put the pair key-value into the given map if the value is not {@code null} or empty string.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 */
	public static void putIfNotEmpty(final Enum<?> key, final Object value, final Map<String, Object> map){
		if(value != null
				&& !(value instanceof final String v && StringHelper.isBlank(v))
				&& !(value instanceof final Collection<?> c && c.isEmpty())
			)
			map.put(key.toString(), (value instanceof final Class<?> cls? cls.getName(): value));
	}

}
