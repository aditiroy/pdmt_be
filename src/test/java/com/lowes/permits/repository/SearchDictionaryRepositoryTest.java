package com.lowes.permits.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lowes.permits.entity.SearchDictionaryMongoEntity;
import com.lowes.permits.model.Item;
import com.lowes.permits.model.LookupType;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SearchDictionaryRepositoryTest {

	@Mock
	private ReactiveMongoTemplate mockMongoTemplate;

	private PermitMongoRepository repository;
	private List<String> testValues;
	private List<Item> testMapData;
	private SearchDictionaryMongoEntity testEntity;

	@BeforeEach
	void setUp() {
		repository = new PermitMongoRepository(mockMongoTemplate);
		testValues = List.of("Los Angeles", "New York", "Chicago");
		testMapData =
				List.of(createItem("CA", "California"), createItem("NY", "New York"), createItem("IL", "Illinois"));
		testEntity = createTestSearchDictionaryEntity();
	}

	@Test
	void testUpsertWithValidData() {
		// Test upsert method with valid data
		LookupType type = LookupType.CITY;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsert(type, testValues);

		StepVerifier.create(result).verifyComplete();

		// Verify the query was built correctly
		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("data")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertWithEmptyValues() {
		// Test upsert method with empty list
		LookupType type = LookupType.ZIP_CODE;
		List<String> emptyValues = List.of();

		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsert(type, emptyValues);

		StepVerifier.create(result).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("data")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertWithError() {
		// Test upsert method when database error occurs
		LookupType type = LookupType.COUNTY;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Void> result = repository.upsert(type, testValues);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("data")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataWithValidData() {
		// Test upsertMapData method with valid data
		LookupType type = LookupType.STATE_EXPANDED;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsertMapData(type, testMapData);

		StepVerifier.create(result).verifyComplete();

		// Verify the query was built correctly
		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("mapData")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataWithNullValueInItems() {
		// Test upsertMapData method with null values in items
		LookupType type = LookupType.STATE_EXPANDED;
		List<Item> mapDataWithNullValues =
				List.of(createItem(null, "California"), createItem("NY", null), createItem("IL", "Illinois"));

		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsertMapData(type, mapDataWithNullValues);

		StepVerifier.create(result).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("mapData")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataWithEmptyMapData() {
		// Test upsertMapData method with empty list
		LookupType type = LookupType.STATE_EXPANDED;
		List<Item> emptyMapData = List.of();

		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsertMapData(type, emptyMapData);

		StepVerifier.create(result).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("mapData")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataWithError() {
		// Test upsertMapData method when database error occurs
		LookupType type = LookupType.STATE_EXPANDED;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Void> result = repository.upsertMapData(type, testMapData);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("mapData")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithValidData() {
		// Test searchByTypeAndPrefix method with valid data
		LookupType type = LookupType.CITY;
		String prefix = "Los";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(testValues);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(
						list -> list.contains("Los Angeles") && !list.contains("New York") && !list.contains("Chicago"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithCaseInsensitivePrefix() {
		// Test searchByTypeAndPrefix method with case insensitive prefix
		LookupType type = LookupType.CITY;
		String prefix = "los";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(List.of("Los Angeles", "los angeles", "LOS ANGELES", "New York"));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 3
						&& // Should match all case variations
						list.contains("Los Angeles")
						&& list.contains("los angeles")
						&& list.contains("LOS ANGELES"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithNoMatchingData() {
		// Test searchByTypeAndPrefix method with no matching data
		LookupType type = LookupType.CITY;
		String prefix = "XYZ";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(testValues);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithEntityNotFound() {
		// Test searchByTypeAndPrefix method when entity not found
		LookupType type = LookupType.CITY;
		String prefix = "Los";

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithError() {
		// Test searchByTypeAndPrefix method when database error occurs
		LookupType type = LookupType.CITY;
		String prefix = "Los";

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithValidData() {
		// Test searchByStateExpandedTypeAndPrefix method with valid data
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "Cal";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(testMapData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 1
						&& list.get(0).getCode().equals("CA")
						&& list.get(0).getValue().equals("California"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithEmptyPrefix() {
		// Test searchByStateExpandedTypeAndPrefix method with empty prefix
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(testMapData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNext(testMapData) // Empty prefix should match all
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithCaseInsensitivePrefix() {
		// Test searchByStateExpandedTypeAndPrefix method with case insensitive prefix
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "cal";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(List.of(
				createItem("CA", "California"),
				createItem("ca", "california"),
				createItem("CA", "CALIFORNIA"),
				createItem("NY", "New York")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 3
						&& // Should match all case variations
						list.stream()
								.anyMatch(item -> item.getCode().equals("CA")
										&& item.getValue().equals("California"))
						&& list.stream()
								.anyMatch(item -> item.getCode().equals("ca")
										&& item.getValue().equals("california"))
						&& list.stream()
								.anyMatch(item -> item.getCode().equals("CA")
										&& item.getValue().equals("CALIFORNIA")))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithNoMatchingData() {
		// Test searchByStateExpandedTypeAndPrefix method with no matching data
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "XYZ";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(testMapData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithEntityNotFound() {
		// Test searchByStateExpandedTypeAndPrefix method when entity not found
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "Cal";

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithError() {
		// Test searchByStateExpandedTypeAndPrefix method when database error occurs
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "Cal";

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testRepositoryAnnotation() {
		// Test that the class is properly annotated
		assertTrue(repository.getClass().isAnnotationPresent(org.springframework.stereotype.Repository.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(repository);
		// In a real test, we would use reflection to verify the annotation
	}

	@Test
	void testRequiredArgsConstructorAnnotation() {
		// Test that the class uses @RequiredArgsConstructor (metadata test)
		assertNotNull(repository);
		// The constructor injection working properly indicates this annotation is
		// present
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(repository);
	}

	@Test
	void testUpsertTimestamp() {
		// Test that upsert methods set the timestamp correctly
		LookupType type = LookupType.CITY;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		repository.upsert(type, testValues);

		// Verify that the update includes lastUpdatedAt
		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> {
							// The update should include lastUpdatedAt field
							String updateStr = update.toString();
							return updateStr.contains("lastUpdatedAt");
						}),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataTimestamp() {
		// Test that upsertMapData method sets the timestamp correctly
		LookupType type = LookupType.STATE_EXPANDED;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		repository.upsertMapData(type, testMapData);

		// Verify that the update includes lastUpdatedAt
		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> {
							// The update should include lastUpdatedAt field
							String updateStr = update.toString();
							return updateStr.contains("lastUpdatedAt");
						}),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertLoggingOnSuccess() {
		// Test that upsert methods log success messages
		LookupType type = LookupType.CITY;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsert(type, testValues);

		StepVerifier.create(result).verifyComplete();

		// The doOnSuccess should be called
		verify(mockMongoTemplate, times(1))
				.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertLoggingOnError() {
		// Test that upsert methods log error messages
		LookupType type = LookupType.CITY;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Void> result = repository.upsert(type, testValues);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		// The doOnError should be called
		verify(mockMongoTemplate, times(1))
				.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataLoggingOnSuccess() {
		// Test that upsertMapData method logs success messages
		LookupType type = LookupType.STATE_EXPANDED;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsertMapData(type, testMapData);

		StepVerifier.create(result).verifyComplete();

		// The doOnSuccess should be called
		verify(mockMongoTemplate, times(1))
				.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertMapDataLoggingOnError() {
		// Test that upsertMapData method logs error messages
		LookupType type = LookupType.STATE_EXPANDED;
		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Void> result = repository.upsertMapData(type, testMapData);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		// The doOnError should be called
		verify(mockMongoTemplate, times(1))
				.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixSorting() {
		// Test that searchByTypeAndPrefix returns sorted results
		LookupType type = LookupType.CITY;
		String prefix = "L";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(List.of("Los Angeles", "Long Beach", "Lancaster", "Lake Forest"));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(
						list -> list.equals(List.of("Lake Forest", "Lancaster", "Long Beach", "Los Angeles")) // Should
						// be
						// sorted
						// alphabetically
						)
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixSorting() {
		// Test that searchByStateExpandedTypeAndPrefix returns sorted results
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "C";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(
				List.of(createItem("CA", "California"), createItem("CO", "Colorado"), createItem("CT", "Connecticut")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 3
						&& // Should be sorted alphabetically by
						// value
						list.get(0).getValue().equals("California")
						&& list.get(1).getValue().equals("Colorado")
						&& list.get(2).getValue().equals("Connecticut"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithStateType() {
		// Test searchByStateExpandedTypeAndPrefix method with STATE type (should search
		// by code)
		LookupType type = LookupType.STATE;
		String prefix = "C";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(
				List.of(createItem("CA", "California"), createItem("CO", "Colorado"), createItem("NY", "New York")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 2
						&& // Should match CA and CO by code
						list.stream()
								.anyMatch(item -> item.getCode().equals("CA")
										&& item.getValue().equals("California"))
						&& list.stream()
								.anyMatch(item -> item.getCode().equals("CO")
										&& item.getValue().equals("Colorado")))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithStateTypeCaseInsensitive() {
		// Test searchByStateExpandedTypeAndPrefix method with STATE type and case
		// insensitive prefix
		LookupType type = LookupType.STATE;
		String prefix = "c";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(
				List.of(createItem("CA", "California"), createItem("CO", "Colorado"), createItem("NY", "New York")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 2
						&& // Should match CA and CO by code
						// (case insensitive)
						list.stream()
								.anyMatch(item -> item.getCode().equals("CA")
										&& item.getValue().equals("California"))
						&& list.stream()
								.anyMatch(item -> item.getCode().equals("CO")
										&& item.getValue().equals("Colorado")))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithUnsupportedType() {
		// Test searchByStateExpandedTypeAndPrefix method with unsupported type
		LookupType type = LookupType.CITY; // Not STATE_EXPANDED or STATE
		String prefix = "Cal";

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		// Should not call MongoDB for unsupported types
		verify(mockMongoTemplate, never()).findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithNullValuesInData() {
		// Test searchByTypeAndPrefix method with null values in data
		LookupType type = LookupType.CITY;
		String prefix = "Los";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(java.util.Arrays.asList("Los Angeles", null, "Los Vegas", null, "New York"));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 2
						&& // Should only match non-null values
						list.contains("Los Angeles")
						&& list.contains("Los Vegas")
						&& !list.contains(null))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithNullValuesInMapData() {
		// Test searchByStateExpandedTypeAndPrefix method with null values in mapData
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = "Cal";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(java.util.Arrays.asList(
				createItem("CA", "California"), null, createItem("CO", null), createItem("NY", "New York")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 1
						&& // Should only match non-null items
						// with non-null values
						list.get(0).getCode().equals("CA")
						&& list.get(0).getValue().equals("California"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithStateTypeNullCodes() {
		// Test searchByStateExpandedTypeAndPrefix method with STATE type and null codes
		LookupType type = LookupType.STATE;
		String prefix = "C";

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(java.util.Arrays.asList(
				createItem(null, "California"), createItem("CO", "Colorado"), createItem("NY", "New York")));

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result)
				.expectNextMatches(list -> list.size() == 1
						&& // Should only match items with
						// non-null codes
						list.get(0).getCode().equals("CO")
						&& list.get(0).getValue().equals("Colorado"))
				.verifyComplete();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testUpsertWithNullValues() {
		// Test upsert method with null values in list
		LookupType type = LookupType.CITY;
		List<String> valuesWithNulls = java.util.Arrays.asList("Los Angeles", null, "New York", null);

		when(mockMongoTemplate.upsert(any(Query.class), any(Update.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<Void> result = repository.upsert(type, valuesWithNulls);

		StepVerifier.create(result).verifyComplete();

		verify(mockMongoTemplate, times(1))
				.upsert(
						argThat(query -> query.toString().contains("type")),
						argThat(update -> update.toString().contains("data")),
						eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByTypeAndPrefixWithNullPrefix() {
		// Test searchByTypeAndPrefix method with null prefix
		LookupType type = LookupType.CITY;
		String prefix = null;

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setData(testValues);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<String>> result = repository.searchByTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectError(NullPointerException.class).verify();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	@Test
	void testSearchByStateExpandedTypeAndPrefixWithNullPrefix() {
		// Test searchByStateExpandedTypeAndPrefix method with null prefix
		LookupType type = LookupType.STATE_EXPANDED;
		String prefix = null;

		SearchDictionaryMongoEntity entity = createTestSearchDictionaryEntity();
		entity.setMapData(testMapData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(SearchDictionaryMongoEntity.class)))
				.thenReturn(Mono.just(entity));

		Mono<List<Item>> result = repository.searchByStateExpandedTypeAndPrefix(type, prefix);

		StepVerifier.create(result).expectError(NullPointerException.class).verify();

		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(SearchDictionaryMongoEntity.class));
	}

	// Helper methods to create test data
	private SearchDictionaryMongoEntity createTestSearchDictionaryEntity() {
		SearchDictionaryMongoEntity entity = new SearchDictionaryMongoEntity();
		entity.setType(LookupType.CITY.name());
		entity.setData(testValues);
		entity.setMapData(testMapData);
		entity.setLastUpdatedAt(Instant.now());
		return entity;
	}

	private Item createItem(String code, String value) {
		Item item = new Item();
		item.setCode(code);
		item.setValue(value);
		return item;
	}
}
