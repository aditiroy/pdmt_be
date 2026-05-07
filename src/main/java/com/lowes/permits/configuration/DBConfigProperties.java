/**
 * ************************************************************** Copyright (C) Lowe's Companies, Inc. All rights
 * reserved. This file is for internal use only at Lowe's Companies, Inc.
 * **************************************************************
 */
package com.lowes.permits.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(value = "postgresdb")
@PropertySource(value = "file:${file.location.secrets}", factory = YamlPropertySourceFactory.class)
public class DBConfigProperties {

	private String host;
	private int port;
	private String password;
	private String username;
	private String database;
	private String schema;
	private String rootCertificate;
}
