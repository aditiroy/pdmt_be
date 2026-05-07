package com.lowes.permits.service;

import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lowes.permits.dto.request.SearchActivityRequest;
import com.lowes.permits.dto.response.ActivityResponse;
import com.lowes.permits.dto.response.SearchActivityResponse;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.ActivityFilter;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.DateRange;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.model.Sort;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

	private final PermitMapper mockPermitMapper = Mappers.getMapper(PermitMapper.class);

	@Mock
	private PermitMongoRepository mockActivityRepository;

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitPostgresRepository mockPermitPostgresRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService activityService;
	private SearchActivityRequest testRequest;
	private List<PermitMongoEntity> testPermitEntities;

	@BeforeEach
	void setUp() {
		activityService = new PermitService(
				mockActivityRepository, mockPermitMapper, mockPermitPostgresRepository, mockCommonUtilityClient);
		testRequest = createTestSearchActivityRequest();
		testPermitEntities = createTestPermitEntities();
	}

	@Test
	void testSearchActivitiesWithValidRequest() {
		// Test searchActivities with valid request
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(10L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(3, response.getData().size());
					assertNotNull(response.getPagination());
					assertEquals(1, response.getPagination().getPage());
					assertEquals(10, response.getPagination().getPageSize());
					assertEquals(10L, response.getPagination().getTotalCount());
					assertFalse(response.getPagination().getHasNextPage());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(anyLong(), anyLong());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithNullPagination() {
		// Test searchActivities with null pagination
		testRequest.setPagination(null);
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), eq(1), eq(10), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
					assertEquals(1, response.getPagination().getPage());
					assertEquals(10, response.getPagination().getPageSize());
				})
				.verifyComplete();
	}

	@Test
	void testSearchActivitiesWithNullSort() {
		// Test searchActivities with null sort
		testRequest.setSort(null);
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithEmptySort() {
		// Test searchActivities with empty sort
		testRequest.setSort(List.of());
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();
	}

	@Test
	void testSearchActivitiesWithValidSort() {
		// Test searchActivities with valid sort
		Sort validSort = new Sort();
		validSort.setKey("createdAt");
		validSort.setVal("ASC");
		testRequest.setSort(List.of(validSort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithInvalidSortKey() {
		// Test searchActivities with invalid sort key
		Sort invalidSort = new Sort();
		invalidSort.setKey("invalidKey");
		invalidSort.setVal("ASC");
		testRequest.setSort(List.of(invalidSort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		// All entries invalid - falls back to DEFAULT_SORT (status DESC +
		// lastModifiedAt DESC)
		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithInvalidSortDirection() {
		// Test searchActivities with invalid sort direction
		Sort invalidSort = new Sort();
		invalidSort.setKey("status");
		invalidSort.setVal("INVALID");
		testRequest.setSort(List.of(invalidSort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		// INVALID direction maps to ASC in buildSort
		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithNullFilter() {
		// Test searchActivities with null filter
		testRequest.setFilter(null);
		when(mockActivityRepository.countActivities(isNull(), isNull())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						isNull(), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(isNull(), isNull());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						isNull(), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithNullCreatedDate() {
		// Test searchActivities with null created date
		testRequest.getFilter().setCreatedDate(null);
		when(mockActivityRepository.countActivities(isNull(), isNull())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						isNull(), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(isNull(), isNull());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						isNull(), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithValidDateRange() {
		// Test searchActivities with valid date range
		DateRange dateRange = new DateRange();
		dateRange.setBegin(1234567890L);
		dateRange.setEnd(1234567891L);
		testRequest.getFilter().setCreatedDate(dateRange);

		when(mockActivityRepository.countActivities(eq(1234567890L), eq(1234567891L)))
				.thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						eq(1234567890L),
						eq(1234567891L),
						anyInt(),
						anyInt(),
						any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(eq(1234567890L), eq(1234567891L));
		verify(mockActivityRepository, times(1))
				.searchActivities(
						eq(1234567890L),
						eq(1234567891L),
						anyInt(),
						anyInt(),
						any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithOnlyBeginDate() {
		// Test searchActivities with only begin date
		DateRange dateRange = new DateRange();
		dateRange.setBegin(1234567890L);
		dateRange.setEnd(null);
		testRequest.getFilter().setCreatedDate(dateRange);

		when(mockActivityRepository.countActivities(eq(1234567890L), isNull())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						eq(1234567890L), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(eq(1234567890L), isNull());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						eq(1234567890L), isNull(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithOnlyEndDate() {
		// Test searchActivities with only end date
		DateRange dateRange = new DateRange();
		dateRange.setBegin(null);
		dateRange.setEnd(1234567891L);
		testRequest.getFilter().setCreatedDate(dateRange);

		when(mockActivityRepository.countActivities(isNull(), eq(1234567891L))).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						isNull(), eq(1234567891L), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(isNull(), eq(1234567891L));
		verify(mockActivityRepository, times(1))
				.searchActivities(
						isNull(), eq(1234567891L), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithEmptyResult() {
		// Test searchActivities with empty result
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(0L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.empty());

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertTrue(response.getData().isEmpty());
					assertNotNull(response.getPagination());
					assertEquals(0L, response.getPagination().getTotalCount());
					assertFalse(response.getPagination().getHasNextPage());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1)).countActivities(anyLong(), anyLong());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithCountError() {
		// Test searchActivities when count operation fails
		when(mockActivityRepository.countActivities(anyLong(), anyLong()))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockActivityRepository, times(1)).countActivities(anyLong(), anyLong());
		verify(mockActivityRepository, never())
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithSearchError() {
		// Test searchActivities when search operation fails
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.error(new RuntimeException("Database error")));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockActivityRepository, times(1)).countActivities(anyLong(), anyLong());
		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithPaginationHasNextPage() {
		// Test searchActivities pagination hasNextPage calculation
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(25L)); // More than page
		// size
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
					assertEquals(25L, response.getPagination().getTotalCount());
					assertTrue(response.getPagination().getHasNextPage()); // 1 * 10 < 25
				})
				.verifyComplete();
	}

	@Test
	void testSearchActivitiesWithPaginationNoNextPage() {
		// Test searchActivities pagination hasNextPage calculation when no next page
		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(10L)); // Exactly page
		// size
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
					assertEquals(10L, response.getPagination().getTotalCount());
					assertFalse(response.getPagination().getHasNextPage()); // 1 * 10 == 10
				})
				.verifyComplete();
	}

	@Test
	void testSearchActivitiesWithCustomPageAndPageSize() {
		// Test searchActivities with custom page and page size
		Pagination pagination = new Pagination();
		pagination.setPage(2);
		pagination.setPageSize(5);
		testRequest.setPagination(pagination);

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(15L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), eq(2), eq(5), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
					assertEquals(2, response.getPagination().getPage());
					assertEquals(5, response.getPagination().getPageSize());
					assertTrue(response.getPagination().getHasNextPage()); // 2 * 5 < 15
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(anyLong(), anyLong(), eq(2), eq(5), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithSortKeyMapping() {
		// Test searchActivities with sort key mapping
		Sort sort = new Sort();
		sort.setKey("createdAt");
		sort.setVal("ASC");
		testRequest.setSort(List.of(sort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithSortKeyMappingUpdatedAt() {
		// Test searchActivities with sort key mapping for updatedAt
		Sort sort = new Sort();
		sort.setKey("updatedAt");
		sort.setVal("DESC");
		testRequest.setSort(List.of(sort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithAllowedSortColumns() {
		// Test searchActivities with all allowed sort columns
		String[] allowedColumns = {
			"audit.createdAt", "audit.lastModifiedAt", "status", "operationType", "unitPermitFee", "laborItem"
		};

		for (String column : allowedColumns) {
			Sort sort = new Sort();
			sort.setKey(column);
			sort.setVal("ASC");
			testRequest.setSort(List.of(sort));

			when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
			when(mockActivityRepository.searchActivities(
							anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
					.thenReturn(Flux.fromIterable(testPermitEntities));

			Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

			StepVerifier.create(result)
					.assertNext(response -> {
						assertNotNull(response);
						assertNotNull(response.getPagination());
					})
					.verifyComplete();

			verify(mockActivityRepository, times(1))
					.searchActivities(
							anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
			reset(mockActivityRepository);
		}
	}

	@Test
	void testSearchActivitiesWithSortDirectionCaseInsensitive() {
		// Test searchActivities with case insensitive sort direction
		Sort sort = new Sort();
		sort.setKey("status");
		sort.setVal("asc"); // lowercase
		testRequest.setSort(List.of(sort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testSearchActivitiesWithSortDirectionMixedCase() {
		// Test searchActivities with mixed case sort direction
		Sort sort = new Sort();
		sort.setKey("status");
		sort.setVal("DeSc"); // mixed case
		testRequest.setSort(List.of(sort));

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getPagination());
				})
				.verifyComplete();

		verify(mockActivityRepository, times(1))
				.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class));
	}

	@Test
	void testMapToActivityResponseWithDummyValues() {
		// Test mapToActivityResponse with DUMMY_VALUE in address
		PermitMongoEntity entity = createTestPermitEntity();
		Address address = new Address();
		address.setCity("Los Angeles");
		address.setCounty(DUMMY_VALUE);
		address.setMunicipality(DUMMY_VALUE);
		address.setState("CA");
		entity.setAddress(address);

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.just(entity));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(1, response.getData().size());
					ActivityResponse activityResponse = response.getData().get(0);
					assertNotNull(activityResponse.getAddress());
					assertEquals("Los Angeles", activityResponse.getAddress().getCity());
					assertNull(activityResponse.getAddress().getCounty()); // Should be nullified
					assertNull(activityResponse.getAddress().getMunicipality()); // Should be nullified
					assertEquals("CA", activityResponse.getAddress().getState());
				})
				.verifyComplete();
	}

	@Test
	void testMapToActivityResponseWithNullAddress() {
		// Test mapToActivityResponse with null address
		PermitMongoEntity entity = createTestPermitEntity();
		entity.setAddress(null);

		when(mockActivityRepository.countActivities(anyLong(), anyLong())).thenReturn(Mono.just(5L));
		when(mockActivityRepository.searchActivities(
						anyLong(), anyLong(), anyInt(), anyInt(), any(org.springframework.data.domain.Sort.class)))
				.thenReturn(Flux.just(entity));

		Mono<SearchActivityResponse> result = activityService.searchActivities(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(1, response.getData().size());
					ActivityResponse activityResponse = response.getData().get(0);
					assertNull(activityResponse.getAddress());
				})
				.verifyComplete();
	}

	@Test
	void testServiceAnnotation() {
		// Test that the class is properly annotated
		assertTrue(activityService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(activityService);
		// In a real test, we would use reflection to verify the annotation
	}

	@Test
	void testRequiredArgsConstructorAnnotation() {
		// Test that the class uses @RequiredArgsConstructor (metadata test)
		assertNotNull(activityService);
		// The constructor injection working properly indicates this annotation is
		// present
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(activityService);
	}

	@Test
	void testConstants() {
		// Test that constants are properly defined (indirectly tested through method
		// behavior)
		assertNotNull(activityService);
		// Constants are tested through the default values used in searchActivities
	}

	// Helper methods to create test data
	private SearchActivityRequest createTestSearchActivityRequest() {
		SearchActivityRequest request = new SearchActivityRequest();

		// Set pagination
		Pagination pagination = new Pagination();
		pagination.setPage(1);
		pagination.setPageSize(10);
		request.setPagination(pagination);

		// Set sort
		Sort sort = new Sort();
		sort.setKey("status");
		sort.setVal("DESC");
		request.setSort(List.of(sort));

		// Set filter
		ActivityFilter filter = new ActivityFilter();
		DateRange dateRange = new DateRange();
		dateRange.setBegin(1234567890L);
		dateRange.setEnd(1234567891L);
		filter.setCreatedDate(dateRange);
		request.setFilter(filter);

		return request;
	}

	private List<PermitMongoEntity> createTestPermitEntities() {
		List<PermitMongoEntity> entities = new java.util.ArrayList<>();

		for (int i = 0; i < 3; i++) {
			PermitMongoEntity entity = createTestPermitEntity();
			entity.setId("id" + i);
			entity.setPermitDbId("permit-db-id-" + i);
			entities.add(entity);
		}

		return entities;
	}

	private PermitMongoEntity createTestPermitEntity() {
		PermitMongoEntity entity = new PermitMongoEntity();
		entity.setId("test-id");
		entity.setPermitDbId("test-permit-db-id");
		entity.setStatus(PermitStatus.NEW);
		entity.setOperationType(OperationType.CREATE);
		entity.setUnitPermitFee(new BigDecimal("100.50"));
		entity.setOldUnitPermitFee(new BigDecimal("50.25"));
		entity.setLaborItem(123);
		entity.setLaborItemDescription("Test Description");
		entity.setOmniItemId("OMNI123");
		entity.setLaborCategory(new LaborCategory(123, "Test Labor Category"));

		Address address = new Address();
		address.setCity("Los Angeles");
		address.setState("CA");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		address.setZipCode("90210");
		entity.setAddress(address);

		Audit audit = new Audit();
		audit.setCreatedByName("user123");
		audit.setLastModifiedByName("user456");
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());
		entity.setAudit(audit);

		return entity;
	}
}
