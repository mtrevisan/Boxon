/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.codecs.managers.BoundedField;
import io.github.mtrevisan.boxon.codecs.managers.ConstructorHelper;
import io.github.mtrevisan.boxon.codecs.managers.EvaluatedField;
import io.github.mtrevisan.boxon.codecs.managers.InjectEventListener;
import io.github.mtrevisan.boxon.codecs.managers.ReflectionHelper;
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitSet;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.Evaluator;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public final class TemplateParser implements TemplateParserInterface{

	@InjectEventListener
	@SuppressWarnings("unused")
	private final EventListener eventListener;

	private final LoaderCodecInterface loaderCodec;
	private final LoaderTemplate loaderTemplate;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodecInterface loaderCodec){
		return new TemplateParser(loaderCodec, EventListener.getNoOpInstance());
	}

	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param eventListener	The event listener.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		return new TemplateParser(loaderCodec, (eventListener != null? eventListener: EventListener.getNoOpInstance()));
	}


	private TemplateParser(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		this.eventListener = eventListener;

		this.loaderCodec = loaderCodec;
		loaderTemplate = LoaderTemplate.create(loaderCodec, eventListener);
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	public void loadDefaultTemplates() throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplates(ReflectionHelper.extractCallerClasses());
	}

	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 */
	public void loadTemplates(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplates(basePackageClasses);
	}

	/**
	 * Constructs a new {@link Template}.
	 *
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
	 * @return	The {@link Template} for the given type.
	 */
	@Override
	public <T> Template<T> createTemplate(final Class<T> type) throws AnnotationException{
		return loaderTemplate.createTemplate(type);
	}

	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	public Template<?> getTemplate(final BitReader reader) throws TemplateException{
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
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReader reader){
		return loaderTemplate.findNextMessageIndex(reader);
	}


	@Override
	public <T> T decode(final Template<T> template, final BitReader reader, final Object parentObject, final Evaluator evaluator)
			throws FieldException{
		final int startPosition = reader.position();

		final T currentObject = ConstructorHelper.getCreator(template.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(currentObject, parentObject);
		//add current object in the context
		parserContext.addCurrentObjectToEvaluatorContext(evaluator);

		//decode message fields:
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			readSkips(skips, reader, parserContext, evaluator);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.getRootObject(), evaluator))
				//... and if so, process it
				decodeField(template, reader, parserContext, field, evaluator);
		}

		processEvaluatedFields(template, parserContext, evaluator);

		readMessageTerminator(template, reader);

		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private <T> void decodeField(final Template<T> template, final BitReader reader, final ParserContext<T> parserContext,
			final BoundedField field, final Evaluator evaluator) throws FieldException{
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		try{
			//decode value from raw message
			final Object value = codec.decode(reader, binding, parserContext.getRootObject(), evaluator);
			//store value in the current object
			field.setFieldValue(parserContext.getCurrentObject(), value);

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

	private static <T> void readSkips(final Skip[] skips, final BitReader reader, final ParserContext<T> parserContext,
			final Evaluator evaluator){
		for(int i = 0; i < skips.length; i ++)
			readSkip(skips[i], reader, parserContext.getRootObject(), evaluator);
	}

	private static void readSkip(final Skip skip, final BitReader reader, final Object rootObject, final Evaluator evaluator){
		final boolean process = evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = evaluator.evaluateSize(skip.size(), rootObject);
		if(size > 0)
			reader.skip(size);
		else{
			final byte terminator = skip.terminator();
			reader.skipUntilTerminator(terminator);
			if(skip.consumeTerminator())
				reader.getBitsSizeOf(terminator);
		}
	}

	private static void readMessageTerminator(final Template<?> template, final BitReader reader) throws TemplateException{
		final MessageHeader header = template.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw TemplateException.create("Message does not terminate with 0x{}", StringHelper.toHexString(messageTerminator));
		}
	}

	private static <T> void verifyChecksum(final Template<T> template, final T data, int startPosition, final BitReader reader){
		if(template.isChecksumPresent()){
			final BoundedField checksumData = template.getChecksum();
			final Checksum checksum = (Checksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.position() - checksum.skipEnd();

			final Checksummer checksummer = ConstructorHelper.getCreator(checksum.algorithm())
				.get();
			final short startValue = checksum.startValue();
			final short calculatedChecksum = checksummer.calculateChecksum(reader.array(), startPosition, endPosition, startValue);
			final Number givenChecksum = checksumData.getFieldValue(data);
			if(givenChecksum == null)
				throw new IllegalArgumentException("Something bad happened, cannot read message checksum");
			if(calculatedChecksum != givenChecksum.shortValue())
				throw new IllegalArgumentException("Calculated checksum (0x"
					+ Integer.toHexString(calculatedChecksum).toUpperCase(Locale.ROOT)
					+ ") does NOT match given checksum (0x"
					+ Integer.toHexString(givenChecksum.shortValue()).toUpperCase(Locale.ROOT) + ")");
		}
	}

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext, final Evaluator evaluator){
		final List<EvaluatedField> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0; i < evaluatedFields.size(); i ++){
			final EvaluatedField field = evaluatedFields.get(i);
			final boolean process = evaluator.evaluateBoolean(field.getBinding().condition(), parserContext.getRootObject());
			if(!process)
				continue;

			eventListener.evaluatingField(template.getType().getName(), field.getFieldName());

			final Object value = evaluator.evaluate(field.getBinding().value(), parserContext.getRootObject(), field.getFieldType());
			field.setFieldValue(parserContext.getCurrentObject(), value);

			eventListener.evaluatedField(template.getType().getName(), field.getFieldName(), value);
		}
	}

	@Override
	public <T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject,
			final Evaluator evaluator) throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(currentObject, parentObject);
		parserContext.addCurrentObjectToEvaluatorContext(evaluator);
		parserContext.setClassName(template.getType().getName());

		//encode message fields:
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			writeSkips(skips, writer, parserContext, evaluator);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.getRootObject(), evaluator)){
				//... and if so, process it
				parserContext.setField(field);
				parserContext.setBinding(field.getBinding());

				ParserHelper.encodeField(parserContext, writer, loaderCodec, eventListener, evaluator);
			}
		}

		final MessageHeader header = template.getHeader();
		if(header != null)
			ParserHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private static boolean shouldProcessField(final String condition, final Object rootObject, final Evaluator evaluator){
		return (condition.isEmpty() || evaluator.evaluateBoolean(condition, rootObject));
	}

	private static <T> void writeSkips(final Skip[] skips, final BitWriter writer, final ParserContext<T> parserContext,
			final Evaluator evaluator){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, parserContext.getRootObject(), evaluator);
	}

	private static void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject, final Evaluator evaluator){
		final boolean process = evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = evaluator.evaluateSize(skip.size(), rootObject);
		if(size > 0)
			/** skip {@link size} bits */
			writer.putBits(BitSet.empty(), size);
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}

}
