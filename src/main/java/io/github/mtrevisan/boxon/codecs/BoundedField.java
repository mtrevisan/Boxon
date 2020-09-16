package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.Skip;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


/** Data associated to an annotated field. */
final class BoundedField{

	/** NOTE: MUST match the name of the method in all the annotations that defines a condition! */
	private static final String CONDITION = "condition";

	private static final String EMPTY_STRING = "";

	private final Field field;
	/** List of skips that happen BEFORE the reading/writing of this variable. */
	private final Skip[] skips;
	private final Annotation binding;

	private final String condition;


	BoundedField(final Field field, final Annotation binding){
		this(field, binding, null);
	}

	BoundedField(final Field field, final Annotation binding, final Skip[] skips){
		this.field = field;
		this.binding = binding;
		this.skips = skips;

		//pre-fetch condition method
		final Method conditionMethod = ReflectionHelper.getAccessibleMethod(binding.annotationType(), CONDITION, String.class);
		condition = ReflectionHelper.invokeMethod(binding, conditionMethod, EMPTY_STRING);
	}

	String getFieldName(){
		return field.getName();
	}

	<T> T getFieldValue(final Object obj){
		return ReflectionHelper.getFieldValue(field, obj);
	}

	void setFieldValue(final Object obj, final Object value){
		ReflectionHelper.setFieldValue(field, obj, value);
	}

	Skip[] getSkips(){
		return skips;
	}

	Annotation getBinding(){
		return binding;
	}

	String getCondition(){
		return condition;
	}

}
