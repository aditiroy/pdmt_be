package com.lowes.permits.service;

import static com.lowes.permits.constants.ApplicationConstants.APPROVED;
import static com.lowes.permits.constants.ApplicationConstants.AVS_SUCCESS;
import static com.lowes.permits.constants.ApplicationConstants.MDC_TRACE_ID_KEY;
import static com.lowes.permits.constants.ApplicationConstants.REJECTED;
import static com.lowes.permits.constants.ApplicationConstants.SYSTEM;
import static com.lowes.permits.constants.ApplicationConstants.UNKNOWN_APPLICATION;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.lowes.permits.dto.request.ApproveRejectOrderModRequest;
import com.lowes.permits.dto.request.CreatePermitRequest;
import com.lowes.permits.dto.request.DeletePermitRequest;
import com.lowes.permits.dto.request.EditActivityFeeRequest;
import com.lowes.permits.dto.request.SearchActivityRequest;
import com.lowes.permits.dto.request.SearchFilterRequest;
import com.lowes.permits.dto.request.SearchOrderModRequest;
import com.lowes.permits.dto.request.SearchPermitRequest;
import com.lowes.permits.dto.request.UpdatePermitRequest;
import com.lowes.permits.dto.response.ActivityResponse;
import com.lowes.permits.dto.response.ApproveRejectOrderModResponse;
import com.lowes.permits.dto.response.CreatePermitResponse;
import com.lowes.permits.dto.response.DeletePermitResponse;
import com.lowes.permits.dto.response.LaborItemResponse;
import com.lowes.permits.dto.response.OrderModResponse;
import com.lowes.permits.dto.response.PermitResponse;
import com.lowes.permits.dto.response.SearchActivityResponse;
import com.lowes.permits.dto.response.SearchFilterResponse;
import com.lowes.permits.dto.response.SearchOrderModResponse;
import com.lowes.permits.dto.response.SearchPermitResponse;
import com.lowes.permits.dto.response.SmartSearchResponse;
import com.lowes.permits.dto.response.UpdatePermitResponse;
import com.lowes.permits.entity.LaborItemMongoEntity;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.exception.PermitMainDbConflictException;
import com.lowes.permits.exception.PermitNotFoundException;
import com.lowes.permits.exception.PermitSearchException;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.mapper.PermitMapper;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.Item;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.LookupType;
import com.lowes.permits.model.MappingContext;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.Pagination;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;
import com.lowes.permits.util.PermitUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermitService {

	private static final int DEFAULT_PAGE = 1;
	private static final int DEFAULT_PAGE_SIZE = 10;
	private static final int DEFAULT_PERMIT_SEARCH_PAGE_SIZE = 20;
	private static final int DEFAULT_ORDER_MOD_PAGE_SIZE = 20;
	private static final Sort DEFAULT_SORT =
			Sort.by(Sort.Direction.DESC, "status").and(Sort.by(Sort.Direction.DESC, "audit.lastModifiedAt"));
	private static final Set<String> ALLOWED_SORT_COLUMNS =
			Set.of("audit.createdAt", "audit.lastModifiedAt", "status", "operationType", "unitPermitFee", "laborItem");
	private static final Set<String> ALLOWED_PERMIT_SEARCH_COLUMNS = Set.of(
			"city",
			"zipcode",
			"labor_category_code",
			"labor_category_description",
			"state",
			"county",
			"municipality",
			"labor_item",
			"labor_item_description",
			"unit_permit_fee",
			"omni_item_id",
			"provider",
			"vbu_number",
			"created_by",
			"updated_by",
			"created_timestamp",
			"updated_timestamp",
			"old_price");
	private static final Map<String, String> ORDER_MOD_COLUMN_TO_MONGO_FIELD = Map.ofEntries(
			Map.entry("id", "_id"),
			Map.entry("labor_category_code", "laborCategoryCode"),
			Map.entry("labor_category_description", "laborCategoryDescription"),
			Map.entry("labor_item", "laborItem"),
			Map.entry("labor_item_description", "laborItemDescription"),
			Map.entry("unit_permit_fee", "unitPermitFee"),
			Map.entry("omni_item_id", "omniItemId"),
			Map.entry("addressLine1", "address.addressLine1"),
			Map.entry("city", "address.city"),
			Map.entry("state", "address.state"),
			Map.entry("zipcode", "address.zipCode"),
			Map.entry("county", "address.county"),
			Map.entry("municipality", "address.municipality"),
			Map.entry("matched_address", "address.matchedAddress"),
			Map.entry("provider", "provider"),
			Map.entry("compliance_status", "complianceStatus"),
			Map.entry("vbu_number", "vbuNumber"),
			Map.entry("created_timestamp", "createdTimestamp"),
			Map.entry("created_by", "createdBy"),
			Map.entry("last_updated_timestamp", "lastUpdatedTimestamp"),
			Map.entry("permit_insert_type", "permitInsertType"),
			Map.entry("old_permit_fee", "oldPermitFee"),
			Map.entry("job_id", "jobId"),
			Map.entry("order_number", "orderNumber"),
			Map.entry("status", "status"));
	private static final Map<LookupType, String> SMART_SEARCH_QUERIES = Map.of(
			LookupType.CITY,
			"SELECT DISTINCT TRIM(city) AS value FROM permitmain.permit_master WHERE city IS NOT NULL AND city <> ''",
			LookupType.STATE,
			"SELECT DISTINCT TRIM(state) AS value FROM permitmain.permit_master WHERE state IS NOT NULL AND state <> ''",
			LookupType.ZIP_CODE,
			"SELECT DISTINCT TRIM(zipcode) AS value FROM permitmain.permit_master WHERE zipcode IS NOT NULL AND zipcode <> ''",
			LookupType.COUNTY,
			"SELECT DISTINCT TRIM(county) AS value FROM permitmain.permit_master WHERE county IS NOT NULL AND county <> ''",
			LookupType.LABOR_CATEGORY_DESCRIPTION,
			"SELECT DISTINCT TRIM(labor_category_description) AS value FROM permitmain.permit_master WHERE labor_category_description IS NOT null and labor_category_description <> ''",
			LookupType.MUNICIPALITY,
			"SELECT DISTINCT TRIM(municipality) AS value FROM permitmain.permit_master WHERE municipality IS NOT null and labor_category_description <> ''");

	private final PermitMongoRepository permitMongoRepository;
	private final PermitMapper permitMapper;
	private final PermitPostgresRepository permitPostgresRepository;
	private final CommonUtilityClient commonUtilityClient;

	public Mono<CreatePermitResponse> createPermit(
			CreatePermitRequest request, String xUserToken, String currentTraceId, String callerApp) {

		MappingContext context =
				new MappingContext(xUserToken, currentTraceId, callerApp, OperationType.CREATE, PermitStatus.NEW);
		PermitMongoEntity entity = permitMapper.createRequestToEntity(request, context);

		PermitDbKey dbKey = PermitUtils.decodePermitDbId(entity.getPermitDbId());

		log.info("Creating permit, xb3TraceId={}", currentTraceId);
		return permitPostgresRepository
				.existsByPermitKey(
						dbKey.getLaborCategoryCode(),
						dbKey.getLaborItem(),
						dbKey.getZipCode(),
						dbKey.getCity(),
						dbKey.getCounty(),
						dbKey.getMunicipality())
				.flatMap(exists -> {
					if (exists) {
						log.warn(
								"Permit already exists in PostgreSQL for permitDbId={}, xb3TraceId={}",
								entity.getPermitDbId(),
								currentTraceId);
						return Mono.error(new PermitMainDbConflictException(
								"Permit already exists in permits main DB for permitDbId: " + entity.getPermitDbId()));
					}
					return permitMongoRepository
							.createPermit(entity)
							.map(saved -> new CreatePermitResponse("Request accepted and is being processed"))
							.doOnSuccess(resp -> log.info("Permit created successfully, xb3TraceId={}", currentTraceId))
							.doOnError(
									ex -> log.error("Error while creating permit, xb3TraceId={}", currentTraceId, ex));
				});
	}

	public Mono<UpdatePermitResponse> updatePermit(
			List<UpdatePermitRequest> request, String xUserToken, String currentTraceId, String callerApp) {

		MappingContext context =
				new MappingContext(xUserToken, currentTraceId, callerApp, OperationType.UPDATE, PermitStatus.NEW);
		log.info("Updating permit, xb3TraceId={}", currentTraceId);
		List<String> updateRequestpermitDbIdList =
				request.stream().map(UpdatePermitRequest::getPermitDbId).toList();
		return permitMongoRepository
				.searchPermitEntityByPermitDbId(updateRequestpermitDbIdList, OperationType.UPDATE)
				.flatMapMany(Flux::fromIterable)
				.map(PermitMongoEntity::getPermitDbId)
				.collectList()
				.defaultIfEmpty(new ArrayList<>())
				.flatMap(existingPermitDbIds -> {
					if (!existingPermitDbIds.isEmpty()) {
						log.info(
								"Found {} existing permits, skipping duplicates, xb3TraceId={}",
								existingPermitDbIds.size(),
								currentTraceId);
					}
					List<PermitMongoEntity> entitiesToInsert = request.stream()
							.filter(updatePermitRequest ->
									!existingPermitDbIds.contains(updatePermitRequest.getPermitDbId()))
							.map(updatePermitRequest ->
									permitMapper.updateRequestToEntity(updatePermitRequest, context))
							.toList();

					if (entitiesToInsert.isEmpty()) {
						return Mono.just(new UpdatePermitResponse(
								"Request accepted and is being processed", existingPermitDbIds));
					}

					return permitMongoRepository
							.createPermitsBulk(entitiesToInsert)
							.map(savedEntities -> new UpdatePermitResponse(
									"Request accepted and is being processed", existingPermitDbIds))
							.doOnSuccess(resp -> log.info("Permit updated successfully, xb3TraceId={}", currentTraceId))
							.doOnError(
									ex -> log.error("Error while updating permit, xb3TraceId={}", currentTraceId, ex));
				});
	}

	public Mono<DeletePermitResponse> removePermit(
			List<DeletePermitRequest> request, String xUserToken, String currentTraceId, String callerApp) {
		MappingContext context =
				new MappingContext(xUserToken, currentTraceId, callerApp, OperationType.DELETE, PermitStatus.NEW);

		List<String> deleteRequestpermitDbIdList =
				request.stream().map(DeletePermitRequest::getPermitDbId).toList();
		return permitMongoRepository
				.searchPermitEntityByPermitDbId(deleteRequestpermitDbIdList, OperationType.DELETE)
				.flatMapMany(Flux::fromIterable)
				.map(PermitMongoEntity::getPermitDbId)
				.collectList()
				.defaultIfEmpty(new ArrayList<>())
				.flatMap(existingPermitDbIds -> {
					if (!existingPermitDbIds.isEmpty()) {
						log.info(
								"Found {} existing permits, skipping duplicates, xb3TraceId={}",
								existingPermitDbIds.size(),
								currentTraceId);
					}
					List<PermitMongoEntity> entitiesToInsert = request.stream()
							.filter(deletePermitRequest ->
									!existingPermitDbIds.contains(deletePermitRequest.getPermitDbId()))
							.map(deletePermitRequest ->
									permitMapper.deleteRequestToEntity(deletePermitRequest, context))
							.toList();

					if (entitiesToInsert.isEmpty()) {
						return Mono.just(new DeletePermitResponse(
								"Request accepted and is being processed", existingPermitDbIds));
					}

					return permitMongoRepository
							.createPermitsBulk(entitiesToInsert)
							.map(savedEntities -> new DeletePermitResponse(
									"Request accepted and is being processed", existingPermitDbIds))
							.doOnSuccess(resp -> log.info(
									"Delete requist for permit persisted in mono successfully, xb3TraceId={}",
									currentTraceId))
							.doOnError(ex -> log.error(
									"Error while persisting delete permit request in mongo, xb3TraceId={}",
									currentTraceId,
									ex));
				});
	}

	public Mono<ApproveRejectOrderModResponse> approveOrderMods(
			ApproveRejectOrderModRequest request, String xUserToken, String currentTraceId, String callerApp) {
		log.info("Approving order-mods, ids={}, xb3TraceId={}", request.getIds(), currentTraceId);
		return processApproveReject(request.getIds(), APPROVED, xUserToken, currentTraceId, callerApp);
	}

	public Mono<ApproveRejectOrderModResponse> rejectOrderMods(
			ApproveRejectOrderModRequest request, String xUserToken, String currentTraceId, String callerApp) {
		log.info("Rejecting order-mods, ids={}, xb3TraceId={}", request.getIds(), currentTraceId);
		return processApproveReject(request.getIds(), REJECTED, xUserToken, currentTraceId, callerApp);
	}

	private Mono<ApproveRejectOrderModResponse> processApproveReject(
			List<String> ids, String newStatus, String xUserToken, String currentTraceId, String callerApp) {
		return permitMongoRepository.findOrderModsByIds(ids).flatMap(entities -> {
			List<String> nonEligibleIds = entities.stream()
					.filter(e -> !AVS_SUCCESS.equals(e.getStatus()))
					.map(OrderModMongoEntity::getId)
					.toList();

			boolean hasEligible = entities.stream().anyMatch(e -> AVS_SUCCESS.equals(e.getStatus()));

			if (!nonEligibleIds.isEmpty()) {
				log.info(
						"Skipping {} order-mods not in AVS_SUCCESS status, xb3TraceId={}",
						nonEligibleIds.size(),
						currentTraceId);
			}

			if (!hasEligible) {
				return Mono.just(
						new ApproveRejectOrderModResponse("Request accepted and is being processed", nonEligibleIds));
			}

			return Flux.fromIterable(entities)
					.filter(e -> AVS_SUCCESS.equals(e.getStatus()))
					.flatMap(e -> {
						e.setStatus(newStatus);
						if (e.getAudit() == null) {
							e.setAudit(new Audit());
						}
						updateAuditMetadata(e.getAudit(), xUserToken, currentTraceId, callerApp);
						return permitMongoRepository.updateOrderMod(e);
					})
					.then(Mono.just(new ApproveRejectOrderModResponse(
							"Request accepted and is being processed", nonEligibleIds)))
					.doOnSuccess(resp ->
							log.info("Order-mods updated to {} successfully, xb3TraceId={}", newStatus, currentTraceId))
					.doOnError(ex -> log.error(
							"Error while updating order-mods to {}, xb3TraceId={}", newStatus, currentTraceId, ex));
		});
	}

	public Mono<SearchActivityResponse> searchActivities(SearchActivityRequest request) {
		int page = extractPage(request);
		int pageSize = extractPageSize(request);
		Sort sort = buildSort(request);

		Long beginDate = null;
		Long endDate = null;

		if (request.getFilter() != null && request.getFilter().getCreatedDate() != null) {
			beginDate = request.getFilter().getCreatedDate().getBegin();
			endDate = request.getFilter().getCreatedDate().getEnd();
		}

		log.info(
				"Searching activities with beginDate={}, endDate={}, page={}, pageSize={}, sort={}",
				beginDate,
				endDate,
				page,
				pageSize,
				sort);

		final Long finalBeginDate = beginDate;
		final Long finalEndDate = endDate;

		return permitMongoRepository
				.countActivities(finalBeginDate, finalEndDate)
				.flatMap(totalCount -> permitMongoRepository
						.searchActivities(finalBeginDate, finalEndDate, page, pageSize, sort)
						.map(this::mapPermitEntity)
						.collectList()
						.map(activities -> buildSearchActivityResponse(activities, totalCount, page, pageSize)));
	}

	public Mono<SearchPermitResponse> searchPermit(SearchPermitRequest request) {
		PermitSearchQueryContext queryContext = new PermitSearchQueryContext();

		Mono<String> whereClauseMono = buildPermitSearchWhereClause(request, queryContext);
		Mono<String> orderByMono = buildPermitSearchOrderByClause(request);
		PermitSearchPaginationParams paginationParams = extractPermitSearchPaginationParams(request);

		return Mono.zip(whereClauseMono, orderByMono)
				.flatMap(tuple -> {
					String whereClause = tuple.getT1();
					String orderBy = tuple.getT2();

					PermitSearchSqlQueries sqlQueries =
							buildPermitSearchSqlQueries(whereClause, orderBy, queryContext, paginationParams);

					return executePermitSearchAndCount(
							sqlQueries, queryContext.params, paginationParams, request.getSort());
				})
				.onErrorMap(ex -> {
					if (ex instanceof IllegalArgumentException) {
						return ex;
					}
					return new PermitSearchException("Failed to search permits", ex);
				});
	}

	public Mono<SearchFilterResponse> searchFilter(SearchFilterRequest request) {

		if (CollectionUtils.isEmpty(request.getFilters())) {
			return Mono.error(new IllegalArgumentException("No supported filters found"));
		}

		for (SearchFilterRequest.Filter filter : request.getFilters()) {
			if ("LABOR_CATEGORIES".equalsIgnoreCase(filter.getType())) {
				return fetchStatesByLaborCategory(filter.getValues());
			} else if ("STATES".equalsIgnoreCase(filter.getType())) {
				return fetchCitiesAndCountyByStates(filter.getValues());
			} else if ("CITIES".equalsIgnoreCase(filter.getType())) {
				return fetchZipcodesByCities(filter.getValues());
			} else if ("COUNTIES".equalsIgnoreCase(filter.getType())) {
				return fetchCitiesAndZipcodesByCounties(filter.getValues());
			} else if ("INITIAL_LOAD".equalsIgnoreCase(filter.getType())) {
				return Mono.zip(fetchLaborCategories(), fetchStateMapping(), fetchAllLaborCategories())
						.map(tuple -> {
							SearchFilterResponse laborCategoriesResponse = tuple.getT1();
							SearchFilterResponse statesResponse = tuple.getT2();
							SearchFilterResponse allLaborCategoriesResponse = tuple.getT3();

							List<SearchFilterResponse.SearchFilterResponseData> mergedData = new ArrayList<>();
							mergedData.addAll(laborCategoriesResponse.getData());
							mergedData.addAll(statesResponse.getData());
							mergedData.addAll(allLaborCategoriesResponse.getData());

							SearchFilterResponse response = new SearchFilterResponse();
							response.setData(mergedData);
							return response;
						});
			}
		}

		return Mono.error(new IllegalArgumentException("No supported filters found"));
	}

	public Mono<Void> syncPermitSearchDictionary() {
		Mono<List<Map<String, String>>> cachedStateMapMono =
				permitMongoRepository.getStateMap().cache();

		int concurrency = 4;

		return Flux.fromArray(LookupType.values())
				.flatMap(
						type -> {
							if (type == LookupType.STATE_EXPANDED) {
								return fetchValuesForStateExpanded(type, cachedStateMapMono)
										.flatMap(values -> permitMongoRepository.upsertMapData(type, values))
										.timeout(Duration.ofSeconds(20))
										.doOnSubscribe(s -> log.info("Sync for {} started", type))
										.doOnSuccess(v -> log.info("Sync for {} completed", type))
										.onErrorResume(ex -> {
											log.error("Skipping lookup type {} due to error", type, ex);
											return Mono.empty();
										});
							}

							return fetchValuesFor(type)
									.flatMap(values -> permitMongoRepository.upsert(type, values))
									.timeout(Duration.ofSeconds(20))
									.doOnSubscribe(s -> log.info("Sync for {} started", type))
									.doOnSuccess(v -> log.info("Sync for {} completed", type))
									.onErrorResume(ex -> {
										log.error("Skipping lookup type {} due to error", type, ex);
										return Mono.empty();
									});
						},
						concurrency)
				.then();
	}

	public Mono<SmartSearchResponse> smartSearch(String text) {
		if (text == null || text.length() < 2) {
			SmartSearchResponse response = new SmartSearchResponse();
			response.setData(new ArrayList<>());
			return Mono.just(response);
		}

		boolean hasLetters = text.chars().anyMatch(Character::isLetter);
		boolean hasDigits = text.chars().anyMatch(Character::isDigit);

		if (hasLetters && hasDigits) {
			SmartSearchResponse response = new SmartSearchResponse();
			response.setData(new ArrayList<>());
			return Mono.just(response);
		}

		if (StringUtils.isNumeric(text)) {
			return searchByTypes(text, List.of(LookupType.ZIP_CODE));
		}

		if (hasLetters) {
			return searchByTypes(
					text,
					List.of(
							LookupType.STATE_EXPANDED,
							LookupType.CITY,
							LookupType.MUNICIPALITY,
							LookupType.COUNTY,
							LookupType.LABOR_CATEGORY_DESCRIPTION,
							LookupType.STATE));
		}

		SmartSearchResponse response = new SmartSearchResponse();
		response.setData(new ArrayList<>());
		return Mono.just(response);
	}

	public Mono<SearchOrderModResponse> searchOrderMods(SearchOrderModRequest request) {
		try {
			Query query = buildOrderModMongoQuery(request);
			query.fields().exclude("message");
			Sort sort = buildOrderModMongoSort(request);
			OrderModPaginationParams paginationParams = extractOrderModPaginationParams(request);
			int skip = paginationParams.offset();

			Query countQuery = buildOrderModMongoQuery(request);

			Mono<List<OrderModResponse>> data = permitMongoRepository
					.searchOrderMods(query, skip, paginationParams.pageSize(), sort)
					.map(permitMapper::toOrderModResponse)
					.collectList();

			Mono<Long> count = permitMongoRepository.countOrderMods(countQuery);

			return Mono.zip(data, count)
					.map(tuple -> new SearchOrderModResponse(
							tuple.getT1(),
							new Pagination(
									paginationParams.page(),
									paginationParams.pageSize(),
									tuple.getT2(),
									paginationParams.offset() + paginationParams.pageSize() < tuple.getT2()),
							request.getSort()))
					.onErrorMap(ex -> {
						if (ex instanceof IllegalArgumentException) {
							return ex;
						}
						return new PermitSearchException("Failed to search order mods", ex);
					});
		} catch (IllegalArgumentException ex) {
			return Mono.error(ex);
		}
	}

	public Mono<PermitMongoEntity> editCreateActivity(
			String activityId, CreatePermitRequest createRequest, String xUserToken, String traceId, String callerApp) {
		return permitMongoRepository
				.findNewPermitEntityById(activityId)
				.flatMap(entity -> {
					if (!OperationType.CREATE.equals(entity.getOperationType())) {
						return Mono.error(
								new IllegalStateException("Only CREATE activities can be edited with create payload"));
					}
					entity.setPermitDbId(PermitMapper.generatePermitDbId(createRequest));
					entity.setLaborItem(createRequest.getLaborItem());
					entity.setLaborItemDescription(createRequest.getLaborItemDescription());
					entity.setUnitPermitFee(createRequest.getUnitPermitFee());
					entity.setEstPermitObtainDays(
							PermitMapper.normalizeEstPermitObtainDays(createRequest.getEstPermitObtainDays()));
					entity.setOmniItemId(createRequest.getOmniItemId());
					entity.setLaborCategory(createRequest.getLaborCategory());
					entity.setAddress(PermitMapper.buildAddressForCreate(createRequest));
					entity.setProvider(PermitMapper.buildProviderForCreate(createRequest));
					entity.setStatus(PermitStatus.NEW);
					updateAuditMetadata(entity.getAudit(), xUserToken, traceId, callerApp);
					return permitMongoRepository.savePermit(entity);
				})
				.switchIfEmpty(
						Mono.error(new PermitNotFoundException("Staged activity not found or already processed")));
	}

	public Mono<PermitMongoEntity> editUpdateActivity(
			String activityId, EditActivityFeeRequest request, String xUserToken, String traceId, String callerApp) {
		return permitMongoRepository
				.findNewPermitEntityById(activityId)
				.flatMap(entity -> {
					if (!OperationType.UPDATE.equals(entity.getOperationType())) {
						return Mono.error(new IllegalStateException("Only UPDATE activities can edit fee"));
					}
					entity.setUnitPermitFee(request.getUnitPermitFee());
					entity.setEstPermitObtainDays(
							PermitMapper.normalizeEstPermitObtainDays(request.getEstPermitObtainDays()));
					entity.setStatus(PermitStatus.NEW);
					updateAuditMetadata(entity.getAudit(), xUserToken, traceId, callerApp);
					return permitMongoRepository.savePermit(entity);
				})
				.switchIfEmpty(
						Mono.error(new PermitNotFoundException("Staged activity not found or already processed")));
	}

	public Mono<Void> undoActivities(List<String> activityIds, String xUserToken, String traceId, String callerApp) {
		return Flux.fromIterable(activityIds)
				.flatMap(id -> undoSingleActivity(id, xUserToken, traceId, callerApp))
				.then();
	}

	private Mono<PermitMongoEntity> undoSingleActivity(
			String activityId, String xUserToken, String traceId, String callerApp) {
		return permitMongoRepository
				.findNewPermitEntityById(activityId)
				.flatMap(entity -> {
					OperationType undoneType =
							switch (entity.getOperationType()) {
								case CREATE -> OperationType.CREATE_UNDONE;
								case UPDATE -> OperationType.UPDATE_UNDONE;
								case DELETE -> OperationType.DELETE_UNDONE;
								default -> throw new IllegalStateException("Only NEW activities can be undone");
							};
					entity.setOperationType(undoneType);
					entity.setStatus(PermitStatus.DELETED);
					updateAuditMetadata(entity.getAudit(), xUserToken, traceId, callerApp);
					return permitMongoRepository.savePermit(entity);
				})
				.switchIfEmpty(
						Mono.error(new PermitNotFoundException("Staged activity not found or already processed")));
	}

	private int extractPage(SearchActivityRequest request) {
		if (request.getPagination() != null && request.getPagination().getPage() != null) {
			return request.getPagination().getPage();
		}
		return DEFAULT_PAGE;
	}

	private int extractPageSize(SearchActivityRequest request) {
		if (request.getPagination() != null && request.getPagination().getPageSize() != null) {
			return request.getPagination().getPageSize();
		}
		return DEFAULT_PAGE_SIZE;
	}

	private Mono<String> buildPermitSearchWhereClause(SearchPermitRequest request, PermitSearchQueryContext context) {
		StringBuilder where = new StringBuilder(" WHERE 1=1 ");

		if (request.getFilter() == null) {
			return Mono.just(where.toString());
		}

		addPermitSearchExactMatchFilters(request, where, context);

		return addPermitSearchPartialSearchFilters(request, where, context).map(StringBuilder::toString);
	}

	private void addPermitSearchExactMatchFilters(
			SearchPermitRequest request, StringBuilder where, PermitSearchQueryContext context) {
		var filter = request.getFilter();

		if (!CollectionUtils.isEmpty(filter.getCities())) {
			appendPermitSearchInFilter(
					where,
					context,
					"city",
					filter.getCities().stream()
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}

		if (!CollectionUtils.isEmpty(filter.getZipCodes())) {
			appendPermitSearchInFilter(
					where,
					context,
					"zipcode",
					filter.getZipCodes().stream()
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}

		if (!CollectionUtils.isEmpty(filter.getLaborCategories())) {
			appendPermitSearchInFilter(
					where,
					context,
					"labor_category_code",
					filter.getLaborCategories().stream()
							.map(LaborCategory::getCode)
							.filter(Objects::nonNull)
							.toList());

			appendPermitSearchInFilter(
					where,
					context,
					"labor_category_description",
					filter.getLaborCategories().stream()
							.map(LaborCategory::getDescription)
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}

		if (!CollectionUtils.isEmpty(filter.getStates())) {
			appendPermitSearchInFilter(
					where,
					context,
					"state",
					filter.getStates().stream()
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}

		if (!CollectionUtils.isEmpty(filter.getCounties())) {
			appendPermitSearchInFilter(
					where,
					context,
					"county",
					filter.getCounties().stream()
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}

		if (!CollectionUtils.isEmpty(filter.getMunicipalities())) {
			appendPermitSearchInFilter(
					where,
					context,
					"municipality",
					filter.getMunicipalities().stream()
							.filter(org.springframework.util.StringUtils::hasText)
							.toList());
		}
	}

	private <T> void appendPermitSearchInFilter(
			StringBuilder where, PermitSearchQueryContext context, String columnName, List<T> values) {
		if (CollectionUtils.isEmpty(values)) {
			return;
		}

		where.append(" AND ").append(columnName).append(" IN (");
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) where.append(", ");
			where.append("$").append(context.idx++);
			context.params.add(values.get(i));
		}
		where.append(")");
	}

	private Mono<StringBuilder> addPermitSearchPartialSearchFilters(
			SearchPermitRequest request, StringBuilder where, PermitSearchQueryContext context) {
		if (request.getFilter().getPartialSearch() == null) {
			return Mono.just(where);
		}

		for (var partialSearch : request.getFilter().getPartialSearch()) {
			if (partialSearch.getFieldsToSearch() != null) {
				for (String field : partialSearch.getFieldsToSearch()) {
					if (isInvalidPermitSearchColumn(field)) {
						log.error("Invalid column name in partial search: {}", field);
						return Mono.error(
								new IllegalArgumentException("Invalid column name: " + field + " in partial search"));
					}
					String validatedColumn = getValidatedPermitSearchColumn(field);
					where.append(" AND CAST(")
							.append(validatedColumn)
							.append(" AS TEXT) ILIKE $")
							.append(context.idx++);
					context.params.add("%" + partialSearch.getPartialText() + "%");
				}
			}
		}
		return Mono.just(where);
	}

	private Mono<String> buildPermitSearchOrderByClause(SearchPermitRequest request) {
		if (CollectionUtils.isEmpty(request.getSort())) {
			return Mono.just(" ORDER BY zipcode ASC");
		}

		var permitSort = request.getSort().get(0);
		if (!org.springframework.util.StringUtils.hasText(permitSort.getKey())
				|| !org.springframework.util.StringUtils.hasText(permitSort.getVal())) {
			return Mono.just(" ORDER BY zipcode ASC");
		}

		if (isInvalidPermitSearchColumn(permitSort.getKey())) {
			log.error("Invalid column name in sort: {}", permitSort.getKey());
			return Mono.error(new IllegalArgumentException("Invalid sort column name: " + permitSort.getKey()));
		}
		if (!isValidPermitSearchSortDirection(permitSort.getVal())) {
			log.error("Invalid sort direction: {}", permitSort.getVal());
			return Mono.error(new IllegalArgumentException("Invalid sort direction: " + permitSort.getVal()));
		}

		String sanitizedColumn = getValidatedPermitSearchColumn(permitSort.getKey());
		String sanitizedDirection = permitSort.getVal().toUpperCase();
		return Mono.just(" ORDER BY " + sanitizedColumn + " " + sanitizedDirection);
	}

	private PermitSearchPaginationParams extractPermitSearchPaginationParams(SearchPermitRequest request) {
		int page = (request.getPagination() != null && request.getPagination().getPage() != null)
				? request.getPagination().getPage()
				: DEFAULT_PAGE;
		int pageSize =
				(request.getPagination() != null && request.getPagination().getPageSize() != null)
						? request.getPagination().getPageSize()
						: DEFAULT_PERMIT_SEARCH_PAGE_SIZE;
		int offset = (page - 1) * pageSize;

		return new PermitSearchPaginationParams(page, pageSize, offset);
	}

	private PermitSearchSqlQueries buildPermitSearchSqlQueries(
			String whereClause,
			String orderBy,
			PermitSearchQueryContext context,
			PermitSearchPaginationParams pagination) {
		String sql = """
				SELECT * FROM permitmain.permit_master
				""" + whereClause + orderBy + " LIMIT $" + context.idx++ + " OFFSET $" + context.idx;

		context.params.add(pagination.pageSize);
		context.params.add(pagination.offset);

		String countSql = "SELECT COUNT(*) FROM permitmain.permit_master " + whereClause;

		return new PermitSearchSqlQueries(sql, countSql);
	}

	private Mono<SearchPermitResponse> executePermitSearchAndCount(
			PermitSearchSqlQueries sqlQueries,
			List<Object> params,
			PermitSearchPaginationParams pagination,
			List<com.lowes.permits.model.Sort> sort) {

		Mono<List<PermitResponse>> data = permitPostgresRepository
				.search(sqlQueries.searchSql, params)
				.map(permitMapper::toPermitResponse)
				.collectList();

		Mono<Long> count = permitPostgresRepository.count(sqlQueries.countSql, params.subList(0, params.size() - 2));

		return Mono.zip(data, count)
				.map(tuple -> new SearchPermitResponse(
						tuple.getT1(),
						new Pagination(
								pagination.page,
								pagination.pageSize,
								tuple.getT2(),
								pagination.offset + pagination.pageSize < tuple.getT2()),
						sort));
	}

	private Mono<SearchFilterResponse> fetchCitiesAndCountyByStates(List<String> states) {
		String citySql = " SELECT cities FROM permitmain.state_cities_counties_view WHERE state = ANY($1) ";
		String countySql = " SELECT counties FROM permitmain.state_cities_counties_view WHERE state = ANY($1) ";

		Mono<List<String>> citiesMono = permitPostgresRepository
				.searchFilter(citySql, List.of(states), "CITIES")
				.flatMap(this::splitCommaSeparatedValues)
				.distinct()
				.sort()
				.collectList();

		Mono<List<String>> countyMono = permitPostgresRepository
				.searchFilter(countySql, List.of(states), "COUNTIES")
				.flatMap(this::splitCommaSeparatedValues)
				.distinct()
				.sort()
				.collectList();

		return Mono.zip(citiesMono, countyMono, (cities, counties) -> {
			SearchFilterResponse.SearchFilterResponseData cityData =
					new SearchFilterResponse.SearchFilterResponseData();
			cityData.setValues(cities);
			cityData.setType("CITIES");
			cityData.setCount(cities.size());

			SearchFilterResponse.SearchFilterResponseData countyData =
					new SearchFilterResponse.SearchFilterResponseData();
			countyData.setValues(counties);
			countyData.setType("COUNTIES");
			countyData.setCount(counties.size());

			List<SearchFilterResponse.SearchFilterResponseData> dataList = new ArrayList<>();
			dataList.add(cityData);
			dataList.add(countyData);

			SearchFilterResponse response = new SearchFilterResponse();
			response.setData(dataList);
			return response;
		});
	}

	private Mono<SearchFilterResponse> fetchCitiesAndZipcodesByCounties(List<String> counties) {
		String citySql = " SELECT cities FROM permitmain.county_cities_view WHERE county = ANY($1) ";
		String zipcodeSql = " SELECT zipcodes FROM permitmain.county_zipcodes_view WHERE county = ANY($1) ";

		Mono<List<String>> citiesMono = permitPostgresRepository
				.searchFilter(citySql, List.of(counties), "CITIES")
				.flatMap(this::splitCommaSeparatedValues)
				.distinct()
				.sort()
				.collectList();

		Mono<List<String>> zipcodesMono = permitPostgresRepository
				.searchFilter(zipcodeSql, List.of(counties), "ZIPCODES")
				.flatMap(this::splitCommaSeparatedValues)
				.distinct()
				.sort()
				.collectList();

		return Mono.zip(citiesMono, zipcodesMono, (cities, zipcodes) -> {
			SearchFilterResponse.SearchFilterResponseData cityData =
					new SearchFilterResponse.SearchFilterResponseData();
			cityData.setValues(cities);
			cityData.setType("CITIES");
			cityData.setCount(cities.size());

			SearchFilterResponse.SearchFilterResponseData zipcodeData =
					new SearchFilterResponse.SearchFilterResponseData();
			zipcodeData.setValues(zipcodes);
			zipcodeData.setType("ZIPCODES");
			zipcodeData.setCount(zipcodes.size());

			List<SearchFilterResponse.SearchFilterResponseData> dataList = new ArrayList<>();
			dataList.add(cityData);
			dataList.add(zipcodeData);

			SearchFilterResponse response = new SearchFilterResponse();
			response.setData(dataList);
			return response;
		});
	}

	private Mono<SearchFilterResponse> fetchZipcodesByCities(List<String> cities) {
		String sql = " SELECT zipcodes FROM permitmain.city_zipcodes_view  WHERE city = ANY($1) ";
		return fetchAndBuildFilterResponse(sql, cities, "ZIPCODES");
	}

	private Mono<SearchFilterResponse> fetchStatesByLaborCategory(List<String> laborCategories) {
		String sql =
				" SELECT states FROM permitmain.labor_category_states_view  WHERE labor_category_description = ANY($1) ";
		return fetchAndBuildFilterResponse(sql, laborCategories, "STATES");
	}

	private Mono<SearchFilterResponse> fetchAllLaborCategories() {
		return permitMongoRepository
				.findAllLaborCategories()
				.map(laborCategory ->
						String.format("%s: %s", laborCategory.getLaborCategoryId(), laborCategory.getName()))
				.collectList()
				.map(values -> buildFilterResponse(values, "ALL_LABOR_CATEGORIES"));
	}

	private Mono<SearchFilterResponse> fetchLaborCategories() {
		String sql =
				" select labor_category_code || ': ' || labor_category_description as labor_category_description from permitmain.labor_category_distinct_view ";
		return fetchAndBuildFilterResponse(sql, null, "LABOR_CATEGORY_DESCRIPTION");
	}

	private Mono<SearchFilterResponse> fetchAndBuildFilterResponse(String sql, List<String> params, String type) {

		return permitPostgresRepository
				.searchFilter(sql, params == null ? null : List.of(params), type)
				.flatMap(this::splitCommaSeparatedValues)
				.distinct()
				.sort()
				.collectList()
				.map(values -> buildFilterResponse(values, type));
	}

	private SearchFilterResponse buildFilterResponse(List<String> values, String valueType) {

		SearchFilterResponse.SearchFilterResponseData data = new SearchFilterResponse.SearchFilterResponseData();
		data.setValues(values);
		data.setType(valueType);
		data.setCount(values.size());

		SearchFilterResponse response = new SearchFilterResponse();
		response.setData(List.of(data));

		return response;
	}

	private Flux<String> splitCommaSeparatedValues(String value) {

		if (value == null || value.isBlank()) {
			return Flux.empty();
		}

		return Flux.fromArray(value.split(",")).map(String::trim).filter(s -> !s.isEmpty());
	}

	private Mono<SmartSearchResponse> searchByTypes(String prefix, List<LookupType> types) {

		return Flux.fromIterable(types)
				.flatMap(type -> {
					if (type == LookupType.STATE_EXPANDED || type == LookupType.STATE) {
						return permitMongoRepository
								.searchByStateExpandedTypeAndPrefix(type, prefix)
								.flatMapMany(Flux::fromIterable)
								.map(value -> {
									SmartSearchResponse.SmartSearchResult result =
											new SmartSearchResponse.SmartSearchResult();
									result.setType(type.name());
									result.setValue(value.getValue());
									result.setCode(value.getCode());
									return result;
								});
					}

					return permitMongoRepository
							.searchByTypeAndPrefix(type, prefix)
							.flatMapMany(Flux::fromIterable)
							.map(value -> {
								SmartSearchResponse.SmartSearchResult result =
										new SmartSearchResponse.SmartSearchResult();
								result.setType(type.name());
								result.setValue(value);
								return result;
							});
				})
				.collectList()
				.map(items -> {
					Map<String, SmartSearchResponse.SmartSearchResult> uniqueMap = new LinkedHashMap<>();

					for (SmartSearchResponse.SmartSearchResult item : items) {
						String type = item.getType().equalsIgnoreCase(LookupType.STATE_EXPANDED.name())
								? LookupType.STATE.name()
								: item.getType();
						String key = type + "_" + item.getValue() + "_" + item.getCode();
						uniqueMap.put(key, item);
					}

					SmartSearchResponse response = new SmartSearchResponse();
					response.setData(new ArrayList<>(uniqueMap.values()));
					return response;
				});
	}

	private Mono<List<String>> fetchValuesFor(LookupType type) {
		String query = getSmartSearchQuery(type);
		if (query == null) {
			return Mono.just(Collections.emptyList());
		}

		return permitPostgresRepository
				.fetchDistinctValues(query)
				.map(this::safeTrim)
				.filter(v -> v != null && !v.isEmpty())
				.distinct()
				.sort()
				.collectList();
	}

	private Mono<List<Item>> fetchValuesForStateExpanded(
			LookupType type, Mono<List<Map<String, String>>> cachedStateMapMono) {
		String codeQuery = getSmartSearchQuery(LookupType.STATE);
		Mono<List<String>> stateCodesMono =
				permitPostgresRepository.fetchDistinctValues(codeQuery).collectList();

		return Mono.zip(cachedStateMapMono, stateCodesMono).map(tuple -> {
			List<Map<String, String>> stateMapList = tuple.getT1();
			List<String> stateCodes = tuple.getT2();

			return stateCodes.stream()
					.map(code -> stateMapList.stream()
							.filter(map -> map.get("stateCode").equalsIgnoreCase(code))
							.map(map -> {
								Item item = new Item();
								item.setValue(map.get("stateName"));
								item.setCode(code);
								return item;
							})
							.findFirst()
							.orElse(null))
					.filter(Objects::nonNull)
					.distinct()
					.collect(Collectors.toList());
		});
	}

	private String safeTrim(String s) {
		return s == null ? null : s.trim();
	}

	private String getSmartSearchQuery(LookupType type) {
		return SMART_SEARCH_QUERIES.get(type);
	}

	private Query buildOrderModMongoQuery(SearchOrderModRequest request) {
		Query query = new Query();
		if (request.getFilter() == null) {
			return query;
		}
		var filter = request.getFilter();

		if (!CollectionUtils.isEmpty(filter.getCities())) {
			List<String> values = filter.getCities().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("address.city").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getZipCodes())) {
			List<String> values = filter.getZipCodes().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("address.zipCode").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getLaborCategories())) {
			List<Integer> codes = filter.getLaborCategories().stream()
					.map(LaborCategory::getCode)
					.filter(Objects::nonNull)
					.toList();
			if (!codes.isEmpty())
				query.addCriteria(Criteria.where("laborCategoryCode").in(codes));

			List<String> descs = filter.getLaborCategories().stream()
					.map(LaborCategory::getDescription)
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!descs.isEmpty())
				query.addCriteria(Criteria.where("laborCategoryDescription").in(descs));
		}
		if (!CollectionUtils.isEmpty(filter.getStates())) {
			List<String> values = filter.getStates().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("address.state").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getCounties())) {
			List<String> values = filter.getCounties().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("address.county").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getMunicipalities())) {
			List<String> values = filter.getMunicipalities().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("address.municipality").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getStatuses())) {
			List<String> values = filter.getStatuses().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty()) query.addCriteria(Criteria.where("status").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getComplianceStatuses())) {
			List<String> values = filter.getComplianceStatuses().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("complianceStatus").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getPermitInsertTypes())) {
			List<String> values = filter.getPermitInsertTypes().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("permitInsertType").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getOrderNumbers())) {
			List<String> values = filter.getOrderNumbers().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty())
				query.addCriteria(Criteria.where("orderNumber").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getJobIds())) {
			List<String> values = filter.getJobIds().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty()) query.addCriteria(Criteria.where("jobId").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getProviders())) {
			List<String> values = filter.getProviders().stream()
					.filter(org.springframework.util.StringUtils::hasText)
					.toList();
			if (!values.isEmpty()) query.addCriteria(Criteria.where("provider").in(values));
		}
		if (!CollectionUtils.isEmpty(filter.getPartialSearch())) {
			for (var partialSearch : filter.getPartialSearch()) {
				if (partialSearch.getFieldsToSearch() != null) {
					for (String field : partialSearch.getFieldsToSearch()) {
						String mongoField = ORDER_MOD_COLUMN_TO_MONGO_FIELD.get(field);
						if (mongoField == null) {
							log.error("Invalid column name in partial search: {}", field);
							throw new IllegalArgumentException("Invalid column name: " + field + " in partial search");
						}
						query.addCriteria(Criteria.where(mongoField)
								.regex(
										".*" + java.util.regex.Pattern.quote(partialSearch.getPartialText()) + ".*",
										"i"));
					}
				}
			}
		}
		return query;
	}

	private Sort buildOrderModMongoSort(SearchOrderModRequest request) {
		if (CollectionUtils.isEmpty(request.getSort())) {
			return Sort.by(Sort.Direction.DESC, "createdTimestamp");
		}
		var s = request.getSort().get(0);
		if (!org.springframework.util.StringUtils.hasText(s.getKey())
				|| !org.springframework.util.StringUtils.hasText(s.getVal())) {
			return Sort.by(Sort.Direction.DESC, "createdTimestamp");
		}
		String mongoField = ORDER_MOD_COLUMN_TO_MONGO_FIELD.get(s.getKey());
		if (mongoField == null) {
			log.error("Invalid sort column name: {}", s.getKey());
			throw new IllegalArgumentException("Invalid sort column name: " + s.getKey());
		}
		if (!isValidOrderModSortDirection(s.getVal())) {
			log.error("Invalid sort direction: {}", s.getVal());
			throw new IllegalArgumentException("Invalid sort direction: " + s.getVal());
		}
		Sort.Direction direction = "DESC".equalsIgnoreCase(s.getVal()) ? Sort.Direction.DESC : Sort.Direction.ASC;
		return Sort.by(direction, mongoField);
	}

	private OrderModPaginationParams extractOrderModPaginationParams(SearchOrderModRequest request) {
		int page = (request.getPagination() != null && request.getPagination().getPage() != null)
				? request.getPagination().getPage()
				: DEFAULT_PAGE;
		int pageSize =
				(request.getPagination() != null && request.getPagination().getPageSize() != null)
						? request.getPagination().getPageSize()
						: DEFAULT_ORDER_MOD_PAGE_SIZE;
		int offset = (page - 1) * pageSize;

		return new OrderModPaginationParams(page, pageSize, offset);
	}

	private Sort buildSort(SearchActivityRequest request) {
		List<com.lowes.permits.model.Sort> sortList = request.getSort();
		if (CollectionUtils.isEmpty(sortList)) {
			return DEFAULT_SORT;
		}
		Sort sort = Sort.unsorted();
		for (com.lowes.permits.model.Sort s : sortList) {
			String mongoKey = mapSortKey(s.getKey());
			if (mongoKey == null) {
				log.warn("Invalid sort key: {}, skipping", s.getKey());
				continue;
			}
			Sort.Direction direction = "DESC".equalsIgnoreCase(s.getVal()) ? Sort.Direction.DESC : Sort.Direction.ASC;
			sort = sort.and(Sort.by(direction, mongoKey));
		}
		return sort.isUnsorted() ? DEFAULT_SORT : sort;
	}

	private String mapSortKey(String key) {
		if (key == null) {
			return null;
		}
		return switch (key) {
			case "createdAt" -> "audit.createdAt";
			case "updatedAt" -> "audit.lastModifiedAt";
			default -> ALLOWED_SORT_COLUMNS.contains(key) ? key : null;
		};
	}

	private Sort.Direction toDirection(String direction) {
		if (direction == null) {
			return Sort.Direction.ASC;
		}
		return "DESC".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
	}

	private SearchActivityResponse buildSearchActivityResponse(
			List<ActivityResponse> activities, Long totalCount, int page, int pageSize) {
		Pagination pagination = new Pagination();
		pagination.setPage(page);
		pagination.setPageSize(pageSize);
		pagination.setTotalCount(totalCount);
		pagination.setHasNextPage((long) page * pageSize < totalCount);

		SearchActivityResponse response = new SearchActivityResponse();
		response.setData(activities);
		response.setPagination(pagination);
		return response;
	}

	private ActivityResponse mapPermitEntity(PermitMongoEntity entity) {
		return permitMapper.toActivityResponse(entity);
	}

	private boolean isInvalidPermitSearchColumn(String columnName) {
		return !ALLOWED_PERMIT_SEARCH_COLUMNS.contains(columnName);
	}

	private String getValidatedPermitSearchColumn(String columnName) {
		if (isInvalidPermitSearchColumn(columnName)) {
			throw new IllegalArgumentException("Invalid column name: " + columnName);
		}
		return ALLOWED_PERMIT_SEARCH_COLUMNS.stream()
				.filter(col -> col.equals(columnName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Column not in whitelist: " + columnName));
	}

	private boolean isValidPermitSearchSortDirection(String direction) {
		return "ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction);
	}

	private boolean isValidOrderModSortDirection(String direction) {
		return "ASC".equalsIgnoreCase(direction) || "DESC".equalsIgnoreCase(direction);
	}

	private void updateAuditMetadata(Audit existingAudit, String xUserToken, String traceId, String callerApp) {
		var userAuditToken = PermitUtils.parseUserToken(xUserToken);

		// Update only last modified fields, preserve creation metadata
		existingAudit.setLastModifiedAt(Instant.now().toEpochMilli());
		if (userAuditToken != null) {
			existingAudit.setLastModifiedByUserGroup(userAuditToken.getUserGroup());
			existingAudit.setLastModifiedByUserRole(
					userAuditToken.getUser() != null ? userAuditToken.getUser().getUserRole() : null);
			existingAudit.setLastModifiedById(
					userAuditToken.getUser() != null ? userAuditToken.getUser().getId() : null);
			existingAudit.setLastModifiedByEmailId(
					userAuditToken.getUser() != null ? userAuditToken.getUser().getEmail() : null);
			existingAudit.setLastModifiedByJobCode(
					userAuditToken.getUser() != null ? userAuditToken.getUser().getJobCode() : null);
			existingAudit.setLastModifiedByName(
					Optional.of(Stream.of(userAuditToken.getFirstName(), userAuditToken.getLastName())
									.filter(name -> name != null && !name.isBlank())
									.collect(Collectors.joining(" ")))
							.filter(name -> !name.isBlank())
							.orElse(null));
		} else {
			existingAudit.setLastModifiedByName(SYSTEM);
		}
		if (StringUtils.isNotEmpty(callerApp)) {
			existingAudit.setLastModifiedByApplicationName(callerApp);
		} else {
			existingAudit.setLastModifiedByApplicationName(UNKNOWN_APPLICATION);
		}
		existingAudit.setLastModifiedByTraceId(traceId != null ? traceId : MDC.get(MDC_TRACE_ID_KEY));
	}

	private Mono<SearchFilterResponse> fetchStateMapping() {
		return permitMongoRepository
				.getStateMap()
				.map(stateMapList -> stateMapList.stream()
						.map(stateMap -> {
							String stateCode = stateMap.get("stateCode");
							String stateName = stateMap.get("stateName");
							return stateCode + " (" + stateName + ")";
						})
						.sorted()
						.toList())
				.map(values -> buildFilterResponse(values, "ALL_STATES"));
	}

	public Mono<LaborItemResponse> getLaborItemDetail(String laborItem) {
		return permitMongoRepository
				.findByLaborItem(laborItem)
				.map(entity -> {
					log.info("LaborItemResponse from the mongo cache");
					return permitMapper.mapToLaborItemResponse(entity, null);
				})
				.switchIfEmpty(Mono.defer(() -> commonUtilityClient
						.getLaborItemResponse(laborItem)
						.flatMap(wrapper -> {
							log.info("LaborItemResponse from bifrost items utility api");
							log.info("Items API response status code: {}", wrapper.getStatusCode());

							if (wrapper.getStatusCode() == 200 && wrapper.getItemResponse() != null) {
								var product = wrapper.getItemResponse().getItems().values().stream()
										.findFirst()
										.map(item -> item.getProduct());

								if (product.isEmpty()
										|| product.get() == null
										|| product.get().getItemNumber() == null
										|| product.get().getLaborDetails() == null
										|| product.get().getLaborDetails().getLaborDescription() == null
										|| product.get().getOmniItemId() == null) {
									return Mono.just(
											permitMapper.mapToLaborItemResponse(
													new LaborItemMongoEntity(),
													"The response of Bifrost item api does not hold the object product or its associated field"));
								}

								LaborItemMongoEntity entity = permitMapper.mapToLaborItemMongoEntity(product.get());
								log.info(
										"Item response details laborItem :{}, laborItemDescription: {}, omniItemId{}",
										entity.getLaborItem(),
										entity.getLaborItemDescription(),
										entity.getOmniItemId());

								return permitMongoRepository
										.saveLaborItem(entity)
										.thenReturn(permitMapper.mapToLaborItemResponse(entity, null));
							} else if (wrapper.getStatusCode() == 204) {
								return Mono.just(permitMapper.mapToLaborItemResponse(
										new LaborItemMongoEntity(),
										"No records found in items API and permit system."));
							} else if (wrapper.getStatusCode() == 400) {
								return Mono.just(
										permitMapper.mapToLaborItemResponse(new LaborItemMongoEntity(), "Bad Request"));
							} else if (wrapper.getStatusCode() == 500) {
								return Mono.just(permitMapper.mapToLaborItemResponse(
										new LaborItemMongoEntity(), "Internal Server Error when hitting bifrost."));
							} else {
								return Mono.just(permitMapper.mapToLaborItemResponse(
										new LaborItemMongoEntity(), "Unexpected error when hitting bifrost."));
							}
						})));
	}

	private static class PermitSearchQueryContext {
		List<Object> params = new ArrayList<>();
		int idx = 1;
	}

	private record PermitSearchPaginationParams(int page, int pageSize, int offset) {}

	private record PermitSearchSqlQueries(String searchSql, String countSql) {}

	private record OrderModPaginationParams(int page, int pageSize, int offset) {}
}
