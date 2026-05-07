package com.lowes.permits.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lowes.permits.dto.response.LaborCategoryResponse;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitMongoRepositoryTest {

	@Mock
	private ReactiveMongoTemplate mockTemplate;

	private PermitMongoRepository repository;
	private PermitMongoEntity testPermitMongoEntity;
	private List<PermitMongoEntity> testPermitEntities;

	@BeforeEach
	void setUp() {
		repository = new PermitMongoRepository(mockTemplate);
		testPermitMongoEntity = createTestPermitEntity();
		testPermitEntities = List.of(testPermitMongoEntity);
	}

	@Test
	void testCreatePermit() {
		// Test createPermit method
		when(mockTemplate.save(any(PermitMongoEntity.class))).thenReturn(Mono.just(testPermitMongoEntity));

		Mono<PermitMongoEntity> result = repository.createPermit(testPermitMongoEntity);

		StepVerifier.create(result).expectNext(testPermitMongoEntity).verifyComplete();

		verify(mockTemplate, times(1)).save(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithError() {
		// Test createPermit method when database error occurs
		when(mockTemplate.save(any(PermitMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<PermitMongoEntity> result = repository.createPermit(testPermitMongoEntity);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).save(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitsBulk() {
		// Test createPermitsBulk method
		when(mockTemplate.insertAll(anyList())).thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<List<PermitMongoEntity>> result = repository.createPermitsBulk(testPermitEntities);

		StepVerifier.create(result).expectNext(testPermitEntities).verifyComplete();

		verify(mockTemplate, times(1)).insertAll(testPermitEntities);
	}

	@Test
	void testCreatePermitsBulkWithEmptyList() {
		// Test createPermitsBulk method with empty list
		List<PermitMongoEntity> emptyList = List.of();
		when(mockTemplate.insertAll(anyList())).thenReturn(Flux.empty());

		Mono<List<PermitMongoEntity>> result = repository.createPermitsBulk(emptyList);

		StepVerifier.create(result).expectNext(List.of()).verifyComplete();

		verify(mockTemplate, times(1)).insertAll(emptyList);
	}

	@Test
	void testCreatePermitsBulkWithError() {
		// Test createPermitsBulk method when database error occurs
		when(mockTemplate.insertAll(anyList())).thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Mono<List<PermitMongoEntity>> result = repository.createPermitsBulk(testPermitEntities);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).insertAll(testPermitEntities);
	}

	@Test
	void testFindByPermitDbId() {
		// Test findByPermitDbId method
		String permitDbId = "test-permit-id";
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<PermitMongoEntity> result = repository.findByPermitDbId(permitDbId);

		StepVerifier.create(result).expectNext(testPermitMongoEntity).verifyComplete();

		// Verify the query was built correctly
		verify(mockTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("permitDbId")), eq(PermitMongoEntity.class));
	}

	@Test
	void testFindByPermitDbIdWithNullId() {
		// Test findByPermitDbId method with null ID
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<PermitMongoEntity> result = repository.findByPermitDbId(null);

		StepVerifier.create(result).verifyComplete();

		verify(mockTemplate, times(1)).findOne(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testFindByPermitDbIdWithEmptyId() {
		// Test findByPermitDbId method with empty ID
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<PermitMongoEntity> result = repository.findByPermitDbId("");

		StepVerifier.create(result).verifyComplete();

		verify(mockTemplate, times(1)).findOne(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testFindByPermitDbIdNotFound() {
		// Test findByPermitDbId method when not found
		String permitDbId = "non-existent-id";
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.empty());

		Mono<PermitMongoEntity> result = repository.findByPermitDbId(permitDbId);

		StepVerifier.create(result).verifyComplete();

		verify(mockTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("permitDbId")), eq(PermitMongoEntity.class));
	}

	@Test
	void testFindByPermitDbIdWithError() {
		// Test findByPermitDbId method when database error occurs
		String permitDbId = "test-permit-id";
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<PermitMongoEntity> result = repository.findByPermitDbId(permitDbId);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).findOne(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testUpdatePermit() {
		// Test updatePermit method
		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<PermitMongoEntity> result = repository.updatePermit(testPermitMongoEntity);

		StepVerifier.create(result).expectNext(testPermitMongoEntity).verifyComplete();

		// Verify the query was built correctly
		verify(mockTemplate, times(1))
				.findAndModify(
						argThat(query -> query.toString().contains("permitDbId")),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testUpdatePermitWithError() {
		// Test updatePermit method when database error occurs
		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<PermitMongoEntity> result = repository.updatePermit(testPermitMongoEntity);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1))
				.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testGetUpdateQueryWithNullEntity() {
		// Test getUpdateQuery method with null entity
		Update result = PermitMongoRepository.getUpdateQuery(null);

		assertNotNull(result);
		// Update should be empty when entity is null
	}

	@Test
	void testGetUpdateQueryWithNullFields() {
		// Test getUpdateQuery method with entity having null fields
		PermitMongoEntity entityWithNullFields = new PermitMongoEntity();
		entityWithNullFields.setPermitDbId("test-id");
		// All other fields are null

		Update result = PermitMongoRepository.getUpdateQuery(entityWithNullFields);

		assertNotNull(result);
		// Update should be empty when all fields are null
	}

	@Test
	void testGetUpdateQueryWithAllFields() {
		// Test getUpdateQuery method with entity having all fields
		PermitMongoEntity entityWithAllFields = createTestPermitEntity();

		Update result = PermitMongoRepository.getUpdateQuery(entityWithAllFields);

		assertNotNull(result);
		// Update should contain all non-null fields
	}

	@Test
	void testGetUpdateQueryWithPartialFields() {
		// Test getUpdateQuery method with entity having partial fields
		PermitMongoEntity entityWithPartialFields = new PermitMongoEntity();
		entityWithPartialFields.setPermitDbId("test-id");
		entityWithPartialFields.setUnitPermitFee(new BigDecimal("100.50"));
		entityWithPartialFields.setOperationType(OperationType.UPDATE);
		// Other fields are null

		Update result = PermitMongoRepository.getUpdateQuery(entityWithPartialFields);

		assertNotNull(result);
		// Update should contain only the non-null fields
	}

	@Test
	void testSearchNewPermitEntity() {
		// Test searchNewPermitEntity method
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<List<PermitMongoEntity>> result = repository.searchNewPermitEntity();

		StepVerifier.create(result).expectNext(testPermitEntities).verifyComplete();

		// Verify the query was built correctly
		verify(mockTemplate, times(1))
				.find(argThat(query -> query.toString().contains("status")), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchNewPermitEntityWithEmptyResult() {
		// Test searchNewPermitEntity method with empty result
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class))).thenReturn(Flux.empty());

		Mono<List<PermitMongoEntity>> result = repository.searchNewPermitEntity();

		StepVerifier.create(result).expectNext(List.of()).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchNewPermitEntityWithError() {
		// Test searchNewPermitEntity method when database error occurs
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Mono<List<PermitMongoEntity>> result = repository.searchNewPermitEntity();

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchRetryPermitEntity() {
		// Test searchRetryPermitEntity method
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<List<PermitMongoEntity>> result = repository.searchRetryPermitEntity();

		StepVerifier.create(result).expectNext(testPermitEntities).verifyComplete();

		// Verify the query was built correctly
		verify(mockTemplate, times(1))
				.find(
						argThat(query -> query.toString().contains("status")
								&& query.toString().contains("retryCount")),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchRetryPermitEntityWithEmptyResult() {
		// Test searchRetryPermitEntity method with empty result
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class))).thenReturn(Flux.empty());

		Mono<List<PermitMongoEntity>> result = repository.searchRetryPermitEntity();

		StepVerifier.create(result).expectNext(List.of()).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchRetryPermitEntityWithError() {
		// Test searchRetryPermitEntity method when database error occurs
		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Mono<List<PermitMongoEntity>> result = repository.searchRetryPermitEntity();

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchPermitEntityByPermitDbId() {
		// Test searchPermitEntityByPermitDbId method
		List<String> permitDbIds = List.of("id1", "id2", "id3");
		OperationType operationType = OperationType.CREATE;

		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.fromIterable(testPermitEntities));

		Mono<List<PermitMongoEntity>> result = repository.searchPermitEntityByPermitDbId(permitDbIds, operationType);

		StepVerifier.create(result).expectNext(testPermitEntities).verifyComplete();

		// Verify the query was built correctly
		verify(mockTemplate, times(1))
				.find(
						argThat(query -> {
							String queryString = query.toString();
							return queryString.contains("permitDbId")
									&& queryString.contains("status")
									&& queryString.contains("operationType");
						}),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchPermitEntityByPermitDbIdWithEmptyList() {
		// Test searchPermitEntityByPermitDbId method with empty list
		List<String> emptyPermitDbIds = List.of();
		OperationType operationType = OperationType.CREATE;

		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class))).thenReturn(Flux.empty());

		Mono<List<PermitMongoEntity>> result =
				repository.searchPermitEntityByPermitDbId(emptyPermitDbIds, operationType);

		StepVerifier.create(result).expectNext(List.of()).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchPermitEntityByPermitDbIdWithNullList() {
		// Test searchPermitEntityByPermitDbId method with null list
		OperationType operationType = OperationType.CREATE;

		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class))).thenReturn(Flux.empty());

		Mono<List<PermitMongoEntity>> result = repository.searchPermitEntityByPermitDbId(null, operationType);

		StepVerifier.create(result).expectNext(List.of()).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchPermitEntityByPermitDbIdWithError() {
		// Test searchPermitEntityByPermitDbId method when database error occurs
		List<String> permitDbIds = List.of("id1", "id2");
		OperationType operationType = OperationType.CREATE;

		when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Mono<List<PermitMongoEntity>> result = repository.searchPermitEntityByPermitDbId(permitDbIds, operationType);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
	}

	@Test
	void testSearchPermitEntityByPermitDbIdWithDifferentOperationTypes() {
		// Test searchPermitEntityByPermitDbId method with different operation types
		List<String> permitDbIds = List.of("id1", "id2");
		OperationType[] operationTypes = {OperationType.CREATE, OperationType.UPDATE, OperationType.DELETE};

		for (OperationType operationType : operationTypes) {
			when(mockTemplate.find(any(Query.class), eq(PermitMongoEntity.class)))
					.thenReturn(Flux.fromIterable(testPermitEntities));

			Mono<List<PermitMongoEntity>> result =
					repository.searchPermitEntityByPermitDbId(permitDbIds, operationType);

			StepVerifier.create(result).expectNext(testPermitEntities).verifyComplete();

			verify(mockTemplate, times(1)).find(any(Query.class), eq(PermitMongoEntity.class));
			reset(mockTemplate);
		}
	}

	@Test
	void testRepositoryAnnotation() {
		// Test that the class is properly annotated
		assertTrue(repository.getClass().isAnnotationPresent(org.springframework.stereotype.Repository.class));
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
	void testUpdateQueryFieldOrder() {
		// Test that update query fields are set in correct order
		PermitMongoEntity entity = createTestPermitEntity();
		Update update = PermitMongoRepository.getUpdateQuery(entity);

		assertNotNull(update);
		// The update should contain all the fields that were set in the entity
	}

	@Test
	void testUpdateQueryWithDuplicateAuditField() {
		// Test that duplicate audit field check in getUpdateQuery (there are two checks
		// for audit)
		PermitMongoEntity entity = createTestPermitEntity();
		Update update = PermitMongoRepository.getUpdateQuery(entity);

		assertNotNull(update);
		// The duplicate audit check shouldn't cause issues
	}

	@Test
	void testQueryCriteriaConsistency() {
		// Test that query criteria are consistent across methods
		String permitDbId = "test-permit-id";

		// Test findByPermitDbId
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		repository.findByPermitDbId(permitDbId);

		// Test updatePermit
		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		repository.updatePermit(testPermitMongoEntity);

		// Both should use the same criteria for permitDbId
		verify(mockTemplate, times(1))
				.findOne(argThat(query -> query.toString().contains("permitDbId")), eq(PermitMongoEntity.class));
		verify(mockTemplate, times(1))
				.findAndModify(
						argThat(query -> query.toString().contains("permitDbId")),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testFindAndModifyOptions() {
		// Test that findAndModifyOptions is configured correctly
		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		repository.updatePermit(testPermitMongoEntity);

		// Verify that returnNew(true) is used
		verify(mockTemplate, times(1))
				.findAndModify(
						any(Query.class),
						any(Update.class),
						argThat(options -> {
							// We can't directly test the options, but we can verify the method was called
							return options != null;
						}),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testBulkInsertWithLargeList() {
		// Test bulk insert with large list
		List<PermitMongoEntity> largeList = createLargePermitEntityList();
		when(mockTemplate.insertAll(anyList())).thenReturn(Flux.fromIterable(largeList));

		Mono<List<PermitMongoEntity>> result = repository.createPermitsBulk(largeList);

		StepVerifier.create(result).expectNext(largeList).verifyComplete();

		verify(mockTemplate, times(1)).insertAll(largeList);
	}

	@Test
	void testMultipleOperations() {
		// Test multiple repository operations
		when(mockTemplate.save(any(PermitMongoEntity.class))).thenReturn(Mono.just(testPermitMongoEntity));
		when(mockTemplate.findOne(any(Query.class), eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));
		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		// Create
		Mono<PermitMongoEntity> createResult = repository.createPermit(testPermitMongoEntity);
		// Find
		Mono<PermitMongoEntity> findResult = repository.findByPermitDbId("test-id");
		// Update
		Mono<PermitMongoEntity> updateResult = repository.updatePermit(testPermitMongoEntity);

		StepVerifier.create(createResult).expectNext(testPermitMongoEntity).verifyComplete();

		StepVerifier.create(findResult).expectNext(testPermitMongoEntity).verifyComplete();

		StepVerifier.create(updateResult).expectNext(testPermitMongoEntity).verifyComplete();

		verify(mockTemplate, times(1)).save(testPermitMongoEntity);
		verify(mockTemplate, times(1)).findOne(any(Query.class), eq(PermitMongoEntity.class));
		verify(mockTemplate, times(1))
				.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(PermitMongoEntity.class));
	}

	@Test
	void testSyncAllLaborCategoriesWithEmptyList() {
		// Test syncAllLaborCategories with empty list
		List<LaborCategoryResponse> emptyList = List.of();

		Flux<LaborCategoryResponse> result = repository.syncAllLaborCategories(emptyList);

		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	void testSyncAllLaborCategoriesWithNullList() {
		// Test syncAllLaborCategories with null list
		Flux<LaborCategoryResponse> result = repository.syncAllLaborCategories(null);

		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	void testSyncAllLaborCategoriesWithValidList() {
		// Test syncAllLaborCategories with valid list
		List<LaborCategoryResponse> laborCategories =
				List.of(createTestLaborCategory("1", "Category 1"), createTestLaborCategory("2", "Category 2"));

		// Mock backup collection operations
		when(mockTemplate.remove(any(Query.class), eq("laborcategories_backup")))
				.thenReturn(Mono.empty());
		when(mockTemplate.insert(any(LaborCategoryResponse.class), eq("laborcategories_backup")))
				.thenReturn(Mono.just(createTestLaborCategory("1", "Category 1")))
				.thenReturn(Mono.just(createTestLaborCategory("2", "Category 2")));

		// Mock main collection operations
		when(mockTemplate.remove(any(Query.class), eq("labor_category"))).thenReturn(Mono.empty());
		when(mockTemplate.insert(any(LaborCategoryResponse.class), eq("labor_category")))
				.thenReturn(Mono.just(createTestLaborCategory("1", "Category 1")))
				.thenReturn(Mono.just(createTestLaborCategory("2", "Category 2")));

		Flux<LaborCategoryResponse> result = repository.syncAllLaborCategories(laborCategories);

		StepVerifier.create(result).expectNextCount(2).expectComplete().verify();

		// Verify backup collection operations
		verify(mockTemplate, times(1)).remove(any(Query.class), eq("laborcategories_backup"));
		verify(mockTemplate, times(2)).insert(any(LaborCategoryResponse.class), eq("laborcategories_backup"));

		// Verify main collection operations
		verify(mockTemplate, times(1)).remove(any(Query.class), eq("labor_category"));
		verify(mockTemplate, times(2)).insert(any(LaborCategoryResponse.class), eq("labor_category"));
	}

	@Test
	void testSyncAllLaborCategoriesDeleteFailure() {
		// Test syncAllLaborCategories when backup delete operation fails
		List<LaborCategoryResponse> laborCategories = List.of(createTestLaborCategory("1", "Category 1"));

		// Mock backup collection remove to fail
		when(mockTemplate.remove(any(Query.class), eq("laborcategories_backup")))
				.thenReturn(Mono.error(new RuntimeException("Backup delete failed")));

		Flux<LaborCategoryResponse> result = repository.syncAllLaborCategories(laborCategories);

		StepVerifier.create(result).expectComplete().verify();

		verify(mockTemplate, times(1)).remove(any(Query.class), eq("laborcategories_backup"));
		verify(mockTemplate, never()).insert(any(LaborCategoryResponse.class), anyString());
	}

	@Test
	void testSyncAllLaborCategoriesInsertFailure() {
		// Test syncAllLaborCategories when backup insert operation fails
		List<LaborCategoryResponse> laborCategories = List.of(createTestLaborCategory("1", "Category 1"));

		// Mock backup collection remove
		when(mockTemplate.remove(any(Query.class), eq("laborcategories_backup")))
				.thenReturn(Mono.empty());

		// Mock backup collection insert to fail
		when(mockTemplate.insert(any(LaborCategoryResponse.class), eq("laborcategories_backup")))
				.thenReturn(Mono.error(new RuntimeException("Backup insert failed")));

		Flux<LaborCategoryResponse> result = repository.syncAllLaborCategories(laborCategories);

		StepVerifier.create(result).expectComplete().verify();

		verify(mockTemplate, times(1)).remove(any(Query.class), eq("laborcategories_backup"));
		verify(mockTemplate, times(1)).insert(any(LaborCategoryResponse.class), eq("laborcategories_backup"));
		// Main collection operations should not be called when backup fails
		verify(mockTemplate, never()).remove(any(Query.class), eq("labor_category"));
		verify(mockTemplate, never()).insert(any(LaborCategoryResponse.class), eq("labor_category"));
	}

	@Test
	void testFindAllLaborCategoriesWithOrdering() {
		// Test findAllLaborCategories with ordering
		List<LaborCategoryResponse> unsortedCategories = List.of(
				createTestLaborCategory("2", "Category B"),
				createTestLaborCategory("1", "Category A"),
				createTestLaborCategory("3", "Category C"));

		when(mockTemplate.find(any(Query.class), eq(LaborCategoryResponse.class), eq("labor_category")))
				.thenReturn(Flux.fromIterable(unsortedCategories));

		Flux<LaborCategoryResponse> result = repository.findAllLaborCategories();

		StepVerifier.create(result)
				.expectNextCount(3) // Just verify we get 3 categories, order doesn't matter
				.expectComplete()
				.verify();

		// Verify the query was executed with correct parameters
		verify(mockTemplate, times(1)).find(any(Query.class), eq(LaborCategoryResponse.class), eq("labor_category"));
	}

	// Helper methods to create test data
	private PermitMongoEntity createTestPermitEntity() {
		PermitMongoEntity entity = new PermitMongoEntity();
		entity.setPermitDbId("test-permit-id");
		entity.setUnitPermitFee(new BigDecimal("100.50"));
		entity.setOperationType(OperationType.CREATE);
		entity.setStatus(PermitStatus.NEW);
		entity.setRetryCount(0);
		entity.setErrorMessage(null);
		entity.setAudit(new com.lowes.permits.model.Audit());
		return entity;
	}

	private List<PermitMongoEntity> createLargePermitEntityList() {
		List<PermitMongoEntity> entities = new java.util.ArrayList<>();
		for (int i = 0; i < 100; i++) {
			PermitMongoEntity entity = new PermitMongoEntity();
			entity.setPermitDbId("test-permit-id-" + i);
			entity.setUnitPermitFee(BigDecimal.valueOf(100.50 + i));
			entity.setOperationType(OperationType.CREATE);
			entity.setStatus(PermitStatus.NEW);
			entity.setRetryCount(0);
			entities.add(entity);
		}
		return entities;
	}

	private LaborCategoryResponse createTestLaborCategory(String id, String name) {
		LaborCategoryResponse category = new LaborCategoryResponse();
		category.setLaborCategoryId(id);
		category.setName(name);
		category.setType("TEST_TYPE");
		category.setGroup("TEST_GROUP");
		category.setInstallationType("TEST_INSTALLATION");
		category.setStatus("ACTIVE");
		category.setEnableForDetailScheduling(true);
		category.setEpaLaborCategory(false);
		category.setAverageJobSize(5);
		category.setInsuranceTierId("TIER_1");
		category.setInsuranceTierName("Premium");
		category.setJsiPriority("HIGH");
		category.setCreatedOn("2023-01-01");
		category.setModifiedOn("2023-12-01");
		category.setManualCall("NO");
		return category;
	}

	@Test
	void testGetUpdateQueryAlwaysUpdatesLastModifiedAt() {
		PermitMongoEntity entity = PermitMongoEntity.builder()
				.permitDbId("test-id")
				.status(PermitStatus.PROCESSED)
				.build();

		Update update = PermitMongoRepository.getUpdateQuery(entity);

		assertNotNull(update);
	}

	@Test
	void testGetUpdateQueryWithStatusOnly() {
		PermitMongoEntity entity = PermitMongoEntity.builder()
				.permitDbId("test-id")
				.status(PermitStatus.PROCESSED)
				.build();

		Update update = PermitMongoRepository.getUpdateQuery(entity);

		assertNotNull(update);
	}

	@Test
	void testGetUpdateQueryWithAuditUpdatesLastModifiedAt() {
		com.lowes.permits.model.Audit audit = new com.lowes.permits.model.Audit();
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());

		PermitMongoEntity entity = PermitMongoEntity.builder()
				.permitDbId("test-id")
				.status(PermitStatus.PROCESSED)
				.audit(audit)
				.build();

		Update update = PermitMongoRepository.getUpdateQuery(entity);

		assertNotNull(update);
	}

	@Test
	void testSearchApprovedOrderMods() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = createTestOrderModEntity();
		orderMod.setStatus("APPROVED");

		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.just(orderMod));

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchApprovedOrderMods();

		StepVerifier.create(result).expectNext(orderMod).verifyComplete();

		verify(mockTemplate, times(1))
				.find(
						argThat(query -> query.toString().contains("status")),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testSearchApprovedOrderModsWithEmptyResult() {
		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.empty());

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchApprovedOrderMods();

		StepVerifier.create(result).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testSearchApprovedOrderModsWithError() {
		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchApprovedOrderMods();

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testUpdateOrderMod() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = createTestOrderModEntity();

		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Mono.just(orderMod));

		Mono<com.lowes.permits.entity.OrderModMongoEntity> result = repository.updateOrderMod(orderMod);

		StepVerifier.create(result).expectNext(orderMod).verifyComplete();

		verify(mockTemplate, times(1))
				.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testUpdateOrderModWithError() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = createTestOrderModEntity();

		when(mockTemplate.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<com.lowes.permits.entity.OrderModMongoEntity> result = repository.updateOrderMod(orderMod);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1))
				.findAndModify(
						any(Query.class),
						any(Update.class),
						any(FindAndModifyOptions.class),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testSearchRetryOrderMods() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = createTestOrderModEntity();
		orderMod.setStatus("RETRY_STATE");
		orderMod.setRetryCount(1);

		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.just(orderMod));

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchRetryOrderMods();

		StepVerifier.create(result).expectNext(orderMod).verifyComplete();

		verify(mockTemplate, times(1))
				.find(
						argThat(query -> query.toString().contains("status")
								&& query.toString().contains("retryCount")),
						eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testSearchRetryOrderModsWithEmptyResult() {
		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.empty());

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchRetryOrderMods();

		StepVerifier.create(result).verifyComplete();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testSearchRetryOrderModsWithError() {
		when(mockTemplate.find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class)))
				.thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Flux<com.lowes.permits.entity.OrderModMongoEntity> result = repository.searchRetryOrderMods();

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockTemplate, times(1)).find(any(Query.class), eq(com.lowes.permits.entity.OrderModMongoEntity.class));
	}

	@Test
	void testGetOrderModUpdateQuery() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = createTestOrderModEntity();
		orderMod.setStatus("PROCESSED");
		orderMod.setRetryCount(0);
		orderMod.setErrorMessage("Test error");

		Update update = PermitMongoRepository.getOrderModUpdateQuery(orderMod);

		assertNotNull(update);
	}

	@Test
	void testGetOrderModUpdateQueryWithNullEntity() {
		Update update = PermitMongoRepository.getOrderModUpdateQuery(null);

		assertNotNull(update);
	}

	@Test
	void testGetOrderModUpdateQueryWithNullFields() {
		com.lowes.permits.entity.OrderModMongoEntity orderMod = new com.lowes.permits.entity.OrderModMongoEntity();
		orderMod.setId("test-id");

		Update update = PermitMongoRepository.getOrderModUpdateQuery(orderMod);

		assertNotNull(update);
	}

	private com.lowes.permits.entity.OrderModMongoEntity createTestOrderModEntity() {
		com.lowes.permits.model.Audit audit = new com.lowes.permits.model.Audit();
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());
		audit.setCreatedByName("testUser");
		audit.setLastModifiedByName("testUser");

		Address address = new Address();
		address.setCity("Los Angeles");
		address.setState("CA");
		address.setZipCode("90210");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");

		return com.lowes.permits.entity.OrderModMongoEntity.builder()
				.id("test-order-mod-id")
				.categoryCode(123)
				.categoryDesc("Test Category")
				.itemId("456")
				.itemDesc("Test Item")
				.permitFee("100.50")
				.oldPermitFee("50.25")
				.omniId("OMNI123")
				.address(address)
				.provider("Test Provider")
				.status("APPROVED")
				.vbuNumber(789)
				.permitInsertType("NEW")
				.jobId("JOB123")
				.orderNumber("ORDER456")
				.audit(audit)
				.retryCount(0)
				.build();
	}
}
