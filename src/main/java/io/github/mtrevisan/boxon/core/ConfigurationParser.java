/**
 * Copyright (c) 2020-2021 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationMessage;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitSet;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.internal.InjectEventListener;
import io.github.mtrevisan.boxon.internal.JavaHelper;
import io.github.mtrevisan.boxon.internal.ReflectionHelper;
import io.github.mtrevisan.boxon.internal.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


final class ConfigurationParser{

	@InjectEventListener
	@SuppressWarnings("unused")
	private final EventListener eventListener;

	private final LoaderCodec loaderCodec;
	private final LoaderTemplate loaderTemplate;
	private final TemplateParser templateParser;


	/**
	 * Create a configuration parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param loaderTemplate	A template loader.
	 * @param templateParser	A template parser.
	 * @return	A configuration parser.
	 */
	public static ConfigurationParser create(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate,
			final TemplateParser templateParser){
		return new ConfigurationParser(loaderCodec, loaderTemplate, templateParser, EventListener.getNoOpInstance());
	}

	/**
	 * Create a configuration parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param loaderTemplate	A template loader.
	 * @param templateParser	A template parser.
	 * @param eventListener	The event listener.
	 * @return	A configuration parser.
	 */
	public static ConfigurationParser create(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate,
			final TemplateParser templateParser, final EventListener eventListener){
		return new ConfigurationParser(loaderCodec, loaderTemplate, templateParser,
			(eventListener != null? eventListener: EventListener.getNoOpInstance()));
	}


	ConfigurationParser(final LoaderCodec loaderCodec, final LoaderTemplate loaderTemplate, final TemplateParser templateParser,
			final EventListener eventListener){
		this.loaderCodec = loaderCodec;
		this.loaderTemplate = loaderTemplate;
		this.templateParser = templateParser;
		this.eventListener = eventListener;
	}

	<T> void encode(final Configuration<?> configuration, final BitWriter writer, final T currentObject, final Version protocol)
			throws FieldException{
		openMessage(configuration, writer);

		//encode message fields:
		final List<ConfigurationField> fields = configuration.getConfigurationFields();
		for(int i = 0; i < fields.size(); i ++){
			final ConfigurationField field = fields.get(i);

			final Annotation annotation = field.getBinding();
			String minProtocol = null;
			String maxProtocol = null;
			if(annotation instanceof io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField){
				final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField binding
					= (io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField)annotation;
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			else if(annotation instanceof io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField){
				final io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField binding
					= (io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField)annotation;
				minProtocol = binding.minProtocol();
				maxProtocol = binding.maxProtocol();
			}
			if(!LoaderConfiguration.shouldBeExtracted(protocol, minProtocol, maxProtocol))
				continue;

			//process skip annotations:
			final ConfigurationSkip[] skips = field.getSkips();
			writeSkips(skips, writer, protocol);

			//process value
			encodeField(configuration, currentObject, writer, field);
		}

		closeMessage(configuration, writer);

		writer.flush();
	}

	private <T> void encodeField(final Configuration<?> configuration, final T currentObject, final BitWriter writer,
			final ConfigurationField field) throws FieldException{
		try{
			final Annotation binding = field.getBinding();
			final CodecInterface<?> codec = retrieveCodec(binding.annotationType());

			eventListener.writingField(configuration.getType().getName(), field.getFieldName(), binding.annotationType().getSimpleName());

			if(binding instanceof io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField){
				//encode value from current object
				final Object value = field.getFieldValue(currentObject);
				//write value to raw message
				codec.encode(writer, binding, field.getFieldType(), value);

				eventListener.writtenField(configuration.getType().getName(), field.getFieldName(), value);
			}
			else if(binding instanceof io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField){
				final io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField compositeBinding
					= (io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField)binding;
				final io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField[] fields = compositeBinding.value();
				final Map<String, String> writings = new HashMap<>(fields.length);
				for(int i = 0; i < fields.length; i ++){
					final CodecInterface<?> compositeCodec = retrieveCodec(fields[i].annotationType());
					//encode value from current object
					final Object value = field.getFieldValue(currentObject);
					//write value to raw message
					final BitWriter compositeWriter = BitWriter.create();
					compositeCodec.encode(compositeWriter, compositeBinding, field.getFieldType(), value);
					compositeWriter.flush();

					writings.put(fields[i].shortDescription(), compositeWriter.toString());
				}
				//TODO compose compositeValue following composition pattern
				final StringBuilder compositeValue
					= new StringBuilder(((io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField)binding).composition());
				for(int i = 0; i < fields.length; i ++){
					final int index = compositeValue.toString().indexOf("{" + i + "}");
					compositeValue.replace(index, index + 3, writings.get(i));
				}
				codec.encode(writer, binding, field.getFieldType(), compositeValue.toString());

				eventListener.writtenField(configuration.getType().getName(), field.getFieldName(), compositeValue.toString());
			}
		}
		catch(final CodecException | AnnotationException e){
			e.setClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());
			throw e;
		}
		catch(final Exception e){
			final FieldException exc = FieldException.create(e);
			exc.setClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());
			throw exc;
		}
	}

	private void openMessage(final Configuration<?> configuration, final BitWriter writer){
		final ConfigurationMessage header = configuration.getHeader();
		if(header != null && !header.start().isEmpty()){
			final String start = header.start();
			final Charset charset = Charset.forName(header.charset());
			writer.putText(start, charset);
		}
	}

	private void closeMessage(final Configuration<?> configuration, final BitWriter writer){
		final ConfigurationMessage header = configuration.getHeader();
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			writer.putText(header.end(), charset);
		}
	}

	private CodecInterface<?> retrieveCodec(final Class<? extends Annotation> annotationType) throws CodecException{
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName());

		setTemplateParser(codec);
		return codec;
	}

	private void setTemplateParser(final CodecInterface<?> codec){
		try{
			ReflectionHelper.setFieldValue(codec, LoaderTemplate.class, loaderTemplate);
		}
		catch(final Exception ignored){}
		try{
			ReflectionHelper.setFieldValue(codec, TemplateParser.class, templateParser);
		}
		catch(final Exception ignored){}
	}

	private <T> void writeSkips(final ConfigurationSkip[] skips, final BitWriter writer, final Version protocol){
		for(int i = 0; i < JavaHelper.lengthOrZero(skips); i ++)
			writeSkip(skips[i], writer, protocol);
	}

	private void writeSkip(final ConfigurationSkip skip, final BitWriter writer, final Version protocol){
		final boolean process = LoaderConfiguration.shouldBeExtracted(protocol, skip.minProtocol(), skip.maxProtocol());
		if(process){
			final int size = Integer.parseInt(skip.size());
			if(size > 0)
				/** skip {@link size} bits */
				writer.putBits(BitSet.empty(), size);
			else if(skip.consumeTerminator())
				//skip until terminator
				writer.putByte(skip.terminator());
		}
	}

}
