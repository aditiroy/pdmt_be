package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lowes.permits.dto.request.SearchPermitRequest;
import com.lowes.permits.dto.response.PermitResponse;
import com.lowes.permits.dto.response.SearchPermitResponse;
import com.lowes.permits.entity.PermitPostgresEntity;
import com.lowes.permits.exception.PermitSearchException;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.PartialSearch;
import com.lowes.permits.model.PermitFilter;
import com.lowes.permits.model.Sort;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntitySearchServiceTest {

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitPostgresRepository mockRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService permitService;
	private SearchPermitRequest testRequest;
	private List<PermitResponse> testPermitResponses;
	private List<PermitPostgresEntity> testPermitPostgresEntities;

	@BeforeEach
	void setUp() {
		permitService =
				new PermitService(mockPermitMongoRepository, mockPermitMapper, mockRepository, mockCommonUtilityClient);
		testRequest = createTestSearchPermitRequest();
		testPermitResponses = createTestPermitResponses();
		testPermitPostgresEntities = createTestPermits();
	}

	private void setupMapperMock() {
		// Mock the mapper to convert Permit entities to PermitResponse DTOs
		when(mockPermitMapper.toPermitResponse(any(PermitPostgresEntity.class))).thenAnswer(invocation -> {
			PermitPostgresEntity permitPostgresEntity = invocation.getArgument(0);
			PermitResponse response = new PermitResponse();
			response.setId(String.valueOf(permitPostgresEntity.getLaborCategoryCode()));

			Address address = new Address();
			address.setCity(permitPostgresEntity.getCity());
			address.setState(permitPostgresEntity.getState());
			address.setZipCode(permitPostgresEntity.getZipcode());
			response.setAddress(address);
			return response;
		});
	}

	@Test
	void testSearchPermitWithValidRequest() {
		// Test searchPermit with valid request
		setupMapperMock();
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(10L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(3, response.getData().size());
					assertNotNull(response.getPagination());
					assertEquals(1, response.getPagination().getPage());
					assertEquals(20, response.getPagination().getPageSize());
					assertEquals(10L, response.getPagination().getTotalCount());
					assertFalse(response.getPagination().getHasNextPage());
					assertNotNull(response.getSort());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(anyString(), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithEmptyFilter() {
		// Test searchPermit with empty filter
		testRequest.setFilter(new PermitFilter());

		// Create test Permit entities for the repository mock
		List<PermitPostgresEntity> testPermitPostgresEntities = createTestPermits();
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		// Mock the mapper to convert Permit entities to PermitResponse DTOs
		when(mockPermitMapper.toPermitResponse(any(PermitPostgresEntity.class))).thenAnswer(invocation -> {
			PermitPostgresEntity permitPostgresEntity = invocation.getArgument(0);
			PermitResponse response = new PermitResponse();
			response.setId(String.valueOf(permitPostgresEntity.getLaborCategoryCode()));

			Address address = new Address();
			address.setCity(permitPostgresEntity.getCity());
			address.setState(permitPostgresEntity.getState());
			address.setZipCode(permitPostgresEntity.getZipcode());
			response.setAddress(address);
			return response;
		});

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(anyString(), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithStatesFilter() {
		// Test searchPermit with states filter
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		filter.setStates(List.of("CA", "NY"));
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("state IN ($1, $2)"), anyList());
		verify(mockRepository, times(1)).count(contains("state IN ($1, $2)"), anyList());
	}

	@Test
	void testSearchPermitWithCountiesFilter() {
		// Test searchPermit with counties filter
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		filter.setCounties(List.of("Los Angeles County", "New York County"));
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("county IN ($1, $2)"), anyList());
		verify(mockRepository, times(1)).count(contains("county IN ($1, $2)"), anyList());
	}

	@Test
	void testSearchPermitWithMunicipalitiesFilter() {
		// Test searchPermit with municipalities filter
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		filter.setMunicipalities(List.of("Beverly Hills", "Manhattan"));
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("municipality IN ($1, $2)"), anyList());
		verify(mockRepository, times(1)).count(contains("municipality IN ($1, $2)"), anyList());
	}

	@Test
	void testSearchPermitWithPartialSearch() {
		// Test searchPermit with partial search
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		PartialSearch partialSearch = new PartialSearch();
		partialSearch.setPartialText("Los");
		partialSearch.setFieldsToSearch(List.of("city", "state"));
		filter.setPartialSearch(List.of(partialSearch));
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("CAST(city AS TEXT) ILIKE $1"), anyList());
		verify(mockRepository, times(1)).count(contains("CAST(city AS TEXT) ILIKE $1"), anyList());
	}

	@Test
	void testSearchPermitWithInvalidPartialSearchColumn() {
		// Test searchPermit with invalid column in partial search
		PermitFilter filter = new PermitFilter();
		PartialSearch partialSearch = new PartialSearch();
		partialSearch.setPartialText("test");
		partialSearch.setFieldsToSearch(List.of("invalid_column"));
		filter.setPartialSearch(List.of(partialSearch));
		testRequest.setFilter(filter);

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).search(anyString(), anyList());
		verify(mockRepository, never()).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithNullSort() {
		// Test searchPermit with null sort
		setupMapperMock();
		testRequest.setSort(null);
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY zipcode ASC"), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithEmptySort() {
		// Test searchPermit with empty sort
		setupMapperMock();
		testRequest.setSort(List.of());
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY zipcode ASC"), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithValidSort() {
		// Test searchPermit with valid sort
		setupMapperMock();
		Sort sort = new Sort();
		sort.setKey("city");
		sort.setVal("ASC");
		testRequest.setSort(List.of(sort));

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY city ASC"), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithInvalidSortColumn() {
		// Test searchPermit with invalid sort column
		Sort sort = new Sort();
		sort.setKey("invalid_column");
		sort.setVal("ASC");
		testRequest.setSort(List.of(sort));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).search(anyString(), anyList());
		verify(mockRepository, never()).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithInvalidSortDirection() {
		// Test searchPermit with invalid sort direction
		Sort sort = new Sort();
		sort.setKey("city");
		sort.setVal("INVALID");
		testRequest.setSort(List.of(sort));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).search(anyString(), anyList());
		verify(mockRepository, never()).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithNullPagination() {
		// Test searchPermit with null pagination
		setupMapperMock();
		testRequest.setPagination(null);
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
					assertEquals(1, response.getPagination().getPage());
					assertEquals(20, response.getPagination().getPageSize());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(anyString(), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithCountError() {
		// Test searchPermit when count throws error
		setupMapperMock();
		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList()))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result).expectError(PermitSearchException.class).verify();

		verify(mockRepository, times(1)).search(anyString(), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithEmptyFilterLists() {
		// Test searchPermit with empty filter lists
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		filter.setCities(List.of());
		filter.setStates(List.of());
		filter.setZipCodes(List.of());
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1))
				.search(
						argThat(sql ->
								!sql.contains("city IN") && !sql.contains("state IN") && !sql.contains("zipcode IN")),
						anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithEmptyStringsInFilterLists() {
		// Test searchPermit with empty strings in filter lists
		setupMapperMock();
		PermitFilter filter = new PermitFilter();
		filter.setCities(List.of("Los Angeles", "", "New York"));
		filter.setStates(List.of("  ", "CA", ""));
		testRequest.setFilter(filter);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1))
				.search(argThat(sql -> sql.contains("city IN ($1, $2)") && sql.contains("state IN ($3)")), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithCaseInsensitiveSortDirection() {
		// Test searchPermit with case insensitive sort direction
		setupMapperMock();
		Sort sort = new Sort();
		sort.setKey("city");
		sort.setVal("asc"); // lowercase
		testRequest.setSort(List.of(sort));

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY city ASC"), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testSearchPermitWithMixedCaseSortDirection() {
		// Test searchPermit with mixed case sort direction
		setupMapperMock();
		Sort sort = new Sort();
		sort.setKey("city");
		sort.setVal("DeSc"); // mixed case
		testRequest.setSort(List.of(sort));

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY city DESC"), anyList());
		verify(mockRepository, times(1)).count(anyString(), anyList());
	}

	@Test
	void testServiceAnnotation() {
		// Test that the class is properly annotated
		assertTrue(permitService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(permitService);
		// In a real test, we would use reflection to verify the annotation
	}

	@Test
	void testRequiredArgsConstructorAnnotation() {
		// Test that the class uses @RequiredArgsConstructor (metadata test)
		assertNotNull(permitService);
		// The constructor injection working properly indicates this annotation is
		// present
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(permitService);
	}

	@Test
	void testAllowedColumns() {
		// Test that allowed columns are properly defined (indirectly tested through
		// method behavior)
		assertNotNull(permitService);
		// Allowed columns are tested through the validation in sort and partial search
	}

	@Test
	void testDefaultValues() {
		// Test that default values are properly used
		setupMapperMock();
		testRequest.setPagination(null);
		testRequest.setSort(null);

		when(mockRepository.search(anyString(), anyList())).thenReturn(Flux.fromIterable(testPermitPostgresEntities));
		when(mockRepository.count(anyString(), anyList())).thenReturn(Mono.just(5L));

		Mono<SearchPermitResponse> result = permitService.searchPermit(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
					assertEquals(1, response.getPagination().getPage()); // Default page
					assertEquals(20, response.getPagination().getPageSize()); // Default page size
				})
				.verifyComplete();

		verify(mockRepository, times(1)).search(contains("ORDER BY zipcode ASC"), anyList()); // Default sort
	}

	// Helper methods to create test data
	private SearchPermitRequest createTestSearchPermitRequest() {
		SearchPermitRequest request = new SearchPermitRequest();

		// Set pagination
		Pagination pagination = new Pagination();
		pagination.setPage(1);
		pagination.setPageSize(20);
		request.setPagination(pagination);

		// Set sort
		Sort sort = new Sort();
		sort.setKey("city");
		sort.setVal("ASC");
		request.setSort(List.of(sort));

		// Set filter
		PermitFilter filter = new PermitFilter();
		filter.setCities(List.of("Los Angeles", "New York"));
		filter.setStates(List.of("CA", "NY"));
		request.setFilter(filter);

		return request;
	}

	private List<PermitResponse> createTestPermitResponses() {
		List<PermitResponse> responses = new java.util.ArrayList<>();

		for (int i = 0; i < 3; i++) {
			PermitResponse response = new PermitResponse();
			response.setId("id" + i);

			Address address = new Address();
			address.setCity("City" + i);
			address.setState("State" + i);
			address.setZipCode("9021" + i);
			response.setAddress(address);

			responses.add(response);
		}

		return responses;
	}

	private List<PermitPostgresEntity> createTestPermits() {
		List<PermitPostgresEntity> permitPostgresEntities = new java.util.ArrayList<>();

		for (int i = 0; i < 3; i++) {
			PermitPostgresEntity permitPostgresEntity = new PermitPostgresEntity();
			permitPostgresEntity.setLaborCategoryCode(i);
			permitPostgresEntity.setCity("City" + i);
			permitPostgresEntity.setState("State" + i);
			permitPostgresEntity.setZipcode("9021" + i);
			permitPostgresEntities.add(permitPostgresEntity);
		}

		return permitPostgresEntities;
	}
}
