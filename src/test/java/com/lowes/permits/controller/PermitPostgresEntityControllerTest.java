package com.lowes.permits.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.lowes.permits.dto.request.BulkUndoActivitiesRequest;
import com.lowes.permits.dto.request.CreateActivityRequest;
import com.lowes.permits.dto.request.CreatePermitRequest;
import com.lowes.permits.dto.request.DeletePermitRequest;
import com.lowes.permits.dto.request.EditActivityFeeRequest;
import com.lowes.permits.dto.request.EditActivityRequest;
import com.lowes.permits.dto.request.SearchActivityRequest;
import com.lowes.permits.dto.request.SearchFilterRequest;
import com.lowes.permits.dto.request.SearchOrderModRequest;
import com.lowes.permits.dto.request.SearchPermitRequest;
import com.lowes.permits.dto.request.UpdatePermitRequest;
import com.lowes.permits.dto.response.ActivityMutationResponse;
import com.lowes.permits.dto.response.CreatePermitResponse;
import com.lowes.permits.dto.response.DeletePermitResponse;
import com.lowes.permits.dto.response.SearchActivityResponse;
import com.lowes.permits.dto.response.SearchFilterResponse;
import com.lowes.permits.dto.response.SearchOrderModResponse;
import com.lowes.permits.dto.response.SearchPermitResponse;
import com.lowes.permits.dto.response.SmartSearchResponse;
import com.lowes.permits.dto.response.UpdatePermitResponse;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.service.PermitExportService;
import com.lowes.permits.service.PermitSchedulerService;
import com.lowes.permits.service.PermitService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresEntityControllerTest {

	@Mock
	private PermitService permitService;

	@Mock
	private PermitSchedulerService permitSchedulerService;

	@Mock
	private PermitExportService permitExportService;

	@InjectMocks
	private PermitController permitController;

	private CreatePermitRequest createPermitRequest;
	private CreateActivityRequest createCreateActivityRequest;
	private CreateActivityRequest updateCreateActivityRequest;
	private CreateActivityRequest deleteCreateActivityRequest;
	private CreateActivityRequest unsupportedCreateActivityRequest;
	private SearchPermitRequest searchPermitRequest;
	private List<DeletePermitRequest> deletePermitRequest;
	private List<UpdatePermitRequest> updatePermitRequest;
	private SearchActivityRequest searchActivityRequest;
	private SearchFilterRequest searchFilterRequest;
	private SearchOrderModRequest searchOrderModRequest;
	private EditActivityRequest editActivityRequest;
	private BulkUndoActivitiesRequest bulkUndoActivitiesRequest;

	@BeforeEach
	void setUp() {
		createPermitRequest = new CreatePermitRequest();
		searchPermitRequest = new SearchPermitRequest();
		DeletePermitRequest deleteReq = new DeletePermitRequest();
		deleteReq.setPermitDbId("test-permit-id");
		deleteReq.setUnitPermitFee(new BigDecimal("100.00"));
		deletePermitRequest = List.of(deleteReq);
		updatePermitRequest = List.of(new UpdatePermitRequest());
		createCreateActivityRequest = new CreateActivityRequest();
		createCreateActivityRequest.setCreateRequest(createPermitRequest);
		updateCreateActivityRequest = new CreateActivityRequest();
		updateCreateActivityRequest.setUpdateRequest(updatePermitRequest);
		deleteCreateActivityRequest = new CreateActivityRequest();
		deleteCreateActivityRequest.setDeleteRequest(deletePermitRequest);
		unsupportedCreateActivityRequest = new CreateActivityRequest();
		unsupportedCreateActivityRequest.setCreateRequest(createPermitRequest);
		searchActivityRequest = new SearchActivityRequest();
		searchFilterRequest = new SearchFilterRequest();
		searchOrderModRequest = new SearchOrderModRequest();

		// Setup EditActivityRequest
		editActivityRequest = new EditActivityRequest();
		editActivityRequest.setOperationType(OperationType.CREATE);
		editActivityRequest.setCreateRequest(createPermitRequest);

		// Setup BulkUndoActivitiesRequest
		bulkUndoActivitiesRequest = new BulkUndoActivitiesRequest();
		bulkUndoActivitiesRequest.setActivityIds(List.of("activity-1", "activity-2"));
	}

	@Test
	void testTriggerSchedulerWithRetryType() {
		doNothing().when(permitSchedulerService).retryUpsertOrDeletePermitEntity();

		ResponseEntity<String> response = permitController.postPermitTriggerScheduler("RETRY");

		assertEquals(ResponseEntity.ok("Retry permit scheduler triggered manually"), response);
		verify(permitSchedulerService, times(1)).retryUpsertOrDeletePermitEntity();
		verify(permitSchedulerService, never()).upsertOrDeletePermitEntity();
	}

	@Test
	void testTriggerSchedulerWithLaborCategory() {
		doNothing().when(permitSchedulerService).syncLaborCategoryData();

		ResponseEntity<String> response = permitController.postPermitTriggerScheduler("LABOR_CATEGORY");

		assertEquals(ResponseEntity.ok("Labor Category Sync triggered manually"), response);
		verify(permitSchedulerService, times(1)).syncLaborCategoryData();
		verify(permitSchedulerService, never()).upsertOrDeletePermitEntity();
	}

	@Test
	void testTriggerSchedulerWithDefaultType() {
		doNothing().when(permitSchedulerService).upsertOrDeletePermitEntity();

		ResponseEntity<String> response = permitController.postPermitTriggerScheduler(null);

		assertEquals(ResponseEntity.ok("New permit scheduler triggered manually"), response);
		verify(permitSchedulerService, times(1)).upsertOrDeletePermitEntity();
		verify(permitSchedulerService, never()).retryUpsertOrDeletePermitEntity();
	}

	@Test
	void testSearch() {
		SearchPermitResponse expectedResponse = new SearchPermitResponse();
		when(permitService.searchPermit(any(SearchPermitRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchPermitResponse>> result =
				permitController.postPermitSearch(searchPermitRequest, "test-token", "trace-id", "caller-app");

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchPermit(any(SearchPermitRequest.class));
	}

	@Test
	void testSyncPermitSearchDictionary() {
		when(permitService.syncPermitSearchDictionary()).thenReturn(Mono.empty());

		ResponseEntity<String> result = permitController.postPermitSearchDictionarySync();

		assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
		assertEquals("Processing in background", result.getBody());

		verify(permitService, times(1)).syncPermitSearchDictionary();
	}

	@Test
	void testSmartSearch() {
		SmartSearchResponse expectedResponse = new SmartSearchResponse();
		when(permitService.smartSearch(anyString())).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SmartSearchResponse>> result =
				permitController.getPermitSmartSearch("test-text", "test-token", "trace-id", "caller-app");

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).smartSearch(anyString());
	}

	@Test
	void testSearchActivities() {
		SearchActivityResponse expectedResponse = new SearchActivityResponse();
		when(permitService.searchActivities(any(SearchActivityRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchActivityResponse>> result = permitController.postPermitActivitiesSearch(
				searchActivityRequest, "test-token", "trace-id", "caller-app");

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchActivities(any(SearchActivityRequest.class));
	}

	@Test
	void testSearchOrderMods() {
		SearchOrderModResponse expectedResponse = new SearchOrderModResponse();
		when(permitService.searchOrderMods(any(SearchOrderModRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchOrderModResponse>> result =
				permitController.postOrderModsSearch(searchOrderModRequest, "test-token", "trace-id", "caller-app");

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchOrderMods(any(SearchOrderModRequest.class));
	}

	@Test
	void testSearchFilter() {
		SearchFilterResponse expectedResponse = new SearchFilterResponse();
		when(permitService.searchFilter(any(SearchFilterRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchFilterResponse>> result =
				permitController.postPermitSearchFilter(searchFilterRequest, "trace-id");

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchFilter(any(SearchFilterRequest.class));
	}

	@Test
	void testSearchWithNullHeaders() {
		SearchPermitResponse expectedResponse = new SearchPermitResponse();
		when(permitService.searchPermit(any(SearchPermitRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchPermitResponse>> result =
				permitController.postPermitSearch(searchPermitRequest, null, null, null);

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchPermit(any(SearchPermitRequest.class));
	}

	@Test
	void testEditActivityWithCreateOperation() {
		when(permitService.editCreateActivity(
						anyString(), any(CreatePermitRequest.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.empty());

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.patchPermitActivityById(
				"activity-1", "test-token", "trace-id", "caller-app", editActivityRequest);

		StepVerifier.create(result)
				.expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK
						&& response.getBody() != null
						&& "Request accepted and is being processed"
								.equals(response.getBody().getMessage()))
				.verifyComplete();

		verify(permitService, times(1))
				.editCreateActivity(anyString(), any(CreatePermitRequest.class), anyString(), anyString(), anyString());
	}

	@Test
	void testEditActivityWithUpdateOperation() {
		EditActivityFeeRequest updateRequest = new EditActivityFeeRequest();
		editActivityRequest.setOperationType(OperationType.UPDATE);
		editActivityRequest.setCreateRequest(null);
		editActivityRequest.setUpdateRequest(updateRequest);

		when(permitService.editUpdateActivity(
						anyString(), any(EditActivityFeeRequest.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.empty());

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.patchPermitActivityById(
				"activity-1", "test-token", "trace-id", "caller-app", editActivityRequest);

		StepVerifier.create(result)
				.expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK
						&& response.getBody() != null
						&& "Request accepted and is being processed"
								.equals(response.getBody().getMessage()))
				.verifyComplete();

		verify(permitService, times(1))
				.editUpdateActivity(
						anyString(), any(EditActivityFeeRequest.class), anyString(), anyString(), anyString());
	}

	@Test
	void testEditActivityWithUnsupportedOperation() {
		editActivityRequest.setOperationType(OperationType.DELETE);

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.patchPermitActivityById(
				"activity-1", "test-token", "trace-id", "caller-app", editActivityRequest);

		StepVerifier.create(result)
				.expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException
						&& "Editing supported only for CREATE and UPDATE activities".equals(throwable.getMessage()))
				.verify();
	}

	@Test
	void testUndoActivities() {
		when(permitService.undoActivities(any(List.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.empty());

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.postPermitActivitiesUndo(
				"test-token", "trace-id", "caller-app", bulkUndoActivitiesRequest);

		StepVerifier.create(result)
				.expectNextMatches(response -> response.getStatusCode() == HttpStatus.OK
						&& response.getBody() != null
						&& "Request accepted and is being processed"
								.equals(response.getBody().getMessage()))
				.verifyComplete();

		verify(permitService, times(1)).undoActivities(any(List.class), anyString(), anyString(), anyString());
	}

	@Test
	void testEditActivityWithError() {
		when(permitService.editCreateActivity(
						anyString(), any(CreatePermitRequest.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.error(new RuntimeException("Service error")));

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.patchPermitActivityById(
				"activity-1", "test-token", "trace-id", "caller-app", editActivityRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1))
				.editCreateActivity(anyString(), any(CreatePermitRequest.class), anyString(), anyString(), anyString());
	}

	@Test
	void testUndoActivitiesWithError() {
		when(permitService.undoActivities(any(List.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.error(new RuntimeException("Service error")));

		Mono<ResponseEntity<ActivityMutationResponse>> result = permitController.postPermitActivitiesUndo(
				"test-token", "trace-id", "caller-app", bulkUndoActivitiesRequest);

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1)).undoActivities(any(List.class), anyString(), anyString(), anyString());
	}

	@Test
	void testSmartSearchWithError() {
		when(permitService.smartSearch(anyString())).thenReturn(Mono.error(new RuntimeException("Smart search error")));

		Mono<ResponseEntity<SmartSearchResponse>> result =
				permitController.getPermitSmartSearch("test-text", "test-token", "trace-id", "caller-app");

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1)).smartSearch(anyString());
	}

	@Test
	void testSearchActivitiesWithError() {
		when(permitService.searchActivities(any(SearchActivityRequest.class)))
				.thenReturn(Mono.error(new RuntimeException("Activity search error")));

		Mono<ResponseEntity<SearchActivityResponse>> result = permitController.postPermitActivitiesSearch(
				searchActivityRequest, "test-token", "trace-id", "caller-app");

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1)).searchActivities(any(SearchActivityRequest.class));
	}

	@Test
	void testSearchOrderModsWithError() {
		when(permitService.searchOrderMods(any(SearchOrderModRequest.class)))
				.thenReturn(Mono.error(new RuntimeException("Order mod search error")));

		Mono<ResponseEntity<SearchOrderModResponse>> result =
				permitController.postOrderModsSearch(searchOrderModRequest, "test-token", "trace-id", "caller-app");

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1)).searchOrderMods(any(SearchOrderModRequest.class));
	}

	@Test
	void testSearchFilterWithError() {
		when(permitService.searchFilter(any(SearchFilterRequest.class)))
				.thenReturn(Mono.error(new RuntimeException("Filter search error")));

		Mono<ResponseEntity<SearchFilterResponse>> result =
				permitController.postPermitSearchFilter(searchFilterRequest, "trace-id");

		StepVerifier.create(result).expectError(RuntimeException.class).verify();

		verify(permitService, times(1)).searchFilter(any(SearchFilterRequest.class));
	}

	@Test
	void testSmartSearchWithNullHeaders() {
		SmartSearchResponse expectedResponse = new SmartSearchResponse();
		when(permitService.smartSearch(anyString())).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SmartSearchResponse>> result =
				permitController.getPermitSmartSearch("test-text", null, null, null);

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).smartSearch(anyString());
	}

	@Test
	void testSearchActivitiesWithNullHeaders() {
		SearchActivityResponse expectedResponse = new SearchActivityResponse();
		when(permitService.searchActivities(any(SearchActivityRequest.class))).thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<SearchActivityResponse>> result =
				permitController.postPermitActivitiesSearch(searchActivityRequest, null, null, null);

		StepVerifier.create(result)
				.expectNextMatches(
						response -> response.getStatusCode() == HttpStatus.OK && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).searchActivities(any(SearchActivityRequest.class));
	}

	@Test
	void testPersistPermitCreate() {
		CreatePermitResponse expectedResponse = new CreatePermitResponse();
		when(permitService.createPermit(any(CreatePermitRequest.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<?>> result = permitController.postPermitActivities(
				OperationType.CREATE, "test-token", "trace-id", "caller-app", createCreateActivityRequest);

		StepVerifier.create(result)
				.expectNextMatches(response ->
						response.getStatusCode() == HttpStatus.ACCEPTED && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1))
				.createPermit(any(CreatePermitRequest.class), anyString(), anyString(), anyString());
	}

	@Test
	void testPersistPermitUpdate() {
		UpdatePermitResponse expectedResponse = new UpdatePermitResponse();
		when(permitService.updatePermit(any(List.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<?>> result = permitController.postPermitActivities(
				OperationType.UPDATE, "test-token", "trace-id", "caller-app", updateCreateActivityRequest);

		StepVerifier.create(result)
				.expectNextMatches(response ->
						response.getStatusCode() == HttpStatus.ACCEPTED && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).updatePermit(any(List.class), anyString(), anyString(), anyString());
	}

	@Test
	void testPersistPermitDelete() {
		DeletePermitResponse expectedResponse =
				new DeletePermitResponse("Permit deleted successfully", List.of("test-permit-id"));
		when(permitService.removePermit(any(List.class), anyString(), anyString(), anyString()))
				.thenReturn(Mono.just(expectedResponse));

		Mono<ResponseEntity<?>> result = permitController.postPermitActivities(
				OperationType.DELETE, "test-token", "trace-id", "caller-app", deleteCreateActivityRequest);

		StepVerifier.create(result)
				.expectNextMatches(response ->
						response.getStatusCode() == HttpStatus.ACCEPTED && response.getBody() == expectedResponse)
				.verifyComplete();

		verify(permitService, times(1)).removePermit(any(List.class), anyString(), anyString(), anyString());
	}

	@Test
	void testPersistPermitUnsupportedOperation() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> permitController.postPermitActivities(
						OperationType.CREATE_UNDONE,
						"test-token",
						"trace-id",
						"caller-app",
						unsupportedCreateActivityRequest));
		assertEquals("Unsupported operation type: CREATE_UNDONE", ex.getMessage());
	}

	@Test
	void testCreateOperationWithMissingCreateRequest() {
		CreateActivityRequest request = new CreateActivityRequest();
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> permitController.postPermitActivities(
						OperationType.CREATE, "test-token", "trace-id", "caller-app", request));
		assertEquals("createRequest is required for CREATE operation", ex.getMessage());
	}

	@Test
	void testUpdateOperationWithMissingUpdateRequest() {
		CreateActivityRequest request = new CreateActivityRequest();
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> permitController.postPermitActivities(
						OperationType.UPDATE, "test-token", "trace-id", "caller-app", request));
		assertEquals("updateRequest is required for UPDATE operation", ex.getMessage());
	}

	@Test
	void testDeleteOperationWithMissingDeleteRequest() {
		CreateActivityRequest request = new CreateActivityRequest();
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> permitController.postPermitActivities(
						OperationType.DELETE, "test-token", "trace-id", "caller-app", request));
		assertEquals("deleteRequest is required for DELETE operation", ex.getMessage());
	}

	@Test
	void testSyncPermitSearchDictionaryWithError() {
		when(permitService.syncPermitSearchDictionary()).thenReturn(Mono.error(new RuntimeException("Sync error")));

		ResponseEntity<String> result = permitController.postPermitSearchDictionarySync();

		assertEquals(HttpStatus.ACCEPTED, result.getStatusCode());
		assertEquals("Processing in background", result.getBody());

		verify(permitService, times(1)).syncPermitSearchDictionary();
	}
}
