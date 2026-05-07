package com.lowes.permits.repository;

import static com.lowes.permits.constants.ApplicationConstants.AUDIT;
import static com.lowes.permits.constants.ApplicationConstants.ERROR_MESSAGE;
import static com.lowes.permits.constants.ApplicationConstants.NEW;
import static com.lowes.permits.constants.ApplicationConstants.OPERATION_TYPE;
import static com.lowes.permits.constants.ApplicationConstants.PERMIT_DB_ID;
import static com.lowes.permits.constants.ApplicationConstants.RETRY_COUNT;
import static com.lowes.permits.constants.ApplicationConstants.RETRY_STATE;
import static com.lowes.permits.constants.ApplicationConstants.STATUS;
import static com.lowes.permits.constants.ApplicationConstants.UNIT_PERMIT_FEE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.lowes.permits.dto.response.LaborCategoryResponse;
import com.lowes.permits.entity.ConfigMongoEntity;
import com.lowes.permits.entity.LaborItemMongoEntity;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.entity.PermitExportMetadataEntity;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.entity.SearchDictionaryMongoEntity;
import com.lowes.permits.model.Item;
import com.lowes.permits.model.LookupType;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PermitMongoRepository {
	private static final String ID = "_id";
	private static final String STATUS_SORT_FIELD = "statusSortOrder";
	private static final List<String> VISIBLE_OPERATION_TYPES = List.of("CREATE", "UPDATE", "DELETE");
	private static final String LABOR_CATEGORY_COLLECTION = "labor_category";
	private static final String LABOR_CATEGORY_BACKUP_COLLECTION = "laborcategories_backup";
	private final ReactiveMongoTemplate template;

	public static Update getUpdateQuery(PermitMongoEntity permitMongoEntity) {
		Update update = new Update();
		if (permitMongoEntity != null) {
			if (permitMongoEntity.getUnitPermitFee() != null) {
				update.set(UNIT_PERMIT_FEE, permitMongoEntity.getUnitPermitFee());
			}
			if (permitMongoEntity.getAudit() != null) {
				update.set(AUDIT, permitMongoEntity.getAudit());
			}
			if (permitMongoEntity.getOperationType() != null) {
				update.set(OPERATION_TYPE, permitMongoEntity.getOperationType());
			}
			if (permitMongoEntity.getStatus() != null) {
				update.set(STATUS, permitMongoEntity.getStatus().toString());
			}
			if (permitMongoEntity.getRetryCount() != null) {
				update.set(RETRY_COUNT, permitMongoEntity.getRetryCount());
			}
			if (permitMongoEntity.getErrorMessage() != null) {
				update.set(ERROR_MESSAGE, permitMongoEntity.getErrorMessage());
			}
		}
		return update;
	}

	public static Update getOrderModUpdateQuery(OrderModMongoEntity orderModEntity) {
		Update update = new Update();
		if (orderModEntity != null) {
			if (orderModEntity.getStatus() != null) {
				update.set(STATUS, orderModEntity.getStatus());
			}
			if (orderModEntity.getRetryCount() != null) {
				update.set(RETRY_COUNT, orderModEntity.getRetryCount());
			}
			if (orderModEntity.getErrorMessage() != null) {
				update.set(ERROR_MESSAGE, orderModEntity.getErrorMessage());
			}
			if (orderModEntity.getAudit() != null) {
				update.set(AUDIT, orderModEntity.getAudit());
			}
		}
		return update;
	}

	public Mono<PermitMongoEntity> createPermit(PermitMongoEntity permitMongoEntity) {
		return template.save(permitMongoEntity);
	}

	public Mono<List<PermitMongoEntity>> createPermitsBulk(List<PermitMongoEntity> permitEntities) {
		return template.insertAll(permitEntities).collectList();
	}

	public Mono<PermitMongoEntity> savePermit(PermitMongoEntity permitMongoEntity) {
		return template.save(permitMongoEntity);
	}

	public Mono<OrderModMongoEntity> saveOrderMod(OrderModMongoEntity orderModMongoEntity) {
		return template.save(orderModMongoEntity);
	}

	public Mono<PermitMongoEntity> findByPermitDbId(String permitDbId) {
		Query query = Query.query(Criteria.where(PERMIT_DB_ID).is(permitDbId));
		return template.findOne(query, PermitMongoEntity.class);
	}

	public Mono<PermitMongoEntity> findNewPermitEntityById(String id) {
		Query query = Query.query(Criteria.where(ID).is(id).and(STATUS).in(PermitStatus.NEW, PermitStatus.RETRY_STATE));
		return template.findOne(query, PermitMongoEntity.class);
	}

	public Mono<PermitMongoEntity> updatePermit(PermitMongoEntity permitMongoEntity) {
		Query query = Query.query(Criteria.where(PERMIT_DB_ID).is(permitMongoEntity.getPermitDbId()));
		var update = getUpdateQuery(permitMongoEntity);
		return template.findAndModify(
				query, update, FindAndModifyOptions.options().returnNew(true), PermitMongoEntity.class);
	}

	public Mono<List<PermitMongoEntity>> searchNewPermitEntity() {
		Query query = Query.query(Criteria.where(STATUS).in(NEW));
		return template.find(query, PermitMongoEntity.class).collectList();
	}

	public Mono<List<PermitMongoEntity>> searchRetryPermitEntity() {
		Query query = Query.query(
				Criteria.where(STATUS).is(RETRY_STATE).and(RETRY_COUNT).lt(3));
		return template.find(query, PermitMongoEntity.class).collectList();
	}

	public Mono<List<PermitMongoEntity>> searchPermitEntityByPermitDbId(
			List<String> permitDbId, OperationType operationType) {

		Query query = Query.query(Criteria.where(PERMIT_DB_ID)
				.in(permitDbId)
				.and(STATUS)
				.is(PermitStatus.NEW)
				.and(OPERATION_TYPE)
				.is(operationType));
		return template.find(query, PermitMongoEntity.class).collectList();
	}

	public Flux<PermitMongoEntity> searchActivities(Long beginDate, Long endDate, int page, int pageSize, Sort sort) {
		List<AggregationOperation> stages = new ArrayList<>();

		stages.add(buildMatchStage(beginDate, endDate));

		boolean sortsByStatus = sort.stream().anyMatch(order -> "status".equals(order.getProperty()));
		if (sortsByStatus) {
			stages.add(buildStatusWeightStage());
		}

		stages.add(buildSortStage(sort));

		int skip = (page - 1) * pageSize;
		stages.add(context -> new Document("$skip", skip));
		stages.add(context -> new Document("$limit", pageSize));

		Aggregation aggregation = Aggregation.newAggregation(stages)
				.withOptions(AggregationOptions.builder().allowDiskUse(true).build());

		log.info("Executing activity search aggregation with sort={}, skip={}, limit={}", sort, skip, pageSize);

		return template.aggregate(aggregation, "permits", PermitMongoEntity.class);
	}

	public Mono<Long> countActivities(Long beginDate, Long endDate) {
		Query query = new Query();
		query.addCriteria(Criteria.where("operationType").in(VISIBLE_OPERATION_TYPES));

		if (beginDate != null && endDate != null) {
			query.addCriteria(Criteria.where("audit.createdAt").gte(beginDate).lte(endDate));
		} else if (beginDate != null) {
			query.addCriteria(Criteria.where("audit.createdAt").gte(beginDate));
		} else if (endDate != null) {
			query.addCriteria(Criteria.where("audit.createdAt").lte(endDate));
		}

		return template.count(query, PermitMongoEntity.class);
	}

	public Flux<OrderModMongoEntity> searchOrderMods(Query query, int skip, int limit, Sort sort) {
		Query sortedQuery = query.with(sort).skip(skip).limit(limit);
		log.info("Executing order-mods search query: {}", sortedQuery);
		return template.find(sortedQuery, OrderModMongoEntity.class);
	}

	public Mono<Long> countOrderMods(Query query) {
		return template.count(query, OrderModMongoEntity.class);
	}

	public Mono<List<OrderModMongoEntity>> findOrderModsByIds(List<String> ids) {
		Query query = Query.query(Criteria.where(ID).in(ids));
		return template.find(query, OrderModMongoEntity.class).collectList();
	}

	public Mono<List<Map<String, String>>> getStateMap() {
		return template.findOne(Query.query(Criteria.where("type").is("STATE_MAP")), ConfigMongoEntity.class)
				.map(ConfigMongoEntity::getData)
				.defaultIfEmpty(Collections.emptyList());
	}

	public Mono<Void> upsert(LookupType type, List<String> value) {
		Update update = createSearchDictionaryBaseUpdate(type).set("data", value);

		return upsertSearchDictionary(type, update)
				.doOnSuccess(v -> log.info("Upserted {} for type {}", value, type))
				.doOnError(ex -> log.error("Failed to upsert {} for type {}", value, type, ex))
				.then();
	}

	public Mono<Void> upsertMapData(LookupType type, List<Item> mapdata) {
		Update update = createSearchDictionaryBaseUpdate(type).set("mapData", mapdata);

		return upsertSearchDictionary(type, update)
				.doOnSuccess(v -> log.info("Upserted mapData {} for type {}", mapdata, type))
				.doOnError(ex -> log.error("Failed to upsert mapData {} for type {}", mapdata, type, ex))
				.then();
	}

	public Mono<List<String>> searchByTypeAndPrefix(LookupType type, String prefix) {
		Query query = Query.query(Criteria.where("type").is(type));

		return template.findOne(query, SearchDictionaryMongoEntity.class)
				.map(entity -> entity.getData().stream()
						.filter(value -> value != null && value.toLowerCase().startsWith(prefix.toLowerCase()))
						.sorted()
						.toList())
				.defaultIfEmpty(Collections.emptyList());
	}

	public Mono<List<Item>> searchByStateExpandedTypeAndPrefix(LookupType type, String prefix) {
		Query query = Query.query(Criteria.where("type").is(LookupType.STATE_EXPANDED));
		if (type == LookupType.STATE_EXPANDED) {
			return template.findOne(query, SearchDictionaryMongoEntity.class)
					.map(entity -> entity.getMapData().stream()
							.filter(item -> item != null
									&& item.getValue() != null
									&& item.getValue().toLowerCase().startsWith(prefix.toLowerCase()))
							.map(data -> {
								Item i = new Item();
								i.setValue(data.getValue());
								i.setCode(data.getCode());
								return i;
							})
							.toList())
					.defaultIfEmpty(Collections.emptyList());
		} else if (type == LookupType.STATE) {
			return template.findOne(query, SearchDictionaryMongoEntity.class)
					.map(entity -> entity.getMapData().stream()
							.filter(item -> item != null
									&& item.getCode() != null
									&& item.getCode().toLowerCase().startsWith(prefix.toLowerCase()))
							.map(data -> {
								Item i = new Item();
								i.setValue(data.getValue());
								i.setCode(data.getCode());
								return i;
							})
							.toList())
					.defaultIfEmpty(Collections.emptyList());
		}
		return Mono.just(Collections.emptyList());
	}

	private AggregationOperation buildMatchStage(Long beginDate, Long endDate) {
		List<Document> andConditions = new ArrayList<>();
		andConditions.add(new Document("operationType", new Document("$in", VISIBLE_OPERATION_TYPES)));

		if (beginDate != null && endDate != null) {
			andConditions.add(new Document("audit.createdAt", new Document("$gte", beginDate).append("$lte", endDate)));
		} else if (beginDate != null) {
			andConditions.add(new Document("audit.createdAt", new Document("$gte", beginDate)));
		} else if (endDate != null) {
			andConditions.add(new Document("audit.createdAt", new Document("$lte", endDate)));
		}

		return context -> new Document("$match", new Document("$and", andConditions));
	}

	private Mono<com.mongodb.client.result.UpdateResult> upsertSearchDictionary(LookupType type, Update update) {
		return template.upsert(buildSearchDictionaryQuery(type), update, SearchDictionaryMongoEntity.class);
	}

	private Query buildSearchDictionaryQuery(LookupType type) {
		return Query.query(Criteria.where("type").is(type));
	}

	private Update createSearchDictionaryBaseUpdate(LookupType type) {
		return new Update().setOnInsert("type", type).set("lastUpdatedAt", Instant.now());
	}

	private AggregationOperation buildStatusWeightStage() {
		Document switchExpr = new Document(
				"$switch",
				new Document()
						.append(
								"branches",
								List.of(
										new Document("case", new Document("$eq", List.of("$status", "PROCESSED")))
												.append("then", 1),
										new Document("case", new Document("$eq", List.of("$status", "RETRY_STATE")))
												.append("then", 2),
										new Document("case", new Document("$eq", List.of("$status", "NEW")))
												.append("then", 2)))
						.append("default", 99));

		return context -> new Document("$addFields", new Document(STATUS_SORT_FIELD, switchExpr));
	}

	private AggregationOperation buildSortStage(Sort sort) {
		Document sortDoc = new Document();
		for (Sort.Order order : sort) {
			int direction = order.isAscending() ? 1 : -1;
			if ("status".equals(order.getProperty())) {
				sortDoc.append(STATUS_SORT_FIELD, direction);
			} else {
				sortDoc.append(order.getProperty(), direction);
			}
		}
		return context -> new Document("$sort", sortDoc);
	}

	public Flux<LaborCategoryResponse> syncAllLaborCategories(List<LaborCategoryResponse> laborCategories) {
		if (laborCategories == null || laborCategories.isEmpty()) {
			return Flux.empty();
		}

		log.info("Syncing {} labor categories using backup-first strategy", laborCategories.size());

		// Step 1: Load data into backup collection, then load into main collection
		return loadLaborCategoryDataToBackup(laborCategories)
				.then()
				// Step 2: Load data into main collection
				.thenMany(Flux.defer(() -> loadLaborCategoryData(laborCategories)))
				.onErrorResume(e -> {
					log.error("Labor category sync failed", e);
					return Flux.empty();
				})
				.doOnComplete(() -> log.info(
						"Successfully synced {} categories to both backup and main collections",
						laborCategories.size()))
				.doOnError(e -> log.error("Labor category sync failed", e));
	}

	private Flux<LaborCategoryResponse> loadLaborCategoryDataToBackup(List<LaborCategoryResponse> laborCategories) {
		log.info("Loading {} labor categories into backup collection", laborCategories.size());

		return template.remove(new Query(), LABOR_CATEGORY_BACKUP_COLLECTION)
				.thenMany(Flux.fromIterable(laborCategories))
				.flatMap(this::insertLaborCategoryToBackup)
				.doOnComplete(() ->
						log.info("Successfully loaded {} categories into backup collection", laborCategories.size()))
				.doOnError(e -> log.error("Failed to load categories into backup collection", e));
	}

	private Flux<LaborCategoryResponse> loadLaborCategoryData(List<LaborCategoryResponse> laborCategories) {
		log.info("Loading {} labor categories into main collection", laborCategories.size());

		return template.remove(new Query(), LABOR_CATEGORY_COLLECTION)
				.thenMany(Flux.fromIterable(laborCategories))
				.flatMap(this::insertLaborCategory)
				.doOnComplete(() ->
						log.info("Successfully loaded {} categories into main collection", laborCategories.size()))
				.doOnError(e -> log.error("Failed to load categories into main collection", e));
	}

	private Mono<LaborCategoryResponse> insertLaborCategoryToBackup(LaborCategoryResponse laborCategory) {
		return template.insert(laborCategory, LABOR_CATEGORY_BACKUP_COLLECTION);
	}

	private Mono<LaborCategoryResponse> insertLaborCategory(LaborCategoryResponse laborCategory) {
		return template.insert(laborCategory, LABOR_CATEGORY_COLLECTION);
	}

	public Flux<LaborCategoryResponse> findAllLaborCategories() {
		Query query = new Query();
		query.fields().include("laborCategoryId").include("name");
		return template.find(query, LaborCategoryResponse.class, LABOR_CATEGORY_COLLECTION);
	}

	public Mono<LaborItemMongoEntity> findByLaborItem(String laborItem) {
		Query query = Query.query(Criteria.where("laborItem").is(laborItem));

		return template.findOne(query, LaborItemMongoEntity.class);
	}

	public Mono<LaborItemMongoEntity> saveLaborItem(LaborItemMongoEntity laborItemMongoEntity) {
		return template.save(laborItemMongoEntity);
	}

	public Mono<PermitExportMetadataEntity> findLatestFileByTypeAndStatus(String type) {
		Query query = Query.query(
						Criteria.where("type").is(type).and("uploadStatus").is("SUCCESS"))
				.with(Sort.by(Sort.Direction.DESC, "updatedAt"))
				.limit(1);
		return template.findOne(query, PermitExportMetadataEntity.class);
	}

	public Mono<PermitExportMetadataEntity> saveExportMetadata(PermitExportMetadataEntity metadata) {
		return template.save(metadata);
	}

	public Flux<OrderModMongoEntity> searchApprovedOrderMods() {
		Query query = Query.query(Criteria.where(STATUS).is("APPROVED"));
		return template.find(query, OrderModMongoEntity.class);
	}

	public Mono<OrderModMongoEntity> updateOrderMod(OrderModMongoEntity orderModEntity) {
		Query query = Query.query(Criteria.where(ID).is(orderModEntity.getId()));
		var update = getOrderModUpdateQuery(orderModEntity);
		return template.findAndModify(
				query, update, FindAndModifyOptions.options().returnNew(true), OrderModMongoEntity.class);
	}

	public Flux<OrderModMongoEntity> searchRetryOrderMods() {
		Query query = Query.query(
				Criteria.where(STATUS).is(RETRY_STATE).and(RETRY_COUNT).lt(3));
		return template.find(query, OrderModMongoEntity.class);
	}
}
