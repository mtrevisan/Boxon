/*
 * Copyright (c) 2020-2024 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.codecs;

import io.github.mtrevisan.boxon.annotations.bindings.BindAsArray;
import io.github.mtrevisan.boxon.annotations.bindings.BindObject;
import io.github.mtrevisan.boxon.annotations.bindings.ByteOrder;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.codecs.behaviors.ObjectBehavior;
import io.github.mtrevisan.boxon.core.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.core.helpers.CodecHelper;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Codec;
import io.github.mtrevisan.boxon.io.Evaluator;
import io.github.mtrevisan.boxon.io.Injected;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;


final class CodecObject implements Codec{

	@Injected
	private Evaluator evaluator;
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Class<? extends Annotation> annotationType(){
		return BindObject.class;
	}

	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws BoxonException{
		final ObjectBehavior behavior = ObjectBehavior.of(annotation);

		final Object instance;
		if(collectionBinding == null){
			final Class<?> chosenAlternativeType = chooseAlternativeType(reader, behavior, rootObject);
			instance = readValue(reader, chosenAlternativeType, rootObject);
		}
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			instance = decodeArray(reader, behavior, arraySize, rootObject);
		}
		else
			instance = decodeList(reader, behavior, rootObject);

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(evaluator, rootObject);
		final Object convertedValue = CodecHelper.converterDecode(chosenConverter, instance);

		CodecHelper.validate(convertedValue, behavior.validator());

		return convertedValue;
	}

	private Object decodeArray(final BitReaderInterface reader, final ObjectBehavior behavior, final int arraySize, final Object rootObject)
			throws BoxonException{
		final Object array = behavior.createArray(arraySize);

		if(CodecHelper.hasSelectAlternatives(behavior.selectFrom().alternatives()))
			readArrayWithAlternatives(reader, array, behavior, rootObject);
		else
			readArrayWithoutAlternatives(reader, array, behavior, rootObject);
		return array;
	}

	private void readArrayWithAlternatives(final BitReaderInterface reader, final Object array, final ObjectBehavior behavior,
			final Object rootObject) throws BoxonException{
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Class<?> chosenAlternativeType = chooseAlternativeType(reader, behavior, rootObject);
			final Object element = readValue(reader, chosenAlternativeType, rootObject);

			Array.set(array, i, element);
		}
	}

	private void readArrayWithoutAlternatives(final BitReaderInterface reader, final Object array, final ObjectBehavior behavior,
			final Object rootObject) throws BoxonException{
		final Template<?> template = templateParser.createTemplate(behavior.objectType());
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readValue(reader, template, rootObject);

			Array.set(array, i, element);
		}
	}

	private List<Object> decodeList(final BitReaderInterface reader, final ObjectBehavior behavior, final Object rootObject)
			throws BoxonException{
		final List<Object> list = CodecHelper.createList(behavior.objectType());

		readListWithAlternatives(reader, list, behavior, rootObject);

		return list;
	}

	private void readListWithAlternatives(final BitReaderInterface reader, final Collection<Object> list, final ObjectBehavior behavior,
			final Object rootObject) throws BoxonException{
		Class<?> chosenAlternativeType;
		while((chosenAlternativeType = chooseAlternativeSeparatedType(reader, behavior, rootObject)) != void.class){
			final Object element = readValue(reader, chosenAlternativeType, rootObject);

			list.add(element);
		}
	}

	private Object readValue(final BitReaderInterface reader, final Class<?> chosenAlternativeType, final Object rootObject)
			throws BoxonException{
		final Template<?> template = templateParser.createTemplate(chosenAlternativeType);
		return readValue(reader, template, rootObject);
	}

	private Object readValue(final BitReaderInterface reader, final Template<?> template, final Object rootObject) throws BoxonException{
		return templateParser.decode(template, reader, rootObject);
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 */
	private Class<?> chooseAlternativeSeparatedType(final BitReaderInterface reader, final ObjectBehavior behavior, final Object rootObject){
		final ObjectChoices.ObjectChoice[] alternatives = behavior.objectChoicesList().alternatives();
		if(!CodecHelper.hasSelectAlternatives((alternatives)))
			return behavior.objectType();

		final boolean hasHeader = addListHeaderToContext(reader, behavior.objectChoicesList());
		if(!hasHeader)
			return void.class;

		return chooseAlternativeType(alternatives, behavior.selectDefault(), rootObject);
	}

	/**
	 * Chooses the alternative type based on the given alternatives, default alternative, evaluator, and root object.
	 * <p>
	 * It evaluates the condition of each alternative using the evaluator with the root object and returns the type of the first alternative
	 * that evaluates to true.
	 * </p>
	 *
	 * @param alternatives	An array of ObjectChoices.ObjectChoice representing the alternatives.
	 * @param defaultAlternative	The default alternative type.
	 * @param rootObject	The root object for the evaluator.
	 * @return	The chosen alternative type. If none of the alternatives evaluate to {@code true}, it returns the default alternative type.
	 */
	private Class<?> chooseAlternativeType(final ObjectChoices.ObjectChoice[] alternatives, final Class<?> defaultAlternative,
			final Object rootObject){
		Class<?> chosenAlternativeType = defaultAlternative;
		for(int i = 0, length = alternatives.length; chosenAlternativeType == defaultAlternative && i < length; i ++){
			final ObjectChoices.ObjectChoice alternative = alternatives[i];

			final String condition = alternative.condition();
			if(evaluator.evaluateBoolean(condition, rootObject))
				chosenAlternativeType = alternative.type();
		}
		return chosenAlternativeType;
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @param behavior	The object behavior containing the alternative choices.
	 * @param rootObject	Root object for the evaluator.
	 * @return	The class type of the chosen alternative, or the default alternative type if no alternative was found.
	 * @throws CodecException	If a codec cannot be found for the chosen alternative.
	 */
	private Class<?> chooseAlternativeType(final BitReaderInterface reader, final ObjectBehavior behavior, final Object rootObject)
			throws CodecException{
		final ObjectChoices objectChoices = behavior.selectFrom();
		final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
		if(!CodecHelper.hasSelectAlternatives(alternatives))
			return behavior.objectType();

		addPrefixToContext(reader, objectChoices);

		final Class<?> chosenAlternativeType = chooseAlternativeType(alternatives, behavior.selectDefault(), rootObject);
		if(chosenAlternativeType != void.class)
			return chosenAlternativeType;

		throw CodecException.createNoCodecForAlternatives(rootObject.getClass());
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the prefix.
	 */
	private void addPrefixToContext(final BitReaderInterface reader, final ObjectChoices objectChoices){
		final byte prefixSize = objectChoices.prefixLength();
		if(prefixSize > 0){
			final BitSet bitmap = reader.readBitSet(prefixSize);
			final ByteOrder byteOrder = objectChoices.byteOrder();
			final BigInteger prefix = BitSetHelper.toObjectiveType(bitmap, prefixSize, byteOrder);

			evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the header.
	 * @return	Whether a prefix was retrieved.
	 */
	private boolean addListHeaderToContext(final BitReaderInterface reader, final ObjectChoicesList objectChoicesList){
		final byte terminator = objectChoicesList.terminator();
		final Charset charset = CharsetHelper.lookup(objectChoicesList.charset());
		final String prefix = reader.readTextUntilTerminatorWithoutConsuming(terminator, charset);
		evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		return !prefix.isEmpty();
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws BoxonException{
		final ObjectBehavior behavior = ObjectBehavior.of(annotation);

		CodecHelper.validate(value, behavior.validator());

		final Class<? extends Converter<?, ?>> chosenConverter = behavior.getChosenConverter(evaluator, rootObject);

		if(collectionBinding == null){
			final ObjectChoices objectChoices = behavior.selectFrom();
			final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
			Class<?> type = behavior.objectType();
			if(CodecHelper.hasSelectAlternatives(alternatives)){
				type = value.getClass();
				final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(objectChoices.alternatives(), type);

				CodecHelper.writeHeader(writer, chosenAlternative, objectChoices, evaluator, rootObject);
			}

			final Object object = CodecHelper.converterEncode(chosenConverter, value);

			writeValue(writer, type, object, rootObject);
		}
		else if(collectionBinding instanceof final BindAsArray superBinding){
			final int arraySize = CodecHelper.evaluateSize(superBinding.size(), evaluator, rootObject);
			final Object[] array = CodecHelper.converterEncode(chosenConverter, value);
			CodecHelper.assertSizeEquals(arraySize, array.length);

			encodeArray(writer, array, behavior, rootObject);
		}
		else{
			final List<Object> list = CodecHelper.converterEncode(chosenConverter, value);

			writeListWithAlternatives(writer, list, rootObject);
		}
	}

	private void encodeArray(final BitWriterInterface writer, final Object[] array, final ObjectBehavior behavior, final Object rootObject)
			throws BoxonException{
		final ObjectChoices objectChoices = behavior.selectFrom();
		if(CodecHelper.hasSelectAlternatives(objectChoices.alternatives()))
			writeArrayWithAlternatives(writer, array, objectChoices, rootObject);
		else
			writeArrayWithoutAlternatives(writer, array, behavior.objectType(), rootObject);
	}

	private void writeArrayWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectChoices selectFrom,
			final Object rootObject) throws BoxonException{
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0, length = array.length; i < length; i ++){
			final Object element = array[i];

			final Class<?> type = element.getClass();
			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writeHeader(writer, chosenAlternative, selectFrom, evaluator, rootObject);

			writeValue(writer, type, element, rootObject);
		}
	}

	private void writeArrayWithoutAlternatives(final BitWriterInterface writer, final Object array, final Class<?> type,
			final Object rootObject) throws BoxonException{
		final Template<?> template = templateParser.createTemplate(type);
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			writeValue(writer, template, element, rootObject);
		}
	}

	private void writeListWithAlternatives(final BitWriterInterface writer, final List<Object> list, final Object rootObject)
			throws BoxonException{
		for(int i = 0, length = list.size(); i < length; i ++){
			final Object element = list.get(i);

			final Class<?> type = element.getClass();
			writeValue(writer, type, element, rootObject);
		}
	}

	private void writeValue(final BitWriterInterface writer, final Class<?> type, final Object object, final Object rootObject)
			throws BoxonException{
		final Template<?> template = templateParser.createTemplate(type);
		writeValue(writer, template, object, rootObject);
	}

	private void writeValue(final BitWriterInterface writer, final Template<?> template, final Object object, final Object rootObject)
			throws BoxonException{
		templateParser.encode(template, writer, rootObject, object);
	}

}
