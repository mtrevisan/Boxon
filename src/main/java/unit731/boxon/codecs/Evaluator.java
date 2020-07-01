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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;


class Evaluator{

	private static final ExpressionParser PARSER = new SpelExpressionParser();
	private static final EvaluationContext CONTEXT = new PrivateEvaluationContext();


	//NOTE: trick to allow accessing private fields
	private static class PrivateEvaluationContext extends StandardEvaluationContext{

		private static class SecurePropertyAccessor extends ReflectivePropertyAccessor{
			@Override
			protected Field findField(final String name, final Class<?> cls, final boolean mustBeStatic){
				final Field[] fields = cls.getDeclaredFields();
				//find in current class
				for(final Field field : fields)
					if(field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers())))
						return field;

				//find in parent class
				if(cls.getSuperclass() != null){
					final Field field = findField(name, cls.getSuperclass(), mustBeStatic);
					if(field != null)
						return field;
				}

				//find in interface
				for(final Class<?> implementedInterface : cls.getInterfaces()){
					final Field field = findField(name, implementedInterface, mustBeStatic);
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

	static void addToContext(final String key, final Object value){
		CONTEXT.setVariable(key, value);
	}

	static <T> T evaluate(final String expression, final Class<T> returnType, final Object data) throws EvaluationException{
		final Expression exp = PARSER.parseExpression(expression);
		return exp.getValue(CONTEXT, data, returnType);
	}

}
