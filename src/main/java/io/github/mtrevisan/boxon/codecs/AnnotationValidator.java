/**
 * Copyright (c) 2020 Mauro Trevisan
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

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;


enum AnnotationValidator{

	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((BindArrayPrimitive)annotation).type();
			if(!ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), ParserDataType.toObjectiveTypeOrDefault(type).getSimpleName());
		}
	},

	ARRAY(BindArray.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArrayPrimitive.class.getSimpleName(), ParserDataType.toPrimitiveTypeOrDefault(type).getSimpleName());

			validateChoice(selectFrom, binding.selectDefault(), type);
		}
	},

	OBJECT(BindObject.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw new AnnotationException("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			validateChoice(selectFrom, binding.selectDefault(), type);
		}
	},

	DECIMAL(BindDecimal.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((BindDecimal)annotation).type();
			final ParserDataType dataType = ParserDataType.fromType(type);
			if(dataType != ParserDataType.FLOAT && dataType != ParserDataType.DOUBLE)
				throw new AnnotationException("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(),
					Double.class.getSimpleName());
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			CodecHelper.assertCharset(binding.charset());
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			CodecHelper.assertCharset(binding.charset());
		}
	},

	CHECKSUM(Checksum.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((Checksum)annotation).type();
			if(!ParserDataType.isPrimitiveOrWrapper(type))
				throw new AnnotationException("Unrecognized type for field {}.{}: {}", getClass().getName(), type.getSimpleName(),
					type.getComponentType().getSimpleName());
		}
	};


	private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationValidator.class);

	private static final Map<Class<? extends Annotation>, AnnotationValidator> ANNOTATION_VALIDATORS = new HashMap<>(5);
	static{
		for(final AnnotationValidator validator : values())
			ANNOTATION_VALIDATORS.put(validator.annotationType, validator);
	}

	private final Class<? extends Annotation> annotationType;


	AnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static AnnotationValidator fromAnnotation(final Annotation annotation){
		return ANNOTATION_VALIDATORS.get(annotation.annotationType());
	}

	abstract void validate(final Annotation annotation) throws AnnotationException;

	private static void validateChoice(final ObjectChoices selectFrom, final Class<?> selectDefault, final Class<?> type)
			throws AnnotationException{
		final int prefixSize = selectFrom.prefixSize();
		validatePrefixSize(prefixSize);

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		validateAlternatives(alternatives, type, prefixSize);

		validateDefaultAlternative(alternatives, type, selectDefault);
	}

	private static void validatePrefixSize(final int prefixSize) throws AnnotationException{
		if(prefixSize < 0)
			throw new AnnotationException("Prefix size must be a non-negative number");
		if(prefixSize > Integer.SIZE)
			throw new AnnotationException("Prefix size cannot be greater than {} bits", Integer.SIZE);
	}

	private static void validateAlternatives(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final int prefixSize) throws AnnotationException{
		final boolean hasPrefixSize = (prefixSize > 0);
		if(hasPrefixSize && alternatives.length == 0)
			throw new AnnotationException("No alternatives present");
		for(final ObjectChoices.ObjectChoice alternative : alternatives)
			validateAlternative(alternative, type, hasPrefixSize);
	}

	private static void validateAlternative(final ObjectChoices.ObjectChoice alternative, final Class<?> type,
			final boolean hasPrefixSize) throws AnnotationException{
		if(!type.isAssignableFrom(alternative.type()))
			throw new AnnotationException("Type of alternative cannot be assigned to (super) type of annotation");

		final String condition = alternative.condition();
		if(condition.isEmpty())
			throw new AnnotationException("All conditions must be non-empty");
		if(hasPrefixSize ^ CodecHelper.containsPrefixReference(condition))
			throw new AnnotationException("All conditions must " + (hasPrefixSize? "": "not ") + "contain a reference to the prefix");
	}

	private static void validateDefaultAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternatives.length == 0)
			LOGGER.warn("Useless definition of default alternative ({}) due to no alternatives present on @BindArray or @BindObject",
				selectDefault.getSimpleName());
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw new AnnotationException("Type of default alternative cannot be assigned to (super) type of annotation");
	}

}
