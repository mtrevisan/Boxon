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
import unit731.boxon.helpers.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


enum Coder implements CoderInterface{

	DOUBLE {
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDouble binding = (BindDouble)annotation;

			final double v = reader.getDouble(binding.byteOrder());

			final Object value = CoderHelper.converterDecode(binding.converter(), v);

			CoderHelper.validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindDouble binding = (BindDouble)annotation;

			CoderHelper.validateData(binding.match(), binding.validator(), value);

			final double v = CoderHelper.converterEncode(binding.converter(), value);

			writer.putDouble(v, binding.byteOrder());
		}

		@Override
		public Class<? extends Annotation> coderType(){
			return BindDouble.class;
		}
	},

	DECIMAL{
		@Override
		public Object decode(final MessageParser messageParser, final BitBuffer reader, final Annotation annotation, final Object data){
			final BindDecimal binding = (BindDecimal)annotation;

			final BigDecimal v = reader.getDecimal(binding.type(), binding.byteOrder());

			final Object value = CoderHelper.converterDecode(binding.converter(), v);

			CoderHelper.validateData(binding.match(), binding.validator(), value);

			return value;
		}

		@Override
		public void encode(final MessageParser messageParser, final BitWriter writer, final Annotation annotation, final Object data,
				final Object value){
			final BindDecimal binding = (BindDecimal)annotation;

			CoderHelper.validateData(binding.match(), binding.validator(), value);

			final BigDecimal v = CoderHelper.converterEncode(binding.converter(), value);

			writer.putDecimal(v, binding.type(), binding.byteOrder());
		}

		@Override
		public Class<? extends Annotation> coderType(){
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
		public Class<? extends Annotation> coderType(){
			return BindChecksum.class;
		}
	};


	static final Map<Class<?>, CoderInterface> CODERS_FROM_ANNOTATION = new HashMap<>();

	static{
		for(final Coder coder : Coder.values())
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
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
	}


	public static void addCoder(final CoderInterface coder){
		if(!CODERS_FROM_ANNOTATION.containsKey(coder.coderType()))
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}

	public static CoderInterface getCoder(final Class<?> type){
		return CODERS_FROM_ANNOTATION.get(type);
	}

}
