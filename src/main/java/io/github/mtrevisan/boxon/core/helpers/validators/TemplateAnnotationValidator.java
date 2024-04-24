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
package io.github.mtrevisan.boxon.core.helpers.validators;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.core.helpers.ValueOf;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.MethodHelper;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;


/**
 * Container of all the validators of a message template.
 */
public enum TemplateAnnotationValidator{

	HEADER(TemplateHeader.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final TemplateHeader binding = (TemplateHeader)annotation;

			//assure uniqueness of the `start`s
			final String[] start = binding.start();
			if(start.length > 0 && hasDuplicates(start))
				throw AnnotationException.create("Wrong annotation parameter for {}: there are duplicates on `start` array {}",
					TemplateHeader.class.getSimpleName(), Arrays.toString(start));

			TemplateAnnotationValidatorHelper.validateCharset(binding.charset());
		}

		private static boolean hasDuplicates(final String[] array){
			final int length = array.length;
			final Collection<String> uniqueSet = new HashSet<>(length);
			for(int i = 0; i < length; i ++)
				if(!uniqueSet.add(array[i]))
					return true;
			return false;
		}
	},

	OBJECT(BindObject.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final BindObject binding = (BindObject)annotation;

			final Class<?> type = binding.type();
			TemplateAnnotationValidatorHelper.validateType(type, BindObject.class);
			if(ParserDataType.isPrimitive(type))
				throw AnnotationException.create("Wrong annotation used for {}, should have been used one of the primitive type's annotations",
					BindObject.class.getSimpleName());

			TemplateAnnotationValidatorHelper.validateObjectChoice(fieldType, binding);
			TemplateAnnotationValidatorHelper.validateObjectChoiceList(fieldType, binding);
		}
	},

	BIT_SET(BindBitSet.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final BindBitSet binding = (BindBitSet)annotation;

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(fieldType, converter, BitSet.class);
		}
	},

	INTEGER(BindInteger.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final BindInteger binding = (BindInteger)annotation;

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(fieldType, converter, BigInteger.class);
		}
	},

	STRING(BindString.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final BindString binding = (BindString)annotation;

			TemplateAnnotationValidatorHelper.validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(fieldType, converter, String.class);
		}
	},

	STRING_TERMINATED(BindStringTerminated.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			TemplateAnnotationValidatorHelper.validateCharset(binding.charset());

			final Class<? extends Converter<?, ?>> converter = binding.converter();
			TemplateAnnotationValidatorHelper.validateConverter(fieldType, converter, String.class);
		}
	},


	CHECKSUM(Checksum.class){
		@Override
		public void validate(final Class<?> fieldType, final Annotation annotation) throws AnnotationException{
			final Checksum binding = (Checksum)annotation;

			final Class<? extends Checksummer> algorithmClass = binding.algorithm();
			if(algorithmClass.isInterface() || Modifier.isAbstract(algorithmClass.getModifiers())
					|| algorithmClass.isAssignableFrom(Checksummer.class))
				throw AnnotationException.create("Unrecognized algorithm, must be a class implementing `"
					+ Checksummer.class.getName() + "`: {}", algorithmClass.getSimpleName());

			final Method interfaceMethod = MethodHelper.getMethods(Checksummer.class)[0];
			final Class<?> interfaceReturnType = interfaceMethod.getReturnType();

			TemplateAnnotationValidatorHelper.validateConverter(fieldType, NullConverter.class, interfaceReturnType);
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
	public static TemplateAnnotationValidator fromAnnotationType(final Class<? extends Annotation> annotationType){
		return VALIDATORS.get(annotationType);
	}

	/**
	 * Validate field and annotation.
	 *
	 * @param fieldType	The field class associated to the annotation.
	 * @param annotation	The annotation.
	 * @throws AnnotationException	If an error is detected.
	 */
	public abstract void validate(Class<?> fieldType, Annotation annotation) throws AnnotationException;

}
