package com.lowes.permits.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;

@Configuration
@Slf4j
public class WebClientConfig {

	@Value("${http.connection.timeout}")
	private int connectionTimeout;

	@Bean("webClient")
	@Primary
	public WebClient apiClient() {

		final int size = 16 * 1024 * 1024;
		var strategies = ExchangeStrategies.builder()
				.codecs(codec -> codec.defaultCodecs().maxInMemorySize(size))
				.build();
		var httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(connectionTimeout))
						.addHandlerLast(new WriteTimeoutHandler(connectionTimeout)));
		var connector = new ReactorClientHttpConnector(httpClient);

		return WebClient.builder()
				.clientConnector(connector)
				.exchangeStrategies(strategies)
				.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
	}
}
