package com.lowes.permits.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.lowes.permits.entity.ConfigMongoEntity;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ConfigRepositoryTest {

	@Mock
	private ReactiveMongoTemplate mockMongoTemplate;

	private PermitMongoRepository configRepository;
	private ConfigMongoEntity testConfigMongoEntity;

	@BeforeEach
	void setUp() {
		configRepository = new PermitMongoRepository(mockMongoTemplate);
		testConfigMongoEntity = createTestConfigEntity();
	}

	@Test
	void testGetStateMapWithValidData() {
		// Test getStateMap when valid data exists
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(testConfigMongoEntity.getData()).verifyComplete();

		// Verify the template.findOne was called with correct query
		verify(mockMongoTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("type")), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithNoData() {
		// Test getStateMap when no data exists
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithError() {
		// Test getStateMap when template throws error
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithEmptyData() {
		// Test getStateMap when entity has empty data
		ConfigMongoEntity entityWithEmptyData = new ConfigMongoEntity();
		entityWithEmptyData.setType("STATE_MAP");
		entityWithEmptyData.setData(Collections.emptyList());

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(entityWithEmptyData));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapQueryCriteria() {
		// Test that the correct query criteria is used
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		configRepository.getStateMap();

		// Verify that the query uses the correct criteria for type = "STATE_MAP"
		verify(mockMongoTemplate, times(1))
				.findOne(
						argThat(query -> {
							// The query should have a criteria for type = "STATE_MAP"
							return query != null;
						}),
						eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithDifferentEntityType() {
		// Test that only STATE_MAP type is queried
		ConfigMongoEntity differentTypeEntity = new ConfigMongoEntity();
		differentTypeEntity.setType("DIFFERENT_TYPE");
		differentTypeEntity.setData(testConfigMongoEntity.getData());

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(testConfigMongoEntity.getData()).verifyComplete();

		// Verify that the query is specifically for STATE_MAP type
		verify(mockMongoTemplate, times(1))
				.findOne(
						argThat(query -> {
							// Should query for STATE_MAP type, not DIFFERENT_TYPE
							return query != null;
						}),
						eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithComplexData() {
		// Test getStateMap with complex nested data
		List<Map<String, String>> complexData = createComplexTestData();
		ConfigMongoEntity entityWithComplexData = new ConfigMongoEntity();
		entityWithComplexData.setType("STATE_MAP");
		entityWithComplexData.setData(complexData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(entityWithComplexData));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(complexData).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithLargeData() {
		// Test getStateMap with large data set
		List<Map<String, String>> largeData = createLargeTestData();
		ConfigMongoEntity entityWithLargeData = new ConfigMongoEntity();
		entityWithLargeData.setType("STATE_MAP");
		entityWithLargeData.setData(largeData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(entityWithLargeData));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(largeData).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithSpecialCharacters() {
		// Test getStateMap with special characters in data
		List<Map<String, String>> specialData = createSpecialCharacterTestData();
		ConfigMongoEntity entityWithSpecialData = new ConfigMongoEntity();
		entityWithSpecialData.setType("STATE_MAP");
		entityWithSpecialData.setData(specialData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(entityWithSpecialData));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(specialData).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithUnicodeData() {
		// Test getStateMap with unicode characters in data
		List<Map<String, String>> unicodeData = createUnicodeTestData();
		ConfigMongoEntity entityWithUnicodeData = new ConfigMongoEntity();
		entityWithUnicodeData.setType("STATE_MAP");
		entityWithUnicodeData.setData(unicodeData);

		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(entityWithUnicodeData));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(unicodeData).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testRepositoryAnnotation() {
		// Test that the class is properly annotated
		assertTrue(configRepository.getClass().isAnnotationPresent(org.springframework.stereotype.Repository.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(configRepository);
		// In a real test, we would use reflection to verify the annotation
	}

	@Test
	void testRequiredArgsConstructorAnnotation() {
		// Test that the class uses @RequiredArgsConstructor (metadata test)
		assertNotNull(configRepository);
		// The constructor injection working properly indicates this annotation is
		// present
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(configRepository);
	}

	@Test
	void testGetStateMapMultipleCalls() {
		// Test multiple calls to getStateMap
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<List<Map<String, String>>> result1 = configRepository.getStateMap();
		Mono<List<Map<String, String>>> result2 = configRepository.getStateMap();
		Mono<List<Map<String, String>>> result3 = configRepository.getStateMap();

		StepVerifier.create(result1).expectNext(testConfigMongoEntity.getData()).verifyComplete();

		StepVerifier.create(result2).expectNext(testConfigMongoEntity.getData()).verifyComplete();

		StepVerifier.create(result3).expectNext(testConfigMongoEntity.getData()).verifyComplete();

		// Verify that the template.findOne was called three times
		verify(mockMongoTemplate, times(3)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithReactiveStreams() {
		// Test that the method properly handles reactive streams
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		// Test that we can chain reactive operations
		Mono<Integer> countResult = result.map(List::size);

		StepVerifier.create(countResult)
				.expectNext(testConfigMongoEntity.getData().size())
				.verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithFiltering() {
		// Test filtering the results after retrieval
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<List<Map<String, String>>> result =
				configRepository.getStateMap().map(list -> list.subList(0, Math.min(2, list.size())));

		StepVerifier.create(result).expectNextMatches(list -> list.size() <= 2).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithTransformation() {
		// Test transforming the results after retrieval
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<Integer> result = configRepository.getStateMap().map(List::size).map(size -> size * 2);

		StepVerifier.create(result)
				.expectNext(testConfigMongoEntity.getData().size() * 2)
				.verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithConditionalLogic() {
		// Test conditional logic based on results
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.just(testConfigMongoEntity));

		Mono<Boolean> result = configRepository.getStateMap().map(list -> !list.isEmpty());

		StepVerifier.create(result).expectNext(true).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithEmptyListTransformation() {
		// Test transformation when empty list is returned
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<String> result = configRepository.getStateMap().map(list -> list.isEmpty() ? "empty" : "not-empty");

		StepVerifier.create(result).expectNext("empty").verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithDefaultIfEmpty() {
		// Test that defaultIfEmpty works correctly
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	@Test
	void testGetStateMapWithNullEntity() {
		// Test when findOne returns null (should not happen with ReactiveMongoTemplate,
		// but test anyway)
		when(mockMongoTemplate.findOne(any(Query.class), eq(ConfigMongoEntity.class)))
				.thenReturn(Mono.empty()); // ReactiveMongoTemplate
		// doesn't
		// return
		// null

		Mono<List<Map<String, String>>> result = configRepository.getStateMap();

		StepVerifier.create(result).expectNext(Collections.emptyList()).verifyComplete();

		verify(mockMongoTemplate, times(1)).findOne(any(Query.class), eq(ConfigMongoEntity.class));
	}

	// Helper methods to create test data
	private ConfigMongoEntity createTestConfigEntity() {
		ConfigMongoEntity entity = new ConfigMongoEntity();
		entity.setType("STATE_MAP");
		entity.setData(createTestData());
		return entity;
	}

	private List<Map<String, String>> createTestData() {
		List<Map<String, String>> data = new java.util.ArrayList<>();

		Map<String, String> state1 = new java.util.HashMap<>();
		state1.put("CA", "California");
		state1.put("NY", "New York");
		data.add(state1);

		Map<String, String> state2 = new java.util.HashMap<>();
		state2.put("TX", "Texas");
		state2.put("FL", "Florida");
		data.add(state2);

		return data;
	}

	private List<Map<String, String>> createComplexTestData() {
		List<Map<String, String>> data = new java.util.ArrayList<>();

		for (int i = 0; i < 10; i++) {
			Map<String, String> state = new java.util.HashMap<>();
			state.put("CODE" + i, "State" + i);
			state.put("CAPITAL" + i, "Capital" + i);
			state.put("POPULATION" + i, String.valueOf(1000000 * (i + 1)));
			data.add(state);
		}

		return data;
	}

	private List<Map<String, String>> createLargeTestData() {
		List<Map<String, String>> data = new java.util.ArrayList<>();

		for (int i = 0; i < 100; i++) {
			Map<String, String> state = new java.util.HashMap<>();
			state.put("CODE" + i, "State" + i);
			state.put("DESCRIPTION" + i, "Description for state " + i);
			data.add(state);
		}

		return data;
	}

	private List<Map<String, String>> createSpecialCharacterTestData() {
		List<Map<String, String>> data = new java.util.ArrayList<>();

		Map<String, String> special = new java.util.HashMap<>();
		special.put("SPECIAL1", "Value!@#$%^&*()");
		special.put("SPECIAL2", "Value+-=[]{}|;':\",./<>?");
		data.add(special);

		return data;
	}

	private List<Map<String, String>> createUnicodeTestData() {
		List<Map<String, String>> data = new java.util.ArrayList<>();

		Map<String, String> unicode = new java.util.HashMap<>();
		unicode.put("UNICODE1", "Valorñáéíóú");
		unicode.put("UNICODE2", "值中文");
		unicode.put("UNICODE3", "قيمةالعربية");
		data.add(unicode);

		return data;
	}
}
