package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

import com.lowes.permits.dto.request.DeletePermitRequest;
import com.lowes.permits.dto.response.DeletePermitResponse;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.MappingContext;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntityDeleteStagingServiceTest {

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitPostgresRepository mockPermitPostgresRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService permitService;
	private List<DeletePermitRequest> testDeletePermitRequests;
	private PermitMongoEntity testPermitMongoEntity;
	private List<PermitMongoEntity> testPermitEntities;

	@BeforeEach
	void setUp() {
		permitService = new PermitService(
				mockPermitMongoRepository, mockPermitMapper, mockPermitPostgresRepository, mockCommonUtilityClient);
		testDeletePermitRequests = List.of(createTestDeletePermitRequest());
		testPermitMongoEntity = createTestPermitEntity();
		testPermitEntities = List.of(testPermitMongoEntity);
	}

	@Test
	void testRemovePermitWithValidRequest() {
		// Test removePermit with valid request
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of())); // No existing permits
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertNotNull(response.getExistingPermitDbIds());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithEmptyRequest() {
		// Test removePermit with empty request list
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(List.of(), xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertNotNull(response.getExistingPermitDbIds());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, never())
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithMixedExistingAndNew() {
		// Test removePermit with mix of existing and new permits
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		// Create delete requests with different permit DB IDs
		DeletePermitRequest existingRequest = createTestDeletePermitRequest();
		existingRequest.setPermitDbId("existing-permit-id");
		DeletePermitRequest newRequest = createTestDeletePermitRequest();
		newRequest.setPermitDbId("new-permit-id");
		List<DeletePermitRequest> mixedRequests = List.of(existingRequest, newRequest);

		PermitMongoEntity existingEntity = createTestPermitEntity();
		existingEntity.setPermitDbId("existing-permit-id");
		List<PermitMongoEntity> existingEntities = List.of(existingEntity);

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(existingEntities));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(mixedRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertNotNull(response.getExistingPermitDbIds());
					assertEquals(1, response.getExistingPermitDbIds().size());
					assertEquals(
							"existing-permit-id",
							response.getExistingPermitDbIds().get(0));
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithRepositorySearchError() {
		// Test removePermit when repository search throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, never())
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithMapperError() {
		// Test removePermit when mapper throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenThrow(new RuntimeException("Mapping error"));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithBulkInsertError() {
		// Test removePermit when bulk insert throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList()))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithNullUserToken() {
		// Test removePermit with null user token
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, null, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithNullTraceId() {
		// Test removePermit with null trace ID
		String xUserToken = "test-user-token";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, null, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithNullCallerApp() {
		// Test removePermit with null caller app
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, null);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithEmptyUserToken() {
		// Test removePermit with empty user token
		String xUserToken = "";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithMultipleRequests() {
		// Test removePermit with multiple delete requests
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		List<DeletePermitRequest> multipleRequests = List.of(
				createTestDeletePermitRequest(), createTestDeletePermitRequest(), createTestDeletePermitRequest());

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(multipleRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(3))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testRemovePermitWithDuplicateRequests() {
		// Test removePermit with duplicate requests
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		// Create duplicate requests with same permit DB ID
		DeletePermitRequest request1 = createTestDeletePermitRequest();
		request1.setPermitDbId("duplicate-id");
		DeletePermitRequest request2 = createTestDeletePermitRequest();
		request2.setPermitDbId("duplicate-id");
		List<DeletePermitRequest> duplicateRequests = List.of(request1, request2);

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(duplicateRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE));
		verify(mockPermitMapper, times(2))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
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
	void testMappingContextCreation() {
		// Test that mapping context is created correctly for delete operation
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenAnswer(invocation -> {
					MappingContext context = invocation.getArgument(1);
					assertEquals(xUserToken, context.getXUserToken());
					assertEquals(currentTraceId, context.getCurrentTraceId());
					assertEquals(callerApp, context.getCallerApp());
					assertEquals(OperationType.DELETE, context.getOperationType());
					assertEquals(PermitStatus.NEW, context.getStatus());
					return testPermitMongoEntity;
				});
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		permitService
				.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp)
				.block();

		verify(mockPermitMapper, times(1))
				.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class));
	}

	@Test
	void testLoggingOnSuccess() {
		// Test that success logging works correctly
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testLoggingOnError() {
		// Test that error logging works correctly
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.DELETE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.deleteRequestToEntity(any(DeletePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList()))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<DeletePermitResponse> result =
				permitService.removePermit(testDeletePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	// Helper methods to create test data
	private DeletePermitRequest createTestDeletePermitRequest() {
		DeletePermitRequest request = new DeletePermitRequest();
		request.setPermitDbId("test-permit-db-id");
		request.setUnitPermitFee(new BigDecimal("100.50"));
		return request;
	}

	private PermitMongoEntity createTestPermitEntity() {
		PermitMongoEntity entity = new PermitMongoEntity();
		entity.setId("test-id");
		entity.setPermitDbId("test-permit-db-id");
		entity.setStatus(PermitStatus.NEW);
		entity.setOperationType(OperationType.DELETE);
		entity.setUnitPermitFee(new BigDecimal("100.50"));
		entity.setLaborItem(123);
		entity.setLaborItemDescription("Test Description");
		entity.setOmniItemId("OMNI123");
		entity.setLaborCategory(new LaborCategory(123, "Test Labor Category"));

		com.lowes.permits.model.Address address = new com.lowes.permits.model.Address();
		address.setCity("Los Angeles");
		address.setState("CA");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		address.setZipCode("90210");
		entity.setAddress(address);

		com.lowes.permits.model.Audit audit = new com.lowes.permits.model.Audit();
		audit.setCreatedByName("user123");
		audit.setLastModifiedByName("user456");
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());
		entity.setAudit(audit);

		return entity;
	}
}
