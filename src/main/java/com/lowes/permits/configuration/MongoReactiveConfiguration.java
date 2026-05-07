package com.lowes.permits.configuration;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.lowes.permits.model.CredentialsDto;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MongoReactiveConfiguration extends AbstractReactiveMongoConfiguration {

	public static final String JKS = "JKS";
	public static final String TLS = "TLS";
	private final CredentialsDto credentialsDto;

	@Value("${spring.data.mongodb.trust.store.path}")
	private String trustStorePath;

	@Value("${spring.mongodb.database}")
	private String dbName;

	@Value("${spring.data.mongodb.connection.pool.initialSize}")
	private int initialSize;

	@Value("${spring.data.mongodb.connection.pool.maxSize}")
	private int maxSize;

	@Value("${spring.data.mongodb.connection.pool.maxIdleTime}")
	private int maxIdleTime;

	@Value("${spring.data.mongodb.connection.pool.timeout}")
	private int connectTimeOut;

	@Value("${spring.data.mongodb.connection.pool.life.time}")
	private int connectionLifeTime;

	@Value("${spring.data.mongodb.connection.pool.idle.time}")
	private int connectionIdleTime;

	public MongoReactiveConfiguration(CredentialsDto credentialsDto) {
		this.credentialsDto = credentialsDto;
	}

	@Override
	protected String getDatabaseName() {
		return dbName;
	}

	@Bean
	public ReactiveMongoTemplate reactiveMongoTemplate() {
		return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
	}

	@Override
	public MongoClient reactiveMongoClient() {
		log.info("establishing mongoDB secure connection");
		try {
			KeyStore trustStore = KeyStore.getInstance(JKS);
			try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
				trustStore.load(
						trustStoreStream,
						credentialsDto.getMongoDbTruststorePassword().toCharArray());
				TrustManagerFactory trustManagerFactory =
						TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(trustStore);
				SSLContext sslContext = SSLContext.getInstance(TLS);
				sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
				ConnectionString connectionString = new ConnectionString(credentialsDto.getMongoDbUri());
				MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
						.applyConnectionString(connectionString)
						.applyToSocketSettings(builder -> builder.connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS))
						// Sets the maximum time to connect to an available socket before throwing a
						// timeout exception.
						.applyToConnectionPoolSettings(poolSettings -> poolSettings
								.minSize(initialSize)
								// Sets the minimum number of connections associated with a connection pool.
								.maxSize(maxSize)
								// Sets the maximum number of connections associated with a connection pool
								.maxWaitTime(maxIdleTime, TimeUnit.SECONDS)
								. // Sets the maximum time to wait for an
								// available connection.
								maxConnectionLifeTime(connectionLifeTime, TimeUnit.MINUTES)
								.maxConnectionIdleTime(connectionIdleTime, TimeUnit.MINUTES))
						.applyToSslSettings(
								builder -> builder.context(sslContext).enabled(true))
						.build();
				log.info("mongoDB secure connection established");
				return MongoClients.create(mongoClientSettings);
			}
		} catch (Exception e) {
			log.error("Failed to configure SSL  for MongoClient: ", e);
			throw new RuntimeException("Failed to configure SSL  for MongoClient", e);
		}
	}
}
