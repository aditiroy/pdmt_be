package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;

import com.lowes.permits.dto.request.SearchOrderModRequest;
import com.lowes.permits.dto.response.OrderModResponse;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.exception.PermitSearchException;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OrderModFilter;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.PartialSearch;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderModSearchServiceTest {

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitPostgresRepository mockPermitPostgresRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService permitService;
	private OrderModMongoEntity testOrderModMongoEntity;
	private OrderModResponse testOrderModResponse;

	@BeforeEach
	void setUp() {
		permitService = new PermitService(
				mockPermitMongoRepository, mockPermitMapper, mockPermitPostgresRepository, mockCommonUtilityClient);
		testOrderModMongoEntity = createTestOrderMod();
		testOrderModResponse = createTestOrderModResponse();
	}

	@Test
	void testSearchOrderModsWithEmptyFilter() {
		// Test search with empty filter
		OrderModFilter filter = new OrderModFilter();
		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	@Test
	void testSearchOrderModsWithCitiesFilter() {
		// Test search with cities filter
		OrderModFilter filter = new OrderModFilter();
		filter.setCities(List.of("Los Angeles", "New York"));
		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	@Test
	void testSearchOrderModsWithLaborCategoriesFilter() {
		// Test search with labor categories filter
		LaborCategory laborCategory = new LaborCategory(123, "Plumbing");
		OrderModFilter filter = new OrderModFilter();
		filter.setLaborCategories(List.of(laborCategory));
		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	@Test
	void testSearchOrderModsWithPartialSearch() {
		// Test search with partial search
		PartialSearch partialSearch = new PartialSearch("test", List.of("city", "state"));
		OrderModFilter filter = new OrderModFilter();
		filter.setPartialSearch(List.of(partialSearch));
		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	@Test
	void testSearchOrderModsWithInvalidPartialSearchColumn() {
		// Test search with invalid column in partial search
		PartialSearch partialSearch = new PartialSearch("test", List.of("invalid_column"));
		OrderModFilter filter = new OrderModFilter();
		filter.setPartialSearch(List.of(partialSearch));
		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		StepVerifier.create(permitService.searchOrderMods(request))
				.expectErrorMatches(ex -> ex instanceof IllegalArgumentException
						&& ex.getMessage().contains("Invalid column name: invalid_column"))
				.verify();
	}

	@Test
	void testSearchOrderModsWithCustomSort() {
		// Test search with custom sorting
		com.lowes.permits.model.Sort sort = new com.lowes.permits.model.Sort();
		sort.setKey("city");
		sort.setVal("ASC");
		SearchOrderModRequest request = new SearchOrderModRequest(null, List.of(sort), null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
					assertNotNull(response.getSort());
					assertEquals(sort, response.getSort().get(0));
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(
						any(Query.class),
						anyInt(),
						anyInt(),
						argThat(s -> s.getOrderFor("address.city") != null
								&& Sort.Direction.ASC.equals(Objects.requireNonNull(s.getOrderFor("address.city"))
										.getDirection())));
	}

	@Test
	void testSearchOrderModsWithInvalidSortColumn() {
		// Test search with invalid sort column
		com.lowes.permits.model.Sort sort = new com.lowes.permits.model.Sort();
		sort.setKey("invalid_column");
		sort.setVal("ASC");
		SearchOrderModRequest request = new SearchOrderModRequest(null, List.of(sort), null);

		StepVerifier.create(permitService.searchOrderMods(request))
				.expectErrorMatches(ex -> ex instanceof IllegalArgumentException
						&& ex.getMessage().contains("Invalid sort column name: invalid_column"))
				.verify();
	}

	@Test
	void testSearchOrderModsWithInvalidSortDirection() {
		// Test search with invalid sort direction
		com.lowes.permits.model.Sort sort = new com.lowes.permits.model.Sort();
		sort.setKey("city");
		sort.setVal("INVALID");
		SearchOrderModRequest request = new SearchOrderModRequest(null, List.of(sort), null);

		StepVerifier.create(permitService.searchOrderMods(request))
				.expectErrorMatches(ex -> ex instanceof IllegalArgumentException
						&& ex.getMessage().contains("Invalid sort direction: INVALID"))
				.verify();
	}

	@Test
	void testSearchOrderModsWithCustomPagination() {
		// Test search with custom pagination
		Pagination pagination = new Pagination(2, 10, null, null);
		SearchOrderModRequest request = new SearchOrderModRequest(null, null, pagination);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(25L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
					assertNotNull(response.getPagination());
					assertEquals(2, response.getPagination().getPage());
					assertEquals(10, response.getPagination().getPageSize());
					assertEquals(25L, response.getPagination().getTotalCount());
					assertTrue(response.getPagination().getHasNextPage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	@Test
	void testSearchOrderModsWithEmptyResult() {
		// Test search with no results
		SearchOrderModRequest request = new SearchOrderModRequest(null, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.empty());
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(0L));

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertTrue(response.getData().isEmpty());
					assertEquals(0L, response.getPagination().getTotalCount());
					assertFalse(response.getPagination().getHasNextPage());
				})
				.verifyComplete();

		verify(mockPermitMapper, never()).toOrderModResponse(any(OrderModMongoEntity.class));
	}

	@Test
	void testSearchOrderModsWithRepositoryError() {
		// Test search when repository throws an error
		SearchOrderModRequest request = new SearchOrderModRequest(null, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.error(new RuntimeException("Database error")));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(0L));

		StepVerifier.create(permitService.searchOrderMods(request))
				.expectErrorMatches(ex ->
						ex instanceof PermitSearchException && ex.getMessage().contains("Failed to search order mods"))
				.verify();
	}

	@Test
	void testSearchOrderModsWithEmptyFilterLists() {
		// Test search with empty filter lists (should be ignored)
		OrderModFilter filter = new OrderModFilter();
		filter.setCities(List.of()); // Empty list
		filter.setStates(List.of("CA")); // Valid list
		filter.setZipCodes(null); // Null list

		SearchOrderModRequest request = new SearchOrderModRequest(filter, null, null);

		when(mockPermitMongoRepository.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class)))
				.thenReturn(Flux.just(testOrderModMongoEntity));
		when(mockPermitMongoRepository.countOrderMods(any(Query.class))).thenReturn(Mono.just(1L));
		when(mockPermitMapper.toOrderModResponse(any(OrderModMongoEntity.class)))
				.thenReturn(testOrderModResponse);

		StepVerifier.create(permitService.searchOrderMods(request))
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals(1, response.getData().size());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1))
				.searchOrderMods(any(Query.class), anyInt(), anyInt(), any(Sort.class));
	}

	// Helper methods to create test data
	private OrderModMongoEntity createTestOrderMod() {
		Address address = new Address();
		address.setAddressLine1("123 Main St");
		address.setCity("Los Angeles");
		address.setState("CA");
		address.setZipCode("90210");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		address.setMatchedAddress("123 Main St, Beverly Hills, CA 90210");

		OrderModMongoEntity entity = new OrderModMongoEntity();
		entity.setId("69daaf06a082187f95ec8fed");
		entity.setCategoryCode(123);
		entity.setCategoryDesc("Plumbing");
		entity.setItemId("456");
		entity.setItemDesc("Pipe Installation");
		entity.setPermitFee("150.00");
		entity.setOmniId("OMNI123");
		entity.setAddress(address);
		entity.setProvider("ABC Plumbing");
		entity.setVbuNumber(789);
		entity.setPermitInsertType("NEW");
		entity.setOldPermitFee("120.00");
		entity.setJobId("JOB123");
		entity.setOrderNumber("ORDER456");
		return entity;
	}

	private OrderModResponse createTestOrderModResponse() {
		OrderModResponse response = new OrderModResponse();
		Address addr = testOrderModMongoEntity.getAddress();
		DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		response.setId(testOrderModMongoEntity.getId());
		response.setLaborCategoryCode(testOrderModMongoEntity.getCategoryCode());
		response.setLaborCategoryDescription(testOrderModMongoEntity.getCategoryDesc());
		response.setLaborItem(Integer.parseInt(testOrderModMongoEntity.getItemId()));
		response.setLaborItemDescription(testOrderModMongoEntity.getItemDesc());
		response.setUnitPermitFee(new BigDecimal(testOrderModMongoEntity.getPermitFee()));
		response.setOmniItemId(testOrderModMongoEntity.getOmniId());
		response.setAddress(addr);
		response.setProvider(testOrderModMongoEntity.getProvider());
		response.setVbuNumber(testOrderModMongoEntity.getVbuNumber());
		response.setPermitInsertType(testOrderModMongoEntity.getPermitInsertType());
		response.setOldPermitFee(new BigDecimal(testOrderModMongoEntity.getOldPermitFee()));
		response.setJobId(testOrderModMongoEntity.getJobId());
		response.setOrderNumber(testOrderModMongoEntity.getOrderNumber());
		return response;
	}
}
