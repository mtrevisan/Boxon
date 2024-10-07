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
package io.github.mtrevisan.boxon.core.parsers;

import io.github.mtrevisan.boxon.annotations.ContextParameter;
import io.github.mtrevisan.boxon.annotations.PostProcess;
import io.github.mtrevisan.boxon.annotations.SkipBits;
import io.github.mtrevisan.boxon.annotations.TemplateHeader;
import io.github.mtrevisan.boxon.core.helpers.templates.SkipParams;
import io.github.mtrevisan.boxon.core.helpers.templates.Template;
import io.github.mtrevisan.boxon.core.helpers.templates.TemplateField;
import io.github.mtrevisan.boxon.exceptions.BoxonException;
import io.github.mtrevisan.boxon.io.BitWriterInterface;
import io.github.mtrevisan.boxon.io.Evaluator;

import java.util.List;


final class TemplateEncoder extends TemplateCoderBase{

	/**
	 * Create a template parser.
	 *
	 * @param evaluator	An evaluator.
	 * @return	A template parser.
	 */
	static TemplateEncoder create(final Evaluator evaluator){
		return new TemplateEncoder(evaluator);
	}


	private TemplateEncoder(final Evaluator evaluator){
		super(evaluator);
	}


	/**
	 * Encodes a message using the provided template and writer.
	 *
	 * @param template	The template used for encoding the message.
	 * @param writer	The writer used for writing the encoded message.
	 * @param parentObject	The parent object of the message being encoded.
	 * @param currentObject	The object to be encoded.
	 * @throws BoxonException	If there is an error encoding a field.
	 */
	<T> void encode(final Template<?> template, final BitWriterInterface writer, final Object parentObject, final T currentObject)
			throws BoxonException{
		final ParserContext<T> parserContext = ParserContext.create(currentObject, parentObject);
		parserContext.setClassName(template.getName());
		evaluator.addCurrentObjectToEvaluatorContext(currentObject);

		preProcessFields(template, parserContext);

		final Object rootObject = parserContext.getRootObject();

		//encode message fields:
		encodeMessageFields(template.getTemplateFields(), writer, rootObject, parserContext);

		final TemplateHeader header = template.getHeader();
		if(header != null)
			ParserWriterHelper.writeAffix(header.end(), header.charset(), writer);
	}

	private void preProcessFields(final Template<?> template, final ParserContext<?> parserContext){
		processFields(template, parserContext, PostProcess::valueEncode);
	}

	private <T> void encodeMessageFields(final List<TemplateField> fields, final BitWriterInterface writer, final Object rootObject,
			final ParserContext<T> parserContext) throws BoxonException{
		for(int i = 0, length = fields.size(); i < length; i ++){
			final TemplateField field = fields.get(i);

			//process skip annotations:
			final SkipParams[] skips = field.getSkips();
			writeSkips(skips, writer, rootObject);

			//check if field has to be processed...
			final boolean shouldProcessField = shouldProcessField(field.getCondition(), rootObject);
			if(shouldProcessField){
				//... and if so, process it
				encodeField(writer, parserContext, field);
			}
		}
	}

	private <T> void encodeField(final BitWriterInterface writer, final ParserContext<T> parserContext, final TemplateField field)
			throws BoxonException{
		final List<ContextParameter> contextParameters = field.getContextParameters();
		addContextParameters(contextParameters);

		parserContext.setField(field);
		parserContext.setFieldName(field.getFieldName());
		parserContext.setBinding(field.getBinding());
		parserContext.setCollectionBinding(field.getCollectionBinding());

		try{
			ParserWriterHelper.encodeField(parserContext, writer, eventListener);
		}
		finally{
			clearContextParameters(contextParameters);
		}
	}

	private void writeSkips(final SkipParams[] skips, final BitWriterInterface writer, final Object rootObject){
		for(int i = 0, length = skips.length; i < length; i ++)
			writeSkip(skips[i], writer, rootObject);
	}

	private void writeSkip(final SkipParams skip, final BitWriterInterface writer, final Object rootObject){
		final boolean process = shouldProcessField(skip.condition(), rootObject);
		if(!process)
			return;

		//choose between skip-by-size and skip-by-terminator
		if(skip.annotationType() == SkipBits.class){
			final int size = evaluator.evaluateSize(skip.size(), rootObject);
			writer.skipBits(size);
		}
		else if(skip.consumeTerminator())
			//skip until terminator
			writer.writeByte(skip.value());
	}

}
