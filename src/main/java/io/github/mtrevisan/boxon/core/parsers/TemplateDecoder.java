/*
 * Copyright (c) 2024 Mauro Trevisan
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
import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.core.codecs.CodecLoader;
import io.github.mtrevisan.boxon.core.helpers.ConstructorHelper;
import io.github.mtrevisan.boxon.core.helpers.templates.EvaluatedField;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.DataException;
import io.github.mtrevisan.boxon.exceptions.TemplateException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.StringHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


/**
 * Responsible for decoding messages based on a given template and bit reader interface.
 * <p>
 * It extends the {@link TemplateCoderBase} class and leverages an evaluator for processing.
 * </p>
 */
final class TemplateDecoder extends TemplateCoderBase{

	/**
	 * Create a template parser.
	 *
	 * @return	A template parser.
	 */
	static TemplateDecoder create(){
		return new TemplateDecoder();
	}


	private TemplateDecoder(){}


	/**
	 * Decodes a message using the provided template and reader.
	 *
	 * @param template	The template used for decoding the message.
	 * @param reader	The reader used to reading the message.
	 * @param parentObject	The parent object of the message being decoded.
	 * @return	The decoded object.
	 * @throws BoxonException	If there is an error decoding a field.
	 */
	Object decode(final Template<?> template, final BitReaderInterface reader, final Object parentObject) throws BoxonException{
		final int startPosition = reader.position();

		Object currentObject = template.createEmptyObject();

		final ParserContext<Object> parserContext = ParserContext.create(currentObject, parentObject);
		Evaluator.addCurrentObjectToEvaluatorContext(currentObject);

		//decode message fields:
		decodeMessageFields(template, reader, parserContext);

		processEvaluatedFields(template, parserContext);

		postProcessFields(template, parserContext);

		readMessageTerminator(template.getHeader(), reader);

		currentObject = parserContext.getCurrentObject();
		verifyChecksum(template, currentObject, startPosition, reader);

		return currentObject;
	}

	private void decodeMessageFields(final Template<?> template, final BitReaderInterface reader, final ParserContext<Object> parserContext)
			throws BoxonException{
		final Object rootObject = parserContext.getRootObject();

		final List<TemplateField> fields = template.getTemplateFields();
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final SkipParams[] skips = field.getSkips();
			readSkips(skips, reader, rootObject);

			//check if the field has to be processed...
			final boolean shouldProcessField = shouldProcessField(field.getCondition(), rootObject);
			if(shouldProcessField)
				//... and if so, process it
				decodeField(template, reader, parserContext, field);
		}
	}

	private static void readSkips(final SkipParams[] skips, final BitReaderInterface reader, final Object rootObject){
		for(int i = 0, length = skips.length; i < length; i ++)
			readSkip(skips[i], reader, rootObject);
	}

	private static void readSkip(final SkipParams skip, final BitReaderInterface reader, final Object rootObject){
		final boolean process = shouldProcessField(skip.condition(), rootObject);
		if(!process)
			return;

		//choose between skip-by-size and skip-by-terminator
		if(skip.annotationType() == SkipBits.class){
			final int size = Evaluator.evaluateSize(skip.size(), rootObject);
			reader.skip(size);
		}
		else{
			final byte terminator = skip.value();
			reader.skipUntilTerminator(terminator);
			if(skip.consumeTerminator())
				//`terminator` is a byte
				reader.skip(Byte.SIZE);
		}
	}

	private void decodeField(final Template<?> template, final BitReaderInterface reader, final ParserContext<Object> parserContext,
			final TemplateField field) throws BoxonException{
		final Annotation binding = field.getBinding();
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final Annotation collectionBinding = field.getCollectionBinding();
		final Codec codec = retrieveCodec(annotationType, template, field);

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		final List<ContextParameter> contextParameters = field.getContextParameters();
		try{
			//save the current object (some annotations can overwrite it)
			final Object currentObject = parserContext.getCurrentObject();

			addContextParameters(contextParameters);

			//decode value from raw message
			final Object rootObject = parserContext.getRootObject();
			final Object value = codec.decode(reader, binding, collectionBinding, rootObject);

			//restore original current object
			Evaluator.addCurrentObjectToEvaluatorContext(currentObject);

			//store value in the current object
			parserContext.setFieldValue(field.getField(), value);

			eventListener.readField(template.toString(), field.getFieldName(), value);
		}
		catch(final BoxonException fe){
			fe.withClassAndField(template.getType(), field.getField());
			throw fe;
		}
		catch(final Exception e){
			throw BoxonException.create(e)
				.withClassAndField(template.getType(), field.getField());
		}
	}

	private static Codec retrieveCodec(final Class<? extends Annotation> annotationType, final Template<?> template,
			final TemplateField field) throws BoxonException{
		final Codec codec = CodecLoader.getCodec(annotationType);
		if(codec == null)
			throw CodecException.createNoCodecForBinding(annotationType)
				.withClassAndField(template.getType(), field.getField());

		return codec;
	}

	private static void readMessageTerminator(final TemplateHeader header, final BitReaderInterface reader) throws TemplateException{
		if(header != null && !header.end().isEmpty()){
			final Charset charset = CharsetHelper.lookup(header.charset());
			final byte[] messageTerminator = header.end()
				.getBytes(charset);
			final byte[] readMessageTerminator = reader.readBytes(messageTerminator.length);
			//verifying terminators
			if(!Arrays.equals(messageTerminator, readMessageTerminator))
				throw TemplateException.create("Message does not terminate with 0x{}", StringHelper.toHexString(messageTerminator));
		}
	}

	private static void verifyChecksum(final Template<?> template, final Object data, final int startPosition,
			final BitReaderInterface reader){
		if(!template.isChecksumPresent())
			return;

		final TemplateField checksumField = template.getChecksum();
		final Checksum checksum = (Checksum)checksumField.getBinding();
		if(!shouldCalculateChecksum(checksum, data))
			return;

		final Number calculatedChecksum = calculateChecksum(startPosition, reader, checksum);
		final Number givenChecksum = (Number)checksumField.getFieldValue(data);
		if(!Objects.equals(calculatedChecksum, givenChecksum))
			throw DataException.create("Calculated checksum (0x{}) does NOT match given checksum (0x{})",
				StringHelper.toHexString(calculatedChecksum.longValue(), Short.BYTES),
				StringHelper.toHexString(givenChecksum.longValue(), Short.BYTES));
	}

	private static <T> boolean shouldCalculateChecksum(final Checksum checksum, final T data){
		return shouldProcessField(checksum.condition(), data);
	}

	private static Number calculateChecksum(final int startPosition, final BitReaderInterface reader, final Checksum checksum){
		final int skipStart = checksum.skipStart();
		final int skipEnd = checksum.skipEnd();
		final Class<? extends Checksummer> algorithm = checksum.algorithm();

		final int endPosition = reader.position();

		final Checksummer checksummer = ConstructorHelper.getEmptyCreator(algorithm)
			.get();
		return checksummer.calculateChecksum(reader.array(), startPosition + skipStart, endPosition - skipEnd);
	}

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext){
		final Object rootObject = parserContext.getRootObject();
		final List<EvaluatedField<Evaluate>> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0, length = evaluatedFields.size(); i < length; i ++){
			final EvaluatedField<Evaluate> field = evaluatedFields.get(i);

			final Evaluate binding = field.getBinding();
			final boolean process = shouldProcessField(binding.condition(), rootObject);
			if(!process)
				continue;

			eventListener.evaluatingField(template.getName(), field.getFieldName());

			final Object value = Evaluator.evaluate(binding.value(), rootObject, field.getFieldType());

			//store value in the current object
			parserContext.setFieldValue(field.getField(), value);

			eventListener.evaluatedField(template.getName(), field.getFieldName(), value);
		}
	}


	private void postProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcess::valueDecode);
	}

}
