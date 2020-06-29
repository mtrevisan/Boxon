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
			validateAnnotation(type, alternatives);

			if(alternatives.length > 0){
				//read prefix
				final int prefixSize = selectFrom.prefixSize();
				if(prefixSize > Integer.SIZE)
					throw new IllegalArgumentException("`prefixSize` cannot be greater than " + Integer.SIZE + " bits");
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				final BitSet bits = reader.getBits(prefixSize);
				final BigInteger prefix = ByteHelper.bitsToBigInteger(bits, prefixSize, prefixByteOrder);

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
			validateAnnotation(type, alternatives);

			if(alternatives.length > 0){
				//write prefix
				final int prefixSize = selectFrom.prefixSize();
				if(prefixSize > Integer.SIZE)
					throw new IllegalArgumentException("`prefixSize` cannot be greater than " + Integer.SIZE + " bits");
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				final Choices.Choice chosenAlternative = chooseAlternative(alternatives, value.getClass());
				//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
				if(chosenAlternative.condition().contains("#" + CONTEXT_CHOICE_PREFIX)){
					long prefixValue = chosenAlternative.prefix();
					final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
					if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
						ByteHelper.reverseBits(bits, prefixSize);

					writer.putBits(bits, prefixSize);
				}

				type = value.getClass();
			}

			final Codec<?> codec = Codec.createFrom(type);

			final Object array = converterEncode(binding.converter(), value);

			messageParser.encode(codec, array, writer);
		}

		private void validateAnnotation(final Class<?> type, final Choices.Choice[] alternatives){
			if(type == Object.class && alternatives.length == 0)
				throw new IllegalArgumentException("`type` argument missing");
			if(type != Object.class && alternatives.length > 0)
				throw new IllegalArgumentException("Cannot define both `type` and `selectFrom`");
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

			final Charset charset = Charset.forName(binding.charset());
			final int size = Evaluator.evaluate(binding.size(), int.class, data);

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

			final Charset charset = Charset.forName(binding.charset());
			final int size = Evaluator.evaluate(binding.size(), int.class, data);

			final String text = converterEncode(binding.converter(), value);

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
			final byte terminator = binding.terminator();
			final boolean consumeTerminator = binding.consumeTerminator();

			final String text = reader.getTextUntilTerminator(terminator, consumeTerminator, charset);

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
			final byte terminator = binding.terminator();
			final boolean consumeTerminator = binding.consumeTerminator();

			final String text = converterEncode(binding.converter(), value);

			writer.putText(text, terminator, consumeTerminator, charset);
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

			final ByteOrder byteOrder = binding.byteOrder();
			final Class<?> type = binding.type();
			validatePrimitiveType(type);
			final Codec<?> codec = Codec.createFrom(type.getComponentType());
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());
			validateObjectiveType(type, objectiveType, codec);

			final Object array = ReflectionHelper.createArrayPrimitive(type, size);
			for(int i = 0; i < size; i ++){
				final Object value = reader.get(objectiveType, byteOrder);
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

			final ByteOrder byteOrder = binding.byteOrder();
			final Class<?> type = binding.type();
			validatePrimitiveType(type);
			final Codec<?> codec = Codec.createFrom(type.getComponentType());
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());
			validateObjectiveType(type, objectiveType, codec);

			final Object array = converterEncode(binding.converter(), value);

			for(int i = 0; i < size; i ++)
				writer.put(Array.get(array, i), byteOrder);
		}

		private void validatePrimitiveType(final Class<?> type){
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(!isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArray.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + ".class`");
		}

		private void validateObjectiveType(final Class<?> type, final Class<?> objectiveType, final Codec<?> codec){
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + codec.getClass().getSimpleName() + "<"
					+ codec + ">: " + type.getComponentType().getSimpleName());
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

			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArrayPrimitive.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + ".class`");
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

			final Object[] array = ReflectionHelper.createArray(type, size);
			if(alternatives.length > 0){
				//read prefix
				final int prefixSize = selectFrom.prefixSize();
				if(prefixSize > Integer.SIZE)
					throw new IllegalArgumentException("`prefixSize` cannot be greater than " + Integer.SIZE + " bits");
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				for(int i = 0; i < size; i ++){
					final BitSet bits = reader.getBits(prefixSize);
					final BigInteger prefix = ByteHelper.bitsToBigInteger(bits, prefixSize, prefixByteOrder);

					//choose class
					final Choices.Choice chosenAlternative = chooseAlternative(alternatives, prefix.intValue(), data);

					//read object
					final Codec<?> subCodec = Codec.createFrom(chosenAlternative.type());

					array[i] = messageParser.decode(subCodec, reader);
				}
			}
			else{
				final Codec<?> codec = Codec.createFrom(type);

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

			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArrayPrimitive.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + "[].class`");
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			final Choices selectFrom = binding.selectFrom();
			@SuppressWarnings("ConstantConditions")
			final Choices.Choice[] alternatives = (selectFrom != null? selectFrom.alternatives(): new Choices.Choice[0]);

			final Object[] array = converterEncode(binding.converter(), value);

			if(alternatives.length > 0){
				//write prefix
				final int prefixSize = selectFrom.prefixSize();
				if(prefixSize > Integer.SIZE)
					throw new IllegalArgumentException("`prefixSize` cannot be greater than " + Integer.SIZE + " bits");
				final ByteOrder prefixByteOrder = selectFrom.byteOrder();

				for(int i = 0; i < size; i ++){
					final Choices.Choice chosenAlternative = chooseAlternative(alternatives, array[i].getClass());
					//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
					if(chosenAlternative.condition().contains("#" + CONTEXT_CHOICE_PREFIX)){
						final long prefixValue = chosenAlternative.prefix();
						final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
						if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
							ByteHelper.reverseBits(bits, prefixSize);

						writer.putBits(bits, prefixSize);
					}

					final Codec<?> codec = Codec.createFrom(array[i].getClass());

					messageParser.encode(codec, array[i], writer);
				}
			}
			else{
				final Codec<?> codec = Codec.createFrom(type);

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
			final ByteOrder byteOrder = binding.byteOrder();
			if(byteOrder == ByteOrder.LITTLE_ENDIAN)
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
			final ByteOrder byteOrder = binding.byteOrder();
			if(byteOrder == ByteOrder.LITTLE_ENDIAN)
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
			final ByteOrder byteOrder = binding.byteOrder();
			if(binding.unsigned()){
				final int v = reader.getShortUnsigned(byteOrder);

				value = converterDecode(binding.converter(), v);
			}
			else{
				final short v = reader.getShort(byteOrder);

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

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putShort(v, byteOrder);
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
			final ByteOrder byteOrder = binding.byteOrder();
			if(binding.unsigned()){
				final long v = reader.getIntegerUnsigned(byteOrder);

				value = converterDecode(binding.converter(), v);
			}
			else{
				final int v = reader.getInteger(byteOrder);

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

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putInteger(v, byteOrder);
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

			final ByteOrder byteOrder = binding.byteOrder();
			final long v = reader.getLong(byteOrder);

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
			final BitSet bits = reader.getBits(size);
			final ByteOrder byteOrder = binding.byteOrder();

			final Object value;
			final boolean allowPrimitive = binding.allowPrimitive();
			if(allowPrimitive && size < Long.SIZE){
				long v = bits.toLongArray()[0];
				if(byteOrder == ByteOrder.BIG_ENDIAN)
					v = Long.reverseBytes(v) >>> (Long.SIZE - size);
				if(!binding.unsigned())
					v = ByteHelper.extendSign(v, size);

				value = converterDecode(binding.converter(), v);
			}
			else{
				final BigInteger v = ByteHelper.bitsToBigInteger(bits, size, byteOrder);

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

			BigInteger v;
			final boolean allowPrimitive = binding.allowPrimitive();
			final int size = Evaluator.evaluate(binding.size(), int.class, data);
			if(allowPrimitive && size < Long.SIZE){
				final long vv = converterEncode(binding.converter(), value);

				v = BigInteger.valueOf(Math.abs(vv));
				if(!binding.unsigned() && vv < 0)
					v = v.negate();
			}
			else
				v = converterEncode(binding.converter(), value);

			final ByteOrder byteOrder = binding.byteOrder();
			final BitSet bits = BitSet.valueOf(ByteHelper.bigIntegerToBytes(v, size, byteOrder));

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

			final ByteOrder byteOrder = binding.byteOrder();
			final float v = reader.getFloat(byteOrder);

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

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putFloat(v, byteOrder);
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

			final ByteOrder byteOrder = binding.byteOrder();
			final double v = reader.getDouble(byteOrder);

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

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putDouble(v, byteOrder);
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

			final Class<?> type = binding.type();
			validateAnnotation(type);

			final ByteOrder byteOrder = binding.byteOrder();
			final BigDecimal v = reader.getDecimal(type, byteOrder);

			final Object value = converterDecode(binding.converter(), v);

			validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindDecimal binding = (BindDecimal)annotation;

			final Class<?> type = binding.type();
			validateAnnotation(type);

			validateData(binding.match(), binding.validator(), value);

			final BigDecimal v = converterEncode(binding.converter(), value);

			final ByteOrder byteOrder = binding.byteOrder();
			writer.putDecimal(v, type, byteOrder);
		}

		private void validateAnnotation(final Class<?> type){
			if(type != Float.class && type != Double.class)
				throw new IllegalArgumentException("Bad type, should have been one of `" + Float.class.getSimpleName()
					+ ".class` or `" + Double.class.getSimpleName() + ".class`");
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

			final Class<?> type = binding.type();
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type);
			validateObjectiveType(objectiveType, type);

			final ByteOrder byteOrder = binding.byteOrder();
			return reader.get(objectiveType, byteOrder);
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindChecksum binding = (BindChecksum)annotation;

			final Class<?> type = binding.type();
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type);
			validateObjectiveType(objectiveType, type);

			final ByteOrder byteOrder = binding.byteOrder();
			writer.put(value, byteOrder);
		}

		private void validateObjectiveType(final Class<?> objectiveType, final Class<?> type){
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + getClass().getSimpleName()
					+ "<" + type.getSimpleName() + ">: " + type.getComponentType().getSimpleName());
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
			try{
				match = Evaluator.evaluate(match, String.class, data);
			}
			catch(final Exception ignored){}

			//try regex expression
			try{
				p = Pattern.compile(match);
			}
			catch(final PatternSyntaxException ignored){}

			//match exact
			if(p == null){
				try{
					p = Pattern.compile("^" + Pattern.quote(match) + "$");
				}
				catch(final PatternSyntaxException ignored){}
			}
		}
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
