package io.github.mtrevisan.boxon.annotations.configurations;

import java.lang.annotation.Annotation;


public final class ConfigurationManagerFactory{

	private ConfigurationManagerFactory(){}

	public static ConfigurationManagerInterface buildManager(final Annotation annotation){
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
