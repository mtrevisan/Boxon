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

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcessField;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindDouble;
import io.github.mtrevisan.boxon.annotations.bindings.BindFloat;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindList;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeSubField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Descriptors of the various binding annotations.
 */
public enum AnnotationDescriptor{

	/**
	 * Descriptor of the {@link TemplateHeader} annotation.
	 */
	TEMPLATE_HEADER(TemplateHeader.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final TemplateHeader binding = (TemplateHeader)annotation;
			putIfNotEmpty(DescriberKey.HEADER_START, Arrays.toString(binding.start()), rootDescription);
			putIfNotEmpty(DescriberKey.HEADER_END, binding.end(), rootDescription);
			putIfNotEmpty(DescriberKey.HEADER_CHARSET, binding.charset(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindObject} annotation.
	 */
	OBJECT(BindObject.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindObject binding = (BindObject)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TYPE, binding.type(), rootDescription);
			describeChoices(binding.selectFrom(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_DEFAULT, binding.selectDefault(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindArrayPrimitive} annotation.
	 */
	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TYPE, binding.type(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindArray} annotation.
	 */
	ARRAY(BindArray.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindArray binding = (BindArray)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TYPE, binding.type(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			describeChoices(binding.selectFrom(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_DEFAULT, binding.selectDefault(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindList} annotation.
	 */
	LIST_SEPARATED(BindList.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindList binding = (BindList)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TYPE, binding.type(), rootDescription);
			describeChoices(binding.selectFrom(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindBitSet} annotation.
	 */
	BIT_SET(BindBitSet.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindBitSet binding = (BindBitSet)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindByte} annotation.
	 */
	BYTE(BindByte.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindByte binding = (BindByte)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindShort} annotation.
	 */
	SHORT(BindShort.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindShort binding = (BindShort)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindInt} annotation.
	 */
	INT(BindInt.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindInt binding = (BindInt)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindInteger} annotation.
	 */
	INTEGER(BindInteger.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindInteger binding = (BindInteger)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindLong} annotation.
	 */
	LONG(BindLong.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindLong binding = (BindLong)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindFloat} annotation.
	 */
	FLOAT(BindFloat.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindFloat binding = (BindFloat)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindDouble} annotation.
	 */
	DOUBLE(BindDouble.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindDouble binding = (BindDouble)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindString} annotation.
	 */
	STRING(BindString.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindString binding = (BindString)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CHARSET, binding.charset(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link BindStringTerminated} annotation.
	 */
	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CHARSET, binding.charset(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TERMINATOR, binding.terminator(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CONSUME_TERMINATOR, binding.consumeTerminator(), rootDescription);
			describeValidator(binding.validator(), rootDescription);
			describeConverter(binding.converter(), rootDescription);
			describeAlternatives(binding.selectConverterFrom().alternatives(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link Skip} annotation.
	 */
	SKIP(Skip.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final Skip binding = (Skip)annotation;
			putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, Skip.class, rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TERMINATOR, binding.terminator(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CONSUME_TERMINATOR, binding.consumeTerminator(), rootDescription);
		}
	},


	/**
	 * Descriptor of the {@link Checksum} annotation.
	 */
	CHECKSUM(Checksum.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final Checksum binding = (Checksum)annotation;
			putIfNotEmpty(DescriberKey.BIND_TYPE, binding.type(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SKIP_START, binding.skipStart(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SKIP_END, binding.skipEnd(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_ALGORITHM, binding.algorithm().getName(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_START_VALUE, binding.startValue(), rootDescription);
		}
	},


	/**
	 * Descriptor of the {@link Evaluate} annotation.
	 */
	EVALUATE(Evaluate.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final Evaluate binding = (Evaluate)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_VALUE, binding.value(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link PostProcessField} annotation.
	 */
	POST_PROCESS_FIELD(PostProcessField.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final PostProcessField binding = (PostProcessField)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_VALUE_DECODE, binding.valueDecode(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_VALUE_ENCODE, binding.valueEncode(), rootDescription);
		}
	},


	/**
	 * Descriptor of the {@link ConfigurationHeader} annotation.
	 */
	CONFIG_HEADER(ConfigurationHeader.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final ConfigurationHeader binding = (ConfigurationHeader)annotation;
			putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, binding.shortDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.HEADER_START, binding.start(), rootDescription);
			putIfNotEmpty(ConfigurationKey.HEADER_END, binding.end(), rootDescription);
			putIfNotEmpty(ConfigurationKey.HEADER_CHARSET, binding.charset(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link ConfigurationField} annotation.
	 */
	CONFIG_FIELD(ConfigurationField.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final ConfigurationField binding = (ConfigurationField)annotation;
			putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, binding.shortDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, binding.unitOfMeasure(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MIN_VALUE, binding.minValue(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MAX_VALUE, binding.maxValue(), rootDescription);
			putIfNotEmpty(ConfigurationKey.PATTERN, binding.pattern(), rootDescription);
			if(binding.enumeration() != NullEnum.class)
				putIfNotEmpty(ConfigurationKey.ENUMERATION, binding.enumeration(), rootDescription);
			putIfNotEmpty(ConfigurationKey.DEFAULT_VALUE, binding.defaultValue(), rootDescription);
			putIfNotEmpty(ConfigurationKey.CHARSET, binding.charset(), rootDescription);
			putIfNotEmpty(ConfigurationKey.RADIX, binding.radix(), rootDescription);
			putIfNotEmpty(ConfigurationKey.TERMINATOR, binding.terminator(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link CompositeConfigurationField} annotation.
	 */
	COMPOSITE_CONFIG_FIELD(CompositeConfigurationField.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final CompositeConfigurationField binding = (CompositeConfigurationField)annotation;
			putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, binding.shortDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			describeComposite(binding.value(), rootDescription);
			putIfNotEmpty(ConfigurationKey.PATTERN, binding.pattern(), rootDescription);
			putIfNotEmpty(ConfigurationKey.COMPOSITION, binding.composition(), rootDescription);
			putIfNotEmpty(ConfigurationKey.CHARSET, binding.charset(), rootDescription);
			putIfNotEmpty(ConfigurationKey.TERMINATOR, binding.terminator(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link AlternativeConfigurationField} annotation.
	 */
	ALTERNATIVE_CONFIG_FIELD(AlternativeConfigurationField.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final AlternativeConfigurationField binding = (AlternativeConfigurationField)annotation;
			putIfNotEmpty(ConfigurationKey.SHORT_DESCRIPTION, binding.shortDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.LONG_DESCRIPTION, binding.longDescription(), rootDescription);
			putIfNotEmpty(ConfigurationKey.UNIT_OF_MEASURE, binding.unitOfMeasure(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(ConfigurationKey.MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			describeAlternatives(binding.value(), rootDescription);
			putIfNotEmpty(ConfigurationKey.VALUE, binding.value(), rootDescription);
			if(binding.enumeration() != NullEnum.class)
				putIfNotEmpty(ConfigurationKey.ENUMERATION, binding.enumeration(), rootDescription);
			putIfNotEmpty(ConfigurationKey.TERMINATOR, binding.terminator(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link Skip} annotation.
	 */
	CONFIG_SKIP(ConfigurationSkip.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final ConfigurationSkip binding = (ConfigurationSkip)annotation;
			putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, ConfigurationSkip.class, rootDescription);
			putIfNotEmpty(DescriberKey.BIND_MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TERMINATOR, binding.terminator(), rootDescription);
		}
	};


	private static final ValueOf<AnnotationDescriptor, Class<? extends Annotation>> DESCRIPTORS
		= ValueOf.create(AnnotationDescriptor.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	/**
	 * Create annotation descriptor from annotation.
	 *
	 * @param annotation	The annotation.
	 * @return	The instance.
	 */
	public static AnnotationDescriptor fromAnnotation(final Annotation annotation){
		return DESCRIPTORS.get(annotation.annotationType());
	}


	AnnotationDescriptor(final Class<? extends Annotation> type){
		annotationType = type;
	}


	/**
	 * Load a description of the given annotation in the given map.
	 *
	 * @param annotation	The annotation from which to extract the description.
	 * @param rootDescription	The map in which to load the description.
	 */
	public abstract void describe(Annotation annotation, Map<String, Object> rootDescription);

	/**
	 * Load a description of the given skip/configuration skip annotations in the given map.
	 *
	 * @param field	The field.
	 * @param rootDescription	The map in which to load the descriptions.
	 */
	public static <F, S extends Annotation> void describeSkips(final F field, final FieldExtractor<F, S> extractor,
			final Collection<Map<String, Object>> rootDescription){
		final S[] skips = extractor.getSkips(field);
		for(int i = 0, length = JavaHelper.lengthOrZero(skips); i < length; i ++){
			final S skip = skips[i];

			final Map<String, Object> skipDescription = new HashMap<>(1);
			fromAnnotation(skip)
				.describe(skip, skipDescription);
			rootDescription.add(skipDescription);
		}
	}

	private static void describeChoices(final ObjectChoices choices, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_PREFIX_LENGTH, choices.prefixLength(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	private static void describeAlternatives(final ObjectChoices.ObjectChoice[] alternatives,
			final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				final ObjectChoices.ObjectChoice alternative = alternatives[i];

				describeObjectChoicesAlternatives(alternative.condition(), alternative.prefix(), alternative.type(), alternativesDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
		}
	}

	private static void describeChoices(final ObjectChoicesList choices, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_CHARSET, choices.charset(), rootDescription);
		putIfNotEmpty(DescriberKey.BIND_TERMINATOR, choices.terminator(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	private static void describeAlternatives(final ObjectChoicesList.ObjectChoiceList[] alternatives,
		final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				final ObjectChoicesList.ObjectChoiceList alternative = alternatives[i];

				describeObjectChoicesAlternatives(alternative.condition(), alternative.prefix(), alternative.type(), alternativesDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
		}
	}

	private static <T> void describeObjectChoicesAlternatives(final String condition, final T prefix, final Class<?> type,
			final Collection<Map<String, Object>> alternativesDescription){
		final Map<String, Object> alternativeDescription = new HashMap<>(3);
		putIfNotEmpty(DescriberKey.BIND_CONDITION, condition, alternativeDescription);
		putIfNotEmpty(DescriberKey.BIND_PREFIX, prefix, alternativeDescription);
		putIfNotEmpty(DescriberKey.BIND_TYPE, type, alternativeDescription);
		alternativesDescription.add(alternativeDescription);
	}

	private static void describeValidator(final Class<? extends Validator<?>> validator, final Map<String, Object> rootDescription){
		if(validator != NullValidator.class)
			rootDescription.put(DescriberKey.BIND_VALIDATOR.toString(), validator.getName());
	}

	private static void describeConverter(final Class<? extends Converter<?, ?>> converter, final Map<String, Object> rootDescription){
		if(converter != NullConverter.class)
			rootDescription.put(DescriberKey.BIND_CONVERTER.toString(), converter.getName());
	}

	private static void describeAlternatives(final ConverterChoices.ConverterChoice[] alternatives,
			final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				final ConverterChoices.ConverterChoice alternative = alternatives[i];

				final Map<String, Object> alternativeDescription = new HashMap<>(2);
				putIfNotEmpty(DescriberKey.BIND_CONDITION, alternative.condition(), alternativeDescription);
				describeConverter(alternative.converter(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
		}
	}

	private static void describeAlternatives(final AlternativeSubField[] alternatives,
		final Map<String, Object> rootDescription){
		final int length = alternatives.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				final AlternativeSubField alternative = alternatives[i];

				describeObjectChoicesAlternatives(alternative, alternativesDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
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

	private static void describeComposite(final CompositeSubField[] composites, final Map<String, Object> rootDescription){
		final int length = composites.length;
		if(length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(length);
			for(int i = 0; i < length; i ++){
				final CompositeSubField composite = composites[i];

				describeFieldComposite(composite, alternativesDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
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
			map.put(key.toString(), value);
	}

	/**
	 * Put the pair key-value into the given map if the value is not {@code null}.
	 *
	 * @param key	The key.
	 * @param type	The class whose simple name will be the value.
	 * @param map	The map in which to load the key-value pair.
	 */
	private static void putIfNotEmpty(final Enum<?> key, final Class<?> type, final Map<String, Object> map){
		if(type != null)
			map.put(key.toString(), type.getSimpleName());
	}

}
