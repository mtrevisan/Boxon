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
package unit731.boxon.codecs;

import unit731.boxon.annotations.Evaluate;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindIf;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.Skip;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the coder is able to decode/encode.
 *
 * @see <a href="https://docs.spring.io/spring/docs/5.2.7.RELEASE/spring-framework-reference/core.html#expressions">Spring Expression Language (SpEL)</a>
 */
class Codec<T>{

	/** Stores the package in which an annotation resides */
	private static final String ANNOTATIONS_PACKAGE;
	static{
		final String annotationCanonicalName = BindIf.class.getCanonicalName();
		ANNOTATIONS_PACKAGE = "@" + annotationCanonicalName.substring(0, annotationCanonicalName.lastIndexOf('.') + 1);
	}


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
		loadAnnotatedFields(getDeclaredFields(cls, true));
	}

	/**
	 * Retrieving fields list of specified class
	 * If `recursively` is {@code true}, retrieving fields from all class hierarchy
	 */
	private Field[] getDeclaredFields(final Class<T> cls, @SuppressWarnings("SameParameterValue") final boolean recursively){
		if(recursively){
			final List<Field> fields = new ArrayList<>();
			Class<? super T> currentType = cls;
			while(currentType != null){
				final List<Field> subfields = Arrays.asList(currentType.getDeclaredFields());
				//place parent's fields before all the child's fields
				fields.addAll(0, subfields);

				currentType = currentType.getSuperclass();
			}
			return fields.toArray(Field[]::new);
		}
		else
			return cls.getDeclaredFields();
	}

	private void loadAnnotatedFields(final Field[] fields){
		for(final Field field : fields){
			final BindIf condition = field.getDeclaredAnnotation(BindIf.class);
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final BindChecksum checksum = field.getDeclaredAnnotation(BindChecksum.class);
			final List<Annotation> annotations = Arrays.stream(field.getDeclaredAnnotations())
				//filter annotations that belong to parsing procedure
				.filter(annotation -> annotation.toString().startsWith(ANNOTATIONS_PACKAGE))
				//filter conditions
				.filter(annotation -> annotation.annotationType() != BindIf.class && annotation.annotationType() != Skip.class)
				.collect(Collectors.toList());
			final List<Annotation> boundedAnnotations = new ArrayList<>();
			for(final Annotation annotation : annotations){
				if(annotation.annotationType() == Evaluate.class)
					evaluatedFields.add(new EvaluatedField(field, (Evaluate)annotation));
				else
					boundedAnnotations.add(annotation);
			}

			validateAnnotation(checksum, boundedAnnotations);

			if(boundedAnnotations.size() == 1)
				boundedFields.add(new BoundedField(field, (skips.length > 0? skips: null), (condition != null? condition.value(): null), boundedAnnotations.get(0)));
			if(checksum != null)
				this.checksum = new BoundedField(field, null, null, checksum);
		}
	}

	private void validateAnnotation(final BindChecksum checksum, final List<Annotation> annotations){
		if(annotations.size() > 1){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			for(final Annotation annotation : annotations)
				sj.add(annotation.annotationType().getSimpleName());
			throw new IllegalArgumentException("Cannot bind more that one annotation on " + cls.getSimpleName() + ": " + sj.toString());
		}
		if(checksum != null && this.checksum != null)
			throw new IllegalArgumentException("Cannot have more than one @" + BindChecksum.class.getSimpleName()
				+ " annotations on class " + cls.getSimpleName());
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
