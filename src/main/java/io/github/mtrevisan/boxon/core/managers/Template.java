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
package io.github.mtrevisan.boxon.core.managers;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.core.managers.validators.TemplateAnnotationValidator;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the codec is able to decode/encode.
 */
public final class Template<T>{

	private static final class Pair{
		private final List<BoundedField> boundedFields;
		private final List<EvaluatedField> evaluatedFields;

		private static Pair of(final List<BoundedField> boundedFields, final List<EvaluatedField> evaluatedFields){
			return new Pair(boundedFields, evaluatedFields);
		}

		private Pair(final List<BoundedField> boundedFields, final List<EvaluatedField> evaluatedFields){
			this.boundedFields = boundedFields;
			this.evaluatedFields = evaluatedFields;
		}
	}


	private final Class<T> type;

	private final MessageHeader header;
	private final List<BoundedField> boundedFields;
	private final List<EvaluatedField> evaluatedFields;
	/**
	 * Necessary to speed up the creation of a {@link Template} (technically not needed because it's already present
	 * somewhere inside {@link #boundedFields}).
	 */
	private BoundedField checksum;


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

		header = type.getAnnotation(MessageHeader.class);
		if(header != null)
			CharsetHelper.assertValidCharset(header.charset());

		final Pair fields = loadAnnotatedFields(type, filterAnnotationsWithCodec);
		boundedFields = Collections.unmodifiableList(fields.boundedFields);
		evaluatedFields = Collections.unmodifiableList(fields.evaluatedFields);

		if(boundedFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
	}

	@SuppressWarnings("ObjectAllocationInLoop")
	private Pair loadAnnotatedFields(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		final List<Field> fields = ReflectionHelper.getAccessibleFields(type);
		final int size = fields.size();
		final List<BoundedField> boundedFields = new ArrayList<>(size);
		final List<EvaluatedField> evaluatedFields = new ArrayList<>(size);
		for(int i = 0; i < size; i ++){
			final Field field = fields.get(i);
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			loadChecksumField(checksum, type, field);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final List<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			try{
				final Annotation validAnnotation = validateField(field, boundedAnnotations);

				if(validAnnotation != null)
					boundedFields.add(BoundedField.create(field, validAnnotation, skips));
			}
			catch(final AnnotationException e){
				e.withClassAndField(type, field);
				throw e;
			}
		}
		return Pair.of(boundedFields, evaluatedFields);
	}

	private void loadChecksumField(final Checksum checksum, final Class<T> type, final Field field) throws AnnotationException{
		if(checksum != null){
			if(this.checksum != null){
				final AnnotationException exception = AnnotationException.create("Cannot have more than one {} annotations on class {}",
					Checksum.class.getSimpleName(), type.getName());
				throw (AnnotationException)exception.withClassAndField(type, field);
			}

			this.checksum = BoundedField.create(field, checksum);
		}
	}

	@SuppressWarnings("ObjectAllocationInLoop")
	private static List<EvaluatedField> extractEvaluations(final Annotation[] declaredAnnotations, final Field field){
		final List<EvaluatedField> evaluations = new ArrayList<>(declaredAnnotations.length);
		for(int i = 0; i < declaredAnnotations.length; i ++){
			final Annotation annotation = declaredAnnotations[i];
			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(new EvaluatedField(field, (Evaluate)annotation));
		}
		return evaluations;
	}

	private static Annotation validateField(final Field field, final List<? extends Annotation> annotations) throws AnnotationException{
		/** filter out {@link Skip} annotations and return the (first) valid binding annotation */
		Annotation foundAnnotation = null;
		for(int i = 0; foundAnnotation == null && i < annotations.size(); i ++){
			final Class<? extends Annotation> annotationType = annotations.get(i).annotationType();
			if(annotationType == Skip.class || annotationType == Skip.Skips.class)
				continue;

			validateAnnotation(field, annotations.get(i));

			foundAnnotation = annotations.get(i);
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
	public MessageHeader getHeader(){
		return header;
	}

	/**
	 * List of {@link BoundedField bounded fields}.
	 *
	 * @return	List of bounded fields.
	 */
	public List<BoundedField> getBoundedFields(){
		return boundedFields;
	}

	/**
	 * List of {@link EvaluatedField evaluated fields}.
	 *
	 * @return	List of evaluated fields.
	 */
	public List<EvaluatedField> getEvaluatedFields(){
		return evaluatedFields;
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
	public BoundedField getChecksum(){
		return checksum;
	}

	/**
	 * Whether this template is well formatted, that is has a header annotation and has some bounded fields.
	 *
	 * @return	Whether this template is well formatted.
	 */
	public boolean canBeCoded(){
		return (header != null && !boundedFields.isEmpty());
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
