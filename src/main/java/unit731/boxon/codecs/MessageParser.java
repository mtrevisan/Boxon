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
import unit731.boxon.utils.ByteHelper;
import unit731.boxon.utils.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @see <a href="https://dzone.com/articles/learn-spring-expression-language-with-examples">Learning the Spring Expression Language (SpEL)</a>
 */
class MessageParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageParser.class.getName());


	private static final AtomicBoolean VERBOSE = new AtomicBoolean(false);


	static void setVerbose(final boolean verbose) throws SecurityException{
		VERBOSE.set(verbose);
	}

	static <T> T decode(final Codec<T> codec, final BitBuffer reader){
		final int startPosition = reader.positionAsBits() / Byte.SIZE;

		final T data = ReflectionHelper.createInstance(codec.getType());

		//parse message's fields:
		final List<Codec.BoundedField> fields = codec.getBoundedFields();
		for(final Codec.BoundedField field : fields){
			skipFields(field.getSkips(), reader, data);

			final String condition = field.getCondition();
			if(condition != null && !Evaluator.evaluate(condition, boolean.class, data))
				//skip field
				continue;

			final Annotation binding = field.getBinding();
			final Class<? extends Annotation> annotationType = binding.annotationType();
			final Coder coder = Coder.CODERS_FROM_ANNOTATION.get(annotationType);
			if(coder == null)
				throw new IllegalArgumentException("Unrecognized annotation for field " + codec + "." + field.getName()
					+ ": @" + annotationType.getSimpleName());

			try{
				final Object value = coder.decode(reader, binding, data);
				ReflectionHelper.setFieldValue(data, field.getName(), value);

				if(VERBOSE.get())
					LOGGER.info("{}: {}", field.getName(), value);
			}
			catch(final Exception e){
				throw new IllegalArgumentException(e.getMessage() + ", field " + codec + "." + field.getName(), e);
			}
		}

		processAssignedFields(codec, data);

		readMessageTerminator(codec, reader);

		verifyChecksum(codec, data, startPosition, reader);

		return data;
	}

	private static <T> void skipFields(final Skip[] skips, final BitBuffer reader, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				skip(skip, reader, data);
	}

	private static <T> void skip(final Skip skip, final BitBuffer reader, final T data){
		final int size = (isNotBlank(skip.size())? Evaluator.evaluate(skip.size(), Integer.class, data): 0);
		if(size > 0)
			//skip `size` bits
			reader.skip(size);
		else
			//skip until terminator
			reader.skipUntilTerminator(skip.terminator(), skip.consumeTerminator());
	}

	private static <T> void readMessageTerminator(final Codec<T> codec, final BitBuffer reader){
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

	private static <T> void verifyChecksum(final Codec<T> codec, final T data, int startPosition, final BitBuffer reader){
		final Codec.BoundedField checksumData = codec.getChecksum();
		if(checksumData != null){
			final BindChecksum checksum = (BindChecksum)checksumData.getBinding();
			startPosition += checksum.skipStart();
			final int endPosition = reader.positionAsBits() / Byte.SIZE - checksum.skipEnd();

			try{
				final Checksummer<?> checksummer = ReflectionHelper.createInstance(checksum.algorithm());
				final long calculatedCRC = ((Number)checksummer.calculateCRC(reader.array(), startPosition, endPosition)).longValue();
				final long givenCRC = ((Number)ReflectionHelper.getFieldValue(data, checksumData.getName())).longValue();
				if(calculatedCRC != givenCRC)
					throw new IllegalArgumentException("Calculated CRC (0x" + Long.toHexString(calculatedCRC).toUpperCase()
						+ ") does NOT match given CRC (0x" + Long.toHexString(givenCRC).toUpperCase() + ")");
			}
			catch(final NoSuchFieldException ignored){}
		}
	}

	private static <T> void processAssignedFields(final Codec<T> codec, final T data){
		try{
			final List<Codec.AssignedField> assignedFields = codec.getAssignedFields();
			for(final Codec.AssignedField field : assignedFields)
				ReflectionHelper.setFieldValue(data, field.getName(), Evaluator.evaluate(field.getBinding().value(), field.getType(), data));
		}
		catch(final NoSuchFieldException ignored){}
	}

	public static <T> void encode(final Codec<?> codec, final T data, final BitWriter writer){
		//encode message's fields:
		final List<Codec.BoundedField> fields = codec.getBoundedFields();
		for(final Codec.BoundedField field : fields){
			addSkippedFields(field.getSkips(), writer, data);

			final String condition = field.getCondition();
			if(condition != null && !Evaluator.evaluate(condition, boolean.class, data))
				//skip field
				continue;

			final Annotation binding = field.getBinding();
			final Class<? extends Annotation> annotationType = binding.annotationType();
			final Coder coder = Coder.CODERS_FROM_ANNOTATION.get(annotationType);
			if(coder == null)
				throw new IllegalArgumentException("Unrecognized annotation for field " + codec + "." + field.getName()
					+ ": @" + annotationType.getSimpleName());

			try{
				final Object value = ReflectionHelper.getFieldValue(data, field.getName());
				coder.encode(writer, binding, data, value);
			}
			catch(final Exception e){
				throw new IllegalArgumentException(e.getMessage() + ", field " + codec + "." + field.getName(), e);
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

	private static <T> void addSkippedFields(final Skip[] skips, final BitWriter writer, final T data){
		if(skips != null)
			for(final Skip skip : skips)
				addSkip(skip, writer, data);
	}

	private static <T> void addSkip(final Skip skip, final BitWriter writer, final T data){
		final int size = (isNotBlank(skip.size())? Evaluator.evaluate(skip.size(), Integer.class, data): 0);
		if(size > 0)
			//skip `size` bits
			writer.putBits(new BitSet(size));
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}

	private static boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

}
