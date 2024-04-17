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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;


/**
 * Declarative data binding parser for message templates.
 */
public final class TemplateParser implements TemplateParserInterface{

	private final LoaderCodecInterface loaderCodec;
	private final Evaluator evaluator;

	private final LoaderTemplate loaderTemplate;

	private EventListener eventListener;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param evaluator	An evaluator.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodecInterface loaderCodec, final Evaluator evaluator){
		return new TemplateParser(loaderCodec, evaluator);
	}

	private TemplateParser(final LoaderCodecInterface loaderCodec, final Evaluator evaluator){
		this.loaderCodec = loaderCodec;
		this.evaluator = evaluator;

		loaderTemplate = LoaderTemplate.create(loaderCodec);

		withEventListener(EventListener.getNoOpInstance());
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	public TemplateParser withEventListener(final EventListener eventListener){
		if(eventListener != null){
			this.eventListener = eventListener;

			loaderTemplate.withEventListener(eventListener);
		}

		return this;
	}

	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public TemplateParser withTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplatesFrom(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If the template error occurs.
	 */
	public TemplateParser withTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplate(templateClass);

		return this;
	}


	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation error occurs.
	 */
	@Override
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return loaderTemplate.createTemplate(type);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader from which to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	public Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		return loaderTemplate.getTemplate(reader);
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	public Template<?> getTemplate(final Class<?> type) throws TemplateException{
		return loaderTemplate.getTemplate(type);
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReaderInterface reader){
		return loaderTemplate.findNextMessageIndex(reader);
	}


	/**
	 * Decodes a message using the provided template and reader.
	 *
	 * @param <T>	The type of the object to be returned as a result of decoding.
	 * @param template	The template used for decoding the message.
	 * @param reader	The reader used for reading the message.
	 * @param parentObject	The parent object of the message being decoded.
	 * @return	The decoded object.
	 * @throws FieldException	If there is an error decoding a field.
	 */
	@Override
	public <T> T decode(final Template<T> template, final BitReaderInterface reader, final Object parentObject) throws FieldException{
		final int startPosition = reader.position();

		T currentObject = createEmptyObject(template);

		final ParserContext<T> parserContext = prepareParserContext(parentObject, currentObject);

		//decode message fields:
		decodeMessageFields(template, reader, parserContext);

		processEvaluatedFields(template, parserContext);

		postProcessFields(template, parserContext);

		readMessageTerminator(template, reader);

		currentObject = parserContext.getCurrentObject();
		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private static <T> T createEmptyObject(final Template<T> template){
		return ConstructorHelper.getEmptyCreator(template.getType())
			.get();
	}

	private <T> ParserContext<T> prepareParserContext(final Object parentObject, final T currentObject){
		//FIXME is there a way to reduce the number of ParserContext objects?
		final ParserContext<T> parserContext = new ParserContext<>(currentObject, parentObject);
		evaluator.addCurrentObjectToEvaluatorContext(currentObject);
		return parserContext;
	}

	private <T> void decodeMessageFields(final Template<T> template, final BitReaderInterface reader, final ParserContext<T> parserContext)
			throws FieldException{
		final Object rootObject = parserContext.getRootObject();

		final List<TemplateField> fields = template.getTemplateFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final SkipParams[] skips = field.getSkips();
			readSkips(skips, reader, rootObject);

			//check if field has to be processed...
			final boolean shouldProcessField = shouldProcessField(field.getCondition(), rootObject);
			if(shouldProcessField)
				//... and if so, process it
				decodeField(template, reader, parserContext, field);
		}
	}

	private void readSkips(final SkipParams[] skips, final BitReaderInterface reader, final Object rootObject){
		for(int i = 0, length = skips.length; i < length; i ++)
			readSkip(skips[i], reader, rootObject);
	}

	private void readSkip(final SkipParams skip, final BitReaderInterface reader, final Object rootObject){
		final boolean process = shouldProcessField(skip.condition(), rootObject);
		if(!process)
			return;

		//choose between skip-by-size and skip-by-terminator
		if(skip.annotationType() == SkipBits.class){
			final int size = evaluator.evaluateSize(skip.size(), rootObject);
			reader.skip(size);
		}
		else{
			final byte terminator = skip.terminator();
			reader.skipUntilTerminator(terminator);
			if(skip.consumeTerminator()){
				final int length = ParserDataType.getSize(terminator);
				reader.skip(length);
			}
		}
	}

	private <T> void decodeField(final Template<T> template, final BitReaderInterface reader, final ParserContext<T> parserContext,
			final TemplateField field) throws FieldException{
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		try{
			//save current object (some annotations can overwrite it)
			final T currentObject = parserContext.getCurrentObject();

			//decode value from raw message
			final Object value = codec.decode(reader, binding, parserContext.getRootObject());

			//restore original current object
			evaluator.addCurrentObjectToEvaluatorContext(currentObject);

			//store value in the current object
			parserContext.setFieldValue(field.getField(), value);

			eventListener.readField(template.toString(), field.getFieldName(), value);
		}
		catch(final FieldException fe){
			fe.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());
			throw fe;
		}
		catch(final Exception e){
			throw FieldException.create(e)
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());
		}
	}

	private static void readMessageTerminator(final Template<?> template, final BitReaderInterface reader) throws TemplateException{
		final TemplateHeader header = template.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = CharsetHelper.lookup(header.charset());
			final byte[] messageTerminator = header.end()
				.getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw TemplateException.create("Message does not terminate with 0x{}", StringHelper.toHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final Template<T> template, final T data, final int startPosition, final BitReaderInterface reader){
		if(!template.isChecksumPresent())
			return;

		final TemplateField checksumField = template.getChecksum();
		final Checksum checksum = (Checksum)checksumField.getBinding();
		if(!shouldCalculateChecksum(checksum, data))
			return;

		final short calculatedChecksum = calculateChecksum(startPosition, reader, checksum);
		final short givenChecksum = ((Number)checksumField.getFieldValue(data))
			.shortValue();
		if(calculatedChecksum != givenChecksum)
			throw DataException.create("Calculated checksum (0x{}) does NOT match given checksum (0x{})",
				StringHelper.toHexString(calculatedChecksum, Short.BYTES),
				StringHelper.toHexString(givenChecksum, Short.BYTES));
	}

	private <T> boolean shouldCalculateChecksum(final Checksum checksum, final T data){
		return shouldProcessField(checksum.condition(), data);
	}

	private static short calculateChecksum(final int startPosition, final BitReaderInterface reader, final Checksum checksum){
		final int skipStart = checksum.skipStart();
		final int skipEnd = checksum.skipEnd();
		final Class<? extends Checksummer> algorithm = checksum.algorithm();

		final int endPosition = reader.position();

		final Checksummer checksummer = ConstructorHelper.getEmptyCreator(algorithm)
			.get();
		return checksummer.calculateChecksum(reader.array(), startPosition + skipStart, endPosition - skipEnd);
	}

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext){
		final Object rootObject = parserContext.getRootObject();
		final List<EvaluatedField<Evaluate>> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0, length = evaluatedFields.size(); i < length; i ++){
			final EvaluatedField<Evaluate> field = evaluatedFields.get(i);

			final Evaluate binding = field.getBinding();
			final boolean process = shouldProcessField(binding.condition(), rootObject);
			if(!process)
				continue;

			eventListener.evaluatingField(template.getType().getName(), field.getFieldName());

			final Object value = evaluator.evaluate(binding.value(), rootObject, field.getFieldType());

			//store value in the current object
			parserContext.setFieldValue(field.getField(), value);

			eventListener.evaluatedField(template.getType().getName(), field.getFieldName(), value);
		}
	}


	/**
	 * Encodes a message using the provided template and writer.
	 *
	 * @param template	The template used for encoding the message.
	 * @param writer	The writer used for writing the encoded message.
	 * @param parentObject	The parent object of the message being encoded.
	 * @param currentObject	The object to be encoded.
	 * @throws FieldException	If there is an error encoding a field.
	 */
	@Override
	public <T> void encode(final Template<?> template, final BitWriterInterface writer, final Object parentObject, final T currentObject)
			throws FieldException{
		final ParserContext<T> parserContext = prepareParserContext(parentObject, currentObject);
		parserContext.setClassName(template.getType().getName());

		preProcessFields(template, parserContext);

		final Object rootObject = parserContext.getRootObject();

		//encode message fields:
		encodeMessageFields(template, writer, rootObject, parserContext);

		final TemplateHeader header = template.getHeader();
		if(header != null)
			ParserWriterHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private void postProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcess::valueDecode);
	}

	private void preProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcess::valueEncode);
	}

	private void processFields(final Template<?> template, final ParserContext<?> parserContext,
			final Function<PostProcess, String> valueExtractor){
		final String templateName = template.getType()
			.getName();
		final List<EvaluatedField<PostProcess>> postProcessedFields = template.getPostProcessedFields();
		for(int i = 0, length = postProcessedFields.size(); i < length; i ++){
			final EvaluatedField<PostProcess> field = postProcessedFields.get(i);

			processField(field, parserContext, templateName, valueExtractor);
		}
	}

	private void processField(final EvaluatedField<PostProcess> field, final ParserContext<?> parserContext, final String templateName,
			final Function<PostProcess, String> valueExtractor){
		final PostProcess binding = field.getBinding();
		final String condition = binding.condition();
		final Object rootObject = parserContext.getRootObject();
		final boolean process = shouldProcessField(condition, rootObject);
		if(!process)
			return;

		final String fieldName = field.getFieldName();
		eventListener.evaluatingField(templateName, fieldName);

		final String expression = valueExtractor.apply(binding);
		final Object value = evaluator.evaluate(expression, rootObject, field.getFieldType());

		//store value in the current object
		parserContext.setFieldValue(field.getField(), value);

		eventListener.evaluatedField(templateName, fieldName, value);
	}

	private <T> void encodeMessageFields(final Template<?> template, final BitWriterInterface writer, final Object rootObject,
		final ParserContext<T> parserContext) throws FieldException{
		final List<TemplateField> fields = template.getTemplateFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final SkipParams[] skips = field.getSkips();
			writeSkips(skips, writer, rootObject);

			//check if field has to be processed...
			final boolean shouldProcessField = shouldProcessField(field.getCondition(), rootObject);
			if(shouldProcessField){
				//... and if so, process it
				parserContext.setField(field);
				parserContext.setFieldName(field.getFieldName());
				parserContext.setBinding(field.getBinding());

				ParserWriterHelper.encodeField(parserContext, writer, loaderCodec, eventListener);
			}
		}
	}

	private boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition != null && (condition.isEmpty() || evaluator.evaluateBoolean(condition, rootObject)));
	}

	private void writeSkips(final SkipParams[] skips, final BitWriterInterface writer, final Object rootObject){
		for(int i = 0, length = skips.length; i < length; i ++)
			writeSkip(skips[i], writer, rootObject);
	}

	private void writeSkip(final SkipParams skip, final BitWriterInterface writer, final Object rootObject){
		final boolean process = shouldProcessField(skip.condition(), rootObject);
		if(!process)
			return;

		//choose between skip-by-size and skip-by-terminator
		if(skip.annotationType() == SkipBits.class){
			final int size = evaluator.evaluateSize(skip.size(), rootObject);
			writer.skipBits(size);
		}
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}


	/**
	 * Extract a template for the given class.
	 *
	 * @param type	The class type.
	 * @return	A template.
	 * @throws AnnotationException	If an annotation error occurs.
	 * @throws TemplateException	If a template error occurs.
	 */
	public Template<?> extractTemplate(final Class<?> type) throws AnnotationException, TemplateException{
		return loaderTemplate.extractTemplate(type);
	}

	/**
	 * Unmodifiable collection of templates.
	 *
	 * @return	Collection of templates.
	 */
	public Collection<Template<?>> getTemplates(){
		return loaderTemplate.getTemplates();
	}

}
