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
package io.github.mtrevisan.boxon.codecs.managers;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.external.codecs.ParserDataType;

import java.lang.annotation.Annotation;


public enum TemplateAnnotationValidator{

	OBJECT(BindObject.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			final ObjectChoices selectFrom = binding.selectFrom();
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
					BindArray.class.getSimpleName(), type.getSimpleName());
		}
	},

	ARRAY(BindArray.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final Class<?> type = binding.type();
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArrayPrimitive.class.getSimpleName(), type.getSimpleName());

			final ObjectChoices selectFrom = binding.selectFrom();
			validateObjectChoice(selectFrom, binding.selectDefault(), type);
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			ValidationHelper.assertValidCharset(binding.charset());
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			ValidationHelper.assertValidCharset(binding.charset());
		}
	},

	CHECKSUM(Checksum.class){
		@Override
		void validate(final Annotation annotation) throws AnnotationException{
			final Class<?> type = ((Checksum)annotation).type();
			final ParserDataType dataType = ParserDataType.fromType(type);
			if(dataType != ParserDataType.BYTE && dataType != ParserDataType.SHORT)
				throw AnnotationException.create("Unrecognized type, must be `byte` or `short`: {}", type.getSimpleName(),
					type.getComponentType().getSimpleName());
		}
	};


	private static final String EMPTY_STRING = "";

	private static final ValueOf<TemplateAnnotationValidator, Class<? extends Annotation>> VALIDATORS
		= ValueOf.create(TemplateAnnotationValidator.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	TemplateAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	static TemplateAnnotationValidator fromAnnotation(final Annotation annotation){
		return VALIDATORS.get(annotation.annotationType());
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
		if(hasPrefixSize ^ ContextHelper.containsPrefixReference(condition))
			throw AnnotationException.create("All conditions must {}contain a reference to the prefix",
				(hasPrefixSize? EMPTY_STRING: "not "));
	}

	private static void validateObjectDefaultAlternative(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> type,
			final Class<?> selectDefault) throws AnnotationException{
		if(selectDefault != void.class && alternatives.length == 0)
			throw AnnotationException.create("Useless empty alternative");
		if(selectDefault != void.class && !type.isAssignableFrom(selectDefault))
			throw AnnotationException.create("Type of default alternative cannot be assigned to (super) type of annotation");
	}

}
