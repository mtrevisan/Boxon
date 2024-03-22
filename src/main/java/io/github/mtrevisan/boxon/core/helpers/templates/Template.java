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
import io.github.mtrevisan.boxon.annotations.PostProcessField;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;

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

	private record Triplet(List<TemplateField> templateFields, List<EvaluatedField<Evaluate>> evaluatedFields,
			List<EvaluatedField<PostProcessField>> postProcessedFields){
		private static Triplet of(final List<TemplateField> templateFields, final List<EvaluatedField<Evaluate>> evaluatedFields,
				final List<EvaluatedField<PostProcessField>> postProcessedFields){
			return new Triplet(templateFields, evaluatedFields, postProcessedFields);
		}
	}


	private final Class<T> type;

	private final TemplateHeader header;
	private final List<TemplateField> templateFields;
	private final List<EvaluatedField<Evaluate>> evaluatedFields;
	private final List<EvaluatedField<PostProcessField>> postProcessedFields;
	/**
	 * Necessary to speed up the creation of a {@link Template} (technically not needed because it's already present
	 * somewhere inside {@link #templateFields}).
	 */
	private TemplateField checksum;


	/**
	 * Create an instance of a template.
	 *
	 * @param type	The template class.
	 * @param filterAnnotationsWithCodec	A function that filters the annotation that have a corresponding codec.
	 * @param <T>	The class type of the template.
	 * @return	An instance of a template.
	 * @throws AnnotationException	If an annotation has validation problems.
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


	@SuppressWarnings("ObjectAllocationInLoop")
	private Triplet loadAnnotatedFields(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		final List<Field> fields = ReflectionHelper.getAccessibleFields(type);
		final int length = fields.size();
		final List<TemplateField> templateFields = new ArrayList<>(length);
		final List<EvaluatedField<Evaluate>> evaluatedFields = new ArrayList<>(length);
		final List<EvaluatedField<PostProcessField>> postProcessedFields = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Field field = fields.get(i);

			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			loadChecksumField(checksum, type, field);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final List<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			postProcessedFields.addAll(extractProcessed(declaredAnnotations, field));

			try{
				final Annotation validAnnotation = validateField(field, boundedAnnotations);

				if(validAnnotation != null || skips.length > 0)
					templateFields.add(TemplateField.create(field, validAnnotation, skips));
			}
			catch(final AnnotationException e){
				e.withClassAndField(type, field);
				throw e;
			}
		}
		return Triplet.of(templateFields, evaluatedFields, postProcessedFields);
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

	@SuppressWarnings("ObjectAllocationInLoop")
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

	@SuppressWarnings("ObjectAllocationInLoop")
	private static List<EvaluatedField<PostProcessField>> extractProcessed(final Annotation[] declaredAnnotations, final Field field){
		final int length = declaredAnnotations.length;
		final List<EvaluatedField<PostProcessField>> processed = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Annotation annotation = declaredAnnotations[i];

			if(annotation.annotationType() == PostProcessField.class)
				processed.add(EvaluatedField.create(field, (PostProcessField)annotation));
		}
		return processed;
	}

	private static Annotation validateField(final Field field, final List<? extends Annotation> annotations) throws AnnotationException{
		/** filter out {@link Skip} annotations and return the (first) valid binding annotation */
		Annotation foundAnnotation = null;
		for(int i = 0, length = annotations.size(); foundAnnotation == null && i < length; i ++){
			final Annotation annotation = annotations.get(i);

			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType == Skip.class || annotationType == Skip.Skips.class)
				continue;

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
	public List<EvaluatedField<PostProcessField>> getPostProcessedFields(){
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
		return (header != null && ! templateFields.isEmpty());
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
