package com.lowes.permits.model;

import java.util.Map;

import lombok.Data;

@Data
public class CredentialsDto {
	private Map<String, String> postgresdb;
	private String mongoDbUri;
	private String mongoDbTruststorePassword;
	private String documentAccessSecret;
}
