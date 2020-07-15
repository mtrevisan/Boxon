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
package unit731.boxon.codecs;

import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.Skip;
import unit731.boxon.annotations.checksummers.Checksummer;
import unit731.boxon.annotations.exceptions.ProtocolMessageException;
import unit731.boxon.helpers.BitSet;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ExceptionHelper;
import unit731.boxon.helpers.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;


final class ProtocolMessageParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolMessageParser.class.getName());


	final Loader loader = new Loader();


	final <T> T decode(final ProtocolMessage<T> protocolMessage, final BitReader reader){
		final int startPosition = reader.position();

		final T data = ReflectionHelper.getCreator(protocolMessage.getType())
			.get();

		//parse message fields:
		final List<ProtocolMessage.BoundedField> fields = protocolMessage.getBoundedFields();
		for(final ProtocolMessage.BoundedField field : fields){
			readSkippedFields(field.getSkips(), reader, data);

			if(skipFieldByCondition(field.getCondition(), data))
				continue;

			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			try{
				final Object value = codec.decode(reader, binding, data);
				ReflectionHelper.setFieldValue(data, field.getName(), value);

				LOGGER.trace("read {} = {}", field.getName(), value);
			}
			catch(final Exception e){
				//this assumes the reading was done correctly
				manageProtocolMessageException(protocolMessage, field, e);
			}
		}

		processEvaluatedFields(protocolMessage, data);

		readMessageTerminator(protocolMessage, reader);

		verifyChecksum(protocolMessage, data, startPosition, reader);

		return data;
	}

	private <T> void readSkippedFields(final Skip[] skips, final BitReader reader, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				readSkip(skip, reader, data);
	}

	private <T> void readSkip(final Skip skip, final BitReader reader, final T data){
		final int size = Evaluator.evaluateSize(skip.size(), data);
		if(size > 0)
			/** skip {@link size} bits */
			reader.skip(size);
		else
			//skip until terminator
			reader.skipUntilTerminator(skip.terminator(), skip.consumeTerminator());
	}

	private <T> boolean skipFieldByCondition(final String condition, final T data){
		return (condition != null && !Evaluator.evaluate(condition, boolean.class, data));
	}

	private <T> void readMessageTerminator(final ProtocolMessage<T> protocolMessage, final BitReader reader){
		final MessageHeader header = protocolMessage.getHeader();
		if(header != null && header.end().length() > 0){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw new IllegalArgumentException("Message does not terminate with 0x"
					+ ByteHelper.toHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final ProtocolMessage<T> protocolMessage, final T data, int startPosition, final BitReader reader){
		final ProtocolMessage.BoundedField checksumData = protocolMessage.getChecksum();
		if(checksumData != null){
			final BindChecksum checksum = (BindChecksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.position() - checksum.skipEnd();

			final Checksummer checksummer = ReflectionHelper.getCreator(checksum.algorithm())
				.get();
			final long startValue = checksum.startValue();
			final long calculatedCRC = checksummer.calculateCRC(reader.array(), startPosition, endPosition, startValue);
			@SuppressWarnings("ConstantConditions")
			final long givenCRC = ((Number)ReflectionHelper.getFieldValue(data, checksumData.getName())).longValue();
			if(calculatedCRC != givenCRC)
				throw new IllegalArgumentException("Calculated CRC (0x" + Long.toHexString(calculatedCRC).toUpperCase()
					+ ") does NOT match given CRC (0x" + Long.toHexString(givenCRC).toUpperCase() + ")");
		}
	}

	private <T> void processEvaluatedFields(final ProtocolMessage<T> protocolMessage, final T data){
		final List<ProtocolMessage.EvaluatedField> evaluatedFields = protocolMessage.getEvaluatedFields();
		for(final ProtocolMessage.EvaluatedField field : evaluatedFields){
			final Object value = Evaluator.evaluate(field.getBinding().value(), field.getType(), data);
			ReflectionHelper.setFieldValue(data, field.getName(), value);
		}
	}

	final <T> void encode(final ProtocolMessage<?> protocolMessage, final T data, final BitWriter writer){
		//encode message's fields:
		final List<ProtocolMessage.BoundedField> fields = protocolMessage.getBoundedFields();
		for(final ProtocolMessage.BoundedField field : fields){
			writeSkippedFields(field.getSkips(), writer, data);

			if(skipFieldByCondition(field.getCondition(), data))
				continue;

			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			try{
				final Object value = ReflectionHelper.getFieldValue(data, field.getName());
				codec.encode(writer, binding, data, value);
			}
			catch(final Exception e){
				//this assumes the writing was done correctly
				manageProtocolMessageException(protocolMessage, field, e);
			}
		}

		final MessageHeader header = protocolMessage.getHeader();
		closeMessage(header, writer);

		writer.flush();
	}

	private void closeMessage(final MessageHeader header, final BitWriter writer){
		if(header != null && header.end().length() > 0){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			writer.putBytes(messageTerminator);
		}
	}

	private CodecInterface<?> retrieveCodec(final Class<? extends Annotation> annotationType){
		final CodecInterface<?> codec = loader.getCodec(annotationType);
		if(codec == null)
			throw new ProtocolMessageException("Cannot find codec for binding @{}", annotationType.getSimpleName());

		setMessageParser(codec);
		return codec;
	}

	private void setMessageParser(final CodecInterface<?> codec){
		try{
			ReflectionHelper.setFieldValue(codec, ProtocolMessageParser.class, this);
		}
		catch(final Exception ignored){}
	}

	private void manageProtocolMessageException(final ProtocolMessage<?> protocolMessage, final ProtocolMessage.BoundedField field, final Exception e){
		final String message = ExceptionHelper.getMessageNoLineNumber(e);
		throw new IllegalArgumentException(message + ", field " + protocolMessage + "." + field.getName());
	}

	private <T> void writeSkippedFields(final Skip[] skips, final BitWriter writer, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				writeSkip(skip, writer, data);
	}

	private <T> void writeSkip(final Skip skip, final BitWriter writer, final T data){
		final int size = Evaluator.evaluateSize(skip.size(), data);
		if(size > 0)
			/** skip {@link size} bits */
			writer.putBits(new BitSet(), size);
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}

}
