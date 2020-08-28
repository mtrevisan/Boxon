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
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindDecimal;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.ParserDataType;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Predicate;


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

	private enum AnnotationValidator{
		ARRAY_PRIMITIVE(BindArrayPrimitive.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final Class<?> type = ((BindArrayPrimitive)annotation).type();
				if(!ParserDataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
						BindArray.class.getSimpleName(), ParserDataType.toObjectiveTypeOrDefault(type).getSimpleName());
			}
		},

		ARRAY(BindArray.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final BindArray binding = (BindArray)annotation;
				final ObjectChoices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				if(ParserDataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used the type `{}.class`",
						BindArrayPrimitive.class.getSimpleName(), ParserDataType.toPrimitiveTypeOrDefault(type).getSimpleName());

				validateChoice(selectFrom, type);
			}
		},

		OBJECT(BindObject.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final BindObject binding = (BindObject)annotation;
				final ObjectChoices selectFrom = binding.selectFrom();
				final Class<?> type = binding.type();
				if(ParserDataType.isPrimitive(type))
					throw new AnnotationException("Bad annotation used for {}, should have been used one of the primitive type's annotations",
						BindObject.class.getSimpleName());

				validateChoice(selectFrom, type);
			}
		},

		DECIMAL(BindDecimal.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final Class<?> type = ((BindDecimal)annotation).type();
				final ParserDataType dataType = ParserDataType.fromType(type);
				if(dataType != ParserDataType.FLOAT && dataType != ParserDataType.DOUBLE)
					throw new AnnotationException("Bad type, should have been one of `{}.class` or `{}.class`", Float.class.getSimpleName(),
						Double.class.getSimpleName());
			}
		},

		STRING(BindString.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final BindString binding = (BindString)annotation;
				CodecHelper.assertCharset(binding.charset());
			}
		},

		STRING_TERMINATED(BindStringTerminated.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final BindStringTerminated binding = (BindStringTerminated)annotation;
				CodecHelper.assertCharset(binding.charset());
			}
		},

		CHECKSUM(Checksum.class){
			@Override
			void validate(final Annotation annotation) throws AnnotationException{
				final Class<?> type = ((Checksum)annotation).type();
				if(!ParserDataType.isPrimitiveOrWrapper(type))
					throw new AnnotationException("Unrecognized type for field {}.{}: {}", getClass().getName(), type.getSimpleName(),
						type.getComponentType().getSimpleName());
			}
		};

		private static final Map<Class<? extends Annotation>, AnnotationValidator> ANNOTATION_VALIDATORS = new HashMap<>(5);
		static{
			for(final AnnotationValidator validator : values())
				ANNOTATION_VALIDATORS.put(validator.annotationType, validator);
		}

		private final Class<? extends Annotation> annotationType;

		AnnotationValidator(final Class<? extends Annotation> type){
			annotationType = type;
		}

		private static AnnotationValidator fromAnnotation(final Annotation annotation){
			return ANNOTATION_VALIDATORS.get(annotation.annotationType());
		}

		abstract void validate(final Annotation annotation) throws AnnotationException;

		private static void validateChoice(final ObjectChoices selectFrom, final Class<?> type) throws AnnotationException{
			final int prefixSize = selectFrom.prefixSize();
			if(prefixSize < 0)
				throw new AnnotationException("Prefix size must be a non-negative number");
			if(prefixSize > Integer.SIZE)
				throw new AnnotationException("Prefix size cannot be greater than {} bits", Integer.SIZE);

			final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
			final boolean hasPrefixSize = (prefixSize > 0);
			if(hasPrefixSize && alternatives.length == 0)
				throw new AnnotationException("No alternatives");
			for(final ObjectChoices.ObjectChoice alternative : alternatives){
				if(!type.isAssignableFrom(alternative.type()))
					throw new AnnotationException("Type of alternative cannot be assigned to (super) type of annotation");

				final String condition = alternative.condition();
				if(condition.isEmpty())
					throw new AnnotationException("All conditions must be non-empty");
				if(hasPrefixSize ^ CodecHelper.containsPrefixReference(condition))
					throw new AnnotationException("All conditions must " + (hasPrefixSize? "": "not ") + "contain a reference to the prefix");
			}
		}
	}


	private final Class<T> type;

	private final MessageHeader header;
	private final DynamicArray<BoundedField> boundedFields = DynamicArray.create(BoundedField.class);
	private final DynamicArray<EvaluatedField> evaluatedFields = DynamicArray.create(EvaluatedField.class);
	/** necessary to speed-up the creation of a {@link Template}
	 * (technically not needed because it's already present somewhere inside {@link #boundedFields}). */
	private BoundedField checksum;


	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the objects to be returned by the {@link Template}.
	 * @param type	The type of the objects to be returned by the {@link Template}.
	 * @param hasCodec	The function to verify the presence of the codec.
	 * @return	A new {@link Template} for the given type.
	 */
	static <T> Template<T> createFrom(final Class<T> type, final Predicate<Class<? extends Annotation>> hasCodec)
			throws AnnotationException{
		return new Template<>(type, hasCodec);
	}

	private Template(final Class<T> type, final Predicate<Class<? extends Annotation>> hasCodec) throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(MessageHeader.class);
		if(header != null)
			CodecHelper.assertCharset(header.charset());

		loadAnnotatedFields(type, ReflectionHelper.getAccessibleFields(type), hasCodec);
	}

	private void loadAnnotatedFields(final Class<T> type, final DynamicArray<Field> fields, final Predicate<? super Class<? extends Annotation>> hasCodec)
			throws AnnotationException{
		boundedFields.ensureCapacity(fields.limit);
		for(int i = 0; i < fields.limit; i ++){
			final Field field = fields.data[i];
			final Skip[] skips = field.getDeclaredAnnotationsByType(Skip.class);
			final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);

			final Annotation[] declaredAnnotations = field.getDeclaredAnnotations();
			final DynamicArray<Annotation> boundedAnnotations = extractAnnotations(declaredAnnotations, hasCodec);
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

	private DynamicArray<Annotation> extractAnnotations(final Annotation[] declaredAnnotations,
			final Predicate<? super Class<? extends Annotation>> hasCodec){
		final DynamicArray<Annotation> annotations = DynamicArray.create(Annotation.class, declaredAnnotations.length);
		for(final Annotation annotation : declaredAnnotations){
			final Class<? extends Annotation> annotationType = annotation.annotationType();
			//NOTE: cannot throw an exception if the loader does not have the codec, due to the possible presence of other
			//annotations that have nothing to do with this library
			if(annotationType != Skip.class && annotationType != Evaluate.class && hasCodec.test(annotationType))
				//stores only the preloaded codecs, ignore other annotations
				annotations.add(annotation);
		}
		return annotations;
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
