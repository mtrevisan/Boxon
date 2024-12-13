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
import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.annotations.bindings.BindBitSet;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.core.helpers.DataTypeHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;


/**
 * Utility class that validates the order and compatibility of annotations on a field.
 * <p>
 * It ensures that annotations on a field follow a specific order and are compatible with each other.
 * </p>
 * <p>
 * Throws {@code AnnotationException} if any validation rule is violated.
 * </p>
 */
public final class TemplateValidator{

	private static final int ORDER_BIND_INDEX = 0;
	private static final int ORDER_CHECKSUM_INDEX = 1;
	private static final int ORDER_EVALUATE_INDEX = 2;
	private static final int ORDER_POST_PROCESS_INDEX = 3;

	private static final Set<Class<? extends Annotation>> ANNOTATIONS_BIND = Set.of(BindBitSet.class, BindInteger.class, BindObject.class,
		BindString.class, BindStringTerminated.class, ConverterChoices.class, ObjectChoices.class, ObjectChoicesList.class);
	private static final Set<Class<? extends Annotation>> ANNOTATIONS_SKIP = Set.of(SkipBits.class,
		SkipUntilTerminator.class);

	private static final String ANNOTATION_NAME_BIND = JavaHelper.commonPrefix(BindBitSet.class, BindInteger.class, BindObject.class,
		BindString.class, BindStringTerminated.class);
	private static final String ANNOTATION_NAME_CONVERTER_CHOICES = ConverterChoices.class.getSimpleName();
	private static final String ANNOTATION_NAME_OBJECT_CHOICES = ObjectChoices.class.getSimpleName();
	private static final String STAR = "*";
	private static final String ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR = ANNOTATION_NAME_BIND + STAR + "`, `"
		+ ANNOTATION_NAME_CONVERTER_CHOICES + "`, or `" + ANNOTATION_NAME_OBJECT_CHOICES + STAR;
	private static final String ANNOTATION_NAME_CHECKSUM = Checksum.class.getSimpleName();
	private static final String ANNOTATION_NAME_EVALUATE = Evaluate.class.getSimpleName();
	private static final String ANNOTATION_NAME_POST_PROCESS = PostProcess.class.getSimpleName();
	private static final String ANNOTATION_NAME_SKIP = JavaHelper.commonPrefix(SkipBits.class, SkipUntilTerminator.class);
	private static final String ANNOTATION_NAME_SKIP_STAR = ANNOTATION_NAME_SKIP + STAR;

	public static final String ANNOTATION_ORDER_ERROR_WRONG_NUMBER = "Wrong number of `{}`: there must be at most one";
	public static final String ANNOTATION_ORDER_ERROR_INCOMPATIBLE = "Incompatible annotations: `{}` and `{}`";
	public static final String ANNOTATION_ORDER_ERROR_WRONG_ORDER = "Wrong order of annotation: a `{}` must precede any `{}`";
	public static final String CHECKSUM_ANNOTATION_MISMATCHED_SIZES = "Wrong checksum field size: the field is {} bits in size, should be {}";


	private TemplateValidator(){}


	static void validateAnnotationsOrder(final Annotation[] annotations) throws AnnotationException{
		final int length = annotations.length;
		if(length <= 1)
			return;

		final boolean[] annotationFound = new boolean[ORDER_POST_PROCESS_INDEX + 1];
		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations[i];

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType == ContextParameter.class)
				continue;

			if(ANNOTATIONS_BIND.contains(annotationType)){
				validateBindAnnotationOrder(annotationFound);

				annotationFound[ORDER_BIND_INDEX] = true;
			}
			else if(annotationType == Checksum.class){
				validateChecksumAnnotationOrder(annotationFound);

				annotationFound[ORDER_CHECKSUM_INDEX] = true;
			}
			else if(annotationType == Evaluate.class){
				validateEvaluateAnnotationOrder(annotationFound);

				annotationFound[ORDER_EVALUATE_INDEX] = true;
			}
			else if(annotationType == PostProcess.class){
				validatePostProcessAnnotationOrder(annotationFound);

				annotationFound[ORDER_POST_PROCESS_INDEX] = true;
			}
			else if(ANNOTATIONS_SKIP.contains(annotationType))
				validateSkipAnnotationOrder(annotationFound);
		}
	}

	private static void validateBindAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_BIND_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR);
		if(annotationFound[ORDER_CHECKSUM_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR, ANNOTATION_NAME_CHECKSUM);
	}

	private static void validateChecksumAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_BIND_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				ANNOTATION_NAME_CHECKSUM, ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR);
		if(annotationFound[ORDER_CHECKSUM_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER, ANNOTATION_NAME_CHECKSUM);
	}

	private static void validateEvaluateAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_CHECKSUM_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				ANNOTATION_NAME_EVALUATE, ANNOTATION_NAME_CHECKSUM);
		if(annotationFound[ORDER_EVALUATE_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER, ANNOTATION_NAME_EVALUATE);
	}

	private static void validatePostProcessAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_POST_PROCESS_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER, ANNOTATION_NAME_POST_PROCESS);
	}

	private static void validateSkipAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_BIND_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				ANNOTATION_NAME_SKIP_STAR, ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR);
		if(annotationFound[ORDER_CHECKSUM_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				ANNOTATION_NAME_SKIP_STAR, ANNOTATION_NAME_CHECKSUM);
		if(annotationFound[ORDER_EVALUATE_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				ANNOTATION_NAME_SKIP_STAR, ANNOTATION_NAME_EVALUATE);
		if(annotationFound[ORDER_POST_PROCESS_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_ORDER,
				ANNOTATION_NAME_SKIP_STAR, ANNOTATION_NAME_POST_PROCESS);
	}

	public static void validateChecksumAnnotation(final Checksum checksum, final Field field) throws AnnotationException{
		final int crcSize = checksum.crcSize();
		final int fieldSize = DataTypeHelper.getSize(field.getType());
		if(crcSize != fieldSize)
			throw AnnotationException.create(CHECKSUM_ANNOTATION_MISMATCHED_SIZES,
				fieldSize, crcSize);
	}

}
