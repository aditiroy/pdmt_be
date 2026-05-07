package com.lowes.permits.controller;

import static com.lowes.permits.constants.ApplicationConstants.ALL_PERMITS;
import static com.lowes.permits.constants.ApplicationConstants.ALL_PERMITS_UPLOAD;
import static com.lowes.permits.constants.ApplicationConstants.LABOR_CATEGORY;
import static com.lowes.permits.constants.ApplicationConstants.RETRY;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lowes.permits.constants.ApplicationConstants;
import com.lowes.permits.dto.request.BulkUndoActivitiesRequest;
import com.lowes.permits.dto.request.CreateActivityRequest;
import com.lowes.permits.dto.request.EditActivityRequest;
import com.lowes.permits.dto.request.SearchActivityRequest;
import com.lowes.permits.dto.request.SearchFilterRequest;
import com.lowes.permits.dto.request.SearchOrderModRequest;
import com.lowes.permits.dto.request.SearchPermitRequest;
import com.lowes.permits.dto.response.ActivityMutationResponse;
import com.lowes.permits.dto.response.ApproveRejectOrderModResponse;
import com.lowes.permits.dto.response.LaborItemResponse;
import com.lowes.permits.dto.response.SearchActivityResponse;
import com.lowes.permits.dto.response.SearchFilterResponse;
import com.lowes.permits.dto.response.SearchOrderModResponse;
import com.lowes.permits.dto.response.SearchPermitResponse;
import com.lowes.permits.dto.response.SmartSearchResponse;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.service.PermitExportService;
import com.lowes.permits.service.PermitSchedulerService;
import com.lowes.permits.service.PermitService;
import com.lowes.permits.util.PermitUtils;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Validated
@Slf4j
@Timed
@RequiredArgsConstructor
public class PermitController {

	private final PermitService permitService;

	private final PermitSchedulerService permitSchedulerService;

	private final PermitExportService permitExportService;

	@PostMapping("/permit/activities")
	public Mono<ResponseEntity<?>> postPermitActivities(
			@RequestParam OperationType operationType,
			@RequestHeader(ApplicationConstants.X_USER_TOKEN) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp,
			@Valid @RequestBody CreateActivityRequest request) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);

		log.info("Received permit request: operation={}, xb3TraceId={}", operationType, currentTraceId);

		validateRequestBody(operationType, request);

		return switch (operationType) {
			case CREATE ->
				permitService
						.createPermit(request.getCreateRequest(), xUserToken, currentTraceId, callerApp)
						.map(response ->
								ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
			case UPDATE ->
				permitService
						.updatePermit(request.getUpdateRequest(), xUserToken, currentTraceId, callerApp)
						.map(response ->
								ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
			case DELETE ->
				permitService
						.removePermit(request.getDeleteRequest(), xUserToken, currentTraceId, callerApp)
						.map(response ->
								ResponseEntity.status(HttpStatus.ACCEPTED).body(response));
			case APPROVE ->
				permitService
						.approveOrderMods(request.getApproveRequest(), xUserToken, currentTraceId, callerApp)
						.map(response -> ResponseEntity.status(HttpStatus.ACCEPTED)
								.<ApproveRejectOrderModResponse>body(response));
			case REJECT ->
				permitService
						.rejectOrderMods(request.getRejectRequest(), xUserToken, currentTraceId, callerApp)
						.map(response -> ResponseEntity.status(HttpStatus.ACCEPTED)
								.<ApproveRejectOrderModResponse>body(response));
			default -> Mono.error(new IllegalArgumentException("Unsupported operation type: " + operationType));
		};
	}

	@PostMapping(value = "/permit/search")
	public Mono<ResponseEntity<SearchPermitResponse>> postPermitSearch(
			@RequestBody SearchPermitRequest request,
			@RequestHeader(value = ApplicationConstants.X_USER_TOKEN, required = false) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		log.info("Received permit search request: {}, xb3TraceId={}", request, currentTraceId);

		return permitService
				.searchPermit(request)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@PostMapping("/permit/search-dictionary/sync")
	public ResponseEntity<String> postPermitSearchDictionarySync() {

		permitService
				.syncPermitSearchDictionary()
				.subscribe(
						result -> log.info("Successfully completed permit search dictionary sync"),
						error -> log.error("Failed to sync permit search dictionary", error));

		return ResponseEntity.accepted().body("Processing in background");
	}

	@GetMapping("/permit/smart-search")
	public Mono<ResponseEntity<SmartSearchResponse>> getPermitSmartSearch(
			@RequestParam String text,
			@RequestHeader(value = ApplicationConstants.X_USER_TOKEN, required = false) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);

		log.info("Received smart Search request: text={}, xb3TraceId={}", text, currentTraceId);

		return permitService
				.smartSearch(text)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@PostMapping("/permit/activities/search")
	public Mono<ResponseEntity<SearchActivityResponse>> postPermitActivitiesSearch(
			@RequestBody SearchActivityRequest request,
			@RequestHeader(value = ApplicationConstants.X_USER_TOKEN, required = false) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		log.info("Received activity search request: {}, xb3TraceId={}", request, currentTraceId);

		return permitService
				.searchActivities(request)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@PatchMapping("/permit/activities/{id}")
	public Mono<ResponseEntity<ActivityMutationResponse>> patchPermitActivityById(
			@PathVariable String id,
			@RequestHeader(ApplicationConstants.X_USER_TOKEN) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp,
			@Valid @RequestBody EditActivityRequest request) {
		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		return switch (request.getOperationType()) {
			case CREATE ->
				permitService
						.editCreateActivity(id, request.getCreateRequest(), xUserToken, currentTraceId, callerApp)
						.thenReturn(buildMutationAcceptedResponse());
			case UPDATE ->
				permitService
						.editUpdateActivity(id, request.getUpdateRequest(), xUserToken, currentTraceId, callerApp)
						.thenReturn(buildMutationAcceptedResponse());
			default ->
				Mono.error(new IllegalArgumentException("Editing supported only for CREATE and UPDATE activities"));
		};
	}

	@PostMapping("/permit/activities/undo")
	public Mono<ResponseEntity<ActivityMutationResponse>> postPermitActivitiesUndo(
			@RequestHeader(ApplicationConstants.X_USER_TOKEN) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp,
			@Valid @RequestBody BulkUndoActivitiesRequest request) {
		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		return permitService
				.undoActivities(request.getActivityIds(), xUserToken, currentTraceId, callerApp)
				.thenReturn(buildMutationAcceptedResponse());
	}

	private ResponseEntity<ActivityMutationResponse> buildMutationAcceptedResponse() {
		return ResponseEntity.status(HttpStatus.OK)
				.body(new ActivityMutationResponse("Request accepted and is being processed"));
	}

	private void validateRequestBody(OperationType operationType, CreateActivityRequest request) {
		switch (operationType) {
			case CREATE -> {
				if (request.getCreateRequest() == null) {
					throw new IllegalArgumentException("createRequest is required for CREATE operation");
				}
			}
			case UPDATE -> {
				if (request.getUpdateRequest() == null
						|| request.getUpdateRequest().isEmpty()) {
					throw new IllegalArgumentException("updateRequest is required for UPDATE operation");
				}
			}
			case DELETE -> {
				if (request.getDeleteRequest() == null
						|| request.getDeleteRequest().isEmpty()) {
					throw new IllegalArgumentException("deleteRequest is required for DELETE operation");
				}
			}
			case APPROVE -> {
				if (request.getApproveRequest() == null
						|| request.getApproveRequest().getIds() == null
						|| request.getApproveRequest().getIds().isEmpty()) {
					throw new IllegalArgumentException("approveRequest is required for APPROVE operation");
				}
			}
			case REJECT -> {
				if (request.getRejectRequest() == null
						|| request.getRejectRequest().getIds() == null
						|| request.getRejectRequest().getIds().isEmpty()) {
					throw new IllegalArgumentException("rejectRequest is required for REJECT operation");
				}
			}
			default -> throw new IllegalArgumentException("Unsupported operation type: " + operationType);
		}
	}

	@PostMapping(value = "/order-mods/search")
	public Mono<ResponseEntity<SearchOrderModResponse>> postOrderModsSearch(
			@RequestBody SearchOrderModRequest request,
			@RequestHeader(value = ApplicationConstants.X_USER_TOKEN, required = false) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		log.info("Received order-mods search request: {}, xb3TraceId={}", request, currentTraceId);

		return permitService
				.searchOrderMods(request)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@PostMapping("/permit/scheduler/trigger")
	public ResponseEntity<String> postPermitTriggerScheduler(@RequestParam(required = false) String type) {
		if (RETRY.equalsIgnoreCase(type)) {
			permitSchedulerService.retryUpsertOrDeletePermitEntity();
			return ResponseEntity.ok("Retry permit scheduler triggered manually");
		} else if (LABOR_CATEGORY.equalsIgnoreCase(type)) {
			permitSchedulerService.syncLaborCategoryData();
			return ResponseEntity.ok("Labor Category Sync triggered manually");
		} else if (ALL_PERMITS_UPLOAD.equalsIgnoreCase(type)) {
			permitSchedulerService.exportPermitsToCsv();
			return ResponseEntity.ok("Permit Export triggered manually");
		} else {
			permitSchedulerService.upsertOrDeletePermitEntity();
			return ResponseEntity.ok("New permit scheduler triggered manually");
		}
	}

	@PostMapping(value = "/permit/search/filter")
	public Mono<ResponseEntity<SearchFilterResponse>> postPermitSearchFilter(
			@RequestBody SearchFilterRequest request,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);
		log.info("Received permit search filter request : {}, xb3TraceId={}", request, currentTraceId);

		return permitService
				.searchFilter(request)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@GetMapping("/permit/laborItem/{laborItem}")
	public Mono<ResponseEntity<LaborItemResponse>> getLaborItemDetail(
			@PathVariable String laborItem,
			@RequestHeader(value = ApplicationConstants.X_USER_TOKEN, required = false) String xUserToken,
			@RequestHeader(value = ApplicationConstants.X_B3_TRACE_ID, required = false) String xB3TraceId,
			@RequestHeader(value = ApplicationConstants.X_CALLER_APP, required = false) String callerApp) {

		final String currentTraceId = PermitUtils.resolveTraceId(xB3TraceId);

		log.info("Received laborItem request: laborItem={}, xb3TraceId={}", laborItem, currentTraceId);

		return permitService
				.getLaborItemDetail(laborItem)
				.map(response -> ResponseEntity.status(HttpStatus.OK).body(response));
	}

	@GetMapping(value = "/permit/download", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<java.util.Map<String, Object>>> getPermitDownloadUrl(
			@RequestParam(defaultValue = ALL_PERMITS) String type) {
		log.info("Received request to generate presigned URL for permits CSV file with type: {}", type);

		if (ALL_PERMITS.equalsIgnoreCase(type)) {
			return permitExportService.generatePreSignedDownloadUrl().map(ResponseEntity::ok);
		}

		return Mono.error(new IllegalArgumentException("Unsupported export type: " + type));
	}
}
