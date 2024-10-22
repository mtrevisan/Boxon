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
package io.github.mtrevisan.boxon.io;

import io.github.mtrevisan.boxon.helpers.ContextHelper;
import io.github.mtrevisan.boxon.helpers.Memoizer;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


/**
 * SpEL expression evaluator.
 */
public final class Evaluator{

	private static final String BOOLEAN_TRUE = Boolean.TRUE.toString();
	private static final String BOOLEAN_FALSE = Boolean.FALSE.toString();


	private static final class EvaluationContext extends StandardEvaluationContext{

		private final Map<String, Object> backupContext = new HashMap<>(0);


		@Override
		public void setVariable(final String name, final Object value){
			handleVariableUpdate(name, value);
		}


		/**
		 * Removes a variable from the evaluation context.
		 *
		 * @param name	The name of the variable to remove.
		 */
		public void removeVariable(final String name){
			handleVariableUpdate(name, null);
		}

		private void handleVariableUpdate(final String name, final Object value){
			super.setVariable(name, value);

			if(value != null)
				backupContext.put(name, value);
			else
				backupContext.remove(name);
		}

		@Override
		public void registerFunction(final String name, final Method method){
			super.registerFunction(name, method);

			backupContext.put(name, method);
		}

		Map<String, Object> getContext(){
			return Collections.unmodifiableMap(backupContext);
		}

		void clearContext(){
			backupContext.clear();
		}
	}


	private static final ExpressionParser PARSER;
	static{
		//allow for immediate compilation of SpEL expressions
		final SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.MIXED, null);
		PARSER = new SpelExpressionParser(config);
	}

	private static Function<String, Expression> CACHED_EXPRESSIONS;
	static{
		initialize(Memoizer.UNBOUNDED_SIZE);
	}


	private static final EvaluationContext CONTEXT = new EvaluationContext();
	static{
		//trick to allow accessing private fields
		CONTEXT.addPropertyAccessor(new ReflectiveProperty());
	}


	private Evaluator(){}


	public static void initialize(final int maxSpELMemoizerSize){
		CACHED_EXPRESSIONS = Memoizer.memoize(PARSER::parseExpression, maxSpELMemoizerSize);
	}


	/**
	 * Add a key-value pair to the context of this evaluator.
	 * <p>Passing {@code null} as {@code value} the corresponding key-value pair will be deleted.</p>
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value (pass {@code null} to remove the {@code key} from the context).
	 */
	public static void putToContext(final String key, final Object value){
		Objects.requireNonNull(key, "Key cannot be null");
		Objects.requireNonNull(value, "Value cannot be null");

		CONTEXT.setVariable(key, value);
	}

	/**
	 * Add a method to the context of this evaluator.
	 *
	 * @param method	The method.
	 */
	public static void putToContext(final Method method){
		Objects.requireNonNull(method, "Method cannot be null");

		CONTEXT.registerFunction(method.getName(), method);
	}

	/**
	 * Remove a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 */
	public static void removeFromContext(final String key){
		Objects.requireNonNull(key, "Key cannot be null");

		CONTEXT.removeVariable(key);
	}

	/**
	 * Remove a method to the context of this evaluator.
	 *
	 * @param method	The method.
	 */
	public static void removeFromContext(final Method method){
		Objects.requireNonNull(method, "Method cannot be null");

		removeFromContext(method.getName());
	}

	/**
	 * Clear the context of this evaluator.
	 */
	public static void clearContext(){
		CONTEXT.clearContext();
	}

	/**
	 * Return the context of the evaluator.
	 *
	 * @return	The context.
	 */
	public static Map<String, Object> getContext(){
		return CONTEXT.getContext();
	}


	/**
	 * Adds the current object to the evaluator context.
	 * <p>The current object is added with the key "self" in the context.</p>
	 * <p>It allows referencing the current object using SpEL expressions.</p>
	 *
	 * @param currentObject	The current object.
	 */
	public static void addCurrentObjectToEvaluatorContext(final Object currentObject){
		putToContext(ContextHelper.CONTEXT_SELF, currentObject);
	}

	/**
	 * Evaluates an expression.
	 *
	 * @param expression	The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression to
	 * 	evaluate (empty string returns {@code true}).
	 * @param rootObject	The context with which to evaluate the given expression.
	 * @param returnType	The class for the return type.
	 * @param <T>	The class type of the result.
	 * @return	The result of the expression.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	public static <T> T evaluate(final String expression, final Object rootObject, final Class<T> returnType){
		final Expression exp = CACHED_EXPRESSIONS.apply(expression);
		return exp.getValue(CONTEXT, rootObject, returnType);
	}

	/**
	 * Convenience method to fast evaluate a boolean value.
	 *
	 * @param expression	The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression to
	 * 	evaluate (empty string returns {@code true}).
	 * @param rootObject	The context with which to evaluate the given expression.
	 * @return	The result of the expression.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	public static boolean evaluateBoolean(final String expression, final Object rootObject){
		return (expression.isEmpty()
			|| BOOLEAN_TRUE.equalsIgnoreCase(expression)
			|| !BOOLEAN_FALSE.equalsIgnoreCase(expression) && evaluate(expression, rootObject, boolean.class));
	}

	/**
	 * Convenience method to fast evaluate a positive integer.
	 *
	 * @param expression	The <a href="https://docs.spring.io/spring-framework/reference/core/expressions.html">SpEL</a> expression to
	 * 	evaluate (empty string returns {@code -1}).
	 * @param rootObject	The context with which to evaluate the given expression.
	 * @return	The size, or a negative number if the expression is not a valid positive integer.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	public static int evaluateSize(final String expression, final Object rootObject){
		int size = -1;
		if(!expression.isEmpty())
			size = (isPositiveInteger(expression)
				? Integer.parseInt(expression)
				: evaluate(expression, rootObject, int.class)
			);
		return size;
	}

	private static boolean isPositiveInteger(final String text){
		final byte[] bytes = text.getBytes(StandardCharsets.US_ASCII);
		for(int i = 0, length = bytes.length; i < length; i ++)
			if(!Character.isDigit(bytes[i]))
				return false;
		return true;
	}


	private static final class ReflectiveProperty extends ReflectivePropertyAccessor{
		@Override
		protected Field findField(final String name, Class<?> cls, final boolean mustBeStatic){
			Field field = null;
			while(field == null && cls != null && cls != Object.class){
				field = findFieldInClass(name, cls, mustBeStatic);

				//go up to parent class
				cls = cls.getSuperclass();
			}
			return field;
		}

		private static Field findFieldInClass(final String name, final Class<?> cls, final boolean mustBeStatic){
			final Field[] declaredFields = cls.getDeclaredFields();
			for(int i = 0, length = declaredFields.length; i < length; i ++){
				final Field field = declaredFields[i];

				if(field.getName().equals(name) && (!mustBeStatic || isStatic(field)))
					return field;
			}
			return null;
		}

		private static boolean isStatic(final Member field){
			return Modifier.isStatic(field.getModifiers());
		}
	}

}
