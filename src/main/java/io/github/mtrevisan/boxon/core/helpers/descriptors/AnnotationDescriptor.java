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
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
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
import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.annotations.configurations.NullEnum;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.core.helpers.extractors.FieldExtractor;
import io.github.mtrevisan.boxon.core.helpers.extractors.SkipParams;
import io.github.mtrevisan.boxon.core.keys.ConfigurationKey;
import io.github.mtrevisan.boxon.core.keys.DescriberKey;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeAlternatives;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeChoices;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeComposite;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeConverter;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeType;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.describeValidator;
import static io.github.mtrevisan.boxon.core.helpers.descriptors.AnnotationDescriptorHelper.putIfNotEmpty;


//FIXME class too long, too cluttered, have too duplications... too anything
/**
 * Descriptors of the various binding annotations.
 */
public enum AnnotationDescriptor{

	/**
	 * Descriptor of the {@link TemplateHeader} annotation.
	 */
	TEMPLATE_HEADER(TemplateHeader.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final BindObject binding = (BindObject)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			describeType(binding.type(), rootDescription);
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			describeType(binding.type(), rootDescription);
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final BindArray binding = (BindArray)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			describeType(binding.type(), rootDescription);
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final BindList binding = (BindList)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			describeType(binding.type(), rootDescription);
			describeChoices(binding.selectFrom(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SELECT_DEFAULT, binding.selectDefault(), rootDescription);
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
	 * Descriptor of the {@link SkipBits} and {@link SkipUntilTerminator} annotation.
	 */
	SKIP(SkipParams.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final SkipParams skipParams = (SkipParams)annotation;
			putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, skipParams.annotationType(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_CONDITION, skipParams.condition(), rootDescription);
			if(skipParams.annotationType() == SkipBits.class)
				putIfNotEmpty(DescriberKey.BIND_SIZE, skipParams.size(), rootDescription);
			else{
				putIfNotEmpty(DescriberKey.BIND_TERMINATOR, skipParams.terminator(), rootDescription);
				putIfNotEmpty(DescriberKey.BIND_CONSUME_TERMINATOR, skipParams.consumeTerminator(), rootDescription);
			}
		}
	},


	/**
	 * Descriptor of the {@link Checksum} annotation.
	 */
	CHECKSUM(Checksum.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final Checksum binding = (Checksum)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BYTE_ORDER, binding.byteOrder(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SKIP_START, binding.skipStart(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SKIP_END, binding.skipEnd(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_ALGORITHM, binding.algorithm(), rootDescription);
		}
	},


	/**
	 * Descriptor of the {@link Evaluate} annotation.
	 */
	EVALUATE(Evaluate.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final Evaluate binding = (Evaluate)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_VALUE, binding.value(), rootDescription);
		}
	},

	/**
	 * Descriptor of the {@link PostProcess} annotation.
	 */
	POST_PROCESS_FIELD(PostProcess.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final PostProcess binding = (PostProcess)annotation;
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
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
	 * Descriptor of the {@link ConfigurationSkip} annotation.
	 */
	CONFIG_SKIP(ConfigurationSkip.class){
		@Override
		public <S> void describe(final S annotation, final Map<String, Object> rootDescription){
			final ConfigurationSkip binding = (ConfigurationSkip)annotation;
			putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, ConfigurationSkip.class, rootDescription);
			putIfNotEmpty(DescriberKey.BIND_MIN_PROTOCOL, binding.minProtocol(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_MAX_PROTOCOL, binding.maxProtocol(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_TERMINATOR, binding.terminator(), rootDescription);
		}
	};


	private static final ValueOf<AnnotationDescriptor, Class<?>> DESCRIPTORS
		= ValueOf.create(AnnotationDescriptor.class, validator -> validator.annotationType);


	private final Class<?> annotationType;


	/**
	 * Create annotation descriptor from annotation.
	 *
	 * @param annotation	The annotation.
	 * @return	The instance.
	 */
	public static AnnotationDescriptor fromAnnotation(final Annotation annotation){
		return DESCRIPTORS.get(annotation.annotationType());
	}

	/**
	 * Create annotation descriptor from annotation.
	 *
	 * @param skip	The skip annotation.
	 * @return	The instance.
	 */
	private static AnnotationDescriptor fromAnnotation(final SkipParams skip){
		return DESCRIPTORS.get(skip.getClass());
	}


	AnnotationDescriptor(final Class<?> type){
		annotationType = type;
	}


	/**
	 * Load a description of the given annotation in the given map.
	 *
	 * @param annotation	The annotation from which to extract the description.
	 * @param rootDescription	The map in which to load the description.
	 */
	public abstract <S> void describe(S annotation, Map<String, Object> rootDescription);

	/**
	 * Load a description of the given skip/configuration skip annotations in the given map.
	 *
	 * @param field	The field.
	 * @param rootDescription	The map in which to load the descriptions.
	 */
	public static <F> void describeSkips(final F field, final FieldExtractor<F> extractor,
			final Collection<Map<String, Object>> rootDescription){
		final SkipParams[] skips = extractor.getSkips(field);
		for(int i = 0, length = JavaHelper.sizeOrZero(skips); i < length; i ++){
			final SkipParams skip = skips[i];

			final Map<String, Object> skipDescription = new HashMap<>(1);
			final AnnotationDescriptor annotationDescriptor = fromAnnotation(skip);
			annotationDescriptor.describe(skip, skipDescription);
			rootDescription.add(skipDescription);
		}
	}

	public static AnnotationDescriptor checkAndGetDescriptor(final Annotation binding) throws FieldException{
		final AnnotationDescriptor descriptor = fromAnnotation(binding);
		if(descriptor == null)
			throw FieldException.create("Cannot extract descriptor for this annotation: {}",
				binding.annotationType().getSimpleName());

		return descriptor;
	}

}
