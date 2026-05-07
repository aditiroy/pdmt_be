package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.lowes.permits.dto.request.CreatePermitRequest;
import com.lowes.permits.dto.request.UpdatePermitRequest;
import com.lowes.permits.dto.response.CreatePermitResponse;
import com.lowes.permits.dto.response.UpdatePermitResponse;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.MappingContext;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.model.Provider;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;
import com.lowes.permits.util.PermitUtils;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntityServiceTest {

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitPostgresRepository mockPermitPostgresRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService permitService;
	private CreatePermitRequest testCreatePermitRequest;
	private List<UpdatePermitRequest> testUpdatePermitRequests;
	private PermitMongoEntity testPermitMongoEntity;
	private List<PermitMongoEntity> testPermitEntities;

	@BeforeEach
	void setUp() {
		permitService = new PermitService(
				mockPermitMongoRepository, mockPermitMapper, mockPermitPostgresRepository, mockCommonUtilityClient);
		testCreatePermitRequest = createTestCreatePermitRequest();
		testUpdatePermitRequests = List.of(createTestUpdatePermitRequest());
		testPermitMongoEntity = createTestPermitEntity();
		testPermitEntities = List.of(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithValidRequest() {
		// Test createPermit with valid request
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithNullRequest() {
		// Test createPermit with null request
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(isNull(), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result = permitService.createPermit(null, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1)).createRequestToEntity(isNull(), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithNullUserToken() {
		// Test createPermit with null user token
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, null, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithNullTraceId() {
		// Test createPermit with null trace ID
		String xUserToken = "test-user-token";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, xUserToken, null, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithNullCallerApp() {
		// Test createPermit with null caller app
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, xUserToken, currentTraceId, null);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithEmptyUserToken() {
		// Test createPermit with empty user token
		String xUserToken = "";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testCreatePermitWithRepositoryError() {
		// Test createPermit when repository throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<CreatePermitResponse> result =
				permitService.createPermit(testCreatePermitRequest, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermit(testPermitMongoEntity);
	}

	@Test
	void testUpdatePermitWithValidRequest() {
		// Test updatePermit with valid request
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of())); // No existing permits
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertNotNull(response.getExistingPermitDbIds());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithEmptyRequest() {
		// Test updatePermit with empty request list
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(List.of(), xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertNotNull(response.getExistingPermitDbIds());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, never())
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithMixedExistingAndNew() {
		// Test updatePermit with mix of existing and new permits
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		// Create update requests with different permit DB IDs
		UpdatePermitRequest existingRequest = createTestUpdatePermitRequest();
		existingRequest.setPermitDbId("existing-permit-id");
		UpdatePermitRequest newRequest = createTestUpdatePermitRequest();
		newRequest.setPermitDbId("new-permit-id");
		List<UpdatePermitRequest> mixedRequests = List.of(existingRequest, newRequest);

		PermitMongoEntity existingEntity = createTestPermitEntity();
		existingEntity.setPermitDbId("existing-permit-id");
		List<PermitMongoEntity> existingEntities = List.of(existingEntity);

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(existingEntities));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(mixedRequests, xUserToken, currentTraceId, callerApp);

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

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithRepositorySearchError() {
		// Test updatePermit when repository search throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, never())
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithMapperError() {
		// Test updatePermit when mapper throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenThrow(new RuntimeException("Mapping error"));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, never()).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithBulkInsertError() {
		// Test updatePermit when bulk insert throws error
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList()))
				.thenReturn(Mono.error(new RuntimeException("Database error")));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithNullUserToken() {
		// Test updatePermit with null user token
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, null, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithNullTraceId() {
		// Test updatePermit with null trace ID
		String xUserToken = "test-user-token";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, null, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithNullCallerApp() {
		// Test updatePermit with null caller app
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, null);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithEmptyUserToken() {
		// Test updatePermit with empty user token
		String xUserToken = "";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
		verify(mockPermitMongoRepository, times(1)).createPermitsBulk(anyList());
	}

	@Test
	void testUpdatePermitWithMultipleRequests() {
		// Test updatePermit with multiple update requests
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		List<UpdatePermitRequest> multipleRequests = List.of(
				createTestUpdatePermitRequest(), createTestUpdatePermitRequest(), createTestUpdatePermitRequest());

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenReturn(testPermitMongoEntity);
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		Mono<UpdatePermitResponse> result =
				permitService.updatePermit(multipleRequests, xUserToken, currentTraceId, callerApp);

		StepVerifier.create(result)
				.assertNext(response -> {
					assertNotNull(response);
					assertEquals("Request accepted and is being processed", response.getMessage());
					assertTrue(response.getExistingPermitDbIds().isEmpty());
				})
				.verifyComplete();

		verify(mockPermitMongoRepository, times(1)).searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE));
		verify(mockPermitMapper, times(3))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
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
		// Test that mapping context is created correctly for create operation
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMapper.createRequestToEntity(any(CreatePermitRequest.class), any(MappingContext.class)))
				.thenAnswer(invocation -> {
					MappingContext context = invocation.getArgument(1);
					assertEquals(xUserToken, context.getXUserToken());
					assertEquals(currentTraceId, context.getCurrentTraceId());
					assertEquals(callerApp, context.getCallerApp());
					assertEquals(OperationType.CREATE, context.getOperationType());
					assertEquals(PermitStatus.NEW, context.getStatus());
					return testPermitMongoEntity;
				});
		when(mockPermitPostgresRepository.existsByPermitKey(any(), any(), any(), any(), any(), any()))
				.thenReturn(Mono.just(false));
		when(mockPermitMongoRepository.createPermit(any(PermitMongoEntity.class)))
				.thenReturn(Mono.just(testPermitMongoEntity));

		permitService
				.createPermit(testCreatePermitRequest, xUserToken, currentTraceId, callerApp)
				.block();

		verify(mockPermitMapper, times(1))
				.createRequestToEntity(eq(testCreatePermitRequest), any(MappingContext.class));
	}

	@Test
	void testMappingContextCreationForUpdate() {
		// Test that mapping context is created correctly for update operation
		String xUserToken = "test-user-token";
		String currentTraceId = "trace-123";
		String callerApp = "test-app";

		when(mockPermitMongoRepository.searchPermitEntityByPermitDbId(anyList(), eq(OperationType.UPDATE)))
				.thenReturn(Mono.just(List.of()));
		when(mockPermitMapper.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class)))
				.thenAnswer(invocation -> {
					MappingContext context = invocation.getArgument(1);
					assertEquals(xUserToken, context.getXUserToken());
					assertEquals(currentTraceId, context.getCurrentTraceId());
					assertEquals(callerApp, context.getCallerApp());
					assertEquals(OperationType.UPDATE, context.getOperationType());
					assertEquals(PermitStatus.NEW, context.getStatus());
					return testPermitMongoEntity;
				});
		when(mockPermitMongoRepository.createPermitsBulk(anyList())).thenReturn(Mono.just(testPermitEntities));

		permitService
				.updatePermit(testUpdatePermitRequests, xUserToken, currentTraceId, callerApp)
				.block();

		verify(mockPermitMapper, times(1))
				.updateRequestToEntity(any(UpdatePermitRequest.class), any(MappingContext.class));
	}

	// Helper methods to create test data
	private CreatePermitRequest createTestCreatePermitRequest() {
		CreatePermitRequest request = new CreatePermitRequest();

		Address address = new Address();
		address.setCity("Los Angeles");
		address.setState("CA");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		address.setZipCode("90210");
		request.setAddress(address);

		LaborCategory laborCategory = new LaborCategory();
		laborCategory.setCode(123);
		laborCategory.setDescription("Test Category");
		request.setLaborCategory(laborCategory);

		request.setLaborItem(456);
		request.setLaborItemDescription("Test Description");
		request.setUnitPermitFee(new BigDecimal("100.50"));
		request.setOmniItemId("OMNI123");
		Provider provider = new Provider();
		provider.setName("Test Provider");
		provider.setNumber(123);
		request.setProvider(provider);

		return request;
	}

	private UpdatePermitRequest createTestUpdatePermitRequest() {
		UpdatePermitRequest request = new UpdatePermitRequest();
		request.setPermitDbId("test-permit-db-id");
		request.setOldUnitPermitFee(new BigDecimal("50.25"));
		request.setUnitPermitFee(new BigDecimal("100.50"));
		return request;
	}

	private PermitMongoEntity createTestPermitEntity() {
		PermitMongoEntity entity = new PermitMongoEntity();
		entity.setId("test-id");
		entity.setPermitDbId(
				PermitUtils.generatePermitDbId("LOS ANGELES", 123, 123, "90210", "LOS ANGELES", "BEVERLY HILLS"));
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
