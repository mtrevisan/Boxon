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
import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.MessageHeader;
import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive;
import io.github.mtrevisan.boxon.annotations.bindings.BindBits;
import io.github.mtrevisan.boxon.annotations.bindings.BindByte;
import io.github.mtrevisan.boxon.annotations.bindings.BindDouble;
import io.github.mtrevisan.boxon.annotations.bindings.BindFloat;
import io.github.mtrevisan.boxon.annotations.bindings.BindInt;
import io.github.mtrevisan.boxon.annotations.bindings.BindInteger;
import io.github.mtrevisan.boxon.annotations.bindings.BindLong;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.BindShort;
import io.github.mtrevisan.boxon.annotations.bindings.BindString;
import io.github.mtrevisan.boxon.annotations.bindings.BindStringTerminated;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.checksummers.Checksummer;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.converters.NullConverter;
import io.github.mtrevisan.boxon.annotations.validators.NullValidator;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


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
	 * Load the specified protocol class annotated with {@link MessageHeader}.
	 *
	 * @param templateClass	Template class.
	 * @return	This instance, used for chaining.
	 * @throws AnnotationException	If the annotation is not well formatted.
	 * @throws TemplateException	If the template is not well formatted.
	 */
	public void loadTemplate(final Class<?> templateClass) throws AnnotationException, TemplateException{
		loaderTemplate.loadTemplate(templateClass);
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
	public <T> T decode(final Template<T> template, final BitReader reader, final Object parentObject) throws FieldException{
		final int startPosition = reader.position();

		final T currentObject = ConstructorHelper.getCreator(template.getType())
			.get();

		final ParserContext<T> parserContext = new ParserContext<>(currentObject, parentObject);
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
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(template.getType().getName(), field.getFieldName());

		eventListener.readingField(template.toString(), field.getFieldName(), annotationType.getSimpleName());

		try{
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

	private static <T> void readSkips(final Skip[] skips, final BitReader reader, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			readSkip(skips[i], reader, parserContext.getRootObject());
	}

	private static void readSkip(final Skip skip, final BitReader reader, final Object rootObject){
		final boolean process = Evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = Evaluator.evaluateSize(skip.size(), rootObject);
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

	private void processEvaluatedFields(final Template<?> template, final ParserContext<?> parserContext){
		final List<EvaluatedField> evaluatedFields = template.getEvaluatedFields();
		for(int i = 0; i < evaluatedFields.size(); i ++){
			final EvaluatedField field = evaluatedFields.get(i);
			final boolean process = Evaluator.evaluateBoolean(field.getBinding().condition(), parserContext.getRootObject());
			if(!process)
				continue;

			eventListener.evaluatingField(template.getType().getName(), field.getFieldName());

			final Object value = Evaluator.evaluate(field.getBinding().value(), parserContext.getRootObject(), field.getFieldType());
			field.setFieldValue(parserContext.getCurrentObject(), value);

			eventListener.evaluatedField(template.getType().getName(), field.getFieldName(), value);
		}
	}

	@Override
	public <T> void encode(final Template<?> template, final BitWriter writer, final Object parentObject, final T currentObject)
			throws FieldException{
		final ParserContext<T> parserContext = new ParserContext<>(currentObject, parentObject);
		parserContext.addCurrentObjectToEvaluatorContext();
		parserContext.setClassName(template.getType().getName());

		//encode message fields:
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

	private static boolean shouldProcessField(final String condition, final Object rootObject){
		return (condition.isEmpty() || Evaluator.evaluateBoolean(condition, rootObject));
	}

	private static <T> void writeSkips(final Skip[] skips, final BitWriter writer, final ParserContext<T> parserContext){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, parserContext.getRootObject());
	}

	private static void writeSkip(final Skip skip, final BitWriter writer, final Object rootObject){
		final boolean process = Evaluator.evaluateBoolean(skip.condition(), rootObject);
		if(!process)
			return;

		final int size = Evaluator.evaluateSize(skip.size(), rootObject);
		if(size > 0)
			/** skip {@link size} bits */
			writer.putBits(BitSet.empty(), size);
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.putByte(skip.terminator());
	}


	/**
	 * Description of all the loaded templates.
	 *
	 * @return	The list of descriptions.
	 */
	public List<Map<String, Object>> describeTemplates(){
		final Collection<Template<?>> templates = loaderTemplate.getTemplates();
		final List<Map<String, Object>> description = new ArrayList<>(templates.size());
		for(final Template<?> template : templates)
			description.add(describeTemplate(template));
		return description;
	}

	/**
	 * Description of all the templates in the given package annotated with {@link MessageHeader}.
	 *
	 * @param templateClasses	Classes to be used ase starting point from which to load annotated classes.
	 * @return	The list of descriptions.
	 * @throws AnnotationException	If an annotation is not well formatted.
	 * @throws TemplateException	If a template is not well formatted.
	 */
	public List<Map<String, Object>> describeTemplates(final Class<?>... templateClasses) throws AnnotationException, TemplateException{
		final List<Map<String, Object>> description = new ArrayList<>(templateClasses.length);
		for(int i = 0; i < templateClasses.length; i ++){
			final Class<?> templateClass = templateClasses[i];
			if(templateClass.isAnnotationPresent(MessageHeader.class)){
				final Template<?> template = loaderTemplate.extractTemplate(templateClass);
				description.add(describeTemplate(template));
			}
		}
		return description;
	}

	private Map<String, Object> describeTemplate(final Template<?> template){
		final Map<String, Object> description = new HashMap<>(2);
		final MessageHeader header = template.getHeader();
		final Map<String, Object> headerDescription = new HashMap<>(3);
		putIfNotEmpty("start", header.start(), headerDescription);
		putIfNotEmpty("end", header.end(), headerDescription);
		putIfNotEmpty("charset", header.charset(), headerDescription);
		description.put("header", headerDescription);
		final List<Map<String, Object>> fieldsDescription = new ArrayList<>(0);
		final List<BoundedField> fields = template.getBoundedFields();
		for(int i = 0; i < fields.size(); i ++){
			final BoundedField field = fields.get(i);
			final Map<String, Object> fieldDescription = new HashMap<>(13);

			final Skip[] skips = field.getSkips();
			describeSkips(skips, fieldDescription);

			putIfNotEmpty("name", field.getFieldName(), fieldDescription);
			putIfNotEmpty("fieldType", field.getFieldType(), fieldDescription);
			final Annotation binding = field.getBinding();
			putIfNotEmpty("annotationType", binding.annotationType(), fieldDescription);

			if(BindArray.class.isInstance(binding)){
				final BindArray annotation = (BindArray)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("type", annotation.type(), fieldDescription);
				putIfNotEmpty("size", annotation.size(), fieldDescription);
				describeChoices(annotation.selectFrom(), fieldDescription);
				putIfNotEmpty("selectDefault", annotation.selectDefault(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindArrayPrimitive.class.isInstance(binding)){
				final BindArrayPrimitive annotation = (BindArrayPrimitive)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("type", annotation.type(), fieldDescription);
				putIfNotEmpty("size", annotation.size(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindBits.class.isInstance(binding)){
				final BindBits annotation = (BindBits)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("size", annotation.size(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindByte.class.isInstance(binding)){
				final BindByte annotation = (BindByte)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindDouble.class.isInstance(binding)){
				final BindDouble annotation = (BindDouble)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindFloat.class.isInstance(binding)){
				final BindFloat annotation = (BindFloat)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindInt.class.isInstance(binding)){
				final BindInt annotation = (BindInt)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindInteger.class.isInstance(binding)){
				final BindInteger annotation = (BindInteger)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("size", annotation.size(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindLong.class.isInstance(binding)){
				final BindLong annotation = (BindLong)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindObject.class.isInstance(binding)){
				final BindObject annotation = (BindObject)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("type", annotation.type(), fieldDescription);
				describeChoices(annotation.selectFrom(), fieldDescription);
				putIfNotEmpty("selectDefault", annotation.selectDefault(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindShort.class.isInstance(binding)){
				final BindShort annotation = (BindShort)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindString.class.isInstance(binding)){
				final BindString annotation = (BindString)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("charset", annotation.charset(), fieldDescription);
				putIfNotEmpty("size", annotation.size(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}
			else if(BindStringTerminated.class.isInstance(binding)){
				final BindStringTerminated annotation = (BindStringTerminated)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("charset", annotation.charset(), fieldDescription);
				putIfNotEmpty("terminator", annotation.terminator(), fieldDescription);
				putIfNotEmpty("consumeTerminator", annotation.consumeTerminator(), fieldDescription);
				describeValidator(annotation.validator(), fieldDescription);
				describeConverter(annotation.converter(), fieldDescription);
				describeAlternatives(annotation.selectConverterFrom().alternatives(), fieldDescription);
			}

			else if(Checksum.class.isInstance(binding)){
				final Checksum annotation = (Checksum)binding;
				putIfNotEmpty("type", annotation.type(), fieldDescription);
				putIfNotEmpty("byteOrder", annotation.byteOrder(), fieldDescription);
				putIfNotEmpty("skipStart", annotation.skipStart(), fieldDescription);
				putIfNotEmpty("skipEnd", annotation.skipEnd(), fieldDescription);
				putIfNotEmpty("algorithm", annotation.algorithm(), fieldDescription);
				putIfNotEmpty("startValue", annotation.startValue(), fieldDescription);
			}
			else if(Evaluate.class.isInstance(binding)){
				final Evaluate annotation = (Evaluate)binding;
				putIfNotEmpty("condition", annotation.condition(), fieldDescription);
				putIfNotEmpty("value", annotation.value(), fieldDescription);
			}

			fieldsDescription.add(fieldDescription);
		}
		description.put("fields", fieldsDescription);
		//TODO add context
		return description;
	}

	private static void describeSkips(final Skip[] skips, final Map<String, Object> rootDescription){
		final Collection<Map<String, Object>> skipsDescription = new ArrayList<>(skips.length);
		for(int j = 0; j < skips.length; j ++){
			final Skip skip = skips[j];
			final Map<String, Object> skipDescription = new HashMap<>(4);
			putIfNotEmpty("condition", skip.condition(), skipDescription);
			putIfNotEmpty("size", skip.size(), skipDescription);
			putIfNotEmpty("terminator", skip.terminator(), skipDescription);
			putIfNotEmpty("consumeTerminator", skip.consumeTerminator(), skipDescription);
			skipsDescription.add(skipDescription);
		}
		if(!skipsDescription.isEmpty())
			rootDescription.put("skips", skipsDescription);
	}

	private static void describeChoices(final ObjectChoices choices, final Map<String, Object> rootDescription){
		putIfNotEmpty("prefixSize", choices.prefixSize(), rootDescription);
		putIfNotEmpty("byteOrder", choices.byteOrder(), rootDescription);
		describeAlternatives(choices.alternatives(), rootDescription);
	}

	private static void describeAlternatives(final ObjectChoices.ObjectChoice[] alternatives,
			final Map<String, Object> rootDescription){
		if(alternatives.length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(alternatives.length);
			for(int j = 0; j < alternatives.length; j ++){
				final ObjectChoices.ObjectChoice alternative = alternatives[j];
				final Map<String, Object> alternativeDescription = new HashMap<>(3);
				putIfNotEmpty("condition", alternative.condition(), alternativeDescription);
				putIfNotEmpty("prefix", alternative.prefix(), alternativeDescription);
				putIfNotEmpty("type", alternative.type(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			rootDescription.put("selectConverterFrom", alternativesDescription);
		}
	}

	private static void describeValidator(final Class<? extends Validator<?>> validator, final Map<String, Object> rootDescription){
		if(validator != NullValidator.class)
			rootDescription.put("validator", validator.getSimpleName());
	}

	private static void describeConverter(final Class<? extends Converter<?, ?>> converter, final Map<String, Object> rootDescription){
		if(converter != NullConverter.class)
			rootDescription.put("converter", converter.getSimpleName());
	}

	private static void describeAlternatives(final ConverterChoices.ConverterChoice[] alternatives,
			final Map<String, Object> rootDescription){
		if(alternatives.length > 0){
			final Collection<Map<String, Object>> alternativesDescription = new ArrayList<>(alternatives.length);
			for(int j = 0; j < alternatives.length; j ++){
				final ConverterChoices.ConverterChoice alternative = alternatives[j];
				final Map<String, Object> alternativeDescription = new HashMap<>(2);
				putIfNotEmpty("condition", alternative.condition(), alternativeDescription);
				describeConverter(alternative.converter(), alternativeDescription);
				alternativesDescription.add(alternativeDescription);
			}
			rootDescription.put("selectConverterFrom", alternativesDescription);
		}
	}

	private static void putIfNotEmpty(final String key, final Object value,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map){
		if(value != null && (!String.class.isInstance(value) || !StringHelper.isBlank((CharSequence)value)))
			map.put(key, value);
	}

	private static void putIfNotEmpty(final String key, final Class<?> type,
			@SuppressWarnings("BoundedWildcard") final Map<String, Object> map){
		if(type != null)
			map.put(key, type.getSimpleName());
	}

}
