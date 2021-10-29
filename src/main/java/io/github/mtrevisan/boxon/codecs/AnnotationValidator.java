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

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.ParserDataType;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;


enum AnnotationValidator{

	OBJECT(BindObject.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			validateObjectChoice(selectFrom, binding.selectDefault(), type);
		}
	},

	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			final Class<?> type = binding.type();
			if(!ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), ParserDataType.toObjectiveTypeOrSelf(type).getSimpleName());
		}
	},

	ARRAY(BindArray.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArrayPrimitive.class.getSimpleName(), ParserDataType.toPrimitiveTypeOrSelf(type).getSimpleName());

			validateObjectChoice(selectFrom, binding.selectDefault(), type);
		}
	},

	DECIMAL(BindDecimal.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindDecimal binding = (BindDecimal)annotation;
			final Class<?> type = binding.type();
			final ParserDataType dataType = ParserDataType.fromType(type);
			if(dataType != ParserDataType.FLOAT && dataType != ParserDataType.DOUBLE)
				throw AnnotationException.create("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(),
					Double.class.getSimpleName());
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			CodecHelper.assertValidCharset(binding.charset());
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			CodecHelper.assertValidCharset(binding.charset());
		}
	},

	CHECKSUM(Checksum.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((Checksum)annotation).type();
			if(!ParserDataType.isPrimitiveOrWrapper(type))
				throw AnnotationException.create("Unrecognized type for field {}.{}: {}", getClass().getName(), type.getSimpleName(),
					type.getComponentType().getSimpleName());
		}
	};


	@InjectEventListener
	private static final EventListener eventListener = EventListener.getNoOpInstance();

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

	private static void validateObjectChoice(final ObjectChoices selectFrom, final Class<?> selectDefault, final Class<?> type)
			throws AnnotationException{
		final int prefixSize = selectFrom.prefixSize();
		validatePrefixSize(prefixSize);

		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		validateObjectAlternatives(alternatives, type, prefixSize);

		validateObjectDefaultAlternative(alternatives, type, selectDefault);
	}

	private static void validatePrefixSize(final int prefixSize) throws AnnotationException{
		if(prefixSize < 0)
			throw AnnotationException.create("Prefix size must be a non-negative number");
		if(prefixSize > Integer.SIZE)
			throw AnnotationException.create("Prefix size cannot be greater than {} bits", Integer.SIZE);
	}

	private static void validateObjectAlternatives(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final int prefixSize) throws AnnotationException{
		final boolean hasPrefixSize = (prefixSize > 0);
		if(hasPrefixSize && alternatives.length == 0)
			throw AnnotationException.create("No alternatives present");
		for(int i = 0; i < alternatives.length; i ++)
			validateAlternative(alternatives[i], type, hasPrefixSize);
	}

	private static void validateAlternative(final ObjectChoices.ObjectChoice alternative, final Class<?> type,
			final boolean hasPrefixSize) throws AnnotationException{
		if(!type.isAssignableFrom(alternative.type()))
			throw AnnotationException.create("Type of alternative cannot be assigned to (super) type of annotation");

		final String condition = alternative.condition();
		if(condition.isEmpty())
			throw AnnotationException.create("All conditions must be non-empty");
		if(hasPrefixSize ^ CodecHelper.containsPrefixReference(condition))
			throw AnnotationException.create("All conditions must {}contain a reference to the prefix", (hasPrefixSize? "": "not "));
	}

	private static void validateObjectDefaultAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternatives.length == 0)
			eventListener.uselessAlternative(selectDefault.getSimpleName());
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw AnnotationException.create("Type of default alternative cannot be assigned to (super) type of annotation");
	}

}
