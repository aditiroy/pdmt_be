package com.lowes.permits.configuration;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowes.permits.model.CredentialsDto;

@Configuration
public class CredentialsConfig {
	@Value("${file.location.secrets}")
	private String filePath;

	@Bean
	public CredentialsDto credentials() throws Exception {
		File secretFile = new File(filePath);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(secretFile, CredentialsDto.class);
	}
}
