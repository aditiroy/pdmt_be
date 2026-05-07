/**
 * ************************************************************** Copyright (C) Lowe's Companies, Inc. All rights
 * reserved. This file is for internal use only at Lowe's Companies, Inc.
 * **************************************************************
 */
package com.lowes.permits.configuration;

import static com.lowes.permits.constants.ApplicationConstants.ROOT;
import static com.lowes.permits.constants.ApplicationConstants.ROOT_EXTENSION;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.client.SSLMode;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@Slf4j
public class PostgresDbConnection extends AbstractR2dbcConfiguration {

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final DBConfigProperties dbConfigProperties;
	private URL rootCert;

	@Value("${datasource.host}")
	private String dataSourceHost;

	@Value("${maxSize}")
	private int maxSize;

	@Value("${initialSize}")
	private int initialSize;

	@Value("${maxIdleTime}")
	private int maxIdleTime;

	@Value("${maxCreateConnectionTime}")
	private int maxCreateConnectionTime;

	@Value("${maxAcquireTime}")
	private int maxAcquireTime;

	@Value("${maxLifeTime}")
	private int maxLifeTime;

	public PostgresDbConnection(DBConfigProperties dbConfigProperties) {
		this.dbConfigProperties = dbConfigProperties;
	}

	@PostConstruct
	public void setUp() throws IOException {
		rootCert = writToFile(dbConfigProperties.getRootCertificate(), ROOT, ROOT_EXTENSION);
	}

	@Bean
	@Override
	public ConnectionFactory connectionFactory() {

		ConnectionFactory connectionFactory;
		try {
			connectionFactory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
					.host(dataSourceHost)
					.port(dbConfigProperties.getPort())
					.username(dbConfigProperties.getUsername())
					.password(dbConfigProperties.getPassword())
					.database(dbConfigProperties.getDatabase())
					.schema(dbConfigProperties.getSchema())
					.enableSsl()
					.sslMode(SSLMode.VERIFY_CA)
					.sslRootCert(rootCert)
					.build());
		} catch (Exception e) {
			log.error("Exception occurred while forming Postgres Connection Factory bean. Exception is ", e);
			throw new RuntimeException(e);
		}

		/*
		 * log.info("=== PostgreSQL ConnectionPool Configuration ===");
		 * log.info("Pool Name: wallstreet-svc-pool"); log.info("Max Size: {}",
		 * maxSize); log.info("Initial Size: {}", initialSize);
		 * log.info("Max Idle Time: {} ms ({})", maxIdleTime,
		 * Duration.ofMillis(maxIdleTime));
		 * log.info("Max Create Connection Time: {} ms ({})", maxCreateConnectionTime,
		 * Duration.ofMillis(maxCreateConnectionTime));
		 * log.info("Max Acquire Time: {} ms ({})", maxAcquireTime,
		 * Duration.ofMillis(maxAcquireTime)); log.info("Max Life Time: {} ms ({})",
		 * maxLifeTime, Duration.ofMillis(maxLifeTime)); log.info("Acquire Retry: 3");
		 * log.info("PostgreSQL ConnectionPool created successfully");
		 * log.info("===============================================");
		 */

		// Create a ConnectionPool for connectionFactory
		ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
				.maxIdleTime(Duration.ofMillis(maxIdleTime))
				.initialSize(initialSize)
				.maxSize(maxSize)
				.maxCreateConnectionTime(Duration.ofMillis(maxCreateConnectionTime))
				.maxAcquireTime(Duration.ofMillis(maxAcquireTime))
				.maxLifeTime(Duration.ofMillis(maxLifeTime))
				.acquireRetry(3)
				.name("wallstreet-svc-pool")
				.build();
		return new ConnectionPool(configuration);
	}

	private URL writToFile(String sslKey, String fileName, String fileExtension) throws IOException {
		File tempFile = File.createTempFile(fileName, fileExtension);
		try (FileWriter file = new FileWriter(tempFile)) {
			file.write(sslKey);
		}
		return tempFile.toURI().toURL();
	}
}
