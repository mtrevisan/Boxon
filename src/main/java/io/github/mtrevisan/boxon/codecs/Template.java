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

import io.github.mtrevisan.boxon.annotations.BindArray;
import io.github.mtrevisan.boxon.annotations.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.BindChecksum;
import io.github.mtrevisan.boxon.annotations.BindDecimal;
import io.github.mtrevisan.boxon.annotations.BindObject;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.DataType;
import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.reflection.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.AnnotationHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the codec is able to decode/encode.
 */
final class Template<T>{

	private static final String EMPTY_STRING = "";


	/** Data associated to an annotated field. */
	static final class BoundedField{

		/** NOTE: MUST match the name of the method in all the annotations that defines a condition */
		private static final String CONDITION = "condition";

		private final Field field;
		private final Skip[] skips;
		private final Annotation binding;


		private BoundedField(final Field field, final Skip[] skips, final Annotation binding){
			this.field = field;
			this.skips = skips;
			this.binding = binding;
		}

		String getName(){
			return field.getName();
		}

		Skip[] getSkips(){
			return skips;
		}

		Annotation getBinding(){
			return binding;
		}

		String getCondition(){
			return ReflectionHelper.getMethodResponse(binding, CONDITION, EMPTY_STRING);
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

	private enum AnnotationValidator{
		ARRAY_PRIMITIVE(BindArrayPrimitive.class){
			@Override
			void validate(final Annotation annotation){
				final Class<?> type = ((BindArrayPrimitive)annotation).type();
				if(!DataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`", BindArray.class.getSimpleName(),
						DataType.toObjectiveTypeOrDefault(type).getSimpleName());
			}
		},

		ARRAY(BindArray.class){
			@Override
			void validate(final Annotation annotation){
				final BindArray binding = (BindArray)annotation;
				final ObjectChoices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				validateChoice(selectFrom);

				if(DataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`", BindArrayPrimitive.class.getSimpleName(),
						DataType.toPrimitiveTypeOrDefault(type).getSimpleName());
			}
		},

		OBJECT(BindObject.class){
			@Override
			void validate(final Annotation annotation){
				final BindObject binding = (BindObject)annotation;
				final ObjectChoices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				validateChoice(selectFrom);

				if(DataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used one of the primitive type's annotations", BindObject.class.getSimpleName());
			}
		},

		DECIMAL(BindDecimal.class){
			@Override
			void validate(final Annotation annotation){
				final Class<?> type = ((BindDecimal)annotation).type();
				if(type != float.class && type != Float.class && type != double.class && type != Double.class)
					throw new AnnotationException("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(), Double.class.getSimpleName());
			}
		},

		CHECKSUM(BindChecksum.class){
			@Override
			void validate(final Annotation annotation){
				final Class<?> type = ((BindChecksum)annotation).type();
				if(!DataType.isPrimitiveOrWrapper(type))
					throw new AnnotationException("Unrecognized type for field {}<{}>: {}", getClass().getSimpleName(), type.getSimpleName(), type.getComponentType().getSimpleName());
			}
		};

		private static final Map<Class<? extends Annotation>, AnnotationValidator> ANNOTATION_VALIDATORS = new HashMap<>(5);
		static{
			for(final AnnotationValidator b : values())
				ANNOTATION_VALIDATORS.put(b.annotationType, b);
		}

		private final Class<? extends Annotation> annotationType;

		AnnotationValidator(final Class<? extends Annotation> type){
			this.annotationType = type;
		}

		private static AnnotationValidator fromAnnotation(final Annotation annotation){
			return ANNOTATION_VALIDATORS.get(annotation.annotationType());
		}

		abstract void validate(final Annotation annotation);

		private static void validateChoice(final ObjectChoices selectFrom){
			final int prefixSize = selectFrom.prefixSize();
			if(prefixSize < 0)
				throw new AnnotationException("`prefixSize` must be a non-negative number");
			if(prefixSize > Integer.SIZE)
				throw new AnnotationException("`prefixSize` cannot be greater than {} bits", Integer.SIZE);

			final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
			if(prefixSize > 0){
				if(alternatives.length == 0)
					throw new AnnotationException("Alternatives missing");
				if(Arrays.stream(alternatives).noneMatch(a -> CodecHelper.CONTEXT_PREFIXED_CHOICE_PREFIX.matcher(a.condition()).find()))
					throw new AnnotationException("Any condition must contain a reference to the prefix");
				for(int i = 0; i < alternatives.length; i ++)
					if(alternatives[i].condition().isEmpty())
						throw new AnnotationException("Any condition must be non-empty, condition at index " + i + " is empty");
			}
			else if(Arrays.stream(alternatives).anyMatch(a -> CodecHelper.CONTEXT_PREFIXED_CHOICE_PREFIX.matcher(a.condition()).find()))
				throw new AnnotationException("Any condition cannot contain a reference to the prefix");
		}
	}


	private final Class<T> cls;

	private final MessageHeader header;
	private final DynamicArray<BoundedField> boundedFields = DynamicArray.create(BoundedField.class);
	private final DynamicArray<EvaluatedField> evaluatedFields = DynamicArray.create(EvaluatedField.class);
	/** necessary to speed-up the creation of a {@link Template} (technically not needed because it's already present somewhere inside {@link #boundedFields}). */
	private BoundedField checksum;


	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the objects to be returned by the {@link Template}.
	 * @param type	The type of the objects to be returned by the {@link Template}.
	 * @param hasCodec	The function to verify the presence of the codec.
	 * @return	A new {@link Template} for the given type.
	 */
	static <T> Template<T> createFrom(final Class<T> type, final Function<Class<? extends Annotation>, Boolean> hasCodec){
		return new Template<>(type, hasCodec);
	}

	private Template(final Class<T> cls, final Function<Class<? extends Annotation>, Boolean> hasCodec){
		this.cls = cls;

		header = cls.getAnnotation(MessageHeader.class);
		//retrieve all declared fields in the current class AND in the parent classes
		loadAnnotatedFields(AnnotationHelper.getDeclaredFields(cls, true), hasCodec);
	}

	private void loadAnnotatedFields(final DynamicArray<Field> fields, final Function<Class<? extends Annotation>, Boolean> hasCodec){
		boundedFields.ensureCapacity(fields.limit);
		for(int i = 0; i < fields.limit; i ++){
			final Field field = fields.data[i];
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final BindChecksum checksum = field.getDeclaredAnnotation(BindChecksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final DynamicArray<Annotation> boundedAnnotations = extractAnnotations(declaredAnnotations, hasCodec);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			validateField(boundedAnnotations, checksum);

			if(boundedAnnotations.limit == 1)
				boundedFields.add(new BoundedField(field, (skips.length > 0? skips: null), boundedAnnotations.data[0]));
			if(checksum != null)
				this.checksum = new BoundedField(field, null, checksum);
		}
	}

	private DynamicArray<Annotation> extractAnnotations(final Annotation[] declaredAnnotations, final Function<Class<? extends Annotation>, Boolean> hasCodec){
		final DynamicArray<Annotation> annotations = DynamicArray.create(Annotation.class, declaredAnnotations.length);
		for(int i = 0; i < declaredAnnotations.length; i ++){
			final Annotation annotation = declaredAnnotations[i];
			final Class<? extends Annotation> annotationType = annotation.annotationType();
			//NOTE: cannot throw an exception if the loader does not have the codec, due to the possible presence of other
			//annotations that have nothing to do with this library
			if(annotationType != Skip.class && annotationType != Evaluate.class && hasCodec.apply(annotationType))
				//stores only the preloaded codecs, ignore other annotations
				annotations.add(annotation);
		}
		return annotations;
	}

	private DynamicArray<EvaluatedField> extractEvaluations(final Annotation[] declaredAnnotations, final Field field){
		final DynamicArray<EvaluatedField> evaluations = DynamicArray.create(EvaluatedField.class, declaredAnnotations.length);
		for(int i = 0; i < declaredAnnotations.length; i ++){
			final Annotation annotation = declaredAnnotations[i];
			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(new EvaluatedField(field, (Evaluate)annotation));
		}
		return evaluations;
	}

	private void validateField(final DynamicArray<Annotation> annotations, final BindChecksum checksum){
		if(annotations.limit > 1){
			final StringJoiner sj = new StringJoiner(", ", "[", "]");
			annotations.join(annotation -> annotation.annotationType().getSimpleName(), sj);
			throw new AnnotationException("Cannot bind more that one annotation on {}: {}", cls.getSimpleName(), sj.toString());
		}

		if(checksum != null && this.checksum != null)
			throw new AnnotationException("Cannot have more than one {} annotations on class {}", BindChecksum.class.getSimpleName(), cls.getSimpleName());

		if(annotations.limit > 0)
			validateAnnotation(annotations.data[0]);
	}

	private void validateAnnotation(final Annotation annotation){
		final AnnotationValidator validator = AnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(annotation);
	}

	Class<T> getType(){
		return cls;
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
		return cls.getSimpleName();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Template<?> rhs = (Template<?>)obj;
		return (cls == rhs.cls);
	}

	@Override
	public int hashCode(){
		return cls.getName().hashCode();
	}

}
