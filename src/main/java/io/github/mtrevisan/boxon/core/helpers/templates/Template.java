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
import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.core.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.core.helpers.FieldAccessor;
import io.github.mtrevisan.boxon.core.helpers.validators.TemplateAnnotationValidator;
import io.github.mtrevisan.boxon.core.parsers.TemplateLoader;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The class containing the information that are used to decode/encode objects.
 *
 * @param <T>	The type of object the codec is able to decode/encode.
 */
public final class Template<T>{

	private record Triplet(List<TemplateField> templateFields, List<EvaluatedField<Evaluate>> evaluatedFields,
			List<EvaluatedField<PostProcess>> postProcessedFields){
		private static Triplet of(final List<TemplateField> templateFields, final List<EvaluatedField<Evaluate>> evaluatedFields,
				final List<EvaluatedField<PostProcess>> postProcessedFields){
			return new Triplet(
				Collections.unmodifiableList(templateFields),
				Collections.unmodifiableList(evaluatedFields),
				Collections.unmodifiableList(postProcessedFields));
		}
	}


	private final Class<T> type;

	private final TemplateHeader header;
	private final List<TemplateField> templateFields;
	private final List<EvaluatedField<Evaluate>> evaluatedFields;
	private final List<EvaluatedField<PostProcess>> postProcessedFields;
	/**
	 * Necessary to speed up the creation of a {@link Template} (technically not needed because it's already present
	 * somewhere inside {@link #templateFields}).
	 */
	private TemplateField checksum;


	/**
	 * Create an instance of a template.
	 *
	 * @param type	The template class.
	 * @param <T>	The class type of the template.
	 * @return	An instance of a template.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	public static <T> Template<T> create(final Class<T> type) throws AnnotationException{
		return new Template<>(type);
	}


	private Template(final Class<T> type)
			throws AnnotationException{
		this.type = type;

		header = type.getAnnotation(TemplateHeader.class);
		//(`ObjectChoices` and `ObjectChoicesList` alternatives may not have a `TemplateHeader`)
		if(header != null){
			final TemplateAnnotationValidator headerValidator = TemplateAnnotationValidator.fromAnnotationType(TemplateHeader.class);
			headerValidator.validate(null, header);
		}

		final Triplet fields = loadAnnotatedFields(type);
		templateFields = fields.templateFields;
		evaluatedFields = fields.evaluatedFields;
		postProcessedFields = fields.postProcessedFields;

		if(templateFields.isEmpty())
			throw AnnotationException.create("No data can be extracted from this class: {}", getName());
	}


	private Triplet loadAnnotatedFields(final Class<T> templateType) throws AnnotationException{
		final List<Field> fields = FieldAccessor.getAccessibleFields(templateType);

		final int length = fields.size();
		final List<TemplateField> templateFields = new ArrayList<>(length);
		final List<EvaluatedField<Evaluate>> evaluatedFields = new ArrayList<>(length);
		final List<EvaluatedField<PostProcess>> postProcessedFields = new ArrayList<>(length);
		for(int i = 0; i < length; i ++){
			final Field field = fields.get(i);

			try{
				//FIXME a cycle between classes
				final Annotation[] declaredAnnotations = TemplateLoader.extractBaseAnnotations(field.getDeclaredAnnotations());
				TemplateValidator.validateAnnotationsOrder(declaredAnnotations);

				final TemplateField templateField = createField(declaredAnnotations, field);
				if(templateField != null)
					templateFields.add(templateField);

				final Checksum checksum = field.getDeclaredAnnotation(Checksum.class);
				loadChecksumField(checksum, field);

				evaluatedFields.addAll(TemplateExtractor.extractAnnotation(Evaluate.class, declaredAnnotations, field));

				postProcessedFields.addAll(TemplateExtractor.extractAnnotation(PostProcess.class, declaredAnnotations, field));
			}
			catch(final AnnotationException ae){
				ae.withClassAndField(templateType, field);
				throw ae;
			}
		}
		return Triplet.of(templateFields, evaluatedFields, postProcessedFields);
	}

	private static TemplateField createField(final Annotation[] declaredAnnotations, final Field field) throws AnnotationException{
		//FIXME a cycle between classes
		final List<Annotation> boundedAnnotations = TemplateLoader.filterAnnotationsWithCodec(declaredAnnotations);
		final Annotation validAnnotation = TemplateExtractor.extractAndValidateAnnotation(field.getType(), boundedAnnotations);
		final List<SkipParams> skips = TemplateExtractor.extractSkips(declaredAnnotations);

		TemplateField templateField = null;
		if(validAnnotation != null || !skips.isEmpty()){
			final Annotation collectionAnnotation = TemplateExtractor.extractCollectionAnnotation(boundedAnnotations);
			final List<ContextParameter> contextParameters = TemplateExtractor.extractContextParameters(boundedAnnotations);

			templateField = TemplateField.create(field, validAnnotation)
				.withCollectionBinding(collectionAnnotation)
				.withSkips(skips)
				.withContextParameters(contextParameters);
		}
		return templateField;
	}

	private void loadChecksumField(final Checksum checksum, final Field field) throws AnnotationException{
		if(checksum != null){
			if(this.checksum != null)
				throw AnnotationException.create("Cannot have more than one {} annotations",
					Checksum.class.getSimpleName());

			this.checksum = TemplateField.create(field, checksum);
		}
	}


	/**
	 * The class type of the template.
	 *
	 * @return	The class type.
	 */
	public Class<T> getType(){
		return type;
	}

	/**
	 * The name of the template.
	 *
	 * @return	The name of the template.
	 */
	public String getName(){
		return type.getName();
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
		return Collections.unmodifiableList(templateFields);
	}

	/**
	 * List of {@link EvaluatedField evaluated fields}.
	 *
	 * @return	List of evaluated fields.
	 */
	public List<EvaluatedField<Evaluate>> getEvaluatedFields(){
		return Collections.unmodifiableList(evaluatedFields);
	}

	/**
	 * List of {@link EvaluatedField processed fields}.
	 *
	 * @return	List of processed fields.
	 */
	public List<EvaluatedField<PostProcess>> getPostProcessedFields(){
		return Collections.unmodifiableList(postProcessedFields);
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
		return (header != null && !templateFields.isEmpty());
	}

	public Object createEmptyObject(){
		return ConstructorHelper.getEmptyCreator(type)
			.get();
	}


	@Override
	public String toString(){
		return getName();
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
		return getName().hashCode();
	}

}
