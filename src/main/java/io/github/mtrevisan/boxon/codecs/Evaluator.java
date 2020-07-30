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
package io.github.mtrevisan.boxon.codecs;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;


final class Evaluator{

	//allow for immediate compilation of SpEL expressions
	private static final SpelParserConfiguration CONFIG = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
	private static final ExpressionParser PARSER = new SpelExpressionParser(CONFIG);
	private static final StandardEvaluationContext CONTEXT = new PrivateEvaluationContext();


	//trick to allow accessing private fields
	private static class PrivateEvaluationContext extends StandardEvaluationContext{

		private static class SecurePropertyAccessor extends ReflectivePropertyAccessor{
			@Override
			protected Field findField(final String name, final Class<?> cls, final boolean mustBeStatic){
				Field field = findInCurrentClass(name, cls, mustBeStatic);
				if(field == null)
					field = findInParentClass(name, cls, mustBeStatic);
				if(field == null)
					field = findInInterface(name, cls, mustBeStatic);
				return field;
			}

			private Field findInCurrentClass(final String name, final Class<?> cls, final boolean mustBeStatic){
				final Field[] fields = cls.getDeclaredFields();
				for(int i = 0; i < fields.length; i ++){
					final Field field = fields[i];
					if(field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers())))
						return field;
				}
				return null;
			}

			private Field findInParentClass(final String name, Class<?> cls, final boolean mustBeStatic){
				cls = cls.getSuperclass();
				return (cls != null? findField(name, cls, mustBeStatic): null);
			}

			private Field findInInterface(final String name, final Class<?> cls, final boolean mustBeStatic){
				final Class<?>[] interfaces = cls.getInterfaces();
				for(int i = 0; i < interfaces.length; i ++){
					final Field field = findField(name, interfaces[i], mustBeStatic);
					if(field != null)
						return field;
				}
				return null;
			}
		}


		@Override
		public List<PropertyAccessor> getPropertyAccessors(){
			return Collections.singletonList(new SecurePropertyAccessor());
		}
	}


	private Evaluator(){}

	/**
	 * Adds a key-value pair to the context of this evaluator.
	 *
	 * @param key	The key used to reference the value.
	 * @param value	The value.
	 */
	static void addToContext(final String key, final Object value){
		CONTEXT.setVariable(key, value);
	}

	/**
	 * Remove a key-value pair from the context of this evaluator.
	 *
	 * @param key	The key used to reference the value to be removed.
	 */
	static void removeFromContext(final String key){
		CONTEXT.setVariable(key, null);
	}

	/**
	 * Adds a method to the context of this evaluator.
	 *
	 * @param method	The method.
	 */
	static void addToContext(final Method method){
		CONTEXT.registerFunction(method.getName(), method);
	}

	static <T> T evaluate(final String expression, final Object rootObject, final Class<T> returnType) throws EvaluationException{
		final Expression exp = PARSER.parseExpression(expression);
		return exp.getValue(CONTEXT, rootObject, returnType);
	}

	/**
	 * Convenience method to fast evaluate a positive integer.
	 *
	 * @param expression	The SpEL expression to evaluate.
	 * @param rootObject	The context with which to evaluate the given expression.
	 * @return	The size, or a negative number if the expression is not a valid positive integer.
	 * @throws EvaluationException	If an error occurs during the evaluation of an expression.
	 */
	static int evaluateSize(final String expression, final Object rootObject) throws EvaluationException{
		int size = -1;
		if(!expression.isBlank())
			size = (isPositiveInteger(expression)? Integer.parseInt(expression): evaluate(expression, rootObject, int.class));
		return size;
	}

	private static boolean isPositiveInteger(final String text){
		for(int i = 0; i < text.length(); i ++)
			if(!Character.isDigit(text.charAt(i)))
				return false;
		return true;
	}

}
