package com.lowes.permits.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.lowes.permits.model.CredentialsDto;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.mongo.MongoLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
public class SchedulerConfig {

	@Bean
	public LockProvider lockProvider(
			CredentialsDto credentialsDto, @Value("${spring.mongodb.database}") String databaseName) {
		// Create a separate blocking MongoClient for Shedlock using the URI from
		// credentials
		MongoClient blockingClient = MongoClients.create(credentialsDto.getMongoDbUri());
		return new MongoLockProvider(blockingClient.getDatabase(databaseName));
	}
}
