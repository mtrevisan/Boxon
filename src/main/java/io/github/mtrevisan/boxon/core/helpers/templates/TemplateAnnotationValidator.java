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
package io.github.mtrevisan.boxon.core.helpers.templates;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindList;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.MethodHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.BitSet;
import java.util.List;


/**
 * Container of all the validators of a message template.
 */
enum TemplateAnnotationValidator{

	OBJECT(BindObject.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;
			final Class<?> type = binding.type();
			TemplateAnnotationValidatorHelper.validateType(type, BindObject.class);
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			TemplateAnnotationValidatorHelper.validateObjectChoice(field, converter, selectFrom, selectDefault, type);
		}
	},

	ARRAY_PRIMITIVE(BindArrayPrimitive.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;
			final Class<?> type = binding.type();
			if(!ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), type.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(field, type, converter);
		}
	},

	ARRAY(BindArray.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindArray binding = (BindArray)annotation;
			final Class<?> type = binding.type();
			TemplateAnnotationValidatorHelper.validateType(type, BindArray.class);
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `{}.class`",
					BindArray.class.getSimpleName(), type.getSimpleName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			TemplateAnnotationValidatorHelper.validateObjectChoice(field, converter, selectFrom, selectDefault, type);
		}
	},

	LIST_SEPARATED(BindList.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindList binding = (BindList)annotation;
			final Class<?> type = binding.type();
			final Class<?> fieldType = field.getType();
			if(!List.class.isAssignableFrom(fieldType))
				throw AnnotationException.create("Bad annotation used for {}, should have been used the type `List<{}>.class`",
					BindList.class.getSimpleName(), type.getName());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			final ObjectChoicesList selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			TemplateAnnotationValidatorHelper.validateObjectChoiceList(field, converter, selectFrom, selectDefault, type);
		}
	},

	BIT_SET(BindBitSet.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindBitSet binding = (BindBitSet)annotation;

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(field, BitSet.class, converter);
		}
	},

	STRING(BindString.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;
			TemplateAnnotationValidatorHelper.validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(field, String.class, converter);
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;
			TemplateAnnotationValidatorHelper.validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(field, String.class, converter);
		}
	},


	CHECKSUM(Checksum.class){
		@Override
		void validate(final Field field, final Annotation annotation) throws AnnotationException{
			final Class<? extends Checksummer> algorithmClass = ((Checksum)annotation).algorithm();
			if(algorithmClass.isInterface() || Modifier.isAbstract(algorithmClass.getModifiers())
					|| algorithmClass.isAssignableFrom(Checksummer.class))
				throw AnnotationException.create("Unrecognized algorithm, must be a class implementing `"
					+ Checksummer.class.getName() + "`: {}", algorithmClass.getSimpleName());

			final Method interfaceMethod = MethodHelper.getMethods(Checksummer.class)[0];
			final Class<?> interfaceReturnType = interfaceMethod.getReturnType();

			TemplateAnnotationValidatorHelper.validateConverter(field, interfaceReturnType, NullConverter.class);
		}
	};


	private static final ValueOf<TemplateAnnotationValidator, Class<? extends Annotation>> VALIDATORS
		= ValueOf.create(TemplateAnnotationValidator.class, validator -> validator.annotationType);


	private final Class<? extends Annotation> annotationType;


	TemplateAnnotationValidator(final Class<? extends Annotation> type){
		annotationType = type;
	}

	/**
	 * Get the validator for the given annotation.
	 *
	 * @param annotationType	The annotation class type.
	 * @return	The validator for the given annotation.
	 */
	static TemplateAnnotationValidator fromAnnotation(final Class<? extends Annotation> annotationType){
		return VALIDATORS.get(annotationType);
	}

	/**
	 * Validate field and annotation.
	 *
	 * @param field	The field associated to the annotation.
	 * @param annotation	The annotation.
	 * @throws AnnotationException	If an error is detected.
	 */
	abstract void validate(Field field, Annotation annotation) throws AnnotationException;

}
