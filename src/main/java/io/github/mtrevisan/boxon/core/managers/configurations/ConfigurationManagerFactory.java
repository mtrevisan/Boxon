/*
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.boxon.core.managers.configurations;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;


/**
 * Factory for the {@link ConfigurationManagerInterface configuration manager}.
 */
public final class ConfigurationManagerFactory{

	private static final Map<Class<? extends Annotation>, Function<Annotation, ConfigurationManagerInterface>> MANAGERS = Map.of(
		ConfigurationField.class, annotation -> new PlainManager((ConfigurationField)annotation),
		CompositeConfigurationField.class, annotation -> new CompositeManager((CompositeConfigurationField)annotation),
		AlternativeConfigurationField.class, annotation -> new AlternativeManager((AlternativeConfigurationField)annotation)
	);


	private ConfigurationManagerFactory(){}


	/**
	 * Construct a configuration manager for the given annotation.
	 *
	 * @param annotation	The annotation to generate the build manager from.
	 * @return	An instance of the configuration manager.
	 */
	public static ConfigurationManagerInterface buildManager(final Annotation annotation){
		return MANAGERS.get(annotation.annotationType())
			.apply(annotation);
	}

}
