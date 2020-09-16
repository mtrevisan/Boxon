package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.Evaluate;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.reflect.Field;


/** Data associated to a directly evaluable field. */
final class EvaluatedField{

	private final Field field;
	private final Evaluate binding;


	EvaluatedField(final Field field, final Evaluate binding){
		this.field = field;
		this.binding = binding;
	}

	String getFieldName(){
		return field.getName();
	}

	Class<?> getFieldType(){
		return field.getType();
	}

	void setFieldValue(final Object obj, final Object value){
		ReflectionHelper.setFieldValue(field, obj, value);
	}

	Evaluate getBinding(){
		return binding;
	}

}
