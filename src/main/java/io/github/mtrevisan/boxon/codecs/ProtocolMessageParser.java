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

import io.github.mtrevisan.boxon.annotations.BindChecksum;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.exceptions.ProtocolMessageException;
import io.github.mtrevisan.boxon.helpers.BitSet;
import io.github.mtrevisan.boxon.helpers.ByteHelper;
import io.github.mtrevisan.boxon.helpers.DynamicArray;
import io.github.mtrevisan.boxon.helpers.ExceptionHelper;
import io.github.mtrevisan.boxon.helpers.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;


final class ProtocolMessageParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolMessageParser.class);

	private static class ParserContext<T>{

		private final Object rootObject;
		private final T currentObject;


		ParserContext(final Object parentObject, final T currentObject){
			rootObject = (parentObject != null? parentObject: currentObject);
			this.currentObject = currentObject;
		}

		void addSelfToEvaluatorContext(){
			Evaluator.addToContext(CodecHelper.CONTEXT_SELF, currentObject);
		}

	}


	final Loader loader = new Loader();


	final <T> T decode(final ProtocolMessage<T> protocolMessage, final BitReader reader, final Object parentObject){
		final int startPosition = reader.position();

		final T currentObject = ReflectionHelper.getCreator(protocolMessage.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);

		//decode message fields:
		final DynamicArray<ProtocolMessage.BoundedField> fields = protocolMessage.getBoundedFields();
		for(int i = 0; i < fields.limit; i ++){
			//add current object in the context
			parserContext.addSelfToEvaluatorContext();

			final ProtocolMessage.BoundedField field = fields.data[i];

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			for(int k = 0; k < (skips != null? skips.length: 0); k ++)
				readSkip(skips[i], reader, parserContext.rootObject);

			//check if field has to be processed...
			if(processField(field.getCondition(), parserContext.rootObject))
				//... and if so, process it
				decodeField(protocolMessage, reader, parserContext, field);
		}

		processEvaluatedFields(protocolMessage, parserContext.rootObject);

		readMessageTerminator(protocolMessage, reader);

		verifyChecksum(protocolMessage, currentObject, startPosition, reader);

		return currentObject;
	}

	private <T> void decodeField(final ProtocolMessage<T> protocolMessage, final BitReader reader, final ParserContext<T> parserContext,
			final ProtocolMessage.BoundedField field){
		final Annotation binding = field.getBinding();
		final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

		try{
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("reading {}.{} with bind {}", protocolMessage, field.getName(), binding.annotationType().getSimpleName());

			final Object value = codec.decode(reader, binding, parserContext.rootObject);
			ReflectionHelper.setFieldValue(parserContext.currentObject, field.getName(), value);

			if(LOGGER.isTraceEnabled())
				LOGGER.trace("read {}.{} = {}", protocolMessage, field.getName(), value);
		}
		catch(final Exception e){
			//this assumes the reading was done correctly
			rethrowException(protocolMessage, field, e);
		}
	}

	private void readSkip(final Skip skip, final BitReader reader, final Object rootObject){
		final String condition = skip.condition();
		final boolean process = (condition.isEmpty() || Evaluator.evaluate(condition, rootObject, boolean.class));
		if(process){
			final int size = Evaluator.evaluateSize(skip.size(), rootObject);
			if(size > 0)
				/** skip {@link size} bits */
				reader.skip(size);
			else
				//skip until terminator
				reader.skipUntilTerminator(skip.terminator(), skip.consumeTerminator());
		}
	}

	private void readMessageTerminator(final ProtocolMessage<?> protocolMessage, final BitReader reader){
		final MessageHeader header = protocolMessage.getHeader();
		if(header != null && header.end().length() > 0){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw new ProtocolMessageException("Message does not terminate with 0x{}", ByteHelper.toHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final ProtocolMessage<T> protocolMessage, final T data, int startPosition, final BitReader reader){
		if(protocolMessage.isChecksumPresent()){
			final ProtocolMessage.BoundedField checksumData = protocolMessage.getChecksum();
			final BindChecksum checksum = (BindChecksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.position() - checksum.skipEnd();

			final Checksummer checksummer = ReflectionHelper.getCreator(checksum.algorithm())
				.get();
			final long startValue = checksum.startValue();
			final long calculatedCRC = checksummer.calculateCRC(reader.array(), startPosition, endPosition, startValue);
			final Number givenCRC = ReflectionHelper.getFieldValue(data, checksumData.getName());
			if(givenCRC == null || calculatedCRC != givenCRC.longValue())
				throw new IllegalArgumentException("Calculated CRC (0x" + Long.toHexString(calculatedCRC).toUpperCase()
					+ ") does NOT match given CRC (0x" + (givenCRC != null? Long.toHexString(givenCRC.longValue()).toUpperCase(): "--") + ")");
		}
	}

	private void processEvaluatedFields(final ProtocolMessage<?> protocolMessage, final Object rootObject){
		final DynamicArray<ProtocolMessage.EvaluatedField> evaluatedFields = protocolMessage.getEvaluatedFields();
		for(int i = 0; i < evaluatedFields.limit; i ++){
			final ProtocolMessage.EvaluatedField field = evaluatedFields.data[i];
			final String condition = field.getBinding().condition();
			final boolean process = (condition.isEmpty() || Evaluator.evaluate(condition, rootObject, boolean.class));
			if(process){
				final Object value = Evaluator.evaluate(field.getBinding().value(), rootObject, field.getType());
				ReflectionHelper.setFieldValue(rootObject, field.getName(), value);
			}
		}
	}

	final <T> void encode(final ProtocolMessage<?> protocolMessage, final BitWriter writer, final Object parentObject, final T currentObject){
		final ParserContext<T> parserContext = new ParserContext<>(parentObject, currentObject);

		//encode message fields:
		final DynamicArray<ProtocolMessage.BoundedField> fields = protocolMessage.getBoundedFields();
		for(int i = 0; i < fields.limit; i ++){
			//add current object in the context
			parserContext.addSelfToEvaluatorContext();

			final ProtocolMessage.BoundedField field = fields.data[i];

			//process skip annotations:
			final Skip[] skips = field.getSkips();
			for(int k = 0; k < (skips != null? skips.length: 0); k ++)
				writeSkip(skips[k], writer, parserContext.rootObject);

			//check if field has to be processed...
			if(processField(field.getCondition(), parserContext.rootObject))
				//... and if so, process it
				encodeField(protocolMessage, writer, parserContext, field);
		}

		closeMessage(protocolMessage, writer);

		writer.flush();
	}

	private <T> void encodeField(final ProtocolMessage<?> protocolMessage, final BitWriter writer, final ParserContext<T> parserContext,
			final ProtocolMessage.BoundedField field){
		final Annotation binding = field.getBinding();
		final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

		try{
			if(LOGGER.isTraceEnabled())
				LOGGER.trace("writing {}.{} with bind {}", protocolMessage.getType().getSimpleName(), field.getName(), binding.annotationType().getSimpleName());

			final Object value = ReflectionHelper.getFieldValue(parserContext.currentObject, field.getName());
			codec.encode(writer, binding, parserContext.rootObject, value);

			if(LOGGER.isTraceEnabled())
				LOGGER.trace("wrote {}.{} = {}", protocolMessage.getType().getSimpleName(), field.getName(), value);
		}
		catch(final Exception e){
			//this assumes the writing was done correctly
			rethrowException(protocolMessage, field, e);
		}
	}

	private boolean processField(final String condition, final Object rootObject){
		return (condition.isEmpty() || Evaluator.evaluate(condition, rootObject, boolean.class));
	}

	private void closeMessage(final ProtocolMessage<?> protocolMessage, final BitWriter writer){
		final MessageHeader header = protocolMessage.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			writer.putBytes(messageTerminator);
		}
	}

	private CodecInterface<?> retrieveCodec(final Class<? extends Annotation> annotationType){
		final CodecInterface<?> codec = loader.getCodec(annotationType);
		if(codec == null)
			throw new ProtocolMessageException("Cannot find codec for binding {}", annotationType.getSimpleName());

		setMessageParser(codec);
		return codec;
	}

	private void setMessageParser(final CodecInterface<?> codec){
		try{
			ReflectionHelper.setFieldValue(codec, ProtocolMessageParser.class, this);
		}
		catch(final Exception ignored){}
	}

	private void rethrowException(final ProtocolMessage<?> protocolMessage, final ProtocolMessage.BoundedField field, final Exception e){
		final String message = ExceptionHelper.getMessageNoLineNumber(e);
		throw new RuntimeException(message + " in field " + protocolMessage + "." + field.getName());
	}

	private void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject){
		final String condition = skip.condition();
		final boolean process = (condition.isEmpty() || Evaluator.evaluate(condition, rootObject, boolean.class));
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
