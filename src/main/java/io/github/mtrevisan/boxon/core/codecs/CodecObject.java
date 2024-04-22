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
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoicesList;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.annotations.validators.Validator;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.CharsetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.CodecInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;


final class CodecObject implements CodecInterface<BindObject>{

	@Injected
	private Evaluator evaluator;
	@Injected
	private TemplateParserInterface templateParser;


	private record CodecBehavior(Class<?> bindingType, ObjectChoices selectFrom, Class<?> selectDefault, ObjectChoicesList objectChoicesList,
			ConverterChoices converterChoices, Class<? extends Converter<?, ?>> defaultConverter, Class<? extends Validator<?>> validator){
		public static CodecBehavior of(final Annotation annotation){
			final BindObject binding = (BindObject)annotation;

			final Class<?> bindingType = binding.type();
			final ObjectChoices selectFrom = binding.selectFrom();
			final Class<?> selectDefault = binding.selectDefault();
			final ObjectChoicesList objectChoicesList = binding.selectFromList();
			final ConverterChoices converterChoices = binding.selectConverterFrom();
			final Class<? extends Converter<?, ?>> defaultConverter = binding.converter();
			final Class<? extends Validator<?>> validator = binding.validator();
			return new CodecBehavior(bindingType, selectFrom, selectDefault, objectChoicesList, converterChoices, defaultConverter, validator);
		}

		private Object createArray(final int arraySize){
			return CodecHelper.createArray(bindingType, arraySize);
		}
	}


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject) throws FieldException{
		final CodecBehavior behavior = CodecBehavior.of(annotation);

		final Object instance;
		if(collectionBinding == null)
			instance = readValue(reader, behavior, rootObject);
		else{
			if(collectionBinding instanceof final BindAsArray ba){
				final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);
				instance = decodeArray(reader, behavior, arraySize, rootObject);
			}
			else
				instance = decodeList(reader, behavior, rootObject);
		}

		final Class<? extends Converter<?, ?>> converterType = CodecHelper.getChosenConverter(behavior.converterChoices,
			behavior.defaultConverter, evaluator, rootObject);
		return CodecHelper.decodeValue(converterType, behavior.validator, instance);
	}

	private Object decodeArray(final BitReaderInterface reader, final CodecBehavior behavior, final int arraySize, final Object rootObject)
			throws FieldException{
		final Object array = behavior.createArray(arraySize);

		if(CodecHelper.hasSelectAlternatives(behavior.selectFrom.alternatives()))
			decodeWithAlternatives(reader, array, behavior, rootObject);
		else
			decodeWithoutAlternatives(reader, array, behavior, rootObject);
		return array;
	}

	private List<Object> decodeList(final BitReaderInterface reader, final CodecBehavior behavior, final Object rootObject)
			throws FieldException{
		final List<Object> list = CodecHelper.createList(behavior.bindingType);

		decodeWithAlternatives(reader, list, behavior, rootObject);

		return list;
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Object array, final CodecBehavior behavior,
			final Object rootObject) throws FieldException{
		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = readValue(reader, behavior, rootObject);

			Array.set(array, i, element);
		}
	}

	private void decodeWithoutAlternatives(final BitReaderInterface reader, final Object array, final CodecBehavior behavior,
			final Object rootObject) throws FieldException{
		final Template<?> template = templateParser.createTemplate(behavior.bindingType);

		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = templateParser.decode(template, reader, rootObject);

			Array.set(array, i, element);
		}
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Collection<Object> list, final CodecBehavior behavior,
			final Object rootObject) throws FieldException{
		Class<?> chosenAlternativeType;
		while((chosenAlternativeType = chooseAlternativeSeparatedType2(reader, behavior, rootObject)) != void.class){
			final Object element = readValue(reader, chosenAlternativeType, rootObject);

			list.add(element);
		}
	}

	private Object readValue(final BitReaderInterface reader, final CodecBehavior behavior, final Object rootObject) throws FieldException{
		final Class<?> chosenAlternativeType = CodecHelper.chooseAlternativeType(reader, behavior.bindingType, behavior.selectFrom,
			behavior.selectDefault, evaluator, rootObject);

		return readValue(reader, chosenAlternativeType, rootObject);
	}

	private Object readValue(final BitReaderInterface reader, final Class<?> chosenAlternativeType, final Object rootObject)
			throws FieldException{
		final Template<?> template = templateParser.createTemplate(chosenAlternativeType);
		return templateParser.decode(template, reader, rootObject);
	}

	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 */
	private Class<?> chooseAlternativeSeparatedType2(final BitReaderInterface reader, final CodecBehavior behavior, final Object rootObject){
		final ObjectChoices.ObjectChoice[] alternatives = behavior.objectChoicesList.alternatives();
		if(!CodecHelper.hasSelectAlternatives((alternatives)))
			return behavior.bindingType;

		final boolean hasHeader = addListHeaderToContext(reader, behavior);
		if(!hasHeader)
			return void.class;

		return CodecHelper.chooseAlternativeType(alternatives, behavior.selectDefault, evaluator, rootObject);
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the header.
	 * @return	Whether a prefix was retrieved.
	 */
	private boolean addListHeaderToContext(final BitReaderInterface reader, final CodecBehavior behavior){
		final byte terminator = behavior.objectChoicesList.terminator();
		final Charset charset = CharsetHelper.lookup(behavior.objectChoicesList.charset());
		final String prefix = reader.getTextUntilTerminatorWithoutConsuming(terminator, charset);
		evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		return !prefix.isEmpty();
	}


	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Annotation collectionBinding,
			final Object rootObject, final Object value) throws FieldException{
		final CodecBehavior behavior = CodecBehavior.of(annotation);

		CodecHelper.validate(value, behavior.validator);

		final Class<? extends Converter<?, ?>> chosenConverter = CodecHelper.getChosenConverter(behavior.converterChoices,
			behavior.defaultConverter, evaluator,rootObject);

		if(collectionBinding == null){
			final ObjectChoices objectChoices = behavior.selectFrom;
			final ObjectChoices.ObjectChoice[] alternatives = objectChoices.alternatives();
			Class<?> type = behavior.bindingType;
			if(CodecHelper.hasSelectAlternatives(alternatives)){
				type = value.getClass();
				final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(objectChoices.alternatives(), type);

				CodecHelper.writeHeader(writer, chosenAlternative, objectChoices, evaluator, rootObject);
			}

			final Object object = CodecHelper.converterEncode(chosenConverter, value);

			writeValue(writer, type, object, rootObject);
		}
		else{
			if(collectionBinding instanceof final BindAsArray ba){
				final int arraySize = CodecHelper.evaluateSize(ba.size(), evaluator, rootObject);

				encodeArray(writer, value, chosenConverter, arraySize, behavior, rootObject);
			}
			else{
				encodeList(writer, value, chosenConverter, rootObject);
			}
		}
	}

	private void encodeArray(final BitWriterInterface writer, final Object value, final Class<? extends Converter<?, ?>> chosenConverter,
			final int arraySize, final CodecBehavior behavior, final Object rootObject) throws FieldException{
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		CodecHelper.assertSizeEquals(arraySize, Array.getLength(array));

		final ObjectChoices objectChoices = behavior.selectFrom;
		if(CodecHelper.hasSelectAlternatives(objectChoices.alternatives()))
			encodeWithAlternatives(writer, array, objectChoices, rootObject);
		else
			encodeWithoutAlternatives(writer, array, behavior.bindingType, rootObject);
	}

	private void encodeList(final BitWriterInterface writer, final Object value, final Class<? extends Converter<?, ?>> chosenConverter,
			final Object rootObject) throws FieldException{
		final List<Object> list = CodecHelper.converterEncode(chosenConverter, value);

		encodeWithAlternatives(writer, list, rootObject);
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectChoices selectFrom,
			final Object rootObject) throws FieldException{
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0, length = array.length; i < length; i ++){
			final Object element = array[i];

			final Class<?> type = element.getClass();
			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writeHeader(writer, chosenAlternative, selectFrom, evaluator, rootObject);

			writeValue(writer, type, element, rootObject);
		}
	}

	private void encodeWithoutAlternatives(final BitWriterInterface writer, final Object array, final Class<?> type,
			final Object rootObject) throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0, length = Array.getLength(array); i < length; i ++){
			final Object element = Array.get(array, i);

			templateParser.encode(template, writer, rootObject, element);
		}
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final List<Object> list, final Object rootObject)
			throws FieldException{
		for(int i = 0, length = list.size(); i < length; i ++){
			final Object element = list.get(i);

			final Class<?> type = element.getClass();
			writeValue(writer, type, element, rootObject);
		}
	}

	private void writeValue(final BitWriterInterface writer, final Class<?> type, final Object object, final Object rootObject)
			throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);
		templateParser.encode(template, writer, rootObject, object);
	}

}
