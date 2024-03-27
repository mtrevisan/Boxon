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
import io.github.mtrevisan.boxon.annotations.PostProcessField;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.logs.EventListener;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;


/**
 * Declarative data binding parser for message templates.
 */
public final class TemplateParser implements TemplateParserInterface{

	private final TemplateParserCore core;

	private final ParserWriterHelper parserWriterHelper;

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
		core = TemplateParserCore.create(loaderCodec, evaluator);

		parserWriterHelper = ParserWriterHelper.create();

		eventListener = EventListener.getNoOpInstance();
	}


	/**
	 * Assign an event listener.
	 *
	 * @param eventListener	The event listener.
	 * @return	This instance, used for chaining.
	 */
	public TemplateParser withEventListener(final EventListener eventListener){
		this.eventListener = eventListener;

		core.withEventListener(eventListener);
		parserWriterHelper.withEventListener(eventListener);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link TemplateHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public TemplateParser withTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		core.loadTemplatesFrom(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link TemplateHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	public TemplateParser withTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		core.loadTemplate(templateClass);

		return this;
	}

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 * @throws AnnotationException	If an annotation has validation problems.
	 */
	@Override
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return core.createTemplate(type);
	}


	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader from which to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 * @throws TemplateException	Whether the template is not valid.
	 */
	public Template<?> getTemplate(final BitReaderInterface reader) throws TemplateException{
		return core.getLoaderTemplate()
			.getTemplate(reader);
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 * @throws TemplateException	Whether the template is not valid.
	 */
	public Template<?> getTemplate(final Class<?> type) throws TemplateException{
		return core.getLoaderTemplate()
			.getTemplate(type);
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReaderInterface reader){
		return core.getLoaderTemplate()
			.findNextMessageIndex(reader);
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

		T currentObject = ReflectionHelper.getEmptyCreator(template.getType())
			.get();

		//FIXME is there a way to reduce the number of ParserContext objects?
		final ParserContext<T> parserContext = new ParserContext<>(core.getEvaluator(), currentObject, parentObject);
		//add current object in the context
		parserContext.addCurrentObjectToEvaluatorContext();

		//decode message fields:
		final List<TemplateField> fields = template.getTemplateFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			readSkips(skips, reader, parserContext);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.getRootObject()))
				//... and if so, process it
				decodeField(template, reader, parserContext, field);
		}

		processEvaluatedFields(template, parserContext);

		postProcessFields(template, parserContext);

		readMessageTerminator(template, reader);

		currentObject = parserContext.getCurrentObject();
		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private <T> void decodeField(final Template<T> template, final BitReaderInterface reader, final ParserContext<T> parserContext,
			final TemplateField field) throws FieldException{
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final LoaderCodecInterface loaderCodec = core.getLoaderCodec();
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		try{
			//FIXME inject evaluator here?

			//decode value from raw message
			final Object value = codec.decode(reader, binding, parserContext.getRootObject());
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

	private <T> void readSkips(final Skip[] skips, final BitReaderInterface reader, final ParserContext<T> parserContext){
		final Object rootObject = parserContext.getRootObject();
		for(int i = 0, length = skips.length; i < length; i ++)
			readSkip(skips[i], reader, rootObject);
	}

	private void readSkip(final Skip skip, final BitReaderInterface reader, final Object rootObject){
		final Evaluator evaluator = core.getEvaluator();
		final boolean process = evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = evaluator.evaluateSize(skip.size(), rootObject);
		if(size > 0)
			reader.skip(size);
		else{
			final byte terminator = skip.terminator();
			reader.skipUntilTerminator(terminator);
			if(skip.consumeTerminator()){
				final int length = ParserDataType.getSize(terminator);
				reader.skip(length);
			}
		}
	}

	private static void readMessageTerminator(final Template<?> template, final BitReaderInterface reader) throws TemplateException{
		final TemplateHeader header = template.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = CharsetHelper.lookup(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw TemplateException.create("Message does not terminate with 0x{}", StringHelper.toHexString(messageTerminator));
		}
	}

	private static <T> void verifyChecksum(final Template<T> template, final T data, final int startPosition,
			final BitReaderInterface reader){
		if(template.isChecksumPresent()){
			final TemplateField checksumData = template.getChecksum();
			final Checksum checksum = (Checksum)checksumData.getBinding();

			final short calculatedChecksum = calculateChecksum(startPosition, reader, checksum);
			final Number givenChecksum = checksumData.getFieldValue(data);
			if(givenChecksum == null)
				throw DataException.create("Something bad happened, cannot read message checksum");
			if(calculatedChecksum != givenChecksum.shortValue()){
				final int mask = ParserDataType.fromType(givenChecksum.getClass())
					.getMask();
				throw DataException.create("Calculated checksum (0x{}) does NOT match given checksum (0x{})",
					StringHelper.toHexString(calculatedChecksum & mask),
					StringHelper.toHexString(givenChecksum.shortValue() & mask));
			}
		}
	}

	private static short calculateChecksum(final int startPosition, final BitReaderInterface reader, final Checksum checksum){
		final int skipStart = checksum.skipStart();
		final int skipEnd = checksum.skipEnd();
		final Class<? extends Checksummer> algorithm = checksum.algorithm();
		final short startValue = checksum.startValue();

		final int endPosition = reader.position();

		final Checksummer checksummer = ReflectionHelper.getEmptyCreator(algorithm)
			.get();
		return checksummer.calculateChecksum(reader.array(), startPosition + skipStart, endPosition - skipEnd, startValue);
	}

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext){
		final List<EvaluatedField<Evaluate>> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0, length = evaluatedFields.size(); i < length; i ++){
			final EvaluatedField<Evaluate> field = evaluatedFields.get(i);

			final Evaluator evaluator = core.getEvaluator();
			final boolean process = evaluator.evaluateBoolean(field.getBinding().condition(), parserContext.getRootObject());
			if(!process)
				continue;

			eventListener.evaluatingField(template.getType().getName(), field.getFieldName());

			final Object value = evaluator.evaluate(field.getBinding().value(), parserContext.getRootObject(), field.getFieldType());
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
		//FIXME is there a way to reduce the number of ParserContext objects?
		final ParserContext<T> parserContext = new ParserContext<>(core.getEvaluator(), currentObject, parentObject);
		parserContext.addCurrentObjectToEvaluatorContext();
		parserContext.setClassName(template.getType().getName());

		preProcessFields(template, parserContext);

		//encode message fields:
		final LoaderCodecInterface loaderCodec = core.getLoaderCodec();
		final List<TemplateField> fields = template.getTemplateFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			writeSkips(skips, writer, parserContext);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.getRootObject())){
				//... and if so, process it
				parserContext.setField(field);
				parserContext.setBinding(field.getBinding());

				parserWriterHelper.encodeField(parserContext, writer, loaderCodec);
			}
		}

		final TemplateHeader header = template.getHeader();
		if(header != null)
			ParserWriterHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private void postProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcessField::valueDecode);
	}

	private void preProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcessField::valueEncode);
	}

	private void processFields(final Template<?> template, final ParserContext<?> parserContext,
			final Function<PostProcessField, String> valueExtractor){
		final String templateName = template.getType().getName();
		final List<EvaluatedField<PostProcessField>> postProcessedFields = template.getPostProcessedFields();
		for(int i = 0, length = postProcessedFields.size(); i < length; i ++){
			final EvaluatedField<PostProcessField> field = postProcessedFields.get(i);

			final PostProcessField binding = field.getBinding();
			final String condition = binding.condition();
			final String expression = valueExtractor.apply(binding);

			processField(condition, expression, parserContext, field, templateName);
		}
	}

	private void processField(final String condition, final String expression, final ParserContext<?> parserContext,
			final EvaluatedField<PostProcessField> field, final String templateName){
		final Evaluator evaluator = core.getEvaluator();
		final Object rootObject = parserContext.getRootObject();
		final boolean process = evaluator.evaluateBoolean(condition, rootObject);
		if(!process)
			return;

		final String fieldName = field.getFieldName();
		eventListener.evaluatingField(templateName, fieldName);

		final Object value = evaluator.evaluate(expression, rootObject, field.getFieldType());
		parserContext.setFieldValue(field.getField(), value);

		eventListener.evaluatedField(templateName, fieldName, value);
	}

	private boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition != null && (condition.isEmpty() || core.getEvaluator().evaluateBoolean(condition, rootObject)));
	}

	private <T> void writeSkips(final Skip[] skips, final BitWriterInterface writer, final ParserContext<T> parserContext){
		final Object rootObject = parserContext.getRootObject();
		for(int i = 0, length = skips.length; i < length; i ++)
			writeSkip(skips[i], writer, rootObject);
	}

	private void writeSkip(final Skip skip, final BitWriterInterface writer, final Object rootObject){
		final Evaluator evaluator = core.getEvaluator();
		final boolean process = evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = evaluator.evaluateSize(skip.size(), rootObject);
		if(size > 0)
			writer.skipBits(size);
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}


	/**
	 * The loader for the templates.
	 *
	 * @return	The loader for the templates.
	 */
	public LoaderTemplate getLoaderTemplate(){
		return core.getLoaderTemplate();
	}

}
