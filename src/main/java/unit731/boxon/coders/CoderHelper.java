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

import unit731.boxon.annotations.Choices;
import unit731.boxon.annotations.converters.Converter;
import unit731.boxon.annotations.validators.Validator;
import unit731.boxon.helpers.ByteHelper;
import unit731.boxon.helpers.ReflectionHelper;

import java.util.BitSet;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


class CoderHelper{

	private static final String CONTEXT_CHOICE_PREFIX = "prefix";


	private CoderHelper(){}

	static Choices.Choice chooseAlternative(final Choices.Choice[] alternatives, final int prefix, final Object data){
		Choices.Choice chosenAlternative = null;

		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, prefix);
		for(final Choices.Choice alternative : alternatives)
			if(Evaluator.evaluate(alternative.condition(), boolean.class, data)){
				chosenAlternative = alternative;
				break;
			}
		Evaluator.addToContext(CONTEXT_CHOICE_PREFIX, null);

		return chosenAlternative;
	}

	static Choices.Choice chooseAlternative(final Choices.Choice[] alternatives, final Class<?> type){
		Choices.Choice chosenAlternative = null;
		for(final Choices.Choice alternative : alternatives)
			if(alternative.type() == type){
				chosenAlternative = alternative;
				break;
			}
		return chosenAlternative;
	}

	static void writePrefix(final BitWriter writer, final Object value, final Choices.Choice chosenAlternative, final Choices selectFrom){
		//if chosenAlternative.condition() contains '#prefix', then write @Choice.Prefix.value()
		if(chosenAlternative.condition().contains("#" + CONTEXT_CHOICE_PREFIX)){
			final int prefixSize = selectFrom.prefixSize();
			final ByteOrder prefixByteOrder = selectFrom.byteOrder();

			final long prefixValue = chosenAlternative.prefix();
			final BitSet bits = BitSet.valueOf(new long[]{prefixValue});
			if(prefixByteOrder == ByteOrder.LITTLE_ENDIAN)
				ByteHelper.reverseBits(bits, prefixSize);

			writer.putBits(bits, prefixSize);
		}
	}

	static <T> void validateData(final String match, @SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType, final T data){
		matchData(match, data);
		validateData(validatorType, data);
	}

	private static <T> void matchData(final String match, final T data){
		final Pattern pattern = extractPattern(match, data);
		if(pattern != null && !pattern.matcher(Objects.toString(data)).matches())
			throw new IllegalArgumentException("Parameter does not match constraint `" + match + "`");
	}

	/** Extract pattern from a SpEL expression, or a string, or a regex pattern */
	private static <T> Pattern extractPattern(String match, final T data){
		Pattern p = null;
		if(isNotBlank(match)){
			//try SpEL expression
			match = extractSpELExpression(match, data);

			//try regex expression
			p = extractRegexExpression(match);

			//match exact
			if(p == null)
				p = extractRegexExpression("^" + Pattern.quote(match) + "$");
		}
		return p;
	}

	private static <T> String extractSpELExpression(String match, final T data){
		try{
			match = Evaluator.evaluate(match, String.class, data);
		}
		catch(final Exception ignored){}
		return match;
	}

	private static Pattern extractRegexExpression(final String match){
		Pattern p = null;
		try{
			p = Pattern.compile(match);
		}
		catch(final PatternSyntaxException ignored){}
		return p;
	}

	private static boolean isNotBlank(final String text){
		return (text != null && !text.trim().isBlank());
	}

	static <T> void validateData(@SuppressWarnings("rawtypes") final Class<? extends Validator> validatorType, final T data){
		@SuppressWarnings("unchecked")
		final Validator<T> validator = ReflectionHelper.createInstance(validatorType);
		if(!validator.validate(data))
			throw new IllegalArgumentException("Validation not passed (" + data + ")");
	}

	static <OUT, IN> OUT converterDecode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType, final IN data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.createInstance(converterType);
		return converter.decode(data);
	}

	static <OUT, IN> IN converterEncode(@SuppressWarnings("rawtypes") final Class<? extends Converter> converterType, final OUT data){
		@SuppressWarnings("unchecked")
		final Converter<IN, OUT> converter = ReflectionHelper.createInstance(converterType);
		return converter.encode(data);
	}

}
