/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.validators;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindDouble;
import io.github.mtrevisan.boxon.annotations.bindings.BindFloat;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.descriptions.DescriberKey;
import io.github.mtrevisan.boxon.core.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Descriptors of the various binding annotations.
 */
public enum AnnotationDescriptor{

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
	 * Descriptor of the {@link BindBitSet} annotation.
	 */
	BIT_SET(BindBitSet.class){
		@Override
		public void describe(final Annotation annotation, final Map<String, Object> rootDescription){
			final BindBitSet binding = (BindBitSet)annotation;
			putIfNotEmpty(DescriberKey.BIND_CONDITION, binding.condition(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, binding.size(), rootDescription);
			putIfNotEmpty(DescriberKey.BIND_BIT_ORDER, binding.bitOrder(), rootDescription);
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
			putIfNotEmpty(DescriberKey.BIND_ALGORITHM, binding.algorithm(), rootDescription);
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
	};


	private static final ValueOf<AnnotationDescriptor, Class<? extends Annotation>> VALIDATORS
		= ValueOf.create(AnnotationDescriptor.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	/**
	 * Create annotation descriptor from annotation.
	 *
	 * @param annotation	The annotation.
	 * @return	The instance.
	 */
	public static AnnotationDescriptor fromAnnotation(final Annotation annotation){
		return VALIDATORS.get(annotation.annotationType());
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
	public abstract void describe(final Annotation annotation, final Map<String, Object> rootDescription);

	/**
	 * Load a description of the given skip annotations in the given map.
	 *
	 * @param skips	The skip annotations from which to extract the description.
	 * @param rootDescription	The map in which to load the descriptions.
	 */
	public static void describeSkips(final Skip[] skips, final Collection<Map<String, Object>> rootDescription){
		for(int j = 0; j < skips.length; j ++){
			final Skip skip = skips[j];
			final Map<String, Object> skipDescription = new HashMap<>(5);
			putIfNotEmpty(DescriberKey.ANNOTATION_TYPE, Skip.class, skipDescription);
			putIfNotEmpty(DescriberKey.BIND_CONDITION, skip.condition(), skipDescription);
			putIfNotEmpty(DescriberKey.BIND_SIZE, skip.size(), skipDescription);
			putIfNotEmpty(DescriberKey.BIND_TERMINATOR, skip.terminator(), skipDescription);
			putIfNotEmpty(DescriberKey.BIND_CONSUME_TERMINATOR, skip.consumeTerminator(), skipDescription);
			rootDescription.add(skipDescription);
		}
	}

	private static void describeChoices(final ObjectChoices choices, final Map<String, Object> rootDescription){
		putIfNotEmpty(DescriberKey.BIND_PREFIX_SIZE, choices.prefixSize(), rootDescription);
		putIfNotEmpty(DescriberKey.BIND_BIT_ORDER, choices.bitOrder(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	private static void describeAlternatives(final ObjectChoices.ObjectChoice[] alternatives,
			final Map<String, Object> rootDescription){
		if(alternatives.length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(alternatives.length);
			for(int j = 0; j < alternatives.length; j ++){
				final ObjectChoices.ObjectChoice alternative = alternatives[j];
				final Map<String, Object> alternativeDescription = new HashMap<>(3);
				putIfNotEmpty(DescriberKey.BIND_CONDITION, alternative.condition(), alternativeDescription);
				putIfNotEmpty(DescriberKey.BIND_PREFIX, alternative.prefix(), alternativeDescription);
				putIfNotEmpty(DescriberKey.BIND_TYPE, alternative.type(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
		}
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
		if(alternatives.length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(alternatives.length);
			for(int j = 0; j < alternatives.length; j ++){
				final ConverterChoices.ConverterChoice alternative = alternatives[j];
				final Map<String, Object> alternativeDescription = new HashMap<>(2);
				putIfNotEmpty(DescriberKey.BIND_CONDITION, alternative.condition(), alternativeDescription);
				describeConverter(alternative.converter(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			rootDescription.put(DescriberKey.BIND_SELECT_CONVERTER_FROM.toString(), alternativesDescription);
		}
	}

	/**
	 * Put the pair key-value into the given map.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 * @param map	The map in which to load the key-value pair.
	 */
	public static void putIfNotEmpty(final DescriberKey key, final Object value,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map){
		if(value != null && (!(value instanceof String) || !StringHelper.isBlank((CharSequence)value)))
			map.put(key.toString(), value);
	}

	/**
	 * Put the pair key-value into the given map.
	 *
	 * @param key	The key.
	 * @param type	The class whose simple name will be the value.
	 * @param map	The map in which to load the key-value pair.
	 */
	public static void putIfNotEmpty(final DescriberKey key, final Class<?> type,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map){
		if(type != null)
			map.put(key.toString(), type.getSimpleName());
	}

}
