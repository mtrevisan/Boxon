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
package unit731.boxon.coders;

import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindBits;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindDecimal;
import unit731.boxon.annotations.BindDouble;
import unit731.boxon.annotations.BindFloat;
import unit731.boxon.annotations.BindInt;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.MessageHeader;
import unit731.boxon.annotations.Skip;
import unit731.boxon.annotations.checksummers.Checksummer;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ExceptionHelper;
import unit731.boxon.helpers.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


class MessageParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageParser.class.getName());

	private static final Map<Class<?>, CoderInterface> CODERS_FROM_ANNOTATION = new HashMap<>();
	static{
		CODERS_FROM_ANNOTATION.put(BindObject.class, new CoderObject());
		CODERS_FROM_ANNOTATION.put(BindString.class, new CoderString());
		CODERS_FROM_ANNOTATION.put(BindStringTerminated.class, new CoderStringTerminated());
		CODERS_FROM_ANNOTATION.put(BindArrayPrimitive.class, new CoderArrayPrimitive());
		CODERS_FROM_ANNOTATION.put(BindArray.class, new CoderArray());
		CODERS_FROM_ANNOTATION.put(BindBits.class, new CoderBits());
		CODERS_FROM_ANNOTATION.put(BindByte.class, new CoderByte());
		CODERS_FROM_ANNOTATION.put(BindShort.class, new CoderShort());
		CODERS_FROM_ANNOTATION.put(BindInt.class, new CoderInt());
		CODERS_FROM_ANNOTATION.put(BindLong.class, new CoderLong());
		CODERS_FROM_ANNOTATION.put(BindInteger.class, new CoderInteger());
		CODERS_FROM_ANNOTATION.put(BindFloat.class, new CoderFloat());
		CODERS_FROM_ANNOTATION.put(BindDouble.class, new CoderDouble());
		CODERS_FROM_ANNOTATION.put(BindDecimal.class, new CoderDecimal());
		CODERS_FROM_ANNOTATION.put(BindChecksum.class, new CoderChecksum());
	}


	private final AtomicBoolean verbose = new AtomicBoolean(false);


	void setVerbose(final boolean verbose) throws SecurityException{
		this.verbose.set(verbose);
	}

	public static void addCoder(final CoderInterface coder){
		if(!CODERS_FROM_ANNOTATION.containsKey(coder.coderType()))
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}

	public static CoderInterface getCoder(final Class<?> type){
		return CODERS_FROM_ANNOTATION.get(type);
	}

	<T> T decode(final Codec<T> codec, final BitBuffer reader){
		final int startPosition = reader.positionAsBits() / Byte.SIZE;

		final T data = ReflectionHelper.createInstance(codec.getType());

		//parse message fields:
		final List<Codec.BoundedField> fields = codec.getBoundedFields();
		for(final Codec.BoundedField field : fields){
			skipFields(field.getSkips(), reader, data);

			if(skipFieldByCondition(field.getCondition(), data))
				continue;

			final Annotation binding = field.getBinding();
			final CoderInterface coder = CODERS_FROM_ANNOTATION.get(binding.annotationType());
			if(coder == null)
				throw new IllegalArgumentException("Cannot find coder for binding @" + binding.annotationType().getSimpleName());

			try{
				final Object value = coder.decode(this, reader, binding, data);
				ReflectionHelper.setFieldValue(data, field.getName(), value);

				if(verbose.get())
					LOGGER.info("{}: {}", field.getName(), value);
			}
			catch(final Exception e){
				manageCodecException(codec, field, e);
			}
		}

		processEvaluatedFields(codec, data);

		readMessageTerminator(codec, reader);

		verifyChecksum(codec, data, startPosition, reader);

		return data;
	}

	private <T> void skipFields(final Skip[] skips, final BitBuffer reader, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				skip(skip, reader, data);
	}

	private <T> void skip(final Skip skip, final BitBuffer reader, final T data){
		final int size = (isNotBlank(skip.size())? Evaluator.evaluate(skip.size(), Integer.class, data): 0);
		if(size > 0)
			//skip `size` bits
			reader.skip(size);
		else
			//skip until terminator
			reader.skipUntilTerminator(skip.terminator(), skip.consumeTerminator());
	}

	private <T> boolean skipFieldByCondition(final String condition, final T data){
		return (condition != null && !Evaluator.evaluate(condition, boolean.class, data));
	}

	private <T> void readMessageTerminator(final Codec<T> codec, final BitBuffer reader){
		final MessageHeader header = codec.getHeader();
		if(header != null && header.end().length() > 0){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			final byte[] readMessageTerminator = reader.getBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw new IllegalArgumentException("Message does not terminate with 0x"
					+ ByteHelper.byteArrayToHexString(messageTerminator));
		}
	}

	private <T> void verifyChecksum(final Codec<T> codec, final T data, int startPosition, final BitBuffer reader){
		final Codec.BoundedField checksumData = codec.getChecksum();
		if(checksumData != null){
			final BindChecksum checksum = (BindChecksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.positionAsBits() / Byte.SIZE - checksum.skipEnd();

			final Checksummer checksummer = ReflectionHelper.createInstance(checksum.algorithm());
			final long startValue = checksum.startValue();
			final long calculatedCRC = checksummer.calculateCRC(reader.array(), startPosition, endPosition, startValue);
			try{
				@SuppressWarnings("ConstantConditions")
				final long givenCRC = ((Number)ReflectionHelper.getFieldValue(data, checksumData.getName())).longValue();
				if(calculatedCRC != givenCRC)
					throw new IllegalArgumentException("Calculated CRC (0x" + Long.toHexString(calculatedCRC).toUpperCase()
						+ ") does NOT match given CRC (0x" + Long.toHexString(givenCRC).toUpperCase() + ")");
			}
			catch(final NoSuchFieldException ignored){}
		}
	}

	private <T> void processEvaluatedFields(final Codec<T> codec, final T data){
		final List<Codec.EvaluatedField> evaluatedFields = codec.getEvaluatedFields();
		for(final Codec.EvaluatedField field : evaluatedFields){
			final Object value = Evaluator.evaluate(field.getBinding().value(), field.getType(), data);
			try{
				ReflectionHelper.setFieldValue(data, field.getName(), value);
			}
			catch(final NoSuchFieldException ignored){}
		}
	}

	<T> void encode(final Codec<?> codec, final T data, final BitWriter writer){
		//encode message's fields:
		final List<Codec.BoundedField> fields = codec.getBoundedFields();
		for(final Codec.BoundedField field : fields){
			addSkippedFields(field.getSkips(), writer, data);

			if(skipFieldByCondition(field.getCondition(), data))
				continue;

			final Annotation binding = field.getBinding();
			final CoderInterface coder = CODERS_FROM_ANNOTATION.get(binding.annotationType());
			if(coder == null)
				throw new IllegalArgumentException("Cannot find coder for binding @" + binding.annotationType().getSimpleName());

			try{
				final Object value = ReflectionHelper.getFieldValue(data, field.getName());
				coder.encode(this, writer, binding, data, value);
			}
			catch(final Exception e){
				manageCodecException(codec, field, e);
			}
		}

		final MessageHeader header = codec.getHeader();
		if(header != null && header.end().length() > 0){
			final Charset charset = Charset.forName(header.charset());
			final byte[] messageTerminator = header.end().getBytes(charset);
			writer.putBytes(messageTerminator);
		}
		writer.flush();
	}

	private void manageCodecException(final Codec<?> codec, final Codec.BoundedField field, final Exception e){
		final String message = ExceptionHelper.getMessageNoLineNumber(e);
		throw new IllegalArgumentException(message + ", field " + codec + "." + field.getName());
	}

	private <T> void addSkippedFields(final Skip[] skips, final BitWriter writer, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				addSkip(skip, writer, data);
	}

	private <T> void addSkip(final Skip skip, final BitWriter writer, final T data){
		final int size = (isNotBlank(skip.size())? Evaluator.evaluate(skip.size(), Integer.class, data): 0);
		if(size > 0)
			//skip `size` bits
			writer.putBits(new BitSet(size), size);
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}

	private boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

}
