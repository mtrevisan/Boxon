/*
 * Copyright (c) 2021-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.internal.managers.configuration;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeSubField;
import io.github.mtrevisan.boxon.internal.JavaHelper;

import java.lang.annotation.Annotation;


@SuppressWarnings("ClassExplicitlyAnnotation")
final class NullAlternativeSubField implements AlternativeSubField{

	@Override
	public Class<? extends Annotation> annotationType(){
		return Annotation.class;
	}

	@Override
	public String longDescription(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String unitOfMeasure(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String minProtocol(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String maxProtocol(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String minValue(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String maxValue(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String pattern(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String defaultValue(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public String charset(){
		return JavaHelper.EMPTY_STRING;
	}

	@Override
	public int radix(){
		return 0;
	}

}
