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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.Checksum;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.external.BitReader;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


final class TemplateParser{

	@InjectEventListener
	@SuppressWarnings("unused")
	private final EventListener eventListener;

	private static final class ParserContext<T>{

		private final Object rootObject;
		private final T currentObject;


		ParserContext(final Object parentObject, final T currentObject){
			rootObject = JavaHelper.nonNullOrDefault(parentObject, currentObject);
			this.currentObject = currentObject;
		}

		void addSelfToEvaluatorContext(){
			Evaluator.addToContext(CodecHelper.CONTEXT_SELF, currentObject);
		}
	}


	private final LoaderCodec loaderCodec;
	private final LoaderTemplate loaderTemplate;


	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param loaderTemplate	A template loader.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate){
		return new TemplateParser(loaderCodec, loaderTemplate, EventListener.getNoOpInstance());
	}

	/**
	 * Create a template parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param loaderTemplate	A template loader.
	 * @param eventListener	The event listener.
	 * @return	A template parser.
	 */
	public static TemplateParser create(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate,
			final EventListener eventListener){
		return new TemplateParser(loaderCodec, loaderTemplate, (eventListener != null? eventListener: EventListener.getNoOpInstance()));
	}


	TemplateParser(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate, final EventListener eventListener){
		this.loaderCodec = loaderCodec;
		this.loaderTemplate = loaderTemplate;
		this.eventListener = eventListener;
	}

	<T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException{
		final int startPosition = reader.position();

		final T currentObject = ReflectionHelper.getCreator(template.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);
		//add current object in the context
		parserContext.addSelfToEvaluatorContext();

		//decode message fields:
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			readSkips(skips, reader, parserContext);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.rootObject))
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
		try{
			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			eventListener.decodingField(template.toString(), field.getFieldName(), binding.annotationType().getSimpleName());

			//decode value from raw message
			final Object value = codec.decode(reader, binding, parserContext.rootObject);
			//store value in the current object
			field.setFieldValue(parserContext.currentObject, value);

			eventListener.decodedField(template.toString(), field.getFieldName(), value);
		}
		catch(final CodecException | AnnotationException | TemplateException e){
			e.setClassNameAndFieldName(template.getType().getName(), field.getFieldName());
			throw e;
		}
		catch(final Exception e){
			final FieldException exc = FieldException.create(e);
			exc.setClassNameAndFieldName(template.getType().getName(), field.getFieldName());
			throw exc;
		}
	}

	private <T> void readSkips(final Skip[] skips, final BitReader reader, final ParserContext<T> parserContext){
		for(int i = 0; i < JavaHelper.lengthOrZero(skips); i ++)
			readSkip(skips[i], reader, parserContext.rootObject);
	}

	private void readSkip(final Skip skip, final BitReader reader, final Object rootObject){
		final boolean process = Evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(process){
			final int size = Evaluator.evaluateSize(skip.size(), rootObject);
			if(size > 0)
				reader.skip(size);
			else{
				reader.skipUntilTerminator(skip.terminator());
				if(skip.consumeTerminator())
					reader.getByte();
			}
		}
	}

	private void readMessageTerminator(final Template<?> template, final BitReader reader) throws TemplateException{
		final MessageHeader header = template.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw TemplateException.create("Message does not terminate with 0x{}", JavaHelper.toHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final Template<T> template, final T data, int startPosition, final BitReader reader){
		if(template.isChecksumPresent()){
			final BoundedField checksumData = template.getChecksum();
			final Checksum checksum = (Checksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.position() - checksum.skipEnd();

			final Checksummer checksummer = ReflectionHelper.getCreator(checksum.algorithm())
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
			final boolean process = Evaluator.evaluateBoolean(field.getBinding().condition(), parserContext.rootObject);
			if(process){
				eventListener.evaluatingField(template.getType().getName(), field.getFieldName());

				final Object value = Evaluator.evaluate(field.getBinding().value(), parserContext.rootObject, field.getFieldType());
				field.setFieldValue(parserContext.currentObject, value);

				eventListener.evaluatedField(template.getType().getName(), field.getFieldName(), value);
			}
		}
	}

	<T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
			throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);
		//add current object in the context
		parserContext.addSelfToEvaluatorContext();

		//encode message fields:
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			writeSkips(skips, writer, parserContext);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.rootObject))
				//... and if so, process it
				encodeField(template, writer, parserContext, field);
		}

		closeMessage(template, writer);

		writer.flush();
	}

	private <T> void encodeField(final Template<?> template, final BitWriter writer, final ParserContext<T> parserContext,
			final BoundedField field) throws FieldException{
		try{
			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			eventListener.writingField(template.getType().getName(), field.getFieldName(), binding.annotationType().getSimpleName());

			//encode value from current object
			final Object value = field.getFieldValue(parserContext.currentObject);
			//write value to raw message
			codec.encode(writer, binding, parserContext.rootObject, value);

			eventListener.writtenField(template.getType().getName(), field.getFieldName(), value);
		}
		catch(final CodecException | AnnotationException e){
			e.setClassNameAndFieldName(template.getType().getName(), field.getFieldName());
			throw e;
		}
		catch(final Exception e){
			final FieldException exc = FieldException.create(e);
			exc.setClassNameAndFieldName(template.getType().getName(), field.getFieldName());
			throw exc;
		}
	}

	private boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition.isEmpty() || Evaluator.evaluate(condition, rootObject, boolean.class));
	}

	private void closeMessage(final Template<?> template, final BitWriter writer){
		final MessageHeader header = template.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			writer.putBytes(messageTerminator);
		}
	}

	private CodecInterface<?> retrieveCodec(final Class<? extends Annotation> annotationType) throws CodecException{
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName());

		setTemplateParser(codec);
		return codec;
	}

	private void setTemplateParser(final CodecInterface<?> codec){
		try{
			ReflectionHelper.setFieldValue(codec, LoaderTemplate.class, loaderTemplate);
		}
		catch(final Exception ignored){}
		try{
			ReflectionHelper.setFieldValue(codec, TemplateParser.class, this);
		}
		catch(final Exception ignored){}
	}

	private <T> void writeSkips(final Skip[] skips, final BitWriter writer, final ParserContext<T> parserContext){
		for(int i = 0; i < JavaHelper.lengthOrZero(skips); i ++)
			writeSkip(skips[i], writer, parserContext.rootObject);
	}

	private void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject){
		final boolean process = Evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(process){
			final int size = Evaluator.evaluateSize(skip.size(), rootObject);
			if(size > 0)
				/** skip {@link size} bits */
				writer.putBits(BitSet.empty(), size);
			else if(skip.consumeTerminator())
				//skip until terminator
				writer.putByte(skip.terminator());
		}
	}

}
