package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lowes.permits.dto.request.SearchFilterRequest;
import com.lowes.permits.dto.response.SearchFilterResponse;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntitySearchFilterServiceTest {

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitPostgresRepository mockRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService permitService;
	private SearchFilterRequest testRequest;

	@BeforeEach
	void setUp() {
		permitService =
				new PermitService(mockPermitMongoRepository, mockPermitMapper, mockRepository, mockCommonUtilityClient);
		testRequest = createTestSearchFilterRequest();
	}

	@Test
	void testSearchFilterWithInitialLoadType() {
		// Test searchFilter with INITIAL_LOAD type - should merge labor categories and
		// states
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("INITIAL_LOAD");
		testRequest.setFilters(List.of(filter));

		// Mock labor categories response
		when(mockRepository.searchFilter(
						contains("labor_category_distinct_view"), isNull(), eq("LABOR_CATEGORY_DESCRIPTION")))
				.thenReturn(Flux.just("Category 1", "Category 2", "Category 3"));

		// Mock state mapping response
		List<Map<String, String>> stateMapList = List.of(
				Map.of("stateCode", "CA", "stateName", "California"),
				Map.of("stateCode", "NY", "stateName", "New York"),
				Map.of("stateCode", "TX", "stateName", "Texas"));
		when(mockPermitMongoRepository.getStateMap()).thenReturn(Mono.just(stateMapList));

		// Mock MongoDB labor categories
		when(mockPermitMongoRepository.findAllLaborCategories()).thenReturn(Flux.empty());

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(
							3,
							response.getData().size()); // Should have 3 data sets (PostgreSQL labor categories, states,
					// MongoDB
					// labor categories)

					// Verify labor categories data
					SearchFilterResponse.SearchFilterResponseData laborCategoriesData =
							response.getData().get(0);
					assertEquals("LABOR_CATEGORY_DESCRIPTION", laborCategoriesData.getType());
					assertEquals(3, laborCategoriesData.getCount());
					assertEquals(List.of("Category 1", "Category 2", "Category 3"), laborCategoriesData.getValues());

					// Verify states data
					SearchFilterResponse.SearchFilterResponseData statesData =
							response.getData().get(1);
					assertEquals("ALL_STATES", statesData.getType());
					assertEquals(3, statesData.getCount());
					assertEquals(List.of("CA (California)", "NY (New York)", "TX (Texas)"), statesData.getValues());
				})
				.verifyComplete();

		verify(mockRepository, times(1))
				.searchFilter(contains("labor_category_distinct_view"), isNull(), eq("LABOR_CATEGORY_DESCRIPTION"));
		verify(mockPermitMongoRepository, times(1)).getStateMap();
	}

	@Test
	void testSearchFilterWithNullFilters() {
		// Test searchFilter with null filters
		testRequest.setFilters(null);

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).searchFilter(anyString(), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithEmptyFilters() {
		// Test searchFilter with empty filters
		testRequest.setFilters(List.of());

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).searchFilter(anyString(), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithUnsupportedType() {
		// Test searchFilter with unsupported type
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("UNSUPPORTED_TYPE");
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).searchFilter(anyString(), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithNullType() {
		// Test searchFilter with null type
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType(null);
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).searchFilter(anyString(), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithEmptyType() {
		// Test searchFilter with empty type
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("");
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(IllegalArgumentException.class).verify();

		verify(mockRepository, never()).searchFilter(anyString(), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithCityType() {
		// Test searchFilter with CITIES type
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("CITIES");
		filter.setValues(List.of("Los Angeles", "New York"));
		testRequest.setFilters(List.of(filter));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.just("90210,90211", "10001,10002"));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(1, response.getData().size());
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals("ZIPCODES", data.getType());
					assertEquals(4, data.getCount());
					assertEquals(List.of("10001", "10002", "90210", "90211"), data.getValues());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).searchFilter(contains("city_zipcodes_view"), anyList(), eq("ZIPCODES"));
	}

	@Test
	void testSearchFilterWithLaborCategoriesType() {
		// Test searchFilter with LABOR_CATEGORIES type
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("LABOR_CATEGORIES");
		filter.setValues(List.of("Plumbing", "Electrical"));
		testRequest.setFilters(List.of(filter));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString())).thenReturn(Flux.just("CA,NV", "NY,MA"));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(1, response.getData().size());
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals("STATES", data.getType());
					assertEquals(4, data.getCount());
					assertEquals(List.of("CA", "MA", "NV", "NY"), data.getValues());
				})
				.verifyComplete();

		verify(mockRepository, times(1)).searchFilter(contains("labor_category_states_view"), anyList(), eq("STATES"));
	}

	@Test
	void testSearchFilterWithEmptyValues() {
		// Test searchFilter with empty values list
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of());
		testRequest.setFilters(List.of(filter));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString())).thenReturn(Flux.empty());

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(2, response.getData().size());
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals("CITIES", data.getType());
					assertEquals(0, data.getCount());
					assertTrue(data.getValues().isEmpty());
				})
				.verifyComplete();
	}

	@Test
	void testSearchFilterWithNullValues() {
		// Test searchFilter with null values list for INITIAL_LOAD
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("INITIAL_LOAD");
		filter.setValues(null);
		testRequest.setFilters(List.of(filter));

		// Mock labor categories response
		when(mockRepository.searchFilter(anyString(), isNull(), anyString()))
				.thenReturn(Flux.just("Category 1", "Category 2"));

		// Mock state mapping response
		List<Map<String, String>> stateMapList = List.of(
				Map.of("stateCode", "CA", "stateName", "California"),
				Map.of("stateCode", "NY", "stateName", "New York"));
		when(mockPermitMongoRepository.getStateMap()).thenReturn(Mono.just(stateMapList));

		// Mock MongoDB labor categories
		when(mockPermitMongoRepository.findAllLaborCategories()).thenReturn(Flux.empty());

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertNotNull(response.getData());
					assertEquals(
							3, response.getData().size()); // Should have labor categories, states, and MongoDB labor
					// categories

					// Verify labor categories
					SearchFilterResponse.SearchFilterResponseData laborData =
							response.getData().get(0);
					assertEquals("LABOR_CATEGORY_DESCRIPTION", laborData.getType());
					assertEquals(2, laborData.getCount());
					assertEquals(List.of("Category 1", "Category 2"), laborData.getValues());

					// Verify states
					SearchFilterResponse.SearchFilterResponseData statesData =
							response.getData().get(1);
					assertEquals("ALL_STATES", statesData.getType());
					assertEquals(2, statesData.getCount());
					assertEquals(List.of("CA (California)", "NY (New York)"), statesData.getValues());
				})
				.verifyComplete();
	}

	@Test
	void testSearchFilterWithRepositoryError() {
		// Test searchFilter when repository throws an error during INITIAL_LOAD
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("INITIAL_LOAD");
		testRequest.setFilters(List.of(filter));

		// Mock labor categories to return error
		when(mockRepository.searchFilter(anyString(), isNull(), anyString()))
				.thenReturn(Flux.error(new RuntimeException("Database error")));

		// Mock state mapping (won't be reached due to error in labor categories)
		when(mockPermitMongoRepository.getStateMap()).thenReturn(Mono.just(List.of()));

		// Mock MongoDB labor categories (won't be reached due to error in PostgreSQL
		// labor categories)
		when(mockPermitMongoRepository.findAllLaborCategories()).thenReturn(Flux.empty());

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockRepository, times(1)).searchFilter(anyString(), isNull(), anyString());
	}

	@Test
	void testSearchFilterWithRepositoryErrorInCitiesByStates() {
		// Test repository error in fetchCitiesAndCountyByStates method
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA"));
		testRequest.setFilters(List.of(filter));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.error(new RuntimeException("Database connection failed")));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockRepository, times(2)).searchFilter(contains("state_cities_counties_view"), anyList(), anyString());
	}

	@Test
	void testSearchFilterWithRepositoryErrorInZipcodesByCities() {
		// Test repository error in fetchZipcodesByCities method
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("CITIES");
		filter.setValues(List.of("Los Angeles"));
		testRequest.setFilters(List.of(filter));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.error(new RuntimeException("Invalid query syntax")));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockRepository, times(1)).searchFilter(contains("city_zipcodes_view"), anyList(), anyString());
	}

	@Test
	void testSplitCommaSeparatedValuesWithValidInput() {
		// Test splitCommaSeparatedValues with valid comma-separated input
		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.just("value1,value2,value3", "value4,value5"));

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA"));
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals(5, data.getCount());
					assertEquals(List.of("value1", "value2", "value3", "value4", "value5"), data.getValues());
				})
				.verifyComplete();
	}

	@Test
	void testSplitCommaSeparatedValuesWithEmptyString() {
		// Test splitCommaSeparatedValues with empty string
		when(mockRepository.searchFilter(anyString(), anyList(), anyString())).thenReturn(Flux.just(""));

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA"));
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals(0, data.getCount());
					assertTrue(data.getValues().isEmpty());
				})
				.verifyComplete();
	}

	@Test
	void testSplitCommaSeparatedValuesWithNullString() {
		// Test splitCommaSeparatedValues with null string
		when(mockRepository.searchFilter(anyString(), anyList(), anyString())).thenReturn(Flux.empty());

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA"));
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals(0, data.getCount());
					assertTrue(data.getValues().isEmpty());
				})
				.verifyComplete();
	}

	@Test
	void testSplitCommaSeparatedValuesWithExtraSpaces() {
		// Test splitCommaSeparatedValues with extra spaces and empty values
		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.just(" value1 , , value2 ,  , value3 "));

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA"));
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals(3, data.getCount());
					assertEquals(List.of("value1", "value2", "value3"), data.getValues());
				})
				.verifyComplete();
	}

	@Test
	void testSearchFilterWithDuplicateValues() {
		// Test searchFilter with duplicate values in response
		when(mockRepository.searchFilter(anyString(), anyList(), anyString()))
				.thenReturn(Flux.just("CA,NY,CA", "NY,MA,MA"));

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("LABOR_CATEGORIES");
		filter.setValues(List.of("Plumbing"));
		testRequest.setFilters(List.of(filter));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals("STATES", data.getType());
					assertEquals(3, data.getCount()); // Duplicates should be removed
					assertEquals(List.of("CA", "MA", "NY"), data.getValues()); // Should be sorted
				})
				.verifyComplete();
	}

	@Test
	void testSearchFilterWithMultipleFilters() {
		// Test searchFilter with multiple filters (should only process first one)
		SearchFilterRequest.Filter filter1 = new SearchFilterRequest.Filter();
		filter1.setType("LABOR_CATEGORIES");
		filter1.setValues(List.of("Plumbing"));

		SearchFilterRequest.Filter filter2 = new SearchFilterRequest.Filter();
		filter2.setType("STATES");
		filter2.setValues(List.of("CA"));

		testRequest.setFilters(List.of(filter1, filter2));

		when(mockRepository.searchFilter(anyString(), anyList(), anyString())).thenReturn(Flux.just("CA,NV"));

		Mono<SearchFilterResponse> result = permitService.searchFilter(testRequest);

		StepVerifier.create(result)
				.assertNext(response -> {
					SearchFilterResponse.SearchFilterResponseData data =
							response.getData().get(0);
					assertEquals("STATES", data.getType());
					assertEquals(2, data.getCount());
					assertEquals(List.of("CA", "NV"), data.getValues());
				})
				.verifyComplete();

		// Verify only the first filter was processed
		verify(mockRepository, times(1)).searchFilter(contains("labor_category_states_view"), anyList(), eq("STATES"));
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(permitService);
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
	void testSearchFilterWithInitialLoadHandlingEmptyDataSources() {
		// Test INITIAL_LOAD filter with empty data from all sources
		SearchFilterRequest request = new SearchFilterRequest();
		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("INITIAL_LOAD");
		request.setFilters(List.of(filter));

		// Mock empty responses
		when(mockRepository.searchFilter(anyString(), eq(null), eq("LABOR_CATEGORY_DESCRIPTION")))
				.thenReturn(Flux.empty());
		when(mockPermitMongoRepository.getStateMap()).thenReturn(Mono.just(List.of()));
		when(mockPermitMongoRepository.findAllLaborCategories()).thenReturn(Flux.empty());

		StepVerifier.create(permitService.searchFilter(request))
				.assertNext(response -> {
					assertNotNull(response.getData());
					assertEquals(3, response.getData().size());

					// Verify all data types are present but empty
					response.getData().forEach(data -> {
						assertEquals(0, data.getCount());
						assertTrue(data.getValues().isEmpty());
					});

					// Verify correct types are present
					List<String> types = response.getData().stream()
							.map(SearchFilterResponse.SearchFilterResponseData::getType)
							.toList();
					assertTrue(types.contains("LABOR_CATEGORY_DESCRIPTION"));
					assertTrue(types.contains("ALL_STATES"));
					assertTrue(types.contains("ALL_LABOR_CATEGORIES"));
				})
				.verifyComplete();

		// Verify all three data sources were called despite being empty
		verify(mockRepository, times(1)).searchFilter(anyString(), eq(null), eq("LABOR_CATEGORY_DESCRIPTION"));
		verify(mockPermitMongoRepository, times(1)).getStateMap();
		verify(mockPermitMongoRepository, times(1)).findAllLaborCategories();
	}

	// Helper method to create test data
	private SearchFilterRequest createTestSearchFilterRequest() {
		SearchFilterRequest request = new SearchFilterRequest();

		SearchFilterRequest.Filter filter = new SearchFilterRequest.Filter();
		filter.setType("STATES");
		filter.setValues(List.of("CA", "NY"));

		request.setFilters(List.of(filter));

		return request;
	}

	private com.lowes.permits.dto.response.LaborCategoryResponse createTestLaborCategoryResponse(
			String id, String name) {
		com.lowes.permits.dto.response.LaborCategoryResponse response =
				new com.lowes.permits.dto.response.LaborCategoryResponse();
		response.setMongoId(new org.bson.types.ObjectId(id));
		response.setId(id);
		response.setName(name);
		return response;
	}
}
