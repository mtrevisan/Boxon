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
package io.github.mtrevisan.boxon.core.helpers.templates;

import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.annotations.PostProcess;

import java.lang.reflect.Field;
import java.util.Objects;


/**
 * Data associated with a directly evaluable field.
 *
 * @param <B>	One of {@link Evaluate} or {@link PostProcess}.
 */
public final class EvaluatedField<B>{

	private final Field field;
	private final B binding;


	static <B> EvaluatedField<B> create(final Field field, final B binding){
		return new EvaluatedField<>(field, binding);
	}


	private EvaluatedField(final Field field, final B binding){
		this.field = field;
		this.binding = binding;
	}


	/**
	 * Retrieves the {@link Field} associated with the evaluated field.
	 *
	 * @return	The {@link Field} associated with the evaluated field.
	 */
	public Field getField(){
		return field;
	}

	/**
	 * The name of the field.
	 *
	 * @return	The name of the field.
	 */
	public String getFieldName(){
		return field.getName();
	}

	/**
	 * The type of the field.
	 *
	 * @return	The type of the field.
	 */
	public Class<?> getFieldType(){
		return field.getType();
	}

	/**
	 * The annotation bound to the field.
	 *
	 * @return	The annotation bound to the field.
	 */
	public B getBinding(){
		return binding;
	}


	@Override
	public String toString(){
		return "EvaluatedField{" + "field=" + field + ", binding=" + binding + '}';
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final EvaluatedField<?> other = (EvaluatedField<?>)obj;
		return (Objects.equals(field, other.field) && Objects.equals(binding, other.binding));
	}

	@Override
	public int hashCode(){
		int result = Objects.hashCode(field);
		result = 31 * result + Objects.hashCode(binding);
		return result;
	}

}
