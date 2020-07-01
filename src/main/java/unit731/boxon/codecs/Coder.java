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

import unit731.boxon.annotations.BindArray;
import unit731.boxon.annotations.BindArrayPrimitive;
import unit731.boxon.annotations.BindBits;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindDouble;
import unit731.boxon.annotations.BindFloat;
import unit731.boxon.annotations.BindInt;
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.BindDecimal;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindObject;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


@SuppressWarnings("unused")
enum Coder implements CoderInterface{

	OBJECT {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindObject binding = (BindObject)annotation;

			Class<?> type = binding.type();
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

			if(alternatives.length > 0){
				//read prefix
				final int prefixSize = selectFrom.prefixSize();
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				final BigInteger prefix = reader.getBigInteger(prefixSize, prefixByteOrder);

				//choose class
				final Choices.Choice chosenAlternative = chooseAlternative(alternatives, prefix.intValue(), data);
				type = chosenAlternative.type();
			}

			final Codec<?> codec = Codec.createFrom(type);

			final Object instance = messageParser.decode(codec, reader);

			final Object value = converterDecode(binding.converter(), instance);

			validateData(binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindObject binding = (BindObject)annotation;

			validateData(binding.validator(), value);

			Class<?> type = binding.type();
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);
			if(alternatives.length > 0){
				//write prefix
				final Choices.Choice chosenAlternative = chooseAlternative(alternatives, value.getClass());
				writePrefix(writer, value, chosenAlternative, selectFrom);

				type = value.getClass();
			}

			final Codec<?> codec = Codec.createFrom(type);

			final Object array = converterEncode(binding.converter(), value);

			messageParser.encode(codec, array, writer);
		}

		@Override
		public Class<?> coderType(){
			return BindObject.class;
		}
	},

	STRING {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindString binding = (BindString)annotation;

			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Charset charset = Charset.forName(binding.charset());
			final String text = reader.getText(size, charset);

			final Object value = converterDecode(binding.converter(), text);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindString binding = (BindString)annotation;

			validateData(binding.match(), binding.validator(), value);

			final String text = converterEncode(binding.converter(), value);

			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Charset charset = Charset.forName(binding.charset());
			writer.putText(text.substring(0, Math.min(text.length(), size)), charset);
		}

		@Override
		public Class<?> coderType(){
			return BindString.class;
		}
	},

	STRING_TERMINATED {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			final Charset charset = Charset.forName(binding.charset());

			final String text = reader.getTextUntilTerminator(binding.terminator(), binding.consumeTerminator(), charset);

			final Object value = converterDecode(binding.converter(), text);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			validateData(binding.match(), binding.validator(), value);

			final Charset charset = Charset.forName(binding.charset());

			final String text = converterEncode(binding.converter(), value);

			writer.putText(text, binding.terminator(), binding.consumeTerminator(), charset);
		}

		@Override
		public Class<?> coderType(){
			return BindStringTerminated.class;
		}
	},

	ARRAY_PRIMITIVE {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;

			final Class<?> type = binding.type();
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());

			final Object array = ReflectionHelper.createArrayPrimitive(type, size);
			for(int i = 0; i < size; i ++){
				final Object value = reader.get(objectiveType, binding.byteOrder());
				Array.set(array, i, value);
			}

			final Object value = converterDecode(binding.converter(), array);

			validateData(binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;

			validateData(binding.validator(), value);

			final int size = Evaluator.evaluate(binding.size(), int.class, data);

			final Object array = converterEncode(binding.converter(), value);

			for(int i = 0; i < size; i ++)
				writer.put(Array.get(array, i), binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindArrayPrimitive.class;
		}
	},

	ARRAY {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindArray binding = (BindArray)annotation;

			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

			final Object[] array = ReflectionHelper.createArray(binding.type(), size);
			if(alternatives.length > 0){
				//read prefix
				final int prefixSize = selectFrom.prefixSize();
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				for(int i = 0; i < size; i ++){
					final BigInteger prefix = reader.getBigInteger(prefixSize, prefixByteOrder);

					//choose class
					final Choices.Choice chosenAlternative = chooseAlternative(alternatives, prefix.intValue(), data);

					//read object
					final Codec<?> subCodec = Codec.createFrom(chosenAlternative.type());

					array[i] = messageParser.decode(subCodec, reader);
				}
			}
			else{
				final Codec<?> codec = Codec.createFrom(binding.type());

				for(int i = 0; i < size; i ++)
					array[i] = messageParser.decode(codec, reader);
			}

			final Object value = converterDecode(binding.converter(), array);

			validateData(binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindArray binding = (BindArray)annotation;

			validateData(binding.validator(), value);

			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

			final Object[] array = converterEncode(binding.converter(), value);

			if(alternatives.length > 0)
				for(int i = 0; i < size; i ++){
					writePrefix(writer, array[i], chooseAlternative(alternatives, array[i].getClass()), selectFrom);

					final Codec<?> codec = Codec.createFrom(array[i].getClass());

					messageParser.encode(codec, array[i], writer);
				}
			else{
				final Codec<?> codec = Codec.createFrom(binding.type());

				for(int i = 0; i < size; i ++)
					messageParser.encode(codec, array[i], writer);
			}
		}

		@Override
		public Class<?> coderType(){
			return BindArray.class;
		}
	},

	BITS{
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindBits binding = (BindBits)annotation;

			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final BitSet bits = reader.getBits(size);
			if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
				ByteHelper.reverseBits(bits, size);

			final Object value = converterDecode(binding.converter(), bits);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindBits binding = (BindBits)annotation;

			validateData(binding.match(), binding.validator(), value);

			final BitSet bits = converterEncode(binding.converter(), value);
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			if(binding.byteOrder() == ByteOrder.LITTLE_ENDIAN)
				ByteHelper.reverseBits(bits, size);

			writer.putBits(bits, size);
		}

		@Override
		public Class<?> coderType(){
			return BindBits.class;
		}
	},

	BYTE {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindByte binding = (BindByte)annotation;

			final Object value;
			if(binding.unsigned()){
				final short v = reader.getByteUnsigned();

				value = converterDecode(binding.converter(), v);
			}
			else{
				final byte v = reader.getByte();

				value = converterDecode(binding.converter(), v);
			}

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindByte binding = (BindByte)annotation;

			validateData(binding.match(), binding.validator(), value);

			final byte v = converterEncode(binding.converter(), value);

			writer.putByte(v);
		}

		@Override
		public Class<?> coderType(){
			return BindByte.class;
		}
	},

	SHORT {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindShort binding = (BindShort)annotation;

			final Object value;
			if(binding.unsigned()){
				final int v = reader.getShortUnsigned(binding.byteOrder());

				value = converterDecode(binding.converter(), v);
			}
			else{
				final short v = reader.getShort(binding.byteOrder());

				value = converterDecode(binding.converter(), v);
			}

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindShort binding = (BindShort)annotation;

			validateData(binding.match(), binding.validator(), value);

			final short v = converterEncode(binding.converter(), value);

			writer.putShort(v, binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindShort.class;
		}
	},

	INT{
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindInt binding = (BindInt)annotation;

			final Object value;
			if(binding.unsigned()){
				final long v = reader.getIntegerUnsigned(binding.byteOrder());

				value = converterDecode(binding.converter(), v);
			}
			else{
				final int v = reader.getInteger(binding.byteOrder());

				value = converterDecode(binding.converter(), v);
			}

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindInt binding = (BindInt)annotation;

			validateData(binding.match(), binding.validator(), value);

			final int v = converterEncode(binding.converter(), value);

			writer.putInteger(v, binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindInt.class;
		}
	},

	LONG {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindLong binding = (BindLong)annotation;

			final long v = reader.getLong(binding.byteOrder());

			final Object value = converterDecode(binding.converter(), v);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindLong binding = (BindLong)annotation;

			validateData(binding.match(), binding.validator(), value);

			final long v = converterEncode(binding.converter(), value);

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putLong(v, byteOrder);
		}

		@Override
		public Class<?> coderType(){
			return BindLong.class;
		}
	},

	INTEGER{
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindInteger binding = (BindInteger)annotation;

			final int size = Evaluator.evaluate(binding.size(), int.class, data);

			final Object value;
			if(binding.allowPrimitive() && size < Long.SIZE){
				final long v = reader.getLong(size, binding.byteOrder(), binding.unsigned());

				value = converterDecode(binding.converter(), v);
			}
			else{
				final BigInteger v = reader.getBigInteger(size, binding.byteOrder());

				value = converterDecode(binding.converter(), v);
			}

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindInteger binding = (BindInteger)annotation;

			validateData(binding.match(), binding.validator(), value);

			final int size = Evaluator.evaluate(binding.size(), int.class, data);

			BigInteger v;
			if(binding.allowPrimitive() && size < Long.SIZE){
				final long vv = converterEncode(binding.converter(), value);

				v = BigInteger.valueOf(Math.abs(vv));
				if(vv < 0)
					v.setBit(size);
			}
			else
				v = converterEncode(binding.converter(), value);

			final ByteOrder byteOrder = binding.byteOrder();
			final BitSet bits = ByteHelper.bigIntegerToBitSet(v, size, byteOrder);

			writer.putBits(bits, size);
		}

		@Override
		public Class<?> coderType(){
			return BindInteger.class;
		}
	},

	FLOAT {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindFloat binding = (BindFloat)annotation;

			final float v = reader.getFloat(binding.byteOrder());

			final Object value = converterDecode(binding.converter(), v);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindFloat binding = (BindFloat)annotation;

			validateData(binding.match(), binding.validator(), value);

			final float v = converterEncode(binding.converter(), value);

			writer.putFloat(v, binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindFloat.class;
		}
	},

	DOUBLE {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDouble binding = (BindDouble)annotation;

			final double v = reader.getDouble(binding.byteOrder());

			final Object value = converterDecode(binding.converter(), v);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindDouble binding = (BindDouble)annotation;

			validateData(binding.match(), binding.validator(), value);

			final double v = converterEncode(binding.converter(), value);

			writer.putDouble(v, binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindDouble.class;
		}
	},

	DECIMAL{
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDecimal binding = (BindDecimal)annotation;

			final BigDecimal v = reader.getDecimal(binding.type(), binding.byteOrder());

			final Object value = converterDecode(binding.converter(), v);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindDecimal binding = (BindDecimal)annotation;

			validateData(binding.match(), binding.validator(), value);

			final BigDecimal v = converterEncode(binding.converter(), value);

			writer.putDecimal(v, binding.type(), binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindDecimal.class;
		}
	},

	CHECKSUM {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindChecksum binding = (BindChecksum)annotation;

			final Class<?> objectiveType = ReflectionHelper.objectiveType(binding.type());

			return reader.get(objectiveType, binding.byteOrder());
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindChecksum binding = (BindChecksum)annotation;

			writer.put(value, binding.byteOrder());
		}

		@Override
		public Class<?> coderType(){
			return BindChecksum.class;
		}
	};


	private static final String CONTEXT_CHOICE_PREFIX = "prefix";

	static final Map<Class<?>, CoderInterface> CODERS_FROM_ANNOTATION = new HashMap<>();

	static{
		for(final Coder coder : Coder.values())
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}


	public static void addCoder(final CoderInterface coder){
		if(!CODERS_FROM_ANNOTATION.containsKey(coder.coderType()))
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}

	public static CoderInterface getCoder(final Class<?> type){
		return CODERS_FROM_ANNOTATION.get(type);
	}

	private static Choices.Choice chooseAlternative(final Choices.Choice[] alternatives, final int prefix, final Object data){
		Choices.Choice chosenAlternative = null;

		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		for(final Choices.Choice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), boolean.class, data)){
				chosenAlternative = alternative;
				break;
			}
		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, null);

		return chosenAlternative;
	}

	private static Choices.Choice chooseAlternative(final Choices.Choice[] alternatives, final Class<?> type){
		Choices.Choice chosenAlternative = null;
		for(final Choices.Choice alternative : alternatives)
			if(alternative.type() == type){
				chosenAlternative = alternative;
				break;
			}
		return chosenAlternative;
	}

	private static void writePrefix(final BitWriter writer, final Object value, final Choices.Choice chosenAlternative, final Choices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
		if(chosenAlternative.condition().contains("#" + CONTEXT_CHOICE_PREFIX)){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final long prefixValue = chosenAlternative.prefix();
			final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				ByteHelper.reverseBits(bits, prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	private static <T> void validateData(final String match, @SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType,
			final T data){
		matchData(match, data);
		validateData(validatorType, data);
	}

	private static <T> void matchData(final String match, final T data){
		final Pattern pattern = extractPattern(match, data);
		if(pattern != null && !pattern.matcher(Objects.toString(data)).matches())
			throw new IllegalArgumentException("Parameter does not match constraint `" + match + "`");
	}

	/** Extract pattern from a SpEL expression, or a string, or a regex pattern */
	private static <T> Pattern extractPattern(String match, final T data){
		Pattern p = null;
		if(isNotBlank(match)){
			//try SpEL expression
			match = extractSpELExpression(match, data);

			//try regex expression
			p = extractRegexExpression(match);

			//match exact
			if(p == null)
				p = extractRegexExpression("^" + Pattern.quote(match) + "$");
		}
		return p;
	}

	private static <T> String extractSpELExpression(String match, final T data){
		try{
			match = Evaluator.evaluate(match, String.class, data);
		}
		catch(final Exception ignored){}
		return match;
	}

	private static Pattern extractRegexExpression(final String match){
		Pattern p = null;
		try{
			p = Pattern.compile(match);
		}
		catch(final PatternSyntaxException ignored){}
		return p;
	}

	private static boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

	private static <T> void validateData(@SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType, final T data){
		@SuppressWarnings("unchecked")
		final Validator<T> validator = ReflectionHelper.createInstance(validatorType);
		if(!validator.validate(data))
			throw new IllegalArgumentException("Validation not passed (" + data + ")");
	}

	private static <OUT, IN> OUT converterDecode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType,
			final IN data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.createInstance(converterType);
		return converter.decode(data);
	}

	private static <OUT, IN> IN converterEncode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType,
			final OUT data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.createInstance(converterType);
		return converter.encode(data);
	}

}
