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
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.helpers.generators.AnnotationCreator;
import io.github.mtrevisan.boxon.core.helpers.validators.TemplateAnnotationValidator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.JavaHelper;
import io.github.mtrevisan.boxon.helpers.ReflectiveClassLoader;
import io.github.mtrevisan.boxon.io.AnnotationValidator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * A utility class designed to extract and process annotations for template-based configurations.
 * <p>
 * Provides methods to extract, validate, and manipulate various types of annotations and their associated parameters.
 * </p>
 */
public final class TemplateExtractor{

	private static final String MULTIPLE_ANNOTATIONS_VALUE = "value";

	private static final Set<Class<? extends Annotation>> SKIP_ANNOTATIONS = Set.of(SkipBits.Skips.class,
		SkipUntilTerminator.Skips.class, ConfigurationSkip.Skips.class);
	private static final Set<Class<? extends Annotation>> ANNOTATIONS_WITHOUT_CODEC = Set.of(ContextParameter.class,
		BindAsArray.class, BindAsList.class);
	private static final Set<Class<? extends Annotation>> LIBRARY_ANNOTATIONS = ReflectiveClassLoader.extractAnnotations(Evaluate.class,
		ElementType.ANNOTATION_TYPE);
	private static final Set<Class<? extends Annotation>> COLLECTION_ANNOTATIONS = Set.of(BindAsArray.class,
		BindAsList.class);

	private static final String LIBRARY_ROOT_PACKAGE_NAME = extractLibraryRootPackage();

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
			//FIXME a cycle between packages (TemplateExtractor > CodecLoader .. CodecObject > Template)
			? CodecLoader.getCustomCodecValidator(annotationType)
			: TemplateAnnotationValidator.fromAnnotationType(annotationType));
		//validate with the provided validator, if any
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


	public static Annotation[] extractBaseAnnotations(final Annotation[] declaredAnnotations){
		final int length = declaredAnnotations.length;
		final Annotation[] annotations = new Annotation[length];
		for(int i = 0; i < length; i ++){
			Annotation declaredAnnotation = declaredAnnotations[i];

			final Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();
			final Annotation[] parentAnnotations = annotationType.getAnnotations();
			final Annotation parentAnnotation = findParentAnnotation(parentAnnotations);
			if(parentAnnotation != null){
				final Annotation[] skips = extractSkips(declaredAnnotation, parentAnnotation);

				declaredAnnotation = createAnnotationWithDefaults(declaredAnnotation, parentAnnotation, skips);
			}

			annotations[i] = declaredAnnotation;
		}
		return annotations;
	}

	private static Annotation[] extractSkips(final Annotation declaredAnnotation, final Annotation parentAnnotation){
		Annotation[] skips = null;
		final Class<? extends Annotation> parentAnnotationType = parentAnnotation.annotationType();
		if(SKIP_ANNOTATIONS.contains(parentAnnotationType)){
			final Map<String, Object> declaredValues = AnnotationCreator.extractAnnotationValues(declaredAnnotation);
			final Annotation[] declaredSkips = (Annotation[])declaredValues.get(MULTIPLE_ANNOTATIONS_VALUE);
			skips = new Annotation[declaredSkips.length];
			int j = 0;
			final Class<? extends Annotation> declaredSkipType = (Class<? extends Annotation>)parentAnnotationType.getEnclosingClass();
			for(int k = 0, skipsLength = declaredSkips.length; k < skipsLength; k ++){
				final Annotation declaredSkip = declaredSkips[k];

				final Class<? extends Annotation> skipAnnotationType = declaredSkip.annotationType();
				final Annotation[] skipParentAnnotations = skipAnnotationType.getAnnotations();
				final Annotation skipParentAnnotation = findParentAnnotation(skipParentAnnotations);

				skips[j ++] = createSkipAnnotation(declaredSkip, declaredSkipType, skipParentAnnotation);
			}
		}
		return skips;
	}

	private static Annotation findParentAnnotation(final Annotation[] parentAnnotations){
		for(int j = 0, length = parentAnnotations.length; j < length; j ++){
			final Annotation parentAnnotation = parentAnnotations[j];

			if(LIBRARY_ANNOTATIONS.contains(parentAnnotation.annotationType()))
				return parentAnnotation;
		}
		return null;
	}

	private static Annotation createSkipAnnotation(final Annotation declaredAnnotation,
		final Class<? extends Annotation> declaredAnnotationType, final Annotation parentAnnotation){
		final Map<String, Object> parentValues = AnnotationCreator.extractAnnotationValues(parentAnnotation);
		final Map<String, Object> declaredValues = AnnotationCreator.extractAnnotationValues(declaredAnnotation);
		populateDefaultValues(declaredValues, parentValues);
		return AnnotationCreator.createAnnotation(declaredAnnotationType, declaredValues);
	}

	private static void populateDefaultValues(final Map<String, Object> values, final Map<String, Object> parentValues){
		//replace with default parent values
		for(final Map.Entry<String, Object> entry : parentValues.entrySet()){
			final String key = entry.getKey();
			final Object value = entry.getValue();

			values.putIfAbsent(key, value);
		}
	}

	private static Annotation createAnnotationWithDefaults(final Annotation declaredAnnotation, final Annotation parentAnnotation,
		final Annotation[] skips){
		final Map<String, Object> parentValues = AnnotationCreator.extractAnnotationValues(parentAnnotation);
		final Map<String, Object> declaredValues = AnnotationCreator.extractAnnotationValues(declaredAnnotation);
		populateDefaultValues(declaredValues, parentValues);
		if(skips != null)
			declaredValues.put(MULTIPLE_ANNOTATIONS_VALUE, skips);
		//create annotation of `foundAnnotation` type with the defaults written in the annotation of `declaredAnnotation` and
		// parameters from `declaredAnnotation`
		return AnnotationCreator.createAnnotation(parentAnnotation.annotationType(), declaredValues);
	}


	public static List<Annotation> filterAnnotationsWithCodec(Annotation[] declaredAnnotations){
		declaredAnnotations = extractBaseAnnotations(declaredAnnotations);
		final int length = declaredAnnotations.length;
		final List<Annotation> annotations = JavaHelper.createListOrEmpty(length);
		for(int i = 0; i < length; i ++){
			final Annotation declaredAnnotation = declaredAnnotations[i];

			final Class<? extends Annotation> annotationType = declaredAnnotation.annotationType();
			if(shouldIncludeAnnotation(annotationType))
				annotations.add(declaredAnnotation);
		}
		return annotations;
	}

	private static boolean shouldIncludeAnnotation(final Class<? extends Annotation> annotationType){
		//FIXME a cycle between packages (TemplateExtractor > CodecLoader .. CodecObject > Template)
		return (CodecLoader.hasCodec(annotationType) || ANNOTATIONS_WITHOUT_CODEC.contains(annotationType));
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
