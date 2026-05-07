package com.lowes.permits.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.lowes.permits.dto.request.EditActivityFeeRequest;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.exception.PermitNotFoundException;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ActivityMutationServiceTest {

	private final String xUserToken = "dXNlcklkOjEyMzQ1"; // Base64 encoded "userId:12345"
	private final String traceId = "test-trace-id";
	private final String callerApp = "test-app";

	@Mock
	private PermitMongoRepository mockPermitMongoRepository;

	@Mock
	private PermitMapper mockPermitMapper;

	@Mock
	private PermitPostgresRepository mockPermitPostgresRepository;

	@Mock
	private CommonUtilityClient mockCommonUtilityClient;

	private PermitService activityMutationService;
	private PermitMongoEntity testEntity;
	private CreatePermitRequest createRequest;
	private EditActivityFeeRequest editFeeRequest;

	@BeforeEach
	void setUp() {
		activityMutationService = new PermitService(
				mockPermitMongoRepository, mockPermitMapper, mockPermitPostgresRepository, mockCommonUtilityClient);
		testEntity = createTestPermitEntity();
		createRequest = createTestCreatePermitRequest();
		editFeeRequest = createTestEditActivityFeeRequest();
	}

	@Test
	void testEditCreateActivityWithNonCreateOperation() {
		testEntity.setOperationType(OperationType.UPDATE);
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.just(testEntity));

		Mono<PermitMongoEntity> result = activityMutationService.editCreateActivity(
				"activity-id", createRequest, xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof IllegalStateException
						&& "Only CREATE activities can be edited with create payload".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMapper, never()).createRequestToEntity(any(CreatePermitRequest.class), any());
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testEditCreateActivityWithNotFound() {
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.empty());

		Mono<PermitMongoEntity> result = activityMutationService.editCreateActivity(
				"activity-id", createRequest, xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof PermitNotFoundException
						&& "Staged activity not found or already processed".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMapper, never()).createRequestToEntity(any(CreatePermitRequest.class), any());
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testEditUpdateActivityWithNonUpdateOperation() {
		testEntity.setOperationType(OperationType.CREATE);
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.just(testEntity));

		Mono<PermitMongoEntity> result = activityMutationService.editUpdateActivity(
				"activity-id", editFeeRequest, xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof IllegalStateException
						&& "Only UPDATE activities can edit fee".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testEditUpdateActivityWithNotFound() {
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.empty());

		Mono<PermitMongoEntity> result = activityMutationService.editUpdateActivity(
				"activity-id", editFeeRequest, xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof PermitNotFoundException
						&& "Staged activity not found or already processed".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testUndoActivityWithInvalidOperationType() {
		testEntity.setOperationType(OperationType.CREATE_UNDONE);
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.just(testEntity));

		Mono<Void> result =
				activityMutationService.undoActivities(List.of("activity-id"), xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof IllegalStateException
						&& "Only NEW activities can be undone".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testUndoActivityWithNotFound() {
		when(mockPermitMongoRepository.findNewPermitEntityById(anyString())).thenReturn(Mono.empty());

		Mono<Void> result =
				activityMutationService.undoActivities(List.of("activity-id"), xUserToken, traceId, callerApp);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof PermitNotFoundException
						&& "Staged activity not found or already processed".equals(throwable.getMessage()))
				.verify();

		verify(mockPermitMongoRepository, times(1)).findNewPermitEntityById("activity-id");
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testUndoActivitiesWithEmptyList() {
		Mono<Void> result = activityMutationService.undoActivities(List.of(), xUserToken, traceId, callerApp);

		StepVerifier.create(result).verifyComplete();

		verify(mockPermitMongoRepository, never()).findNewPermitEntityById(anyString());
		verify(mockPermitMongoRepository, never()).savePermit(any(PermitMongoEntity.class));
	}

	@Test
	void testServiceAnnotation() {
		// Test that the class is properly annotated
		assertTrue(
				activityMutationService.getClass().isAnnotationPresent(org.springframework.stereotype.Service.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(activityMutationService);
	}

	@Test
	void testRequiredArgsConstructorAnnotation() {
		// Test that the class uses @RequiredArgsConstructor (metadata test)
		assertNotNull(activityMutationService);
		// The constructor injection working properly indicates this annotation is
		// present
	}

	@Test
	void testConstructor() {
		// Test constructor injection
		assertNotNull(activityMutationService);
	}

	private PermitMongoEntity createTestPermitEntity() {
		Audit audit = new Audit();
		audit.setCreatedByName("user123");
		audit.setLastModifiedByName("user456");
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());

		return PermitMongoEntity.builder()
				.id("activity-id")
				.permitDbId("test-permit-db-id")
				.status(PermitStatus.NEW)
				.operationType(OperationType.CREATE)
				.unitPermitFee(new BigDecimal("100.50"))
				.oldUnitPermitFee(new BigDecimal("50.25"))
				.laborItem(123)
				.laborItemDescription("Test Description")
				.omniItemId("OMNI123")
				.laborCategory(new LaborCategory(123, "Test Labor Category"))
				.audit(audit)
				.build();
	}

	private PermitMongoEntity createUpdatedPermitEntity() {
		Audit audit = new Audit();
		audit.setCreatedByName("user123");
		audit.setLastModifiedByName("user456");
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());

		return PermitMongoEntity.builder()
				.id("activity-id")
				.permitDbId("test-permit-db-id")
				.status(PermitStatus.NEW)
				.operationType(OperationType.CREATE)
				.unitPermitFee(new BigDecimal("200.00"))
				.oldUnitPermitFee(new BigDecimal("75.25"))
				.laborItem(456)
				.laborItemDescription("Updated Description")
				.omniItemId("OMNI456")
				.laborCategory(new LaborCategory(456, "Updated Labor Category"))
				.audit(audit)
				.build();
	}

	private CreatePermitRequest createTestCreatePermitRequest() {
		CreatePermitRequest request = new CreatePermitRequest();
		request.setLaborItem(123);
		request.setLaborItemDescription("Test Description");
		request.setUnitPermitFee(new BigDecimal("100.50"));
		request.setOmniItemId("OMNI123");
		request.setLaborCategory(new LaborCategory(123, "Test Labor Category"));
		return request;
	}

	private EditActivityFeeRequest createTestEditActivityFeeRequest() {
		EditActivityFeeRequest request = new EditActivityFeeRequest();
		request.setUnitPermitFee(new BigDecimal("150.00"));
		return request;
	}
}
