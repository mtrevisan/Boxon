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
import io.github.mtrevisan.boxon.core.helpers.extractors.SkipParams;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.FieldAccessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T>	The type of object the codec is able to decode/encode.
 */
public final class Template<T>{

	private static final String ANNOTATION_NAME_BIND = "Bind";
	private static final String ANNOTATION_NAME_CONVERTER_CHOICES = "ConverterChoices";
	private static final String ANNOTATION_NAME_OBJECT_CHOICES = "ObjectChoices";
	private static final String ANNOTATION_NAME_CHECKSUM = "Checksum";
	private static final String ANNOTATION_NAME_EVALUATE = "Evaluate";
	private static final String ANNOTATION_NAME_POST_PROCESS = "PostProcess";
	private static final String ANNOTATION_NAME_SKIP = "Skip";

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


	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	private Template(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(TemplateHeader.class);
		if(header != null){
			try{
				CharsetHelper.lookup(header.charset());
			}
			catch(final UnsupportedCharsetException ignored){
				throw AnnotationException.create("Invalid charset: '{}'", header.charset());
			}
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
		for(int i = 0; i < length; i ++){
			final Field field = fields.get(i);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			validateAnnotationsOrder(declaredAnnotations);

			final List<SkipParams> skips = extractSkips(declaredAnnotations);

			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			loadChecksumField(checksum, type, field);

			final List<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			postProcessedFields.addAll(extractProcessed(declaredAnnotations, field));

			try{
				final Annotation validAnnotation = extractAndValidateAnnotation(field, boundedAnnotations);

				if(validAnnotation != null || !skips.isEmpty())
					templateFields.add(TemplateField.create(field, validAnnotation, skips));
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

		boolean bindFound = false;
		boolean checksumFound = false;
		boolean evaluateFound = false;
		boolean postProcessFound = false;
		boolean skipFound = false;

		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations[i];

			final String annotationName = annotation.annotationType().getName();
			if(annotationName.startsWith(ANNOTATION_NAME_BIND) || annotationName.equals(ANNOTATION_NAME_CONVERTER_CHOICES)
					|| annotationName.startsWith(ANNOTATION_NAME_OBJECT_CHOICES)){
				validateBindAnnotationOrder(bindFound, checksumFound, skipFound);

				bindFound = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_CHECKSUM)){
				validateChecksumAnnotationOrder(bindFound, checksumFound, skipFound);

				checksumFound = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_EVALUATE)){
				validateEvaluateAnnotationOrder(checksumFound, evaluateFound, skipFound);

				evaluateFound = true;
			}
			else if(annotationName.equals(ANNOTATION_NAME_POST_PROCESS)){
				validatePostProcessAnnotationOrder(postProcessFound, skipFound);

				postProcessFound = true;
			}
			else if(annotationName.startsWith(ANNOTATION_NAME_SKIP)){
				validateSkipAnnotationOrder(bindFound, checksumFound, evaluateFound, postProcessFound);

				skipFound = true;
			}
		}
	}

	private static void validateBindAnnotationOrder(final boolean bindFound, final boolean checksumFound, final boolean skipFound)
			throws AnnotationException{
		if(bindFound)
			throw AnnotationException.create("Wrong number of `Bind*`, `ConverterChoices`, or `ObjectChoices*`: there must be at most one");
		if(checksumFound)
			throw AnnotationException.create("Incompatible annotations: `Bind*`, `ConverterChoices`, or `ObjectChoices*` and `Checksum`");
		if(skipFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Bind*`, `ConverterChoices`, or `ObjectChoices*`");
	}

	private static void validateChecksumAnnotationOrder(final boolean bindFound, final boolean checksumFound, final boolean skipFound)
			throws AnnotationException{
		if(bindFound)
			throw AnnotationException.create("Incompatible annotations: `Checksum` and `Bind*`, `ConverterChoices`, or `ObjectChoices*`");
		if(checksumFound)
			throw AnnotationException.create("Wrong number of `Checksum`: there must be at most one");
		if(skipFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Checksum");
	}

	private static void validatePostProcessAnnotationOrder(final boolean postProcessFound, final boolean skipFound)
			throws AnnotationException{
		if(postProcessFound)
			throw AnnotationException.create("Wrong number of `PostProcess`: there must be at most one");
		if(skipFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `PostProcess`");
	}

	private static void validateEvaluateAnnotationOrder(final boolean checksumFound, final boolean evaluateFound, final boolean skipFound)
			throws AnnotationException{
		if(checksumFound)
			throw AnnotationException.create("Incompatible annotations: `Evaluate` and `Checksum`");
		if(evaluateFound)
			throw AnnotationException.create("Wrong number of `Evaluate`: there must be at most one");
		if(skipFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Evaluate");
	}

	private static void validateSkipAnnotationOrder(final boolean bindFound, final boolean checksumFound, final boolean evaluateFound,
		final boolean postProcessFound) throws AnnotationException{
		if(bindFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Bind*`, `ConverterChoices`, or `ObjectChoices*`");
		if(checksumFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Checksum`");
		if(evaluateFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `Evaluate`");
		if(postProcessFound)
			throw AnnotationException.create("Wrong order of annotation: a `Skip*` must precede any `PostProcess`");
	}


	private static List<SkipParams> extractSkips(final Annotation[] annotations){
		final int length = annotations.length;
		final List<SkipParams> skips = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = annotations[i];

			if(annotation instanceof SkipBits)
				skips.add(SkipParams.create((SkipBits)annotation));
			else if(annotation instanceof SkipUntilTerminator)
				skips.add(SkipParams.create((SkipUntilTerminator)annotation));
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
		final int length = declaredAnnotations.length;
		final List<EvaluatedField<Evaluate>> evaluations = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = declaredAnnotations[i];

			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(EvaluatedField.create(field, (Evaluate)annotation));
		}
		return evaluations;
	}

	private static List<EvaluatedField<PostProcess>> extractProcessed(final Annotation[] declaredAnnotations, final Field field){
		final int length = declaredAnnotations.length;
		final List<EvaluatedField<PostProcess>> processed = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = declaredAnnotations[i];

			if(annotation.annotationType() == PostProcess.class)
				processed.add(EvaluatedField.create(field, (PostProcess)annotation));
		}
		return processed;
	}

	/**
	 * Validates a field and return the first valid binding annotation.
	 *
	 * @param field	The field to validate.
	 * @param annotations	The list of annotations on the field.
	 * @return	The first valid binding annotation, or {@code null} if none are found.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	private static Annotation extractAndValidateAnnotation(final Field field, final List<? extends Annotation> annotations)
			throws AnnotationException{
		/** return the (first) valid binding annotation */
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			validateAnnotation(field, annotation);

			foundAnnotation = annotation;
		}
		return foundAnnotation;
	}

	private static void validateAnnotation(final Field field, final Annotation annotation) throws AnnotationException{
		final TemplateAnnotationValidator validator = TemplateAnnotationValidator.fromAnnotation(annotation.annotationType());
		if(validator != null)
			validator.validate(field, annotation);
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
		return type.getName().hashCode();
	}

}
