/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.function.Function;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the codec is able to decode/encode.
 */
final class Template<T>{

	/** Data associated to an annotated field. */
	static final class BoundedField{

		/** NOTE: MUST match the name of the method in all the annotations that defines a condition! */
		private static final String CONDITION = "condition";

		private static final String EMPTY_STRING = "";

		private final Field field;
		private final Skip[] skips;
		private final Annotation binding;

		private final Method condition;
		//FIXME https://www.jboss.org/optaplanner/blog/2018/01/09/JavaReflectionButMuchFaster.html
		//extract getter and setter?


		private BoundedField(final Field field, final Annotation binding){
			this(field, binding, null);
		}

		private BoundedField(final Field field, final Annotation binding, final Skip[] skips){
			this.field = field;
			this.binding = binding;
			this.skips = skips;

			//pre-fetch condition method
			condition = ReflectionHelper.getAccessibleMethod(binding.annotationType(), CONDITION, String.class);
		}

		String getFieldName(){
			return field.getName();
		}

		<T> T getFieldValue(final Object obj){
			return ReflectionHelper.getFieldValue(field, obj);
		}

		void setFieldValue(final Object obj, final Object value){
			ReflectionHelper.setFieldValue(field, obj, value);
		}

		Annotation getBinding(){
			return binding;
		}

		String getCondition(){
			return ReflectionHelper.invokeMethod(binding, condition, EMPTY_STRING);
		}

		Skip[] getSkips(){
			return skips;
		}
	}

	/** Data associated to a directly evaluable field. */
	static final class EvaluatedField{

		private final Field field;
		private final Evaluate binding;


		private EvaluatedField(final Field field, final Evaluate binding){
			this.field = field;
			this.binding = binding;
		}

		String getFieldName(){
			return field.getName();
		}

		Class<?> getFieldType(){
			return field.getType();
		}

		void setFieldValue(final Object obj, final Object value){
			ReflectionHelper.setFieldValue(field, obj, value);
		}

		Evaluate getBinding(){
			return binding;
		}
	}


	private final Class<T> type;

	private final MessageHeader header;
	private final DynamicArray<BoundedField> boundedFields = DynamicArray.create(BoundedField.class);
	private final DynamicArray<EvaluatedField> evaluatedFields = DynamicArray.create(EvaluatedField.class);
	/**
	 * Necessary to speed-up the creation of a {@link Template} (technically not needed because it's already present
	 * somewhere inside {@link #boundedFields}).
	 */
	private BoundedField checksum;


	Template(final Class<T> type, final Function<Annotation[], DynamicArray<Annotation>> filterAnnotationsWithCodec) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(MessageHeader.class);
		if(header != null)
			CodecHelper.assertCharset(header.charset());

		loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type), filterAnnotationsWithCodec);

		if(boundedFields.isEmpty())
			throw new AnnotationException("No data can be extracted from this class: {}", type.getName());
	}

	private void loadAnnotatedFields(final Class<T> type, final DynamicArray<Field> fields, final Function<Annotation[], DynamicArray<Annotation>> filterAnnotationsWithCodec)
			throws AnnotationException{
		boundedFields.ensureCapacity(fields.limit);
		for(int i = 0; i < fields.limit; i ++){
			final Field field = fields.data[i];
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final DynamicArray<Annotation> boundedAnnotations = filterAnnotationsWithCodec.apply(declaredAnnotations);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			try{
				validateField(boundedAnnotations, checksum);
			}
			catch(final AnnotationException e){
				e.setClassNameAndFieldName(type.getName(), field.getName());
				throw e;
			}

			if(boundedAnnotations.limit == 1)
				boundedFields.add(new BoundedField(field, boundedAnnotations.data[0], (skips.length > 0? skips: null)));
			if(checksum != null)
				this.checksum = new BoundedField(field, checksum);
		}
	}

	private DynamicArray<EvaluatedField> extractEvaluations(final Annotation[] declaredAnnotations, final Field field){
		final DynamicArray<EvaluatedField> evaluations = DynamicArray.create(EvaluatedField.class, declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations){
			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(new EvaluatedField(field, (Evaluate)annotation));
		}
		return evaluations;
	}

	private void validateField(final DynamicArray<? extends Annotation> annotations, final Checksum checksum)
			throws AnnotationException{
		if(annotations.limit > 1){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			annotations.join(annotation -> annotation.annotationType().getSimpleName(), sj);
			throw new AnnotationException("Cannot bind more that one annotation on {}: {}", type.getName(), sj.toString());
		}

		if(checksum != null && this.checksum != null)
			throw new AnnotationException("Cannot have more than one {} annotations on class {}", Checksum.class.getSimpleName(),
				type.getName());

		if(annotations.limit > 0)
			validateAnnotation(annotations.data[0]);
	}

	private void validateAnnotation(final Annotation annotation) throws AnnotationException{
		final AnnotationValidator validator = AnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(annotation);
	}

	Class<T> getType(){
		return type;
	}

	MessageHeader getHeader(){
		return header;
	}

	DynamicArray<BoundedField> getBoundedFields(){
		return boundedFields;
	}

	DynamicArray<EvaluatedField> getEvaluatedFields(){
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
