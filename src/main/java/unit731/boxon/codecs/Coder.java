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
import unit731.boxon.annotations.BindBit;
import unit731.boxon.annotations.BindByte;
import unit731.boxon.annotations.BindChecksum;
import unit731.boxon.annotations.BindDouble;
import unit731.boxon.annotations.BindFloat;
import unit731.boxon.annotations.BindInteger;
import unit731.boxon.annotations.BindLong;
import unit731.boxon.annotations.BindDecimal;
import unit731.boxon.annotations.BindNumber;
import unit731.boxon.annotations.BindShort;
import unit731.boxon.annotations.BindString;
import unit731.boxon.annotations.BindStringTerminated;
import unit731.boxon.annotations.transformers.Transformer;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.utils.ByteHelper;
import unit731.boxon.utils.ReflectionHelper;

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
enum Coder{

	STRING {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindString binding = (BindString)annotation;

			final Charset charset = Charset.forName(binding.charset());
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			final String text = reader.getText(size, charset);

			final Object value = transformerDecode(binding.transformer(), text);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindString binding = (BindString)annotation;

			final Charset charset = Charset.forName(binding.charset());
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final String text = transformerEncode(binding.transformer(), value);

			writer.putText(text.substring(0, Math.min(text.length(), size)), charset);
		}

		@Override
		Class<?> coderType(){
			return BindString.class;
		}
	},

	STRING_TERMINATED {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			final Charset charset = Charset.forName(binding.charset());
			final byte terminator = binding.terminator();
			final boolean consumeTerminator = binding.consumeTerminator();

			final String text = reader.getTextUntilTerminator(terminator, consumeTerminator, charset);

			final Object value = transformerDecode(binding.transformer(), text);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindStringTerminated binding = (BindStringTerminated)annotation;

			final Charset charset = Charset.forName(binding.charset());
			final byte terminator = binding.terminator();
			final boolean consumeTerminator = binding.consumeTerminator();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final String text = transformerEncode(binding.transformer(), value);

			writer.putText(text, terminator, consumeTerminator, charset);
		}

		@Override
		Class<?> coderType(){
			return BindStringTerminated.class;
		}
	},

	ARRAY_PRIMITIVE {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;

			final ByteOrder byteOrder = binding.byteOrder();
			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(!isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArray.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + ".class`");
			final Codec<?> codec = Codec.createFrom(type.getComponentType());
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			final Object array = ReflectionHelper.createArrayPrimitive(type, size);
			final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + codec.getClass().getSimpleName() + "<"
					+ codec + ">: " + type.getComponentType().getSimpleName());
			for(int i = 0; i < size; i ++){
				final Object value = reader.get(objectiveType, byteOrder);
				Array.set(array, i, value);
			}

			final Object value = transformerDecode(binding.transformer(), array);

			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindArrayPrimitive binding = (BindArrayPrimitive)annotation;

			final ByteOrder byteOrder = binding.byteOrder();
			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(!isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArray.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + ".class`");
			final Codec<?> codec = Codec.createFrom(type.getComponentType());
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			final Class<?> objectiveType = ReflectionHelper.objectiveType(type.getComponentType());
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + codec.getClass().getSimpleName() + "<"
					+ codec + ">: " + type.getComponentType().getSimpleName());

			validateData(binding.validator(), value);

			final Object array = transformerEncode(binding.transformer(), value);
			for(int i = 0; i < size; i ++)
				writer.put(Array.get(array, i), byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindArrayPrimitive.class;
		}
	},

	ARRAY {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindArray binding = (BindArray)annotation;

			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArrayPrimitive.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + ".class`");
			final Codec<?> codec = Codec.createFrom(type);
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			final Object[] array = ReflectionHelper.createArray(type, size);
			for(int i = 0; i < size; i ++)
				array[i] = MessageParser.decode(codec, reader);

			final Object output = transformerDecode(binding.transformer(), array);

			validateData(binding.validator(), output);

			return output;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindArray binding = (BindArray)annotation;

			final Class<?> type = binding.type();
			final boolean isPrimitive = (type.isArray() && type.getComponentType().isPrimitive());
			if(isPrimitive)
				throw new IllegalArgumentException("Bad annotation used, @" + BindArrayPrimitive.class.getSimpleName()
					+ " should have been used with type `" + type.getSimpleName() + "[].class`");
			final Codec<?> codec = Codec.createFrom(type);
			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);

			validateData(binding.validator(), value);

			final Object[] array = transformerEncode(binding.transformer(), value);
			for(int i = 0; i < size; i ++)
				MessageParser.encode(codec, array[i], writer);
		}

		@Override
		Class<?> coderType(){
			return BindArray.class;
		}
	},

	BIT {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindBit binding = (BindBit)annotation;

			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);
			final ByteOrder byteOrder = binding.byteOrder();

			final BitSet bits = reader.getBits(size);
			if(byteOrder == ByteOrder.BIG_ENDIAN)
				ByteHelper.reverseBits(bits, size);

			final Object value = transformerDecode(binding.transformer(), bits);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindBit binding = (BindBit)annotation;

			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);
			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final BitSet bits = transformerEncode(binding.transformer(), value);
			if(byteOrder == ByteOrder.BIG_ENDIAN)
				ByteHelper.reverseBits(bits, size);

			writer.putBits(bits, size);
		}

		@Override
		Class<?> coderType(){
			return BindBit.class;
		}
	},

	BYTE {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindByte binding = (BindByte)annotation;

			final Object value;
			if(binding.unsigned()){
				final short v = reader.getByteUnsigned();

				value = transformerDecode(binding.transformer(), v);
			}
			else{
				final byte v = reader.getByte();

				value = transformerDecode(binding.transformer(), v);
			}

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindByte binding = (BindByte)annotation;

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final byte v = transformerEncode(binding.transformer(), value);

			writer.putByte(v);
		}

		@Override
		Class<?> coderType(){
			return BindByte.class;
		}
	},

	SHORT {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindShort binding = (BindShort)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			final Object value;
			if(binding.unsigned()){
				final int v = reader.getShortUnsigned(byteOrder);

				value = transformerDecode(binding.transformer(), v);
			}
			else{
				final short v = reader.getShort(byteOrder);

				value = transformerDecode(binding.transformer(), v);
			}

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindShort binding = (BindShort)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final short v = transformerEncode(binding.transformer(), value);

			writer.putShort(v, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindShort.class;
		}
	},

	INTEGER {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindInteger binding = (BindInteger)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			final Object value;
			if(binding.unsigned()){
				final long v = reader.getIntegerUnsigned(byteOrder);

				value = transformerDecode(binding.transformer(), v);
			}
			else{
				final int v = reader.getInteger(byteOrder);

				value = transformerDecode(binding.transformer(), v);
			}

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindInteger binding = (BindInteger)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final int v = transformerEncode(binding.transformer(), value);

			writer.putInteger(v, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindInteger.class;
		}
	},

	LONG {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindLong binding = (BindLong)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			final long v = reader.getLong(byteOrder);

			final Object value = transformerDecode(binding.transformer(), v);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindLong binding = (BindLong)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final long v = transformerEncode(binding.transformer(), value);

			writer.putLong(v, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindLong.class;
		}
	},

	NUMBER {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindNumber binding = (BindNumber)annotation;

			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);
			final ByteOrder byteOrder = binding.byteOrder();
			final boolean allowPrimitive = binding.allowPrimitive();

			final BitSet bits = reader.getBits(size);
			if(byteOrder == ByteOrder.BIG_ENDIAN)
				ByteHelper.reverseBits(bits, size);
			final Object value;
			if(allowPrimitive && size < Long.SIZE){
				long v = bits.toLongArray()[0];
				if(!binding.unsigned())
					v = ByteHelper.extendSign(v, size);

				value = transformerDecode(binding.transformer(), v);
			}
			else{
				BigInteger v;
				//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
				final byte[] bigArray = ByteHelper.reverseBytes(bits.toByteArray());
				if(bigArray[0] != 0x00 && !binding.unsigned())
					v = new BigInteger(-1, ByteHelper.invertBytes(bigArray))
						.subtract(BigInteger.ONE);
				else
					v = new BigInteger(1, bigArray);

				value = transformerDecode(binding.transformer(), v);
			}

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindNumber binding = (BindNumber)annotation;

			final int size = Evaluator.evaluate(binding.size(), Integer.class, data);
			final ByteOrder byteOrder = binding.byteOrder();
			final boolean allowPrimitive = binding.allowPrimitive();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final BigInteger v;
			if(allowPrimitive && size < Long.SIZE){
				final long vv = transformerEncode(binding.transformer(), value);
				final long mask = ByteHelper.mask(size);
				if(binding.unsigned() || vv >= 0)
					v = BigInteger.valueOf(vv & mask);
				else
					v = BigInteger.valueOf(Math.abs(vv) & mask)
						.negate();
			}
			else{
				//TODO mask value with `2^size-1`
//				final BigInteger mask = new BigInteger();
				v = transformerEncode(binding.transformer(), value);
			}

			//NOTE: need to reverse the bytes because BigInteger is big-endian and BitSet is little-endian
			final BitSet bits = BitSet.valueOf(ByteHelper.reverseBytes(ByteHelper.bigIntegerToBytes(v, size)));
			if(byteOrder == ByteOrder.BIG_ENDIAN)
				ByteHelper.reverseBits(bits, size);
			writer.putBits(bits, size);
		}

		@Override
		Class<?> coderType(){
			return BindNumber.class;
		}
	},

	FLOAT {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindFloat binding = (BindFloat)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			final float v = reader.getFloat(byteOrder);

			final Object value = transformerDecode(binding.transformer(), v);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindFloat binding = (BindFloat)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final float v = transformerEncode(binding.transformer(), value);

			writer.putFloat(v, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindFloat.class;
		}
	},

	DOUBLE {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDouble binding = (BindDouble)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			final double v = reader.getDouble(byteOrder);

			final Object value = transformerDecode(binding.transformer(), v);

			matchData(binding.match(), v);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindDouble binding = (BindDouble)annotation;

			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final double v = transformerEncode(binding.transformer(), value);

			writer.putDouble(v, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindDouble.class;
		}
	},

	DECIMAL{
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDecimal binding = (BindDecimal)annotation;

			final Class<?> type = binding.type();
			if(type != Float.class && type != Double.class)
				throw new IllegalArgumentException("Bad type, should have been one of `" + Float.class.getSimpleName()
					+ ".class` or `" + Double.class.getSimpleName() + ".class`");
			final ByteOrder byteOrder = binding.byteOrder();

			final BigDecimal v = reader.getDecimal(type, byteOrder);

			final Object value = transformerDecode(binding.transformer(), v);

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			return value;
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindDecimal binding = (BindDecimal)annotation;

			final Class<?> type = binding.type();
			if(type != Float.class && type != Double.class)
				throw new IllegalArgumentException("Bad type, should have been one of `" + Float.class.getSimpleName()
					+ ".class` or `" + Double.class.getSimpleName() + ".class`");
			final ByteOrder byteOrder = binding.byteOrder();

			matchData(binding.match(), value);
			validateData(binding.validator(), value);

			final BigDecimal v = transformerEncode(binding.transformer(), value);

			writer.putDecimal(v, type, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindDecimal.class;
		}
	},

	CHECKSUM {
		@Override
		Object decode(final BitBuffer reader, final Annotation annotation, final Object data){
			final BindChecksum binding = (BindChecksum)annotation;

			final Class<?> type = binding.type();
			final ByteOrder byteOrder = binding.byteOrder();

			final Class<?> objectiveType = ReflectionHelper.objectiveType(type);
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + getClass().getSimpleName()
					+ "<" + type.getSimpleName() + ">: " + type.getComponentType().getSimpleName());

			return reader.get(objectiveType, byteOrder);
		}

		@Override
		void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value){
			final BindChecksum binding = (BindChecksum)annotation;

			final Class<?> type = binding.type();
			final ByteOrder byteOrder = binding.byteOrder();

			final Class<?> objectiveType = ReflectionHelper.objectiveType(type);
			if(objectiveType == null)
				throw new IllegalArgumentException("Unrecognized type for field " + getClass().getSimpleName()
					+ "<" + type.getSimpleName() + ">: " + type.getComponentType().getSimpleName());

			writer.put(value, byteOrder);
		}

		@Override
		Class<?> coderType(){
			return BindChecksum.class;
		}
	};


	static final Map<Class<?>, Coder> CODERS_FROM_ANNOTATION = new HashMap<>();
	static{
		for(final Coder coder : Coder.values())
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}

	abstract Object decode(final BitBuffer reader, final Annotation annotation, final Object data);

	abstract void encode(final BitWriter writer, final Annotation annotation, final Object data, final Object value);

	abstract Class<?> coderType();


	private static <T> void matchData(final String match, final T data){
		final Pattern pattern = extractPattern(match, data);
		if(pattern != null && !pattern.matcher(Objects.toString(data)).matches())
			throw new IllegalArgumentException("Parameter does not match constraint `" + match + "`");
	}

	/** Extract pattern from a SpEL expression, or a string, or a real pattern */
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

	private static <OUT, IN> OUT transformerDecode(@SuppressWarnings("rawtypes") final Class<? extends Transformer> transformerType,
			final IN data){
		@SuppressWarnings("unchecked")
		final Transformer<IN, OUT> transformer = ReflectionHelper.createInstance(transformerType);
		return transformer.decode(data);
	}

	private static <OUT, IN> IN transformerEncode(@SuppressWarnings("rawtypes") final Class<? extends Transformer> transformerType,
			final OUT data){
		@SuppressWarnings("unchecked")
		final Transformer<IN, OUT> transformer = ReflectionHelper.createInstance(transformerType);
		return transformer.encode(data);
	}

}
