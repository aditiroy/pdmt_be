package com.lowes.permits.configuration;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.lowes.permits.model.CredentialsDto;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

@Component
@Slf4j
public class DocumentConfig {
	private final CredentialsDto credentialsDto;

	@Value("${document.access.name}")
	private String awsAccessKey;

	@Value("${document.url}")
	private String documentUrl;

	public DocumentConfig(CredentialsDto credentialsDto) {
		this.credentialsDto = credentialsDto;
	}

	@Bean
	public AmazonS3 amazonS3Client() {
		BasicAWSCredentials awsCredentials =
				new BasicAWSCredentials(awsAccessKey, credentialsDto.getDocumentAccessSecret());
		AmazonS3ClientBuilder builder =
				AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
		builder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(documentUrl, ""));
		builder.setPathStyleAccessEnabled(true);
		return builder.build();
	}

	@Bean
	public S3AsyncClient amazonS3ClientAsync() {
		// Create the Netty Async HTTP Client
		SdkAsyncHttpClient nettyHttpClient = NettyNioAsyncHttpClient.builder()
				.connectionMaxIdleTime(Duration.ofMinutes(3))
				.readTimeout(Duration.ofMinutes(6))
				.connectionTimeToLive(Duration.ofMinutes(5))
				.connectionAcquisitionTimeout(Duration.ofMinutes(6))
				.connectionTimeout(Duration.ofMinutes(4))
				.maxConcurrency(17000)
				.maxPendingConnectionAcquires(17000)
				.build();
		S3AsyncClientBuilder builder = S3AsyncClient.builder()
				.httpClient(nettyHttpClient)
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(awsAccessKey, credentialsDto.getDocumentAccessSecret())))
				.region(Region.US_EAST_1)
				.forcePathStyle(true);
		try {
			builder.endpointOverride(new URI(documentUrl));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return builder.build();
	}
}
