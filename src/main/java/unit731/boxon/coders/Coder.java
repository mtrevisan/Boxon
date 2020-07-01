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


enum Coder{
	AS;

	static final Map<Class<?>, CoderInterface> CODERS_FROM_ANNOTATION = new HashMap<>();

	static{
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
		CODERS_FROM_ANNOTATION.put(BindDouble.class, new CoderDouble());
		CODERS_FROM_ANNOTATION.put(BindDecimal.class, new CoderDecimal());
		CODERS_FROM_ANNOTATION.put(BindChecksum.class, new CoderChecksum());
	}


	public static void addCoder(final CoderInterface coder){
		if(!CODERS_FROM_ANNOTATION.containsKey(coder.coderType()))
			CODERS_FROM_ANNOTATION.put(coder.coderType(), coder);
	}

	public static CoderInterface getCoder(final Class<?> type){
		return CODERS_FROM_ANNOTATION.get(type);
	}

}
