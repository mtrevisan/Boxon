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
package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationHeader;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationSkip;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.BitWriter;
import io.github.mtrevisan.boxon.external.CodecInterface;
import io.github.mtrevisan.boxon.external.EventListener;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;


public final class ConfigurationParser{

	@InjectEventListener
	@SuppressWarnings("unused")
	private final EventListener eventListener;

	private final LoaderCodecInterface loaderCodec;


	/**
	 * Create a configuration parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @return	A configuration parser.
	 */
	public static ConfigurationParser create(final LoaderCodecInterface loaderCodec){
		return new ConfigurationParser(loaderCodec, EventListener.getNoOpInstance());
	}

	/**
	 * Create a configuration parser.
	 *
	 * @param loaderCodec	A codec loader.
	 * @param eventListener	The event listener.
	 * @return	A configuration parser.
	 */
	public static ConfigurationParser create(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		return new ConfigurationParser(loaderCodec, (eventListener != null? eventListener: EventListener.getNoOpInstance()));
	}


	private ConfigurationParser(final LoaderCodecInterface loaderCodec, final EventListener eventListener){
		this.loaderCodec = loaderCodec;
		this.eventListener = eventListener;
	}

	public <T> void encode(final Configuration<?> configuration, final BitWriter writer, final T currentObject, final Version protocol)
			throws FieldException{
		final ConfigurationHeader header = configuration.getHeader();
		openMessage(header, writer);

		//encode message fields:
		final List<ConfigField> fields = configuration.getConfigurationFields();
		for(int i = 0; i < fields.size(); i ++){
			final ConfigField field = fields.get(i);

			final ConfigurationManagerInterface manager = ConfigurationManagerFactory.buildManager(field.getBinding());
			final Annotation annotation = manager.shouldBeExtracted(protocol);
			if(annotation.annotationType() == Annotation.class)
				continue;

			//process skip annotations:
			final ConfigurationSkip[] skips = field.getSkips();
			writeSkips(skips, writer, protocol);

			//process value
			encodeField(configuration, currentObject, writer, field, annotation);
			if(annotation != field.getBinding())
				encodeField(configuration, currentObject, writer, field, field.getBinding());
		}

		closeMessage(header, writer);

		writer.flush();
	}

	private <T> void encodeField(final Configuration<?> configuration, final T currentObject, final BitWriter writer,
			final ConfigField field, final Annotation binding) throws FieldException{
		final Class<? extends Annotation> annotationType = binding.annotationType();
		eventListener.writingField(configuration.getType().getName(), field.getFieldName(), annotationType.getSimpleName());

		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());

		try{
			//encode value from current object
			final Object value = field.getFieldValue(currentObject);
			//write value to raw message
			codec.encode(writer, binding, field.getFieldType(), value);

			eventListener.writtenField(configuration.getType().getName(), field.getFieldName(), value);
		}
		catch(final Exception e){
			throw FieldException.create(e)
				.withClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());
		}
	}

	private static void openMessage(final ConfigurationHeader header, final BitWriter writer){
		if(header != null && !header.start().isEmpty()){
			final String start = header.start();
			final Charset charset = Charset.forName(header.charset());
			writer.putText(start, charset);
		}
	}

	private static void closeMessage(final ConfigurationHeader header, final BitWriter writer){
		if(header != null && !header.end().isEmpty()){
			final Charset charset = Charset.forName(header.charset());
			writer.putText(header.end(), charset);
		}
	}

	private static void writeSkips(final ConfigurationSkip[] skips, final BitWriter writer, final Version protocol){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, protocol);
	}

	private static void writeSkip(final ConfigurationSkip skip, final BitWriter writer, final Version protocol){
		final boolean process = ManagerHelper.shouldBeExtracted(protocol, skip.minProtocol(), skip.maxProtocol());
		if(process)
			writer.putText(skip.terminator(), StandardCharsets.UTF_8);
	}

}
