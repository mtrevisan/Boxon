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
package io.github.mtrevisan.boxon.core.helpers;

import io.github.mtrevisan.boxon.helpers.JavaHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;


/**
 * Utilities for mapping object fields to a {@link Map}.
 */
public final class FieldMapper{

	private FieldMapper(){}


	/**
	 * Returns the value of the field represented by this {@code Field}, on the specified object.
	 *
	 * @param object	Object from which the represented field's value is to be extracted.
	 * @param field	The field whose value is to be extracted.
	 * @return	The value.
	 */
	public static Object getFieldValue(final Object object, final Field field){
		try{
			return field.get(object);
		}
		catch(final IllegalAccessException ignored){
			//should never happen
			return null;
		}
	}

	/**
	 * Maps the fields of an object to a Map, where the keys are the field names and the values are the field values.
	 *
	 * @param object	The object whose fields should be mapped.
	 * @return	A Map containing the field names as keys and the field values as values.
	 */
	public static Map<String, Object> mapObject(final Object object){
		if(object == null)
			return null;

		final List<Field> fields = FieldAccessor.getAccessibleFields(object.getClass());
		final int size = fields.size();
		final Map<String, Object> map = JavaHelper.createMapOrEmpty(size);
		for(int i = 0; i < size; i ++)
			addFieldToMap(fields.get(i), object, map);
		return map;
	}

	private static void addFieldToMap(final Field field, final Object object, final Map<String, Object> map){
		final String key = field.getName();
		final Object value = getFieldValue(object, field);
		map.put(key, value);
	}

}
