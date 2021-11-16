/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
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

		static Pair of(final List<BoundedField> boundedFields, final List<EvaluatedField> evaluatedFields){
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


	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
	Template(final Class<T> type, final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(MessageHeader.class);
		if(header != null)
			ValidatorHelper.assertValidCharset(header.charset());

		final Pair fields = loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type),
			filterAnnotationsWithCodec);
		boundedFields = Collections.unmodifiableList(fields.boundedFields);
		evaluatedFields = Collections.unmodifiableList(fields.evaluatedFields);

		if(boundedFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", type.getName());
	}

	@SuppressWarnings("ObjectAllocationInLoop")
	private Pair loadAnnotatedFields(final Class<T> type, final List<Field> fields,
			final Function<Annotation[], List<Annotation>> filterAnnotationsWithCodec) throws AnnotationException{
		final List<BoundedField> boundedFields = new ArrayList<>(fields.size());
		final List<EvaluatedField> evaluatedFields = new ArrayList<>(fields.size());
		for(int i = 0; i < fields.size(); i ++){
			final Field field = fields.get(i);
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final List<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			try{
				final Annotation validAnnotation = validateField(boundedAnnotations, checksum);

				if(validAnnotation != null){
					boundedFields.add(new BoundedField(field, validAnnotation, (skips.length > 0? skips: null)));
					if(checksum != null)
						this.checksum = new BoundedField(field, checksum);
				}
			}
			catch(final AnnotationException e){
				e.setClassNameAndFieldName(type.getName(), field.getName());
				throw e;
			}
		}
		return Pair.of(boundedFields, evaluatedFields);
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

	private Annotation validateField(final List<? extends Annotation> annotations, final Checksum checksum) throws AnnotationException{
		//filter out `@Skip` annotations
		Annotation foundAnnotation = null;
		for(int i = 0; i < annotations.size(); i ++){
			final Class<? extends Annotation> annotationType = annotations.get(i).annotationType();
			if(annotationType != Skip.class && annotationType != Skip.Skips.class){
				if(foundAnnotation != null){
					final StringJoiner sj = new StringJoiner(", ", "[", "]");
					for(int j = 0; j < annotations.size(); j ++)
						sj.add(annotations.get(j).annotationType().getSimpleName());
					throw AnnotationException.create("Cannot bind more that one annotation on {}: {}", type.getName(), sj.toString());
				}

				validateAnnotation(annotations.get(i));

				if(checksum != null && this.checksum != null)
					throw AnnotationException.create("Cannot have more than one {} annotations on class {}", Checksum.class.getSimpleName(),
						type.getName());

				foundAnnotation = annotations.get(i);
			}
		}
		return foundAnnotation;
	}

	private static void validateAnnotation(final Annotation annotation) throws AnnotationException{
		final TemplateAnnotationValidator validator = TemplateAnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(annotation);
	}

	Class<T> getType(){
		return type;
	}

	MessageHeader getHeader(){
		return header;
	}

	List<BoundedField> getBoundedFields(){
		return boundedFields;
	}

	List<EvaluatedField> getEvaluatedFields(){
		return evaluatedFields;
	}

	boolean isChecksumPresent(){
		return (checksum != null);
	}

	BoundedField getChecksum(){
		return checksum;
	}

	boolean canBeCoded(){
		return (header != null && !boundedFields.isEmpty());
	}

	@Override
	public String toString(){
		return type.getSimpleName();
	}

	@Override
	@SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
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
