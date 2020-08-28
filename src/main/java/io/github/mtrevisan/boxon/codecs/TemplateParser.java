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
import io.github.mtrevisan.boxon.internal.DynamicArray;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;


final class TemplateParser{

	private static final Logger LOGGER = JavaHelper.getLoggerFor(TemplateParser.class);

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


	final Loader loader = new Loader();


	<T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException{
		final int startPosition = reader.position();

		final T currentObject = ReflectionHelper.getCreator(template.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);

		//decode message fields:
		final DynamicArray<Template.BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.limit; i ++){
			//add current object in the context
			parserContext.addSelfToEvaluatorContext();

			final Template.BoundedField field = fields.data[i];

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			for(int k = 0; k < JavaHelper.lengthOrZero(skips); k ++)
				readSkip(skips[i], reader, parserContext.rootObject);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.rootObject))
				//... and if so, process it
				decodeField(template, reader, parserContext, field);
		}

		processEvaluatedFields(template, parserContext.rootObject);

		readMessageTerminator(template, reader);

		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private <T> void decodeField(final Template<T> template, final BitReader reader, final ParserContext<T> parserContext,
			final Template.BoundedField field) throws FieldException{
		try{
			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			if(LOGGER != null)
				LOGGER.trace("reading {}.{} with bind {}", template, field.getFieldName(), binding.annotationType().getSimpleName());

			//decode value from raw message
			final Object value = codec.decode(reader, binding, parserContext.rootObject);
			//store value in the current object
			field.setFieldValue(parserContext.currentObject, value);

			if(LOGGER != null)
				LOGGER.trace("read {}.{} = {}", template, field.getFieldName(), value);
		}
		catch(final CodecException | AnnotationException | TemplateException e){
			e.setClassNameAndFieldName(template.toString(), field.getFieldName());
			throw e;
		}
		catch(final Exception e){
			final FieldException exc = new FieldException(e);
			exc.setClassNameAndFieldName(template.toString(), field.getFieldName());
			throw exc;
		}
	}

	private void readSkip(final Skip skip, final BitReader reader, final Object rootObject){
		final String condition = skip.condition();
		final boolean process = Evaluator.evaluateBoolean(condition, rootObject);
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
				throw new TemplateException("Message does not terminate with 0x{}", JavaHelper.toHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final Template<T> template, final T data, int startPosition, final BitReader reader){
		if(template.isChecksumPresent()){
			final Template.BoundedField checksumData = template.getChecksum();
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
				throw new IllegalArgumentException("Calculated checksum (0x" + Integer.toHexString(calculatedChecksum).toUpperCase(Locale.ROOT)
					+ ") does NOT match given checksum (0x" + Integer.toHexString(givenChecksum.shortValue()).toUpperCase(Locale.ROOT) + ")");
		}
	}

	private void processEvaluatedFields(final Template<?> template, final Object rootObject){
		final DynamicArray<Template.EvaluatedField> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0; i < evaluatedFields.limit; i ++){
			final Template.EvaluatedField field = evaluatedFields.data[i];
			final String condition = field.getBinding().condition();
			final boolean process = Evaluator.evaluateBoolean(condition, rootObject);
			if(process){
				if(LOGGER != null)
					LOGGER.trace("evaluating {}.{}", template.getType().getSimpleName(), field.getFieldName());

				final Object value = Evaluator.evaluate(field.getBinding().value(), rootObject, field.getFieldType());
				field.setFieldValue(rootObject, value);

				if(LOGGER != null)
					LOGGER.trace("wrote {}.{} = {}", template.getType().getSimpleName(), field.getFieldName(), value);
			}
		}
	}

	<T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
			throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);

		//encode message fields:
		final DynamicArray<Template.BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.limit; i ++){
			//add current object in the context
			parserContext.addSelfToEvaluatorContext();

			final Template.BoundedField field = fields.data[i];

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			for(int k = 0; k < JavaHelper.lengthOrZero(skips); k ++)
				writeSkip(skips[k], writer, parserContext.rootObject);

			//check if field has to be processed...
			if(shouldProcessField(field.getCondition(), parserContext.rootObject))
				//... and if so, process it
				encodeField(template, writer, parserContext, field);
		}

		closeMessage(template, writer);

		writer.flush();
	}

	private <T> void encodeField(final Template<?> template, final BitWriter writer, final ParserContext<T> parserContext,
			final Template.BoundedField field) throws FieldException{
		try{
			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			if(LOGGER != null)
				LOGGER.trace("writing {}.{} with bind {}", template.getType().getSimpleName(), field.getFieldName(),
					binding.annotationType().getSimpleName());

			//encode value from current object
			final Object value = field.getFieldValue(parserContext.currentObject);
			//write value to raw message
			codec.encode(writer, binding, parserContext.rootObject, value);

			if(LOGGER != null)
				LOGGER.trace("wrote {}.{} = {}", template.getType().getSimpleName(), field.getFieldName(), value);
		}
		catch(final CodecException | AnnotationException e){
			e.setClassNameAndFieldName(template.toString(), field.getFieldName());
			throw e;
		}
		catch(final Exception e){
			final FieldException exc = new FieldException(e);
			exc.setClassNameAndFieldName(template.toString(), field.getFieldName());
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
		final CodecInterface<?> codec = loader.getCodec(annotationType);
		if(codec == null)
			throw new CodecException("Cannot find codec for binding {}", annotationType.getSimpleName());

		setTemplateParser(codec);
		return codec;
	}

	private void setTemplateParser(final CodecInterface<?> codec){
		try{
			ReflectionHelper.setFieldValue(codec, TemplateParser.class, this);
		}
		catch(final Exception ignored){}
	}

	private void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject){
		final String condition = skip.condition();
		final boolean process = Evaluator.evaluateBoolean(condition, rootObject);
		if(process){
			final int size = Evaluator.evaluateSize(skip.size(), rootObject);
			if(size > 0)
				/** skip {@link size} bits */
				writer.putBits(new BitSet(), size);
			else if(skip.consumeTerminator())
				//skip until terminator
				writer.putByte(skip.terminator());
		}
	}

}
