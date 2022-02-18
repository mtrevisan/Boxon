/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.core.codecs.LoaderCodecInterface;
import io.github.mtrevisan.boxon.core.codecs.TemplateParserInterface;
import io.github.mtrevisan.boxon.core.helpers.Evaluator;
import io.github.mtrevisan.boxon.core.managers.BoundedField;
import io.github.mtrevisan.boxon.core.managers.EvaluatedField;
import io.github.mtrevisan.boxon.core.managers.Template;
import io.github.mtrevisan.boxon.core.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.core.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.descriptions.DescriberKey;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;
import io.github.mtrevisan.boxon.logs.EventListener;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Declarative data binding parser for message templates.
 */
public final class TemplateParser implements TemplateParserInterface{

	private final TemplateParserCore core;
	private final Map<String, Object> backupContext = new ConcurrentHashMap<>(0);

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
	 * @return	The current instance.
	 */
	public TemplateParser withEventListener(final EventListener eventListener){
		this.eventListener = eventListener;

		core.withEventListener(eventListener);
		parserWriterHelper.withEventListener(eventListener);

		return this;
	}


	/**
	 * Loads all the protocol classes annotated with {@link MessageHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public TemplateParser withTemplatesFrom(final Class<?>... basePackageClasses) throws AnnotationException, TemplateException{
		core.loadTemplates(basePackageClasses);

		return this;
	}

	/**
	 * Load the specified protocol class annotated with {@link MessageHeader}.
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
	 * @param <T>	The type of the object to be returned as a {@link Template}.
	 * @param type	The class of the object to be returned as a {@link Template}.
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


	@Override
	public <T> T decode(final Template<T> template, final BitReaderInterface reader, final Object parentObject) throws FieldException{
		final int startPosition = reader.position();

		final T currentObject = ConstructorHelper.getCreator(template.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(core.getEvaluator(), currentObject, parentObject);
		//add current object in the context
		parserContext.addCurrentObjectToEvaluatorContext();

		//decode message fields:
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			readSkips(skips, reader, parserContext);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.getRootObject()))
				//... and if so, process it
				decodeField(template, reader, parserContext, field);
		}

		processEvaluatedFields(template, parserContext);

		readMessageTerminator(template, reader);

		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private <T> void decodeField(final Template<T> template, final BitReaderInterface reader, final ParserContext<T> parserContext,
			final BoundedField field) throws FieldException{
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

	private <T> void readSkips(final Skip[] skips, final BitReaderInterface reader, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			readSkip(skips[i], reader, parserContext.getRootObject());
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
		final MessageHeader header = template.getHeader();
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
			final BoundedField checksumData = template.getChecksum();
			final Checksum checksum = (Checksum)checksumData.getBinding();

			final short calculatedChecksum = calculateChecksum(startPosition, reader, checksum);
			final Number givenChecksum = checksumData.getFieldValue(data);
			if(givenChecksum == null)
				throw new IllegalArgumentException("Something bad happened, cannot read message checksum");
			if(calculatedChecksum != givenChecksum.shortValue()){
				final int mask = ParserDataType.fromType(givenChecksum.getClass())
					.getMask();
				throw new IllegalArgumentException("Calculated checksum (0x"
					+ Integer.toHexString(calculatedChecksum & mask).toUpperCase(Locale.ROOT)
					+ ") does NOT match given checksum (0x"
					+ Integer.toHexString(givenChecksum.shortValue() & mask).toUpperCase(Locale.ROOT) + ")");
			}
		}
	}

	private static short calculateChecksum(final int startPosition, final BitReaderInterface reader, final Checksum checksum){
		final int skipStart = checksum.skipStart();
		final int skipEnd = checksum.skipEnd();
		final Class<? extends Checksummer> algorithm = checksum.algorithm();
		final short startValue = checksum.startValue();

		final int endPosition = reader.position();

		final Checksummer checksummer = ConstructorHelper.getCreator(algorithm)
			.get();
		return checksummer.calculateChecksum(reader.array(), startPosition + skipStart, endPosition - skipEnd, startValue);
	}

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext){
		final List<EvaluatedField> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0; i < evaluatedFields.size(); i ++){
			final EvaluatedField field = evaluatedFields.get(i);
			final Evaluator evaluator = core.getEvaluator();
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
	public <T> void encode(final Template<?> template, final BitWriterInterface writer, final Object parentObject, final T currentObject)
			throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(core.getEvaluator(), currentObject, parentObject);
		parserContext.addCurrentObjectToEvaluatorContext();
		parserContext.setClassName(template.getType().getName());

		//encode message fields:
		final LoaderCodecInterface loaderCodec = core.getLoaderCodec();
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

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

		final MessageHeader header = template.getHeader();
		if(header != null)
			ParserWriterHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition.isEmpty() || core.getEvaluator().evaluateBoolean(condition, rootObject));
	}

	private <T> void writeSkips(final Skip[] skips, final BitWriterInterface writer, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, parserContext.getRootObject());
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
	 * Add a key-value pair to the backup of the context.
	 *
	 * @param key	The key.
	 * @param value	The value.
	 */
	public void addToBackupContext(final String key, final Object value){
		backupContext.put(key, value.toString());
	}

	/**
	 * Add a map to the backup of the context.
	 *
	 * @param context	The map.
	 */
	public void addToBackupContext(final Map<String, Object> context){
		for(final Map.Entry<String, Object> entry : context.entrySet())
			addToBackupContext(entry.getKey(), entry.getValue());
	}

	/**
	 * Add a method to the backup of the context.
	 *
	 * @param method	The method.
	 */
	public void addToBackupContext(final Method method){
		@SuppressWarnings("unchecked")
		Collection<String> v = (Collection<String>)backupContext.get(DescriberKey.CONTEXT_METHODS.toString());
		if(v == null){
			v = ConcurrentHashMap.newKeySet(1);
			backupContext.put(DescriberKey.CONTEXT_METHODS.toString(), v);
		}
		v.add(method.toString());
	}

	/**
	 * The loader for the templates.
	 *
	 * @return	The loader for the templates.
	 */
	public LoaderTemplate getLoaderTemplate(){
		return core.getLoaderTemplate();
	}

	/**
	 * The backup of the context.
	 *
	 * @return	The backup of the context.
	 */
	public Map<String, Object> getBackupContext(){
		return Collections.unmodifiableMap(backupContext);
	}

}
