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
import io.github.mtrevisan.boxon.codecs.managers.ConfigField;
import io.github.mtrevisan.boxon.codecs.managers.ConfigurationMessage;
import io.github.mtrevisan.boxon.codecs.managers.InjectEventListener;
import io.github.mtrevisan.boxon.codecs.managers.ParserHelper;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationHelper;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerFactory;
import io.github.mtrevisan.boxon.codecs.managers.configuration.ConfigurationManagerInterface;
import io.github.mtrevisan.boxon.exceptions.AnnotationException;
import io.github.mtrevisan.boxon.exceptions.CodecException;
import io.github.mtrevisan.boxon.exceptions.ConfigurationException;
import io.github.mtrevisan.boxon.exceptions.EncodeException;
import io.github.mtrevisan.boxon.exceptions.FieldException;
import io.github.mtrevisan.boxon.external.codecs.BitWriter;
import io.github.mtrevisan.boxon.external.codecs.CodecInterface;
import io.github.mtrevisan.boxon.external.logs.EventListener;
import io.github.mtrevisan.boxon.external.semanticversioning.Version;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;


public final class ConfigurationParser{

	@InjectEventListener
	@SuppressWarnings("unused")
	private final EventListener eventListener;

	private final LoaderCodecInterface loaderCodec;
	private final LoaderConfiguration loaderConfiguration;


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
		this.eventListener = eventListener;

		this.loaderCodec = loaderCodec;
		loaderConfiguration = LoaderConfiguration.create(eventListener);
	}


	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 * <p>This method SHOULD BE called from a method inside a class that lies on a parent of all the protocol classes.</p>
	 *
	 * @throws IllegalArgumentException	If the codecs was not loaded yet.
	 */
	public void loadDefaultConfigurations() throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadDefaultConfigurations();
	}

	/**
	 * Loads all the configuration classes annotated with {@link ConfigurationHeader}.
	 *
	 * @param basePackageClasses	Classes to be used ase starting point from which to load configuration classes.
	 */
	public void loadConfigurations(final Class<?>... basePackageClasses) throws AnnotationException, ConfigurationException{
		loaderConfiguration.loadConfigurations(basePackageClasses);
	}

	public List<ConfigurationMessage<?>> getConfigurations(){
		return loaderConfiguration.getConfigurations();
	}

	/**
	 * Retrieve the configuration by class, filled with data, and considering the protocol version.
	 *
	 * @param configuration	The configuration message.
	 * @param data	The data to load into the configuration.
	 * @param protocol	The protocol the data refers to.
	 * @return	The configuration data.
	 */
	public Object getConfigurationWithDefaults(final ConfigurationMessage<?> configuration, final Map<String, Object> data,
			final Version protocol) throws EncodeException, CodecException{
		return loaderConfiguration.getConfigurationWithDefaults(configuration, data, protocol);
	}

	/**
	 * Retrieve the configuration by class.
	 *
	 * @return	The configuration.
	 */
	public ConfigurationMessage<?> getConfiguration(final String configurationType) throws EncodeException{
		return loaderConfiguration.getConfiguration(configurationType);
	}


	public <T> void encode(final ConfigurationMessage<?> configuration, final BitWriter writer, final T currentObject, final Version protocol)
			throws FieldException{
		final ConfigurationHeader header = configuration.getHeader();
		ParserHelper.writeAffix(header.start(), header.charset(), writer);

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
			encodeField(configuration, currentObject, field, writer, annotation);
			if(annotation != field.getBinding())
				encodeField(configuration, currentObject, field, writer, field.getBinding());
		}

		ParserHelper.writeAffix(header.end(), header.charset(), writer);

		writer.flush();
	}

	private <T> void encodeField(final ConfigurationMessage<?> configuration, final T currentObject, final ConfigField field,
			final BitWriter writer, final Annotation binding) throws FieldException{
		final Class<? extends Annotation> annotationType = binding.annotationType();
		final CodecInterface<?> codec = loaderCodec.getCodec(annotationType);
		if(codec == null)
			throw CodecException.create("Cannot find codec for binding {}", annotationType.getSimpleName())
				.withClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());

		eventListener.writingField(configuration.getType().getName(), field.getFieldName(), annotationType.getSimpleName());

		try{
			//encode value from current object
			final Object value = field.getFieldValue(currentObject);
			//write value to raw message
			codec.encode(writer, binding, field.getFieldType(), value);

			eventListener.writtenField(configuration.getType().getName(), field.getFieldName(), value);
		}
		catch(final FieldException fe){
			fe.withClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());
			throw fe;
		}
		catch(final Exception e){
			throw FieldException.create(e)
				.withClassNameAndFieldName(configuration.getType().getName(), field.getFieldName());
		}
	}

	private static void writeSkips(final ConfigurationSkip[] skips, final BitWriter writer, final Version protocol){
		for(int i = 0; i < skips.length; i ++)
			writeSkip(skips[i], writer, protocol);
	}

	private static void writeSkip(final ConfigurationSkip skip, final BitWriter writer, final Version protocol){
		final boolean process = ConfigurationHelper.shouldBeExtracted(protocol, skip.minProtocol(), skip.maxProtocol());
		if(process)
			writer.putText(skip.terminator());
	}

}
