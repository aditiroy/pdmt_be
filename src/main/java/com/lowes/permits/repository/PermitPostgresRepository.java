package com.lowes.permits.repository;

import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.lowes.permits.entity.OrderModPostgresEntity;
import com.lowes.permits.entity.PermitPostgresEntity;

import io.r2dbc.spi.Row;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermitPostgresRepository {
	private final DatabaseClient client;

	public Flux<PermitPostgresEntity> search(String sql, List<Object> params) {
		log.info("Executing SQL: {}", sql);
		DatabaseClient.GenericExecuteSpec spec = client.sql(sql);
		for (int i = 0; i < params.size(); i++) {
			spec = spec.bind(i, params.get(i));
		}
		return spec.map((row, meta) -> mapPermitPostgresEntity(row)).all();
	}

	public Mono<Long> count(String sql, List<Object> params) {

		DatabaseClient.GenericExecuteSpec spec = client.sql(sql);

		for (int i = 0; i < params.size(); i++) {
			spec = spec.bind(i, params.get(i));
		}

		return spec.map((row, meta) -> row.get(0, Long.class)).one();
	}

	public Flux<String> fetchDistinctValues(String query) {
		return client.sql(query)
				.map(row -> row.get("value", String.class))
				.all()
				.filter(Objects::nonNull)
				.distinct();
	}

	public Flux<String> searchFilter(String sql, List<Object> params, String type) {

		DatabaseClient.GenericExecuteSpec spec = client.sql(sql);

		if (params != null && !params.isEmpty()) {
			for (int i = 0; i < params.size(); i++) {
				Object param = params.get(i);

				if (param instanceof List<?> listParam) {
					String[] array = listParam.stream()
							.filter(Objects::nonNull)
							.map(String.class::cast)
							.toArray(String[]::new);

					spec = spec.bind(i, array);
				} else {
					spec = spec.bind(i, param);
				}
			}
		}

		return spec.fetch().all().flatMap(row -> {
			// text[] column
			Object value = row.get(type);
			if (value instanceof String[] array) {
				return Flux.fromArray(array);
			}
			if (value instanceof String str) {
				return Flux.just(str);
			}
			return Flux.empty();
		});
	}

	public Mono<Integer> upsertOrDeletePermitEntity(String sql, List<Object> params, List<Class<?>> types) {

		DatabaseClient.GenericExecuteSpec spec = client.sql(sql);

		for (int i = 0; i < params.size(); i++) {
			Object value = params.get(i);
			Class<?> type = types.get(i);

			if (value == null) {
				spec = spec.bindNull(i, type);
			} else if (value instanceof List<?> listParam) {
				String[] array = listParam.stream()
						.filter(Objects::nonNull)
						.map(String.class::cast)
						.toArray(String[]::new);
				spec = spec.bind(i, array);
			} else {
				spec = spec.bind(i, value);
			}
		}
		return spec.fetch().rowsUpdated().map(Long::intValue);
	}

	public Flux<OrderModPostgresEntity> fetchOrderModsByQuery(String sql) {
		return client.sql(sql)
				.map((row, meta) -> mapOrderModPostgresEntity(row))
				.all();
	}

	public Mono<Integer> bulkInsertOrderModsToPermitMaster(List<OrderModPostgresEntity> orderModPostgresEntities) {
		if (orderModPostgresEntities == null || orderModPostgresEntities.isEmpty()) {
			return Mono.just(0);
		}

		StringBuilder sql = new StringBuilder("INSERT INTO permitmain.permit_master ("
				+ "labor_category_code, labor_category_description, zipcode, city, state, labor_item, labor_item_description, "
				+ "unit_permit_fee, old_price, omni_item_id, provider, vbu_number, created_by, created_timestamp, updated_by, updated_timestamp, "
				+ "county, municipality) VALUES ");

		List<Object> allParams = new ArrayList<>();
		for (int i = 0; i < orderModPostgresEntities.size(); i++) {
			OrderModPostgresEntity orderModPostgresEntity = orderModPostgresEntities.get(i);
			if (i > 0) {
				sql.append(", ");
			}
			int paramStart = i * 18 + 1;
			sql.append("($")
					.append(paramStart)
					.append(",$")
					.append(paramStart + 1)
					.append(",$")
					.append(paramStart + 2)
					.append(",$")
					.append(paramStart + 3)
					.append(",$")
					.append(paramStart + 4)
					.append(",$")
					.append(paramStart + 5)
					.append(",$")
					.append(paramStart + 6)
					.append(",$")
					.append(paramStart + 7)
					.append(",$")
					.append(paramStart + 8)
					.append(",$")
					.append(paramStart + 9)
					.append(",$")
					.append(paramStart + 10)
					.append(",$")
					.append(paramStart + 11)
					.append(",$")
					.append(paramStart + 12)
					.append(",$")
					.append(paramStart + 13)
					.append(",$")
					.append(paramStart + 14)
					.append(",$")
					.append(paramStart + 15)
					.append(",$")
					.append(paramStart + 16)
					.append(",$")
					.append(paramStart + 17)
					.append(")");

			allParams.addAll(Arrays.asList(
					orderModPostgresEntity.getLaborCategoryCode() != null
							? orderModPostgresEntity.getLaborCategoryCode()
							: null,
					orderModPostgresEntity.getLaborCategoryDescription(),
					orderModPostgresEntity.getZipcode(),
					orderModPostgresEntity.getCity(),
					orderModPostgresEntity.getState(),
					orderModPostgresEntity.getLaborItem() != null ? orderModPostgresEntity.getLaborItem() : null,
					orderModPostgresEntity.getLaborItemDescription(),
					orderModPostgresEntity.getUnitPermitFee(),
					orderModPostgresEntity.getOldPermitFee(),
					orderModPostgresEntity.getOmniItemId(),
					orderModPostgresEntity.getProvider(),
					orderModPostgresEntity.getVbuNumber() != null ? orderModPostgresEntity.getVbuNumber() : null,
					orderModPostgresEntity.getCreatedBy(),
					orderModPostgresEntity.getCreatedTimestamp(),
					orderModPostgresEntity.getUpdatedBy(),
					orderModPostgresEntity.getLastUpdatedTimestamp(),
					orderModPostgresEntity.getCounty(),
					orderModPostgresEntity.getMunicipality()));
		}

		DatabaseClient.GenericExecuteSpec spec = client.sql(sql.toString());
		for (int i = 0; i < allParams.size(); i++) {
			Object value = allParams.get(i);
			if (value == null) {

				int paramPosition = i % 18;
				Class<?> paramType =
						switch (paramPosition) {
							case 0, 5 -> Integer.class;
							case 1, 2, 3, 4, 9, 10, 11, 16, 17 -> String.class;
							case 7, 8 -> BigDecimal.class;
							case 13, 14 -> LocalDateTime.class;
							default -> String.class;
						};
				spec = spec.bindNull(i, paramType);
			} else {
				spec = spec.bind(i, value);
			}
		}

		return spec.fetch().rowsUpdated().map(Long::intValue);
	}

	private PermitPostgresEntity mapPermitPostgresEntity(io.r2dbc.spi.Readable row) {
		PermitPostgresEntity entity = new PermitPostgresEntity();
		entity.setLaborCategoryCode(row.get("labor_category_code", Integer.class));
		entity.setLaborCategoryDescription(row.get("labor_category_description", String.class));
		entity.setZipcode(row.get("zipcode", String.class));
		entity.setLaborItem(row.get("labor_item", String.class));
		entity.setCity(row.get("city", String.class));
		entity.setState(row.get("state", String.class));
		entity.setLaborItemDescription(row.get("labor_item_description", String.class));
		entity.setOmniItemId(row.get("omni_item_id", String.class));
		entity.setUnitPermitFee(toBigDecimal(row.get("unit_permit_fee")));
		entity.setCounty(row.get("county", String.class));
		entity.setProvider(row.get("provider", String.class));
		entity.setVbuNumber(row.get("vbu_number", String.class));
		entity.setCreatedBy(row.get("created_by", String.class));
		entity.setCreatedTimestamp(row.get("created_timestamp", LocalDateTime.class));
		entity.setUpdatedBy(row.get("updated_by", String.class));
		entity.setUpdatedTimestamp(row.get("updated_timestamp", LocalDateTime.class));
		entity.setMunicipality(row.get("municipality", String.class));
		entity.setEstPermitObtainDays(row.get("est_permit_obtain_days", Integer.class));
		return entity;
	}

	private OrderModPostgresEntity mapOrderModPostgresEntity(io.r2dbc.spi.Readable row) {
		OrderModPostgresEntity entity = new OrderModPostgresEntity();
		entity.setId(row.get("id", UUID.class));
		entity.setLaborCategoryCode(row.get("labor_category_code", Integer.class));
		entity.setLaborCategoryDescription(row.get("labor_category_description", String.class));
		entity.setLaborItem(row.get("labor_item", Integer.class));
		entity.setLaborItemDescription(row.get("labor_item_description", String.class));
		entity.setUnitPermitFee(toBigDecimal(row.get("unit_permit_fee")));
		entity.setOmniItemId(row.get("omni_item_id", String.class));
		entity.setStreetAddress(row.get("street_address", String.class));
		entity.setCity(row.get("city", String.class));
		entity.setState(row.get("state", String.class));
		entity.setZipcode(row.get("zipcode", String.class));
		entity.setCounty(row.get("county", String.class));
		entity.setMunicipality(row.get("municipality", String.class));
		entity.setMatchedAddress(row.get("matched_address", String.class));
		entity.setProvider(row.get("provider", String.class));
		entity.setComplianceStatus(row.get("compliance_status", String.class));
		entity.setVbuNumber(row.get("vbu_number", Integer.class));
		entity.setCreatedTimestamp(row.get("created_timestamp", LocalDateTime.class));
		entity.setCreatedBy(row.get("created_by", String.class));
		entity.setUpdatedBy(row.get("updated_by", String.class));
		entity.setLastUpdatedTimestamp(row.get("last_updated_timestamp", LocalDateTime.class));
		entity.setPermitInsertType(row.get("permit_insert_type", String.class));
		entity.setOldPermitFee(toBigDecimal(row.get("old_permit_fee")));
		entity.setJobId(row.get("job_id", String.class));
		entity.setOrderNumber(row.get("order_number", String.class));
		return entity;
	}

	private BigDecimal toBigDecimal(Object value) {
		return value == null ? null : new BigDecimal(value.toString());
	}

	public Mono<Boolean> existsByPermitKey(
			Integer laborCategoryCode,
			Integer laborItem,
			String zipcode,
			String city,
			String county,
			String municipality) {
		String sql = "SELECT COUNT(*) FROM permitmain.permit_master "
				+ "WHERE labor_category_code = $1 AND labor_item = $2 AND zipcode = $3 "
				+ "AND city = $4 AND county = $5 AND municipality = $6";

		DatabaseClient.GenericExecuteSpec spec = client.sql(sql);
		if (laborCategoryCode == null) {
			spec = spec.bindNull(0, Integer.class);
		} else {
			spec = spec.bind(0, laborCategoryCode);
		}
		if (laborItem == null) {
			spec = spec.bindNull(1, Integer.class);
		} else {
			spec = spec.bind(1, laborItem);
		}
		spec = spec.bind(2, zipcode != null ? zipcode : "");
		spec = spec.bind(3, city != null ? city : "");
		spec = spec.bind(4, county != null ? county : "");
		spec = spec.bind(5, municipality != null ? municipality : "");

		return spec.map((row, meta) -> row.get(0, Long.class)).one().map(count -> count != null && count > 0);
	}

	public Flux<byte[]> exportPermits(String query) {
		log.info("Starting CSV export with batching (2000 records per batch)");

		return client.sql(query)
				.map((row, meta) -> formatCsvRow(row) + "\n")
				.all()
				.buffer(2000)
				.map(csvRows -> {
					StringBuilder batch = new StringBuilder(csvRows.size() * 250);
					for (String csvRow : csvRows) {
						batch.append(csvRow);
					}
					return batch.toString().getBytes(StandardCharsets.UTF_8);
				})
				.doOnSubscribe(s -> log.info("CSV export stream started - processing in batches of 2000 records"))
				.doOnComplete(() -> log.info("CSV export completed successfully - all batches streamed"))
				.doOnError(e -> log.error("CSV export failed", e));
	}

	private String formatCsvRow(Row row) {
		return escapeCsv(row.get("labor_category_code", String.class)) + ","
				+ escapeCsv(row.get("labor_category_description", String.class)) + ","
				+ escapeCsv(row.get("zipcode", String.class)) + "," + escapeCsv(row.get("city", String.class)) + ","
				+ escapeCsv(row.get("state", String.class)) + "," + escapeCsv(row.get("labor_item", String.class)) + ","
				+ escapeCsv(row.get("labor_item_description", String.class)) + ","
				+ escapeCsv(row.get("unit_permit_fee", BigDecimal.class)) + ","
				+ escapeCsv(row.get("omni_item_id", String.class)) + ","
				+ escapeCsv(checkDummyValue(row.get("county", String.class)))
				+ "," + escapeCsv(row.get("provider", String.class)) + ","
				+ escapeCsv(row.get("vbu_number", String.class)) + "," + escapeCsv(row.get("created_by", String.class))
				+ "," + escapeCsv(row.get("updated_by", String.class)) + ","
				+ escapeCsv(row.get("created_timestamp", LocalDateTime.class)) + ","
				+ escapeCsv(row.get("updated_timestamp", LocalDateTime.class)) + ","
				+ escapeCsv(checkDummyValue(row.get("municipality", String.class))) + ","
				+ escapeCsv(row.get("est_permit_obtain_days", Integer.class));
	}

	private String checkDummyValue(String value) {
		if (DUMMY_VALUE.equals(value)) {
			return null;
		}
		return value;
	}

	private String escapeCsv(Object value) {
		if (value == null) {
			return "";
		}
		String str = value.toString();
		if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
			return "\"" + str.replace("\"", "\"\"") + "\"";
		}
		return str;
	}
}
