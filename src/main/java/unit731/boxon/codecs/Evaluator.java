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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


class Evaluator{

	private static final ExpressionParser PARSER = new SpelExpressionParser();
	private static final EvaluationContext CONTEXT = new PrivateEvaluationContext();


	//NOTE: trick to allow accessing private fields
	private static class PrivateEvaluationContext extends StandardEvaluationContext{

		private static class SecurePropertyAccessor extends ReflectivePropertyAccessor{
			@Override
			protected Field findField(final String name, final Class<?> clazz, final boolean mustBeStatic){
				final Field[] fields = clazz.getDeclaredFields();
				//find in current class
				for(final Field field : fields)
					if(field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers())))
						return field;

				//find in parent class
				if(clazz.getSuperclass() != null){
					final Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
					if(field != null)
						return field;
				}

				//find in interface
				for(Class<?> implementedInterface : clazz.getInterfaces()){
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
