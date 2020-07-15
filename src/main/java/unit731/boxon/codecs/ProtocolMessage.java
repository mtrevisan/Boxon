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

import unit731.boxon.annotations.exceptions.AnnotationException;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T> The type of object the codec is able to decode/encode.
 */
final class ProtocolMessage<T>{

	/** Data associated to an annotated field */
	static class BoundedField{

		private final Field field;
		private final Skip[] skips;
		private final String condition;
		private final Annotation binding;


		private BoundedField(final Field field, final Skip[] skips, final String condition, final Annotation binding){
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
				if(!ReflectionHelper.isArrayOfPrimitives(type))
					throw new AnnotationException("Bad annotation used, @{} should have been used with type `{}.class`", BindArray.class.getSimpleName(),
						ReflectionHelper.PRIMITIVE_WRAPPER_MAP.getOrDefault(type.getComponentType(), type.getComponentType()).getSimpleName());
			}
		},
		ARRAY(BindArray.class){
			@Override
			void validate(final Annotation annotation){
				final BindArray binding = (BindArray)annotation;
				final Choices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				validateChoice(selectFrom, type);

				if(ReflectionHelper.isArrayOfPrimitives(type))
					throw new AnnotationException("Bad annotation used, @{} should have been used with type `{}[].class`", BindArrayPrimitive.class.getSimpleName(),
						ReflectionHelper.WRAPPER_PRIMITIVE_MAP.getOrDefault(type.getComponentType(), type.getComponentType()).getSimpleName());
			}
		},
		OBJECT(BindObject.class){
			@Override
			void validate(final Annotation annotation){
				final BindObject binding = (BindObject)annotation;
				final Choices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				validateChoice(selectFrom, type);
			}
		},
		DECIMAL(BindDecimal.class){
			@Override
			void validate(final Annotation annotation){
				final Class<?> type = ((BindDecimal)annotation).type();
				if(type != Float.class && type != Double.class)
					throw new AnnotationException("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(), Double.class.getSimpleName());
			}
		},
		CHECKSUM(BindChecksum.class){
			@Override
			void validate(final Annotation annotation){
				final Class<?> type = ((BindChecksum)annotation).type();
				if(!ReflectionHelper.isPrimitiveOrPrimitiveWrapper(type))
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

		private static void validateChoice(final Choices selectFrom, final Class<?> type){
			final int prefixSize = selectFrom.prefixSize();
			if(prefixSize > Integer.SIZE)
				throw new AnnotationException("`prefixSize` cannot be greater than {} bits", Integer.SIZE);

			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): null);
			if(type == Object.class && alternatives != null && alternatives.length == 0)
				throw new AnnotationException("`type` argument missing");
		}
	}


	private final Class<T> cls;

	private final MessageHeader header;
	private final List<BoundedField> boundedFields = new ArrayList<>(0);
	private final List<EvaluatedField> evaluatedFields = new ArrayList<>(0);
	/** necessary to speed-up the creation of a ProtocolMessage (technically not needed because it's already present somewhere inside {@link #boundedFields}) */
	private BoundedField checksum;


	/**
	 * Constructs a new {@link ProtocolMessage}.
	 *
	 * @param <T>	The type of the objects to be returned by the {@link ProtocolMessage}.
	 * @param type   The type of the objects to be returned by the {@link ProtocolMessage}.
	 * @param loader	The loader used to verify if a codec annotation is valid.
	 * @return	A new {@link ProtocolMessage} for the given type.
	 */
	static <T> ProtocolMessage<T> createFrom(final Class<T> type, final Loader loader){
		return new ProtocolMessage<>(type, loader);
	}

	private ProtocolMessage(final Class<T> cls, final Loader loader){
		this.cls = cls;

		header = cls.getAnnotation(MessageHeader.class);
		//retrieve all declared fields in the current class, therefore NOT in the parent classes
		loadAnnotatedFields(AnnotationHelper.getDeclaredFields(cls, true), loader);
	}

	private void loadAnnotatedFields(final Field[] fields, final Loader loader){
		for(final Field field : fields){
			final BindIf condition = field.getDeclaredAnnotation(BindIf.class);
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final BindChecksum checksum = field.getDeclaredAnnotation(BindChecksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final List<Annotation> boundedAnnotations = extractAnnotations(declaredAnnotations, loader);
			evaluatedFields.addAll(extractEvaluations(declaredAnnotations, field));

			validateField(boundedAnnotations, checksum);

			if(boundedAnnotations.size() == 1)
				boundedFields.add(new BoundedField(field, (skips.length > 0? skips: null),
					(condition != null? condition.value(): null), boundedAnnotations.get(0)));
			if(checksum != null)
				this.checksum = new BoundedField(field, null, null, checksum);
		}
	}

	private List<Annotation> extractAnnotations(final Annotation[] declaredAnnotations, final Loader loader){
		final List<Annotation> annotations = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations){
			final Class<? extends Annotation> annotationType = annotation.annotationType();
			if(annotationType != BindIf.class && annotationType != Skip.class && annotationType != Evaluate.class
					&& loader.getCodec(annotationType) != null)
				annotations.add(annotation);
		}
		return annotations;
	}

	private Collection<EvaluatedField> extractEvaluations(final Annotation[] declaredAnnotations, final Field field){
		final Collection<EvaluatedField> evaluations = new ArrayList<>(declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations)
			if(annotation.annotationType() == Evaluate.class)
				evaluations.add(new EvaluatedField(field, (Evaluate)annotation));
		return evaluations;
	}

	private void validateField(final List<Annotation> annotations, final BindChecksum checksum){
		if(annotations.size() > 1){
			final String aa = annotations.stream()
				.map(Annotation::annotationType)
				.distinct()
				.map(Class::getSimpleName)
				.collect(Collectors.joining(", ", "[", "]"));
			throw new AnnotationException("Cannot bind more that one annotation on {}: {}", cls.getSimpleName(), aa);
		}

		if(checksum != null && this.checksum != null)
			throw new AnnotationException("Cannot have more than one @{} annotations on class {}", BindChecksum.class.getSimpleName(), cls.getSimpleName());

		if(!annotations.isEmpty())
			validateAnnotation(annotations.get(0));
	}

	private void validateAnnotation(final Annotation annotation){
		final AnnotationValidator validator = AnnotationValidator.fromAnnotation(annotation);
		if(validator != null)
			validator.validate(annotation);
	}

	final Class<T> getType(){
		return cls;
	}

	final MessageHeader getHeader(){
		return header;
	}

	final List<BoundedField> getBoundedFields(){
		return boundedFields;
	}

	final List<EvaluatedField> getEvaluatedFields(){
		return evaluatedFields;
	}

	final BoundedField getChecksum(){
		return checksum;
	}

	final boolean canBeDecoded(){
		return (header != null && !boundedFields.isEmpty());
	}

	@Override
	public final String toString(){
		return cls.getSimpleName();
	}

}
