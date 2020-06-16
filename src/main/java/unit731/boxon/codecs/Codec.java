package unit731.boxon.codecs;

import unit731.boxon.annotations.Assign;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindIf;
import unit731.boxon.annotations.MessageHeader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
		private final String condition;
		private final Annotation binding;


		private BoundedField(final Field field, final String condition, final Annotation binding){
			Objects.requireNonNull(field);
			Objects.requireNonNull(binding);

			this.field = field;
			this.condition = condition;
			this.binding = binding;
		}

		String getName(){
			return field.getName();
		}

		String getCondition(){
			return condition;
		}

		Annotation getBinding(){
			return binding;
		}
	}

	/** Data associated to a directly assignable field */
	static class AssignedField{

		private final Field field;
		private final Assign binding;


		private AssignedField(final Field field, final Assign binding){
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

		Assign getBinding(){
			return binding;
		}
	}


	private final Class<T> cls;

	private final MessageHeader header;
	private final List<BoundedField> boundedFields = new ArrayList<>(0);
	private final List<AssignedField> assignedFields = new ArrayList<>(0);

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
		loadAnnotatedFields(getDeclaredFields(cls, false));
	}

	/**
	 * Retrieving fields list of specified class
	 * If `recursively` is {@code true}, retrieving fields from all class hierarchy
	 */
	private Field[] getDeclaredFields(final Class<T> cls, final boolean recursively){
		if(recursively){
			final List<Field> fields = new ArrayList<>();
			Class<? super T> currentType = cls;
			while(currentType != null){
				final Field[] declaredFieldsOfSuper = currentType.getDeclaredFields();
				Collections.addAll(fields, declaredFieldsOfSuper);

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
			final BindChecksum checksum = field.getDeclaredAnnotation(BindChecksum.class);
			final List<Annotation> annotations = Arrays.stream(field.getDeclaredAnnotations())
				//filter annotations that belong to parsing procedure
				.filter(annotation -> annotation.toString().startsWith(ANNOTATIONS_PACKAGE))
				//filter conditions
				.filter(annotation -> annotation.annotationType() != BindIf.class)
				.collect(Collectors.toList());
			final List<Annotation> boundedAnnotations = new ArrayList<>();
			for(final Annotation annotation : annotations){
				if(annotation.annotationType() == Assign.class)
					assignedFields.add(new AssignedField(field, (Assign)annotation));
				else
					boundedAnnotations.add(annotation);
			}

			validateAnnotation(checksum, boundedAnnotations);

			if(boundedAnnotations.size() == 1)
				boundedFields.add(new BoundedField(field, (condition != null? condition.value(): null), boundedAnnotations.get(0)));
			if(checksum != null)
				this.checksum = new BoundedField(field, null, checksum);
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

	List<AssignedField> getAssignedFields(){
		return assignedFields;
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
