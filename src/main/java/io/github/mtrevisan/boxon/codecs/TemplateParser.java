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
import io.github.mtrevisan.boxon.codecs.managers.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.DescriberKey;
import io.github.mtrevisan.boxon.external.codecs.BitReader;
import io.github.mtrevisan.boxon.external.codecs.BitSet;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.codecs.ParserDataType;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.internal.StringHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public final class TemplateParser implements TemplateParserInterface{

	private final EventListener eventListener;

	private final TemplateParserCore core;
	private final Map<String, Object> backupContext = new HashMap<>(0);


	/**
	 * Create a template parser.
	 *
	 * @param core	The core of the template parser.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final TemplateParserCore core){
		return new TemplateParser(core);
	}

	private TemplateParser(final TemplateParserCore core){
		eventListener = core.getEventListener();

		this.core = core;
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
		return core.createTemplate(type);
	}


	/**
	 * Retrieve the next template.
	 *
	 * @param reader	The reader to read the header from.
	 * @return	The template that is able to decode/encode the next message in the given reader.
	 */
	public Template<?> getTemplate(final BitReader reader) throws TemplateException{
		return core.getLoaderTemplate().getTemplate(reader);
	}

	/**
	 * Retrieve the template by class.
	 *
	 * @param type	The class to retrieve the template.
	 * @return	The template that is able to decode/encode the given class.
	 */
	public Template<?> getTemplate(final Class<?> type) throws TemplateException{
		return core.getLoaderTemplate().getTemplate(type);
	}

	/**
	 * Tries to infer the next message start by scanning all templates in header-start-length order.
	 *
	 * @param reader	The reader.
	 * @return	The index of the next message.
	 */
	public int findNextMessageIndex(final BitReader reader){
		return core.getLoaderTemplate().findNextMessageIndex(reader);
	}


	@Override
	public <T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException{
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

	private <T> void decodeField(final Template<T> template, final BitReader reader, final ParserContext<T> parserContext,
			final BoundedField field) throws FieldException{
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final CodecInterface<?> codec = core.getLoaderCodec().getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		try{
			//FIXME inject evaluator?

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

	private <T> void readSkips(final Skip[] skips, final BitReader reader, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			readSkip(skips[i], reader, parserContext.getRootObject());
	}

	private void readSkip(final Skip skip, final BitReader reader, final Object rootObject){
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
				reader.getBits(length);
			}
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
	public <T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
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

				ParserHelper.encodeField(parserContext, writer, loaderCodec, eventListener);
			}
		}

		final MessageHeader header = template.getHeader();
		if(header != null)
			ParserHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition.isEmpty() || core.getEvaluator().evaluateBoolean(condition, rootObject));
	}

	private <T> void writeSkips(final Skip[] skips, final BitWriter writer, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, parserContext.getRootObject());
	}

	private void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject){
		final Evaluator evaluator = core.getEvaluator();
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


	public void addToBackupContext(final String key, final Object value){
		backupContext.put(key, value.toString());
	}

	public void addToBackupContext(final Map<String, Object> context){
		for(final Map.Entry<String, Object> entry : context.entrySet())
			addToBackupContext(entry.getKey(), entry.getValue());
	}

	public void addToBackupContext(final Method method){
		@SuppressWarnings("unchecked")
		Collection<String> v = (Collection<String>)backupContext.get(DescriberKey.CONTEXT_METHODS.toString());
		if(v == null){
			v = new HashSet<>(1);
			backupContext.put(DescriberKey.CONTEXT_METHODS.toString(), v);
		}
		v.add(method.toString());
	}

	public TemplateParserCore getTemplateParserCore(){
		return core;
	}

	public Map<String, Object> getBackupContext(){
		return Collections.unmodifiableMap(backupContext);
	}

}
