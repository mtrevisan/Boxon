package io.github.mtrevisan.boxon.codecs;

import io.github.mtrevisan.boxon.annotations.configurations.AlternativeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.CompositeConfigurationField;
import io.github.mtrevisan.boxon.annotations.configurations.ConfigurationField;

import java.lang.annotation.Annotation;


final class ConfigurationManagerFactory{

	private ConfigurationManagerFactory(){}

	static ConfigurationManagerInterface buildManager(final Annotation annotation){
		ConfigurationManagerInterface manager = null;
		if(ConfigurationField.class.isInstance(annotation))
			manager = new PlainManager((ConfigurationField)annotation);
		else if(CompositeConfigurationField.class.isInstance(annotation))
			manager = new CompositeManager((CompositeConfigurationField)annotation);
		else if(AlternativeConfigurationField.class.isInstance(annotation))
			manager = new AlternativeManager((AlternativeConfigurationField)annotation);
		return manager;
	}

}
