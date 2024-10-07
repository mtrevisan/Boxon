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

import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.helpers.validators.TemplateAnnotationValidator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.io.AnnotationValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;


public final class TemplateExtractor{

	private static final String LIBRARY_ROOT_PACKAGE_NAME = extractLibraryRootPackage();

	private static final Collection<Class<? extends Annotation>> COLLECTION_ANNOTATIONS = new HashSet<>(List.of(BindAsArray.class,
		BindAsList.class));

	/** Mapping of annotations to functions that extract skip parameters. */
	private static final Map<Class<? extends Annotation>, Function<Annotation, List<SkipParams>>> ANNOTATION_MAPPING
		= new HashMap<>(4);
	static{
		ANNOTATION_MAPPING.put(SkipBits.class, annotation
			-> Collections.singletonList(SkipParams.create((SkipBits)annotation)));
		ANNOTATION_MAPPING.put(SkipBits.Skips.class, annotation
			-> Arrays.stream(((SkipBits.Skips)annotation).value())
				.map(SkipParams::create)
				.collect(Collectors.toList()));
		ANNOTATION_MAPPING.put(SkipUntilTerminator.class, annotation
			-> Collections.singletonList(SkipParams.create((SkipUntilTerminator)annotation)));
		ANNOTATION_MAPPING.put(SkipUntilTerminator.Skips.class, annotation
			-> Arrays.stream(((SkipUntilTerminator.Skips)annotation).value())
				.map(SkipParams::create)
				.collect(Collectors.toList()));
	}


	private TemplateExtractor(){}


	static List<SkipParams> extractSkips(final Annotation[] annotations){
		final int length = annotations.length;
		final List<SkipParams> skips = JavaHelper.createListOrEmpty(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations[i];

			final Function<Annotation, List<SkipParams>> processingFun = ANNOTATION_MAPPING.get(annotation.annotationType());
			if(processingFun != null)
				skips.addAll(processingFun.apply(annotation));
		}
		return skips;
	}

	static <T extends Annotation> List<EvaluatedField<T>> extractAnnotation(final Class<T> annotationType,
			final Annotation[] declaredAnnotations, final Field field){
		final int length = declaredAnnotations.length;
		final List<EvaluatedField<T>> evaluations = JavaHelper.createListOrEmpty(length);
		for(int i = 0; i < length; i++){
			final Annotation declaredAnnotation = declaredAnnotations[i];

			if(declaredAnnotation.annotationType() == annotationType)
				evaluations.add(EvaluatedField.create(field, (T)declaredAnnotation));
		}
		return evaluations;
	}

	static List<ContextParameter> extractContextParameters(final List<? extends Annotation> annotations){
		final int length = annotations.size();
		final List<ContextParameter> contextParameters = JavaHelper.createListOrEmpty(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType == ContextParameter.class)
				contextParameters.add((ContextParameter)annotation);
		}
		return Collections.unmodifiableList(contextParameters);
	}

	/**
	 * Validates a field and return the first valid binding annotation.
	 *
	 * @param fieldType	The field class to validate.
	 * @param annotations	The list of annotations on the field.
	 * @return	The first valid binding annotation, or {@code null} if none are found.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	static Annotation extractAndValidateAnnotation(final Class<?> fieldType, final List<? extends Annotation> annotations)
			throws AnnotationException{
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final boolean validAnnotation = isValidAnnotation(annotation, fieldType);
			if(validAnnotation)
				foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	private static boolean isValidAnnotation(final Annotation annotation, final Class<?> fieldType) throws AnnotationException{
		final Class<? extends Annotation> annotationType = annotation.annotationType();
		boolean validAnnotation = isCustomAnnotation(annotationType);
		final AnnotationValidator validator = (validAnnotation
			? CodecLoader.getCustomCodecValidator(annotationType)
			: TemplateAnnotationValidator.fromAnnotationType(annotationType));
		//validate with provided validator, if any
		if(validator != null){
			validator.validate(fieldType, annotation);
			validAnnotation = true;
		}
		return validAnnotation;
	}

	/**
	 * Return the first collection binding annotation.
	 *
	 * @param annotations	The list of annotations on the field.
	 * @return	The first collection binding annotation, or {@code null} if none are found.
	 */
	static Annotation extractCollectionAnnotation(final List<? extends Annotation> annotations){
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(COLLECTION_ANNOTATIONS.contains(annotationType))
				foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	private static boolean isCustomAnnotation(final Class<? extends Annotation> annotationType){
		final String annotationPackageName = annotationType.getPackageName();
		return !annotationPackageName.startsWith(LIBRARY_ROOT_PACKAGE_NAME);
	}

	private static String extractLibraryRootPackage(){
		final String packageName = TemplateExtractor.class.getPackageName();
		final String[] packageParts = packageName.split("\\.");
		final StringJoiner sj = new StringJoiner(".");
		for(int i = 0, length = Math.min(4, packageParts.length); i < length; i ++)
			sj.add(packageParts[i]);
		return sj.toString();
	}

}
