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

import io.github.mtrevisan.boxon.annotations.bindings.BindArray;
import io.github.mtrevisan.boxon.annotations.bindings.ConverterChoices;
import io.github.mtrevisan.boxon.annotations.bindings.ObjectChoices;
import io.github.mtrevisan.boxon.annotations.converters.Converter;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.helpers.BitSetHelper;
import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.Evaluator;
import io.github.mtrevisan.boxon.helpers.Injected;
import io.github.mtrevisan.boxon.io.BitReaderInterface;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.ByteOrder;
import io.github.mtrevisan.boxon.io.CodecInterface;
import io.github.mtrevisan.boxon.io.ParserDataType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.BitSet;


final class CodecArray implements CodecInterface<BindArray>{

	@SuppressWarnings("unused")
	@Injected
	private Evaluator evaluator;
	@SuppressWarnings("unused")
	@Injected
	private TemplateParserInterface templateParser;


	@Override
	public Object decode(final BitReaderInterface reader, final Annotation annotation, final Object rootObject) throws FieldException{
		final BindArray binding = interpretBinding(annotation);

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);

		final Class<?> bindingType = binding.type();
		final Object[] array = createArray(bindingType, size);
		if(hasSelectAlternatives(binding))
			decodeWithAlternatives(reader, array, binding, rootObject);
		else
			decodeWithoutAlternatives(reader, array, bindingType);

		return convertValue(binding, rootObject, array);
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] createArray(final Class<? extends T> type, final int length) throws AnnotationException{
		if(ParserDataType.isPrimitive(type))
			throw AnnotationException.create("Argument cannot be a primitive: {}", type);

		return (T[])Array.newInstance(type, length);
	}

	private void decodeWithAlternatives(final BitReaderInterface reader, final Object[] array, final BindArray binding,
			final Object rootObject) throws FieldException{
		for(int i = 0, length = array.length; i < length; i ++){
			final Class<?> chosenAlternativeType = chooseAlternativeType(reader, binding, rootObject);

			//read object
			final Template<?> subTemplate = templateParser.createTemplate(chosenAlternativeType);
			array[i] = templateParser.decode(subTemplate, reader, rootObject);
		}
	}

	private void decodeWithoutAlternatives(final BitReaderInterface reader, final Object[] array, final Class<?> type)
			throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0, length = array.length; i < length; i ++)
			array[i] = templateParser.decode(template, reader, null);
	}

	@Override
	public void encode(final BitWriterInterface writer, final Annotation annotation, final Object rootObject, final Object value)
			throws FieldException{
		final BindArray binding = interpretBinding(annotation);

		CodecHelper.validate(value, binding.validator());

		final Class<? extends Converter<?, ?>> chosenConverter = getChosenConverter(binding, rootObject);
		final Object[] array = CodecHelper.converterEncode(chosenConverter, value);

		final int size = CodecHelper.evaluateSize(binding.size(), evaluator, rootObject);
		CodecHelper.assertSizeEquals(size, Array.getLength(array));

		if(hasSelectAlternatives(binding))
			encodeWithAlternatives(writer, array, binding.selectFrom(), rootObject);
		else
			encodeWithoutAlternatives(writer, array, binding.type());
	}

	private void encodeWithAlternatives(final BitWriterInterface writer, final Object[] array, final ObjectChoices selectFrom,
			final Object rootObject) throws FieldException{
		final ObjectChoices.ObjectChoice[] alternatives = selectFrom.alternatives();
		for(int i = 0, length = array.length; i < length; i ++){
			final Object elem = array[i];

			final Class<?> type = elem.getClass();
			final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, type);

			CodecHelper.writeHeader(writer, chosenAlternative, selectFrom, evaluator, rootObject);

			final Template<?> template = templateParser.createTemplate(type);

			templateParser.encode(template, writer, null, elem);
		}
	}

	private void encodeWithoutAlternatives(final BitWriterInterface writer, final Object[] array, final Class<?> type) throws FieldException{
		final Template<?> template = templateParser.createTemplate(type);

		for(int i = 0, length = array.length; i < length; i ++)
			templateParser.encode(template, writer, null, array[i]);
	}


	/**
	 * Gets the alternative class type that parses the next data.
	 *
	 * @param reader	The reader from which to read the data from.
	 * @return	The class type of the chosen alternative.
	 * @throws CodecException   If a codec cannot be found for the chosen alternative.
	 */
	private Class<?> chooseAlternativeType(final BitReaderInterface reader, final BindArray binding, final Object rootObject)
			throws CodecException{
		if(!hasSelectAlternatives(binding))
			return binding.type();

		addPrefixToContext(reader, binding);

		final ObjectChoices.ObjectChoice[] alternatives = binding.selectFrom().alternatives();
		final ObjectChoices.ObjectChoice chosenAlternative = CodecHelper.chooseAlternative(alternatives, evaluator, rootObject);
		final Class<?> chosenAlternativeType = (!CodecHelper.isEmptyChoice(chosenAlternative)
			? chosenAlternative.type()
			: binding.selectDefault());

		if(chosenAlternativeType == void.class)
			throw CodecException.create("Cannot find a valid codec from given alternatives for {}",
				rootObject.getClass().getSimpleName());

		return chosenAlternativeType;
	}

	/**
	 * Whether the select-object-from binding has any alternatives.
	 *
	 * @return	Whether the select-object-from binding has any alternatives.
	 */
	private static boolean hasSelectAlternatives(final BindArray binding){
		return (binding.selectFrom().alternatives().length > 0);
	}

	/**
	 * Add the prefix to the evaluator context if needed.
	 *
	 * @param reader	The reader from which to read the prefix.
	 */
	private void addPrefixToContext(final BitReaderInterface reader, final BindArray binding){
		final byte prefixSize = binding.selectFrom().prefixLength();
		if(prefixSize > 0){
			final BitSet bitmap = reader.getBitSet(prefixSize);
			final ByteOrder byteOrder = binding.selectFrom().byteOrder();
			final BigInteger prefix = BitSetHelper.toObjectiveType(bitmap, prefixSize, byteOrder);

			evaluator.putToContext(ContextHelper.CONTEXT_CHOICE_PREFIX, prefix);
		}
	}

	private <IN, OUT> OUT convertValue(final BindArray binding, final Object rootObject, final IN value){
		final Class<? extends Converter<?, ?>> converterType = getChosenConverter(binding, rootObject);
		final OUT convertedValue = CodecHelper.converterDecode(converterType, value);
		CodecHelper.validate(convertedValue, binding.validator());
		return convertedValue;
	}

	/**
	 * Get the first converter that matches the condition.
	 *
	 * @return	The converter class.
	 */
	private Class<? extends Converter<?, ?>> getChosenConverter(final BindArray binding, final Object rootObject){
		final ConverterChoices.ConverterChoice[] alternatives = binding.selectConverterFrom().alternatives();
		for(int i = 0, length = alternatives.length; i < length; i ++){
			final ConverterChoices.ConverterChoice alternative = alternatives[i];

			if(evaluator.evaluateBoolean(alternative.condition(), rootObject))
				return alternative.converter();
		}
		return binding.converter();
	}

}
