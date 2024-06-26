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
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.SkipUntilTerminator;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindAsList;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.validators.TemplateAnnotationValidator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.io.AnnotationValidator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T>	The type of object the codec is able to decode/encode.
 */
public final class Template<T>{

	private static final int ORDER_BIND_INDEX = 0;
	private static final int ORDER_CHECKSUM_INDEX = 1;
	private static final int ORDER_EVALUATE_INDEX = 2;
	private static final int ORDER_POST_PROCESS_INDEX = 3;

	private static final String ANNOTATION_NAME_BIND = "Bind";
	private static final String ANNOTATION_NAME_BIND_AS = "BindAs";
	private static final String ANNOTATION_NAME_CONVERTER_CHOICES = "ConverterChoices";
	private static final String ANNOTATION_NAME_OBJECT_CHOICES = "ObjectChoices";
	private static final String STAR = "*";
	private static final String ANNOTATION_NAME_BIND_STAR_CONVERTER_CHOICES_OBJECT_CHOICES_STAR = ANNOTATION_NAME_BIND + STAR + "`, `"
		+ ANNOTATION_NAME_CONVERTER_CHOICES + "`, or `" + ANNOTATION_NAME_OBJECT_CHOICES + STAR;
	private static final String ANNOTATION_NAME_CHECKSUM = "Checksum";
	private static final String ANNOTATION_NAME_EVALUATE = "Evaluate";
	private static final String ANNOTATION_NAME_POST_PROCESS = "PostProcess";
	private static final String ANNOTATION_NAME_SKIP = "Skip";
	private static final String ANNOTATION_NAME_SKIP_STAR = ANNOTATION_NAME_SKIP + STAR;

	public static final String ANNOTATION_ORDER_ERROR_WRONG_NUMBER = "Wrong number of `{}`: there must be at most one";
	public static final String ANNOTATION_ORDER_ERROR_INCOMPATIBLE = "Incompatible annotations: `{}` and `{}`";
	public static final String ANNOTATION_ORDER_ERROR_WRONG_ORDER = "Wrong order of annotation: a `{}` must precede any `{}`";

	private static final String LIBRARY_ROOT_PACKAGE_NAME = extractLibraryRootPackage();

	private static Function<Class<? extends Annotation>, AnnotationValidator> customCodecValidatorExtractor = type -> null;

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

	/**
	 * Sets a custom codec validator extractor.
	 *
	 * @param customCodecValidatorExtractor	A function that extracts a custom codec validator based on the annotation class.
	 */
	public static void setCustomCodecValidatorExtractor(
			final Function<Class<? extends Annotation>, AnnotationValidator> customCodecValidatorExtractor){
		Template.customCodecValidatorExtractor = customCodecValidatorExtractor;
	}

	private record Triplet(List<TemplateField> templateFields, List<EvaluatedField<Evaluate>> evaluatedFields,
			List<EvaluatedField<PostProcess>> postProcessedFields){
		private static Triplet of(final List<TemplateField> templateFields, final List<EvaluatedField<Evaluate>> evaluatedFields,
				final List<EvaluatedField<PostProcess>> postProcessedFields){
			return new Triplet(templateFields, evaluatedFields, postProcessedFields);
		}
	}


	private final Class<T> type;

	private final TemplateHeader header;
	private final List<TemplateField> templateFields;
	private final List<EvaluatedField<Evaluate>> evaluatedFields;
	private final List<EvaluatedField<PostProcess>> postProcessedFields;
	/**
	 * Necessary to speed up the creation of a {@link Template} (technically not needed because it's already present
	 * somewhere inside {@link #templateFields}).
	 */
	private TemplateField checksum;


	/**
	 * Create an instance of a template.
	 *
	 * @param type	The template class.
	 * @param <T>	The class type of the template.
	 * @return	An instance of a template.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	public static <T> Template<T> create(final Class<T> type) throws AnnotationException{
		return new Template<>(type, List::of);
	}

	/**
	 * Create an instance of a template.
	 *
	 * @param type	The template class.
	 * @param filterAnnotationsWithCodec	A function that filters the annotation that have a corresponding codec.
	 * @param <T>	The class type of the template.
	 * @return	An instance of a template.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	public static <T> Template<T> create(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		return new Template<>(type, filterAnnotationsWithCodec);
	}


	private Template(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(TemplateHeader.class);
		//(`ObjectChoice` object may not have a `TemplateHeader`)
		if(header != null){
			final TemplateAnnotationValidator headerValidator = TemplateAnnotationValidator.fromAnnotationType(TemplateHeader.class);
			headerValidator.validate(null, header);
		}

		final Triplet fields = loadAnnotatedFields(type, filterAnnotationsWithCodec);
		templateFields = Collections.unmodifiableList(fields.templateFields);
		evaluatedFields = Collections.unmodifiableList(fields.evaluatedFields);
		postProcessedFields = Collections.unmodifiableList(fields.postProcessedFields);

		if(templateFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
	}


	private Triplet loadAnnotatedFields(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		final List<Field> fields = FieldAccessor.getAccessibleFields(type);
		final int length = fields.size();
		final List<TemplateField> templateFields = new ArrayList<>(length);
		final List<EvaluatedField<Evaluate>> evaluatedFields = new ArrayList<>(length);
		final List<EvaluatedField<PostProcess>> postProcessedFields = new ArrayList<>(length);
		for(final Field field : fields){
			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			validateAnnotationsOrder(declaredAnnotations);

			final List<SkipParams> skips = extractSkips(declaredAnnotations);

			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			loadChecksumField(checksum, type, field);

			final List<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			postProcessedFields.addAll(extractProcessed(declaredAnnotations, field));

			try{
				final Annotation validAnnotation = extractAndValidateAnnotation(field.getType(), boundedAnnotations);
				final Annotation collectionAnnotation = extractCollectionAnnotation(boundedAnnotations);

				if(validAnnotation != null || !skips.isEmpty())
					templateFields.add(TemplateField.create(field, validAnnotation, collectionAnnotation, skips));
			}
			catch(final AnnotationException e){
				e.withClassAndField(type, field);
				throw e;
			}
		}
		return Triplet.of(templateFields, evaluatedFields, postProcessedFields);
	}


	private static void validateAnnotationsOrder(final Annotation[] annotations) throws AnnotationException{
		final int length = annotations.length;
		if(length <= 1)
			return;

		final boolean[] annotationFound = new boolean[ORDER_POST_PROCESS_INDEX + 1];
		for(final Annotation annotation : annotations){
			final String annotationName = annotation.annotationType()
				.getSimpleName();

			if(annotationName.startsWith(ANNOTATION_NAME_BIND) && !annotationName.startsWith(ANNOTATION_NAME_BIND_AS)
				|| annotationName.equals(ANNOTATION_NAME_CONVERTER_CHOICES) || annotationName.startsWith(ANNOTATION_NAME_OBJECT_CHOICES)){
				validateBindAnnotationOrder(annotationFound);

				annotationFound[ORDER_BIND_INDEX] = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_CHECKSUM)){
				validateChecksumAnnotationOrder(annotationFound);

				annotationFound[ORDER_CHECKSUM_INDEX] = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_EVALUATE)){
				validateEvaluateAnnotationOrder(annotationFound);

				annotationFound[ORDER_EVALUATE_INDEX] = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_POST_PROCESS)){
				validatePostProcessAnnotationOrder(annotationFound);

				annotationFound[ORDER_POST_PROCESS_INDEX] = true;
			}
			else if(annotationName.startsWith(ANNOTATION_NAME_SKIP))
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
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				ANNOTATION_NAME_CHECKSUM);
	}

	private static void validateEvaluateAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_CHECKSUM_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_INCOMPATIBLE,
				ANNOTATION_NAME_EVALUATE, ANNOTATION_NAME_CHECKSUM);
		if(annotationFound[ORDER_EVALUATE_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				ANNOTATION_NAME_EVALUATE);
	}

	private static void validatePostProcessAnnotationOrder(final boolean[] annotationFound) throws AnnotationException{
		if(annotationFound[ORDER_POST_PROCESS_INDEX])
			throw AnnotationException.create(ANNOTATION_ORDER_ERROR_WRONG_NUMBER,
				ANNOTATION_NAME_POST_PROCESS);
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


	private static List<SkipParams> extractSkips(final Annotation[] annotations){
		final List<SkipParams> skips = new ArrayList<>(annotations.length);
		for(final Annotation annotation : annotations){
			final Function<Annotation, List<SkipParams>> processingFun = ANNOTATION_MAPPING.get(annotation.annotationType());
			if(processingFun != null)
				skips.addAll(processingFun.apply(annotation));
		}
		return skips;
	}

	private void loadChecksumField(final Checksum checksum, final Class<T> type, final Field field) throws AnnotationException{
		if(checksum != null){
			if(this.checksum != null){
				final AnnotationException exception = AnnotationException.create("Cannot have more than one {} annotations on class {}",
					Checksum.class.getSimpleName(), type.getName());
				throw (AnnotationException)exception.withClassAndField(type, field);
			}

			this.checksum = TemplateField.create(field, checksum);
		}
	}

	private static List<EvaluatedField<Evaluate>> extractEvaluations(final Annotation[] declaredAnnotations, final Field field){
		final List<EvaluatedField<Evaluate>> evaluations = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations)
			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(EvaluatedField.create(field, (Evaluate)annotation));
		return evaluations;
	}

	private static List<EvaluatedField<PostProcess>> extractProcessed(final Annotation[] declaredAnnotations, final Field field){
		final List<EvaluatedField<PostProcess>> processed = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations)
			if(annotation.annotationType() == PostProcess.class)
				processed.add(EvaluatedField.create(field, (PostProcess)annotation));
		return processed;
	}

	/**
	 * Validates a field and return the first valid binding annotation.
	 *
	 * @param fieldType	The field class to validate.
	 * @param annotations	The list of annotations on the field.
	 * @return	The first valid binding annotation, or {@code null} if none are found.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	private static Annotation extractAndValidateAnnotation(final Class<?> fieldType, final List<? extends Annotation> annotations)
			throws AnnotationException{
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			boolean validAnnotation = isCustomAnnotation(annotationType);
			final AnnotationValidator validator = (validAnnotation
				? customCodecValidatorExtractor.apply(annotationType)
				: TemplateAnnotationValidator.fromAnnotationType(annotationType));
			//validate with provided validator, if any
			if(validator != null){
				validator.validate(fieldType, annotation);
				validAnnotation = true;
			}

			if(validAnnotation)
				foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	/**
	 * Return the first collection binding annotation.
	 *
	 * @param annotations	The list of annotations on the field.
	 * @return	The first collection binding annotation, or {@code null} if none are found.
	 */
	private static Annotation extractCollectionAnnotation(final List<? extends Annotation> annotations){
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType == BindAsArray.class || annotationType == BindAsList.class)
				foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	private static boolean isCustomAnnotation(final Class<? extends Annotation> annotationType){
		final String annotationPackageName = annotationType.getPackageName();
		return !annotationPackageName.startsWith(LIBRARY_ROOT_PACKAGE_NAME);
	}

	private static String extractLibraryRootPackage(){
		final String packageName = Template.class.getPackageName();
		final String[] packageParts = packageName.split("\\.");
		final StringJoiner sj = new StringJoiner(".");
		for(int i = 0, length = Math.min(4, packageParts.length); i < length; i ++)
			sj.add(packageParts[i]);
		return sj.toString();
	}

	/**
	 * The class type of this template.
	 *
	 * @return	The class type.
	 */
	public Class<T> getType(){
		return type;
	}

	/**
	 * The header of this template.
	 *
	 * @return	The header annotation.
	 */
	public TemplateHeader getHeader(){
		return header;
	}

	/**
	 * List of {@link TemplateField template fields}.
	 *
	 * @return	List of template fields.
	 */
	public List<TemplateField> getTemplateFields(){
		return templateFields;
	}

	/**
	 * List of {@link EvaluatedField evaluated fields}.
	 *
	 * @return	List of evaluated fields.
	 */
	public List<EvaluatedField<Evaluate>> getEvaluatedFields(){
		return evaluatedFields;
	}

	/**
	 * List of {@link EvaluatedField processed fields}.
	 *
	 * @return	List of processed fields.
	 */
	public List<EvaluatedField<PostProcess>> getPostProcessedFields(){
		return postProcessedFields;
	}

	/**
	 * Whether a field is annotated with {@link Checksum}.
	 *
	 * @return	Whether a field is annotated with {@link Checksum}.
	 */
	public boolean isChecksumPresent(){
		return (checksum != null);
	}

	/**
	 * Checksum bound data.
	 *
	 * @return	Checksum bound data.
	 */
	public TemplateField getChecksum(){
		return checksum;
	}

	/**
	 * Whether this template is well formatted, that it has a header annotation and has some template fields.
	 *
	 * @return	Whether this template is well formatted.
	 */
	public boolean canBeCoded(){
		return (header != null && !templateFields.isEmpty());
	}

	@Override
	public String toString(){
		return type.getSimpleName();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Template<?> rhs = (Template<?>)obj;
		return (type == rhs.type);
	}

	@Override
	public int hashCode(){
		return type.getName()
			.hashCode();
	}

}
