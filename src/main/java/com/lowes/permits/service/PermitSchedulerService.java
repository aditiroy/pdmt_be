package com.lowes.permits.service;

import static com.lowes.permits.constants.ApplicationConstants.DELETE;
import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;
import static com.lowes.permits.constants.ApplicationConstants.NEW;
import static com.lowes.permits.constants.ApplicationConstants.PROCESSED;
import static com.lowes.permits.constants.ApplicationConstants.RETRY_STATE;
import static com.lowes.permits.constants.ApplicationConstants.UPDATE;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lowes.permits.dto.response.LaborCategoryResponse;
import com.lowes.permits.entity.OrderModMongoEntity;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.http.CommonUtilityClient;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.repository.PermitMongoRepository;
import com.lowes.permits.repository.PermitPostgresRepository;
import com.lowes.permits.util.PermitUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermitSchedulerService {

	private static final String INSERT_PERMIT_SQL = "INSERT INTO permitmain.permit_master ("
			+ "labor_category_code, labor_category_description, zipcode, city, state, labor_item, labor_item_description, "
			+ "unit_permit_fee, omni_item_id, provider, vbu_number, created_by, created_timestamp, updated_by, updated_timestamp, "
			+ "county, municipality, est_permit_obtain_days) "
			+ "VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)";

	private static final String INSERT_ORDER_MOD_SQL = "INSERT INTO permitmain.permit_master ("
			+ "labor_category_code, labor_category_description, zipcode, city, state, labor_item, labor_item_description, "
			+ "unit_permit_fee, old_price, omni_item_id, provider, vbu_number, created_by, created_timestamp, updated_by, updated_timestamp, "
			+ "county, municipality) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)";

	private static final String UPDATE_PERMIT_SQL = "UPDATE permitmain.permit_master "
			+ "SET unit_permit_fee = $1, old_price = $2, updated_by = $3, updated_timestamp = $4, est_permit_obtain_days = $5  "
			+ "WHERE labor_category_code = $6 AND labor_item = $7 AND zipcode = $8 AND city = $9 AND county = $10 AND municipality = $11";

	private static final String UPDATE_ORDER_MOD_SQL = "UPDATE permitmain.permit_master "
			+ "SET unit_permit_fee = $1, old_price = $2, updated_by = $3, updated_timestamp = $4, municipality = $5 "
			+ "WHERE labor_category_code = $6 AND labor_item = $7 AND zipcode = $8 AND city = $9 AND county = $10 AND municipality = ANY($11)";

	private static final String DELETE_SQL = "DELETE FROM permitmain.permit_master "
			+ "WHERE labor_category_code = $1 AND labor_item = $2 AND zipcode = $3 AND city = $4 AND county = $5 AND municipality = $6";

	private final PermitMongoRepository permitMongoRepository;
	private final PermitPostgresRepository permitPostgresRepository;
	private final PermitService permitService;
	private final CommonUtilityClient commonUtilityClient;
	private final PermitExportService permitExportService;

	@Value("${labor-category-excluded-list}")
	private String excludedIds;

	private Set<String> excludedIdSet;

	@PostConstruct
	void initExcludedIds() {
		excludedIdSet = (excludedIds != null && !excludedIds.trim().isEmpty())
				? Arrays.stream(excludedIds.split(",")).map(String::trim).collect(Collectors.toSet())
				: Collections.emptySet();
	}

	@Scheduled(cron = "${scheduler.permit-entity.cron}")
	@SchedulerLock(name = "upsertOrDeletePermitEntity", lockAtLeastFor = "PT10M", lockAtMostFor = "PT15M")
	public void upsertOrDeletePermitEntity() {

		AtomicInteger totalCount = new AtomicInteger();
		AtomicInteger processedCount = new AtomicInteger();
		AtomicInteger failedCount = new AtomicInteger();

		AtomicInteger orderModsTotalCount = new AtomicInteger();
		AtomicInteger orderModsProcessedCount = new AtomicInteger();
		AtomicInteger orderModsFailedCount = new AtomicInteger();

		// Process Permit entities
		permitMongoRepository
				.searchNewPermitEntity()
				.doOnNext(list -> {
					totalCount.set(list.size());
					log.info("Permit Main scheduler started. Total permits fetched: {}", list.size());
				})
				.flatMapMany(Flux::fromIterable)
				.flatMap(permit -> processPermit(permit)
						.doOnSuccess(v -> processedCount.incrementAndGet())
						.doOnError(e -> failedCount.incrementAndGet()))
				.then()
				.doOnSuccess(v -> log.info(
						"Permit Main scheduler completed. Total: {}, Processed: {}, Failed: {}",
						totalCount.get(),
						processedCount.get(),
						failedCount.get()))
				// Process OrderMods migration
				.then(Mono.defer(() -> {
					log.info("===== ORDER MODS MIGRATION STARTED =====");

					return permitMongoRepository
							.searchApprovedOrderMods()
							.collectList()
							.doOnNext(list -> {
								orderModsTotalCount.set(list.size());
								log.info("Order Mods migration started. Total order mods fetched: {}", list.size());
							})
							.flatMapMany(Flux::fromIterable)
							.flatMap(orderMod -> processOrderMods(orderMod)
									.doOnSuccess(v -> orderModsProcessedCount.incrementAndGet())
									.doOnError(e -> {
										orderModsFailedCount.incrementAndGet();
										log.error("Failed to process orderMod id: {}", orderMod.getId(), e);
									})
									.onErrorResume(e -> Mono.empty()))
							.then()
							.doOnSuccess(v -> log.info(
									"Order Mods migration completed. Total: {}, Processed: {}, Failed: {}",
									orderModsTotalCount.get(),
									orderModsProcessedCount.get(),
									orderModsFailedCount.get()));
				}))
				// Sync search dictionary
				.then(permitService
						.syncPermitSearchDictionary()
						.doOnError(e -> log.error("Search dictionary sync failed", e))
						.onErrorResume(e -> Mono.empty()))
				.doOnError(e -> log.error("Fatal error in permit scheduler", e))
				.subscribe();
	}

	@Scheduled(cron = "${scheduler.permit-entity-retry.cron}")
	@SchedulerLock(name = "retryUpsertOrDeletePermitEntity", lockAtLeastFor = "PT10M", lockAtMostFor = "PT15M")
	public void retryUpsertOrDeletePermitEntity() {
		log.info("===== RETRY UPSERT OR DELETE PERMIT ENTITY SCHEDULER TRIGGERED =====");

		AtomicInteger totalCount = new AtomicInteger();
		AtomicInteger processedCount = new AtomicInteger();
		AtomicInteger failedCount = new AtomicInteger();

		AtomicInteger orderModsTotalCount = new AtomicInteger();
		AtomicInteger orderModsProcessedCount = new AtomicInteger();
		AtomicInteger orderModsFailedCount = new AtomicInteger();

		// Process Permit entities retry
		permitMongoRepository
				.searchRetryPermitEntity()
				.doOnNext(list -> {
					totalCount.set(list.size());
					log.info("Permit Retry scheduler started. Total permits fetched: {}", list.size());
				})
				.flatMapMany(Flux::fromIterable)
				.flatMap(permit -> processPermit(permit)
						.doOnSuccess(v -> processedCount.incrementAndGet())
						.doOnError(e -> failedCount.incrementAndGet()))
				.then()
				.doOnSuccess(v -> log.info(
						"Permit Retry scheduler completed. Total: {}, Processed: {}, Failed: {}",
						totalCount.get(),
						processedCount.get(),
						failedCount.get()))
				.then(Mono.defer(() -> {
					log.info("===== ORDER MODS RETRY MIGRATION STARTED =====");

					return permitMongoRepository
							.searchRetryOrderMods()
							.collectList()
							.doOnNext(list -> {
								orderModsTotalCount.set(list.size());
								log.info(
										"Order Mods Retry scheduler started. Total order mods fetched: {}",
										list.size());
							})
							.flatMapMany(Flux::fromIterable)
							.flatMap(orderMod -> processOrderMods(orderMod)
									.doOnSuccess(v -> orderModsProcessedCount.incrementAndGet())
									.doOnError(e -> {
										orderModsFailedCount.incrementAndGet();
										log.error("Failed to retry orderMod id: {}", orderMod.getId(), e);
									})
									.onErrorResume(e -> Mono.empty()))
							.then()
							.doOnSuccess(v -> log.info(
									"Order Mods Retry scheduler completed. Total: {}, Processed: {}, Failed: {}",
									orderModsTotalCount.get(),
									orderModsProcessedCount.get(),
									orderModsFailedCount.get()));
				}))
				.doOnError(e -> log.error("Fatal error in retry scheduler", e))
				.subscribe();
	}

	private Mono<Void> processPermit(PermitMongoEntity permit) {

		PermitDbKey permitDbKey = PermitUtils.decodePermitDbId(permit.getPermitDbId());

		log.info("Processing permitDbId={} operationType={}", permit.getPermitDbId(), permit.getOperationType());

		Mono<Void> dbOperation =
				switch (permit.getOperationType()) {
					case UPDATE -> updatePermit(permit, permitDbKey);
					case DELETE -> deletePermit(permitDbKey);
					case CREATE -> createPermit(permit);
					default -> {
						log.warn(
								"Unsupported operation type: {} for permitDbId: {}",
								permit.getOperationType(),
								permit.getPermitDbId());
						yield Mono.empty();
					}
				};

		PermitMongoEntity processedPermit = PermitMongoEntity.builder()
				.permitDbId(permit.getPermitDbId())
				.status(PermitStatus.PROCESSED)
				.build();

		return dbOperation
				.then(permitMongoRepository.updatePermit(processedPermit))
				.onErrorResume(ex -> {
					log.error("Permit Main processing failed. permitDbId={}", permit.getPermitDbId(), ex);

					PermitMongoEntity failedPermit = PermitMongoEntity.builder()
							.permitDbId(permit.getPermitDbId())
							.status(PermitStatus.RETRY_STATE)
							.retryCount(permit.getRetryCount() == null ? 0 : permit.getRetryCount() + 1)
							.errorMessage(ex.getMessage())
							.build();

					return permitMongoRepository.updatePermit(failedPermit);
				})
				.then();
	}

	private Mono<Void> updatePermit(PermitMongoEntity permit, PermitDbKey key) {
		List<Object> params = Arrays.asList(
				permit.getUnitPermitFee(),
				permit.getOldUnitPermitFee(),
				permit.getAudit() != null ? permit.getAudit().getLastModifiedByName() : null,
				toOffsetDateTime(permit.getAudit() != null ? permit.getAudit().getLastModifiedAt() : null),
				permit.getEstPermitObtainDays(),
				key.getLaborCategoryCode(),
				key.getLaborItem(),
				key.getZipCode(),
				key.getCity(),
				key.getCounty(),
				key.getMunicipality());

		List<Class<?>> types = List.of(
				BigDecimal.class,
				BigDecimal.class,
				String.class,
				LocalDateTime.class,
				Integer.class,
				Integer.class,
				Integer.class,
				String.class,
				String.class,
				String.class,
				String.class);
		return permitPostgresRepository
				.upsertOrDeletePermitEntity(UPDATE_PERMIT_SQL, params, types)
				.flatMap(this::validateMutationResult);
	}

	private Mono<Void> deletePermit(PermitDbKey key) {
		List<Object> params = List.of(
				key.getLaborCategoryCode(),
				key.getLaborItem(),
				key.getZipCode(),
				key.getCity(),
				key.getCounty(),
				key.getMunicipality());
		List<Class<?>> types =
				List.of(Integer.class, Integer.class, String.class, String.class, String.class, String.class);
		return permitPostgresRepository
				.upsertOrDeletePermitEntity(DELETE_SQL, params, types)
				.flatMap(this::validateMutationResult);
	}

	private Mono<Void> createPermit(PermitMongoEntity permit) {
		log.info("Creating permit in PostgreSQL for permitDbId={}", permit.getPermitDbId());

		List<Object> params = Arrays.asList(
				permit.getLaborCategory() != null ? permit.getLaborCategory().getCode() : null,
				permit.getLaborCategory() != null ? permit.getLaborCategory().getDescription() : null,
				permit.getAddress() != null ? permit.getAddress().getZipCode() : null,
				permit.getAddress() != null ? permit.getAddress().getCity() : null,
				permit.getAddress() != null ? permit.getAddress().getState() : null,
				permit.getLaborItem(),
				permit.getLaborItemDescription(),
				permit.getUnitPermitFee(),
				permit.getOmniItemId(),
				permit.getProvider() != null ? permit.getProvider().getName() : null,
				permit.getProvider() != null ? permit.getProvider().getNumber() : null,
				permit.getAudit() != null ? permit.getAudit().getCreatedByName() : null,
				toOffsetDateTime(permit.getAudit() != null ? permit.getAudit().getCreatedAt() : null),
				permit.getAudit() != null ? permit.getAudit().getLastModifiedByName() : null,
				toOffsetDateTime(permit.getAudit() != null ? permit.getAudit().getLastModifiedAt() : null),
				permit.getAddress() != null ? permit.getAddress().getCounty() : null,
				permit.getAddress() != null ? permit.getAddress().getMunicipality() : null,
				permit.getEstPermitObtainDays() != null && permit.getEstPermitObtainDays() > 0
						? permit.getEstPermitObtainDays()
						: null);

		List<Class<?>> types = List.of(
				Integer.class,
				String.class,
				String.class,
				String.class,
				String.class,
				Integer.class,
				String.class,
				BigDecimal.class,
				String.class,
				String.class,
				Integer.class,
				String.class,
				LocalDateTime.class,
				String.class,
				LocalDateTime.class,
				String.class,
				String.class,
				Integer.class);

		return permitPostgresRepository
				.upsertOrDeletePermitEntity(INSERT_PERMIT_SQL, params, types)
				.doOnSuccess(rows -> log.info(
						"PostgreSQL insert completed. Rows affected: {} for permitDbId={}",
						rows,
						permit.getPermitDbId()))
				.doOnError(ex -> log.error("PostgreSQL insert failed for permitDbId={}", permit.getPermitDbId(), ex))
				.flatMap(this::validateMutationResult);
	}

	private Mono<Void> validateMutationResult(Integer rows) {
		if (rows == 0) {
			return Mono.error(new IllegalStateException("No rows affected"));
		}
		return Mono.empty();
	}

	private LocalDateTime toOffsetDateTime(Long epochMillis) {
		return epochMillis == null
				? null
				: Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDateTime();
	}

	@Scheduled(cron = "${scheduler.labor-category.cron}")
	@SchedulerLock(name = "syncLaborCategoryData", lockAtLeastFor = "PT10M", lockAtMostFor = "PT15M")
	public void syncLaborCategoryData() {
		log.info("===== LABOR CATEGORY DATA SYNC SCHEDULER TRIGGERED =====");

		AtomicInteger totalCount = new AtomicInteger();
		AtomicInteger processedCount = new AtomicInteger();
		AtomicInteger excludedCount = new AtomicInteger();
		AtomicInteger failedCount = new AtomicInteger();

		commonUtilityClient
				.getLaborCategoryResponseList()
				.doOnNext(list -> {
					totalCount.set(list.size());
					log.info("Labor Category sync started. Total categories fetched: {}", list.size());
				})
				.flatMapMany(Flux::fromIterable)
				.filter(this::processLaborCategories)
				.doOnNext(category -> log.debug(
						"Processing labor category with id: {}, name: {}",
						category.getLaborCategoryId(),
						category.getName()))
				.collectList()
				.flatMap(filteredCategories -> {
					excludedCount.set(totalCount.get() - filteredCategories.size());
					log.info("Filtered categories: {} (excluded: {})", filteredCategories.size(), excludedCount.get());

					return permitMongoRepository
							.syncAllLaborCategories(filteredCategories)
							.doOnNext(category -> processedCount.incrementAndGet())
							.doOnError(e -> failedCount.incrementAndGet())
							.then()
							.doOnSuccess(
									v -> log.info("Successfully synced {} labor categories", processedCount.get()));
				})
				.doOnSuccess(v -> log.info(
						"Labor Category sync completed. Total: {}, Processed: {}, Excluded: {}, Failed: {}",
						totalCount.get(),
						processedCount.get(),
						excludedCount.get(),
						failedCount.get()))
				.doOnError(e -> log.error("Fatal error in labor category scheduler", e))
				.subscribe();
	}

	private boolean processLaborCategories(LaborCategoryResponse category) {
		if (category.getLaborCategoryId() == null) return true;

		boolean isExcluded =
				excludedIdSet.contains(category.getLaborCategoryId().trim());

		if (isExcluded) {
			log.debug("Labor category with id {} is excluded", category.getLaborCategoryId());
		}

		return !isExcluded;
	}

	@Scheduled(cron = "${scheduler.permit-export.cron}")
	@SchedulerLock(name = "exportPermitsCsv", lockAtLeastFor = "PT10M", lockAtMostFor = "PT30M")
	public void exportPermitsToCsv() {
		log.info("===== PERMIT CSV EXPORT SCHEDULER TRIGGERED =====");

		permitExportService
				.exportPermitsToCsvAndUpload()
				.doOnSuccess(key -> log.info("CSV export completed successfully. File uploaded to ECS: {}", key))
				.doOnError(e -> log.error("CSV export to ECS failed", e))
				.subscribe();
	}

	private Mono<Void> processOrderMods(OrderModMongoEntity orderMod) {
		String insertType = orderMod.getPermitInsertType();

		enrichOrderModAddress(orderMod.getAddress());

		log.info(
				"Processing orderMod id={} insertType={} retryCount={}",
				orderMod.getId(),
				insertType,
				orderMod.getRetryCount());

		Mono<Void> dbOperation =
				switch (insertType != null ? insertType.toUpperCase() : "") {
					case NEW -> insertOrderModToPermitMain(orderMod);
					case UPDATE -> updateOrderModInPermitMain(orderMod);
					case DELETE -> deleteOrderModFromPermitMain(orderMod);
					default -> {
						log.warn("Unknown permitInsertType: {} for orderMod id: {}", insertType, orderMod.getId());
						yield Mono.empty();
					}
				};

		OrderModMongoEntity processedOrderMod = OrderModMongoEntity.builder()
				.id(orderMod.getId())
				.status(PROCESSED)
				.build();

		return dbOperation
				.then(permitMongoRepository.updateOrderMod(processedOrderMod))
				.onErrorResume(ex -> {
					log.error("OrderMod processing failed. orderModId={}", orderMod.getId(), ex);

					OrderModMongoEntity failedOrderMod = OrderModMongoEntity.builder()
							.id(orderMod.getId())
							.status(RETRY_STATE)
							.retryCount(orderMod.getRetryCount() == null ? 0 : orderMod.getRetryCount() + 1)
							.errorMessage(ex.getMessage())
							.build();

					return permitMongoRepository.updateOrderMod(failedOrderMod);
				})
				.then();
	}

	private void enrichOrderModAddress(Address address) {
		if (address == null) return;
		if (StringUtils.isNotEmpty(address.getCity())) {
			address.setCity(address.getCity().toUpperCase());
		}
		if (StringUtils.isNotEmpty(address.getCounty())) {
			String county = address.getCounty().trim();
			if (county.toLowerCase().endsWith("county")) {
				county = county.substring(0, county.length() - 6).trim();
			}
			address.setCounty(county.toUpperCase());
		}
		if (StringUtils.isNotEmpty(address.getMunicipality())) {
			address.setMunicipality(address.getMunicipality().toUpperCase());
		}
	}

	private Mono<Void> insertOrderModToPermitMain(OrderModMongoEntity orderMod) {
		List<Object> params = Arrays.asList(
				orderMod.getCategoryCode(),
				orderMod.getCategoryDesc(),
				orderMod.getAddress() != null ? orderMod.getAddress().getZipCode() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getCity() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getState() : null,
				orderMod.getItemId() != null ? Integer.parseInt(orderMod.getItemId()) : null,
				orderMod.getItemDesc(),
				orderMod.getPermitFee() != null ? new BigDecimal(orderMod.getPermitFee()) : null,
				orderMod.getOldPermitFee() != null ? new BigDecimal(orderMod.getOldPermitFee()) : null,
				orderMod.getOmniId(),
				orderMod.getProvider(),
				orderMod.getVbuNumber(),
				orderMod.getAudit() != null ? orderMod.getAudit().getCreatedByName() : null,
				toOffsetDateTime(
						orderMod.getAudit() != null ? orderMod.getAudit().getCreatedAt() : null),
				orderMod.getAudit() != null ? orderMod.getAudit().getLastModifiedByName() : null,
				toOffsetDateTime(
						orderMod.getAudit() != null ? orderMod.getAudit().getLastModifiedAt() : null),
				orderMod.getAddress() != null ? orderMod.getAddress().getCounty() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getMunicipality() : null);

		List<Class<?>> types = Arrays.asList(
				Integer.class,
				String.class,
				String.class,
				String.class,
				String.class,
				Integer.class,
				String.class,
				BigDecimal.class,
				BigDecimal.class,
				String.class,
				String.class,
				Integer.class,
				String.class,
				LocalDateTime.class,
				String.class,
				LocalDateTime.class,
				String.class,
				String.class);

		return permitPostgresRepository
				.upsertOrDeletePermitEntity(INSERT_ORDER_MOD_SQL, params, types)
				.flatMap(this::validateMutationResult)
				.then()
				.doOnSuccess(v -> log.info("Successfully inserted NEW orderMod id: {}", orderMod.getId()))
				.onErrorResume(e -> handleInsertError(orderMod, e));
	}

	private Mono<Void> updateOrderModInPermitMain(OrderModMongoEntity orderMod) {
		String municipality =
				orderMod.getAddress() != null ? orderMod.getAddress().getMunicipality() : null;
		String municipalityToSet = StringUtils.isEmpty(municipality) ? DUMMY_VALUE : municipality;

		if (StringUtils.isEmpty(municipality) || DUMMY_VALUE.equals(municipality)) {
			// Only DUMMY_VALUE case - update directly
			return updateOrderModWithMunicipality(orderMod, municipalityToSet, List.of(DUMMY_VALUE))
					.then()
					.doOnSuccess(v -> log.info("Successfully updated orderMod id: {}", orderMod.getId()));
		} else {
			// Try actual municipality first
			return updateOrderModWithMunicipality(orderMod, municipalityToSet, List.of(municipality))
					.flatMap(rows -> {
						if (rows == 0) {
							// Not found with actual municipality, try DUMMY_VALUE
							log.info(
									"No record with municipality={}, trying DUMMY_VALUE for orderMod id={}",
									municipality,
									orderMod.getId());
							return updateOrderModWithMunicipality(orderMod, municipalityToSet, List.of(DUMMY_VALUE));
						}
						// Successfully updated actual municipality record
						return Mono.just(rows);
					})
					.then()
					.doOnSuccess(v -> log.info("Successfully updated orderMod id: {}", orderMod.getId()));
		}
	}

	private Mono<Integer> updateOrderModWithMunicipality(
			OrderModMongoEntity orderMod, String municipalityToSet, List<String> municipalitySearchList) {

		List<Object> params = Arrays.asList(
				orderMod.getPermitFee() != null ? new BigDecimal(orderMod.getPermitFee()) : null,
				orderMod.getOldPermitFee() != null ? new BigDecimal(orderMod.getOldPermitFee()) : null,
				orderMod.getAudit() != null ? orderMod.getAudit().getLastModifiedByName() : null,
				toOffsetDateTime(
						orderMod.getAudit() != null ? orderMod.getAudit().getLastModifiedAt() : null),
				municipalityToSet,
				orderMod.getCategoryCode(),
				orderMod.getItemId() != null ? Integer.parseInt(orderMod.getItemId()) : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getZipCode() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getCity() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getCounty() : null,
				municipalitySearchList);

		List<Class<?>> types = Arrays.asList(
				BigDecimal.class,
				BigDecimal.class,
				String.class,
				LocalDateTime.class,
				String.class,
				Integer.class,
				Integer.class,
				String.class,
				String.class,
				String.class,
				List.class);

		return permitPostgresRepository
				.upsertOrDeletePermitEntity(UPDATE_ORDER_MOD_SQL, params, types)
				.flatMap(rows -> {
					if (rows == 0) {
						return Mono.just(0);
					}
					return Mono.just(rows);
				})
				.onErrorReturn(0);
	}

	private Mono<Void> deleteOrderModFromPermitMain(OrderModMongoEntity orderMod) {
		List<Object> params = Arrays.asList(
				orderMod.getCategoryCode(),
				orderMod.getItemId() != null ? Integer.parseInt(orderMod.getItemId()) : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getZipCode() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getCity() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getCounty() : null,
				orderMod.getAddress() != null ? orderMod.getAddress().getMunicipality() : null);

		List<Class<?>> types =
				Arrays.asList(Integer.class, Integer.class, String.class, String.class, String.class, String.class);

		return permitPostgresRepository
				.upsertOrDeletePermitEntity(DELETE_SQL, params, types)
				.flatMap(this::validateMutationResult)
				.then()
				.doOnSuccess(v -> log.info("Successfully deleted orderMod id: {}", orderMod.getId()));
	}

	private Mono<Void> handleInsertError(OrderModMongoEntity orderMod, Throwable e) {
		if (e.getMessage() != null
				&& (e.getMessage().contains("unique constraint")
						|| e.getMessage().contains("duplicate key"))) {
			log.info("Unique constraint violation for orderMod id: {}, treating as update", orderMod.getId());
			return updateOrderModInPermitMain(orderMod).then();
		}
		return Mono.error(e);
	}
}
