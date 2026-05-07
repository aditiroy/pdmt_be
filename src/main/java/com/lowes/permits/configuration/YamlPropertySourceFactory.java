package com.lowes.permits.configuration;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

@Configuration
public class YamlPropertySourceFactory extends DefaultPropertySourceFactory {
	public YamlPropertySourceFactory() {}

	public PropertySource createPropertySource(String name, EncodedResource resource) throws IOException {
		List<PropertySource<?>> propertySourceList =
				(new YamlPropertySourceLoader()).load(resource.getResource().getFilename(), resource.getResource());
		return !propertySourceList.isEmpty()
				? propertySourceList.iterator().next()
				: super.createPropertySource(name, resource);
	}
}
