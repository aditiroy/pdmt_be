package com.lowes.permits.http;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClient;

import com.lowes.permits.dto.request.ItemRequest;
import com.lowes.permits.dto.response.ItemResponse;
import com.lowes.permits.dto.response.ItemResponseWrapper;
import com.lowes.permits.dto.response.LaborCategoryResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CommonUtilityClient {
	private final WebClient webClient;

	@Value("${common.utility.api.host.url}")
	private String COMMON_UTILITY_API_HOST_URL;

	@Value("${common.utility.bifrost.items.url}")
	private String ITEMS_URL;

	public CommonUtilityClient(WebClient.Builder webClientBuilder) {
		this.webClient = webClientBuilder.build();
	}

	public Mono<List<LaborCategoryResponse>> getLaborCategoryResponseList() {
		String url = COMMON_UTILITY_API_HOST_URL + "/labor-categories";

		return webClient
				.get()
				.uri(url)
				.exchangeToMono(clientResponse -> {
					if (clientResponse.statusCode().is4xxClientError()) {
						return clientResponse
								.createException()
								.flatMap(ex -> Mono.error(new HttpClientErrorException(
										HttpStatus.valueOf(
												clientResponse.statusCode().value()),
										ex.getResponseBodyAsString())));
					} else if (clientResponse.statusCode().is5xxServerError()) {
						return clientResponse
								.createException()
								.flatMap(ex -> Mono.error(new HttpServerErrorException(
										HttpStatus.valueOf(
												clientResponse.statusCode().value()),
										ex.getResponseBodyAsString())));
					}
					return clientResponse.bodyToMono(LaborCategoryResponse[].class);
				})
				.map(List::of)
				.timeout(Duration.ofMinutes(1))
				.doOnError(error -> log.error("Error fetching labor categories from URL: {}", url, error));
	}

	public Mono<ItemResponseWrapper> getLaborItemResponse(String laborItem) {

		ItemRequest itemRequest = getItemRequest(laborItem);

		return webClient
				.post()
				.uri(ITEMS_URL)
				.bodyValue(itemRequest)
				.exchangeToMono(clientResponse -> {
					int statusCode = clientResponse.statusCode().value();
					log.info("Calling Bifrost Utility Items API at URL: {} with request: {}", ITEMS_URL, itemRequest);
					log.info("Bifrost Job management Utility Items API response status code: {}", statusCode);
					if (statusCode == 200) {
						return clientResponse
								.bodyToMono(ItemResponse.class)
								.map(itemResponse -> ItemResponseWrapper.builder()
										.statusCode(statusCode)
										.itemResponse(itemResponse)
										.build());
					} else {
						log.error("Bifrost Job management Utility Items API non-200 response - Status: {}", statusCode);
						return clientResponse
								.bodyToMono(String.class)
								.defaultIfEmpty("")
								.doOnNext(body -> log.error("API non-200 Response body: {}", body))
								.map(body -> ItemResponseWrapper.builder()
										.statusCode(statusCode)
										.itemResponse(null)
										.build());
					}
				})
				.doOnError(error -> log.error(
						"Error fetching labor item details for: {} from URL: {}. Error: {}",
						laborItem,
						ITEMS_URL,
						error.getMessage(),
						error));
	}

	private ItemRequest getItemRequest(String laborItem) {
		ItemRequest.OmniItemId omniItemId =
				ItemRequest.OmniItemId.builder().itemNumber(laborItem).qty(1).build();

		return ItemRequest.builder()
				.omniItemIds(List.of(omniItemId))
				.responseGroup("small")
				.clients(List.of("PRODUCT"))
				.site("online")
				.clientType("PERMIT-MANAGEMENT")
				.build();
	}
}
