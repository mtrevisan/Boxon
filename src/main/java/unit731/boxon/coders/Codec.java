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
package unit731.boxon.coders;

import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindDecimal;
import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.Evaluate;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindIf;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.Skip;
import unit731.boxon.helpers.AnnotationHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the coder is able to decode/encode.
 */
class Codec<T>{

	/** Data associated to an annotated field */
	static class BoundedField{

		private final Field field;
		private final Skip[] skips;
		private final String condition;
		private final Annotation binding;


		private BoundedField(final Field field, final Skip[] skips, final String condition, final Annotation binding){
			Objects.requireNonNull(field);
			Objects.requireNonNull(binding);

			this.field = field;
			this.skips = skips;
			this.condition = condition;
			this.binding = binding;
		}

		String getName(){
			return field.getName();
		}

		Skip[] getSkips(){
			return skips;
		}

		String getCondition(){
			return condition;
		}

		Annotation getBinding(){
			return binding;
		}
	}

	/** Data associated to a directly evaluable field */
	static class EvaluatedField{

		private final Field field;
		private final Evaluate binding;


		private EvaluatedField(final Field field, final Evaluate binding){
			Objects.requireNonNull(field);
			Objects.requireNonNull(binding);

			this.field = field;
			this.binding = binding;
		}

		String getName(){
			return field.getName();
		}

		Class<?> getType(){
			return field.getType();
		}

		Evaluate getBinding(){
			return binding;
		}
	}


	private final Class<T> cls;

	private final MessageHeader header;
	private final List<BoundedField> boundedFields = new ArrayList<>(0);
	private final List<EvaluatedField> evaluatedFields = new ArrayList<>(0);
	/** necessary to speed-up the creation of a Codec (technically not needed because it's already present somewhere inside {@link #boundedFields}) */
	private BoundedField checksum;


	/**
	 * Constructs a new {@link Codec}.
	 *
	 * @param <T>	The type of the objects to be returned by the {@link Codec}.
	 * @param type	The type of the objects to be returned by the {@link Codec}.
	 * @return	A new {@link Codec} for the given type.
	 */
	static <T> Codec<T> createFrom(final Class<T> type){
		return new Codec<>(type);
	}

	private Codec(final Class<T> cls){
		Objects.requireNonNull(cls);

		this.cls = cls;

		header = cls.getAnnotation(MessageHeader.class);
		//retrieve all declared fields in the current class, therefore NOT in the parent classes
		loadAnnotatedFields(AnnotationHelper.getDeclaredFields(cls, true));
	}

	private void loadAnnotatedFields(final Field[] fields){
		for(final Field field : fields){
			final BindIf condition = field.getDeclaredAnnotation(BindIf.class);
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final BindChecksum checksum = field.getDeclaredAnnotation(BindChecksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final List<Annotation> boundedAnnotations = extractAnnotations(declaredAnnotations);
			final List<Evaluate> evaluatedAnnotations = extractEvaluations(declaredAnnotations);
			for(final Evaluate evaluatedAnnotation : evaluatedAnnotations)
				evaluatedFields.add(new EvaluatedField(field, evaluatedAnnotation));

			validateField(boundedAnnotations, checksum);

			if(boundedAnnotations.size() == 1)
				boundedFields.add(new BoundedField(field, (skips.length > 0? skips: null), (condition != null? condition.value(): null), boundedAnnotations.get(0)));
			if(checksum != null)
				this.checksum = new BoundedField(field, null, null, checksum);
		}
	}

	private List<Annotation> extractAnnotations(final Annotation[] declaredAnnotations){
		final List<Annotation> annotations = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations){
			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType != BindIf.class && annotationType != Skip.class && annotationType != Evaluate.class)
				annotations.add(annotation);
		}
		return annotations;
	}

	private List<Evaluate> extractEvaluations(final Annotation[] declaredAnnotations){
		final List<Evaluate> annotations = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations){
			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType == Evaluate.class)
				annotations.add((Evaluate)annotation);
		}
		return annotations;
	}

	private void validateField(final List<Annotation> annotations, final BindChecksum checksum){
		if(annotations.size() > 1){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final Annotation annotation : annotations)
				sj.add(annotation.annotationType().getSimpleName());
			throw new IllegalArgumentException("Cannot bind more that one annotation on " + cls.getSimpleName() + ": " + sj.toString());
		}

		if(checksum != null && this.checksum != null)
			throw new IllegalArgumentException("Cannot have more than one @" + BindChecksum.class.getSimpleName()
				+ " annotations on class " + cls.getSimpleName());

		if(!annotations.isEmpty()){
			final Annotation annotation = annotations.get(0);
			if(annotation instanceof BindArrayPrimitive)
				validateAnnotation((BindArrayPrimitive)annotation);
			else if(annotation instanceof BindArray)
				validateAnnotation((BindArray)annotation);
			else if(annotation instanceof BindObject)
				validateAnnotation((BindObject)annotation);
			else if(annotation instanceof BindDecimal)
				validateAnnotation((BindDecimal)annotation);
			else if(annotation instanceof BindChecksum)
				validateAnnotation((BindChecksum)annotation);
		}
	}

	private void validateAnnotation(final BindArrayPrimitive binding){
		final Class<?> type = binding.type();
		final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
		if(!isPrimitive)
			throw new IllegalArgumentException("Bad annotation used, @" + BindArray.class.getSimpleName()
				+ " should have been used with type `" + type.getSimpleName() + ".class`");

		final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());
		if(objectiveType == null){
			final Codec<?> codec = Codec.createFrom(type.getComponentType());
			throw new IllegalArgumentException("Unrecognized type for field " + codec.getClass().getSimpleName() + "<"
				+ codec + ">: " + type.getComponentType().getSimpleName());
		}
	}

	private void validateAnnotation(final BindArray binding){
		final Choices selectFrom = binding.selectFrom();
		final Class<?> type = binding.type();
		validateChoice(selectFrom, type);

		final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
		if(isPrimitive)
			throw new IllegalArgumentException("Bad annotation used, @" + BindArrayPrimitive.class.getSimpleName()
				+ " should have been used with type `" + type.getSimpleName() + ".class`");
	}

	private void validateAnnotation(final BindObject binding){
		final Choices selectFrom = binding.selectFrom();
		final Class<?> type = binding.type();
		validateChoice(selectFrom, type);
	}

	private void validateChoice(final Choices selectFrom, final Class<?> type){
		final int prefixSize = selectFrom.prefixSize();
		if(prefixSize > Integer.SIZE)
			throw new IllegalArgumentException("`prefixSize` cannot be greater than " + Integer.SIZE + " bits");

		@SuppressWarnings("ConstantConditions")
		final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);
		if(type == Object.class && alternatives.length == 0)
			throw new IllegalArgumentException("`type` argument missing");
	}

	private void validateAnnotation(final BindDecimal binding){
		final Class<?> type = binding.type();
		if(type != Float.class && type != Double.class)
			throw new IllegalArgumentException("Bad type, should have been one of `" + Float.class.getSimpleName()
				+ ".class` or `" + Double.class.getSimpleName() + ".class`");
	}

	private void validateAnnotation(final BindChecksum binding){
		final Class<?> type = binding.type();
		final Class<?> objectiveType = ReflectionHelper.objectiveType(type);
		if(objectiveType == null)
			throw new IllegalArgumentException("Unrecognized type for field " + getClass().getSimpleName()
				+ "<" + type.getSimpleName() + ">: " + type.getComponentType().getSimpleName());
	}

	Class<T> getType(){
		return cls;
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

	BoundedField getChecksum(){
		return checksum;
	}

	boolean canBeDecoded(){
		return (header != null && !boundedFields.isEmpty());
	}

	@Override
	public String toString(){
		return cls.getSimpleName();
	}

}
