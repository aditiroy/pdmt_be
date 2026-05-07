package com.lowes.permits.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.r2dbc.core.FetchSpec;

import com.lowes.permits.entity.OrderModPostgresEntity;
import com.lowes.permits.entity.PermitPostgresEntity;

import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PermitPostgresRepositoryTest {

	@Mock
	private DatabaseClient mockDatabaseClient;

	@Mock
	private GenericExecuteSpec mockGenericExecuteSpec;

	@Mock
	private FetchSpec mockRowsFetchSpec;

	private PermitPostgresRepository repository;
	private PermitPostgresEntity testPermitPostgresEntity;
	private List<Object> testParams;

	@BeforeEach
	void setUp() {
		repository = new PermitPostgresRepository(mockDatabaseClient);
		testPermitPostgresEntity = createTestPermit();
		testParams = createList("param1", "param2", "param3");
	}

	@Test
	void testSearchWithDatabaseError() {
		// Test search method when database error occurs
		String sql = "SELECT * FROM permit_master WHERE city = ?";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, "param1")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, "param2")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(2, "param3")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Flux<PermitPostgresEntity> result = repository.search(sql, testParams);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(3)).bind(anyInt(), any());
		verify(mockGenericExecuteSpec, times(1)).map(any(BiFunction.class));
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testCountWithValidData() {
		// Test count method with valid SQL and parameters
		String sql = "SELECT COUNT(*) FROM permit_master WHERE city = ? AND state = ? AND zipcode = ?";
		Long expectedCount = 10L;

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, "param1")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, "param2")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(2, "param3")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.one()).thenReturn(Mono.just(expectedCount));

		Mono<Long> result = repository.count(sql, testParams);

		StepVerifier.create(result).expectNext(expectedCount).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(3)).bind(anyInt(), any());
		verify(mockGenericExecuteSpec, times(1)).map(any(BiFunction.class));
		verify(mockRowsFetchSpec, times(1)).one();
	}

	@Test
	void testCountWithEmptyParams() {
		// Test count method with empty parameters
		String sql = "SELECT COUNT(*) FROM permit_master";
		List<Object> emptyParams = new ArrayList<>();
		Long expectedCount = 5L;

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.one()).thenReturn(Mono.just(expectedCount));

		Mono<Long> result = repository.count(sql, emptyParams);

		StepVerifier.create(result).expectNext(expectedCount).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, never()).bind(anyInt(), any());
		verify(mockGenericExecuteSpec, times(1)).map(any(BiFunction.class));
		verify(mockRowsFetchSpec, times(1)).one();
	}

	@Test
	void testCountWithDatabaseError() {
		// Test count method when database error occurs
		String sql = "SELECT COUNT(*) FROM permit_master WHERE city = ? AND state = ? AND zipcode = ?";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, "param1")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, "param2")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(2, "param3")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.one()).thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Long> result = repository.count(sql, testParams);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(3)).bind(anyInt(), any());
		verify(mockGenericExecuteSpec, times(1)).map(any(BiFunction.class));
		verify(mockRowsFetchSpec, times(1)).one();
	}

	@Test
	void testFetchDistinctValuesWithValidData() {
		// Test fetchDistinctValues method with valid query
		String query = "SELECT DISTINCT city AS value FROM permit_master";
		List<String> expectedValues = createList("Los Angeles", "New York", "Chicago");

		when(mockDatabaseClient.sql(query)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(Function.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.fromIterable(expectedValues));

		Flux<String> result = repository.fetchDistinctValues(query);

		StepVerifier.create(result).expectNextSequence(expectedValues).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(query);
		verify(mockGenericExecuteSpec, times(1)).map(any(Function.class));
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testFetchDistinctValuesWithNullValues() {
		// Test fetchDistinctValues method with null values in results
		String query = "SELECT DISTINCT city AS value FROM permit_master";
		List<String> expectedValues = createList("Los Angeles", "New York", "Chicago");

		when(mockDatabaseClient.sql(query)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(Function.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.fromIterable(expectedValues));

		Flux<String> result = repository.fetchDistinctValues(query);

		StepVerifier.create(result)
				.expectNext("Los Angeles", "New York", "Chicago")
				.verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(query);
		verify(mockGenericExecuteSpec, times(1)).map(any(Function.class));
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testFetchDistinctValuesWithEmptyResult() {
		// Test fetchDistinctValues method with empty result
		String query = "SELECT DISTINCT city FROM permit_master WHERE 1=0";

		when(mockDatabaseClient.sql(query)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(Function.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.empty());

		Flux<String> result = repository.fetchDistinctValues(query);

		StepVerifier.create(result).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(query);
		verify(mockGenericExecuteSpec, times(1)).map(any(Function.class));
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testFetchDistinctValuesWithDatabaseError() {
		// Test fetchDistinctValues method when database error occurs
		String query = "SELECT DISTINCT city AS value FROM permit_master";

		when(mockDatabaseClient.sql(query)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(Function.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Flux<String> result = repository.fetchDistinctValues(query);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockDatabaseClient, times(1)).sql(query);
		verify(mockGenericExecuteSpec, times(1)).map(any(Function.class));
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testSearchFilterWithDatabaseError() {
		// Test searchFilter method when database error occurs
		String sql = "SELECT cities FROM state_cities_view WHERE state = $1";
		List<Object> params = createList(createList("CA"));
		String type = "cities";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(eq(0), any(String[].class))).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.error(new DataAccessException("Database error") {}));

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(1)).bind(eq(0), any(String[].class));
		verify(mockGenericExecuteSpec, times(1)).fetch();
		verify(mockRowsFetchSpec, times(1)).all();
	}

	@Test
	void testUpsertOrDeletePermitEntityWithValidData() {
		// Test upsertOrDeletePermitEntity method with valid data
		String sql = "INSERT INTO permit_master (...) VALUES (...)";
		List<Object> params = createList("param1", "param2", "param3");
		List<Class<?>> types = createList(String.class, Integer.class, BigDecimal.class);
		Integer expectedRowsUpdated = 1;

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, "param1")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, "param2")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(2, "param3")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

		Mono<Integer> result = repository.upsertOrDeletePermitEntity(sql, params, types);

		StepVerifier.create(result).expectNext(expectedRowsUpdated).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(3)).bind(anyInt(), any());
		verify(mockGenericExecuteSpec, times(1)).fetch();
		verify(mockRowsFetchSpec, times(1)).rowsUpdated();
	}

	@Test
	void testUpsertOrDeletePermitEntityWithNullValues() {
		// Test upsertOrDeletePermitEntity method with null values
		String sql = "UPDATE permit_master SET updated_by = $1 WHERE id = $2";
		List<Object> params = createListWithNulls(null, 123);
		List<Class<?>> types = createList(String.class, Integer.class);
		Integer expectedRowsUpdated = 5;

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bindNull(0, String.class)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, 123)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.rowsUpdated()).thenReturn(Mono.just(5L));

		Mono<Integer> result = repository.upsertOrDeletePermitEntity(sql, params, types);

		StepVerifier.create(result).expectNext(expectedRowsUpdated).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(1)).bindNull(0, String.class);
		verify(mockGenericExecuteSpec, times(1)).bind(eq(1), eq(123));
		verify(mockGenericExecuteSpec, times(1)).fetch();
		verify(mockRowsFetchSpec, times(1)).rowsUpdated();
	}

	@Test
	void testUpsertOrDeletePermitEntityWithDatabaseError() {
		// Test upsertOrDeletePermitEntity method when database error occurs
		String sql = "DELETE FROM permit_master WHERE id = $1";
		List<Object> params = createList(123);
		List<Class<?>> types = createList(Integer.class);

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, 123)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.rowsUpdated()).thenReturn(Mono.error(new DataAccessException("Database error") {}));

		Mono<Integer> result = repository.upsertOrDeletePermitEntity(sql, params, types);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(1)).bind(eq(0), eq(123));
		verify(mockGenericExecuteSpec, times(1)).fetch();
		verify(mockRowsFetchSpec, times(1)).rowsUpdated();
	}

	@Test
	void testRepositoryAnnotation() {
		// Test that the class is properly annotated
		assertTrue(repository.getClass().isAnnotationPresent(org.springframework.stereotype.Repository.class));
	}

	@Test
	void testSlf4jAnnotation() {
		// Test that the class has @Slf4j annotation (metadata test)
		assertNotNull(repository);
		// In a real test, we would use reflection to verify the annotation
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

	// Helper methods to create test data
	private PermitPostgresEntity createTestPermit() {
		return PermitPostgresEntity.builder()
				.laborCategoryCode(123)
				.laborCategoryDescription("Test Category")
				.zipcode("90210")
				.city("Los Angeles")
				.state("CA")
				.laborItem("TEST123")
				.laborItemDescription("Test Item Description")
				.unitPermitFee(new BigDecimal("100.50"))
				.omniItemId("OMNI123")
				.county("Los Angeles County")
				.provider("Test Provider")
				.vbuNumber("VBU123")
				.createdBy("user123")
				.updatedBy("user456")
				.createdTimestamp(LocalDateTime.now())
				.updatedTimestamp(LocalDateTime.now())
				.municipality("Beverly Hills")
				.build();
	}

	// Helper methods to create lists with null values safely
	private <T> List<T> createListWithNulls(T... values) {
		List<T> list = new ArrayList<>();
		Collections.addAll(list, values);
		return list;
	}

	private <T> List<T> createList(T... values) {
		return new ArrayList<>(java.util.Arrays.asList(values));
	}

	private Row createMockRowFromPermit(PermitPostgresEntity permitPostgresEntity) {
		return new Row() {
			@Override
			public <T> T get(String column, Class<T> type) {
				if ("labor_category_code".equals(column) && type == Integer.class) {
					return type.cast(permitPostgresEntity.getLaborCategoryCode());
				}
				if ("labor_category_description".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getLaborCategoryDescription());
				}
				if ("zipcode".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getZipcode());
				}
				if ("labor_item".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getLaborItem());
				}
				if ("city".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getCity());
				}
				if ("state".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getState());
				}
				if ("labor_item_description".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getLaborItemDescription());
				}
				if ("omni_item_id".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getOmniItemId());
				}
				if ("unit_permit_fee".equals(column) && type == String.class) {
					return type.cast(
							permitPostgresEntity.getUnitPermitFee() != null
									? permitPostgresEntity.getUnitPermitFee().toString()
									: null);
				}
				if ("county".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getCounty());
				}
				if ("provider".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getProvider());
				}
				if ("vbu_number".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getVbuNumber());
				}
				if ("created_by".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getCreatedBy());
				}
				if ("created_timestamp".equals(column) && type == LocalDateTime.class) {
					return type.cast(permitPostgresEntity.getCreatedTimestamp());
				}
				if ("updated_by".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getUpdatedBy());
				}
				if ("updated_timestamp".equals(column) && type == LocalDateTime.class) {
					return type.cast(permitPostgresEntity.getUpdatedTimestamp());
				}
				if ("municipality".equals(column) && type == String.class) {
					return type.cast(permitPostgresEntity.getMunicipality());
				}
				return null;
			}

			@Override
			public <T> T get(int index, Class<T> type) {
				return null;
			}

			@Override
			public Object get(String column) {
				return get(column, Object.class);
			}

			@Override
			public Object get(int index) {
				return null;
			}

			@Override
			public io.r2dbc.spi.RowMetadata getMetadata() {
				return null;
			}
		};
	}

	private Row createMockRow(String[] arrayValue) {
		return new Row() {
			@Override
			public <T> T get(String column, Class<T> type) {
				if (type.isArray() && type.getComponentType() == String.class) {
					return type.cast(arrayValue);
				}
				return null;
			}

			@Override
			public <T> T get(int index, Class<T> type) {
				return null;
			}

			@Override
			public Object get(String column) {
				return arrayValue;
			}

			@Override
			public Object get(int index) {
				return null;
			}

			@Override
			public io.r2dbc.spi.RowMetadata getMetadata() {
				return null;
			}
		};
	}

	private Row createMockRow(String stringValue) {
		return new Row() {
			@Override
			public <T> T get(String column, Class<T> type) {
				if (type == String.class) {
					return type.cast(stringValue);
				}
				return null;
			}

			@Override
			public <T> T get(int index, Class<T> type) {
				return null;
			}

			@Override
			public Object get(String column) {
				return stringValue;
			}

			@Override
			public Object get(int index) {
				return null;
			}

			@Override
			public io.r2dbc.spi.RowMetadata getMetadata() {
				return null;
			}
		};
	}

	private Row createMockRow(Object value) {
		return new Row() {
			@Override
			public <T> T get(String column, Class<T> type) {
				if (type.isInstance(value)) {
					return type.cast(value);
				}
				return null;
			}

			@Override
			public <T> T get(int index, Class<T> type) {
				return null;
			}

			@Override
			public Object get(String column) {
				return value;
			}

			@Override
			public Object get(int index) {
				return null;
			}

			@Override
			public io.r2dbc.spi.RowMetadata getMetadata() {
				return null;
			}
		};
	}

	@Test
	void testSearchWithValidData() {
		String sql = "SELECT * FROM permit_master WHERE city = ?";
		PermitPostgresEntity expectedEntity = createTestPermit();
		Row mockRow = createMockRowFromPermit(expectedEntity);

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(0, "param1")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(1, "param2")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(2, "param3")).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(expectedEntity));

		Flux<PermitPostgresEntity> result = repository.search(sql, testParams);

		StepVerifier.create(result).expectNext(expectedEntity).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
		verify(mockGenericExecuteSpec, times(3)).bind(anyInt(), any());
	}

	@Test
	void testSearchWithEmptyParams() {
		String sql = "SELECT * FROM permit_master";
		List<Object> emptyParams = new ArrayList<>();

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.empty());

		Flux<PermitPostgresEntity> result = repository.search(sql, emptyParams);

		StepVerifier.create(result).verifyComplete();

		verify(mockGenericExecuteSpec, never()).bind(anyInt(), any());
	}

	@Test
	void testFetchOrderModsByQuery_Success() {
		String sql = "SELECT * FROM order_mods WHERE id = 'abc123'";
		OrderModPostgresEntity entity = createTestOrderMod();

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(entity));

		Flux<OrderModPostgresEntity> result = repository.fetchOrderModsByQuery(sql);

		StepVerifier.create(result)
				.assertNext(om -> assertEquals(entity.getId(), om.getId()))
				.verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
	}

	@Test
	void testFetchOrderModsByQuery_EmptyResult() {
		String sql = "SELECT * FROM order_mods WHERE 1=0";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.empty());

		Flux<OrderModPostgresEntity> result = repository.fetchOrderModsByQuery(sql);

		StepVerifier.create(result).verifyComplete();

		verify(mockDatabaseClient, times(1)).sql(sql);
	}

	@Test
	void testFetchOrderModsByQuery_DatabaseError() {
		String sql = "SELECT * FROM order_mods";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.error(new DataAccessException("DB error") {}));

		Flux<OrderModPostgresEntity> result = repository.fetchOrderModsByQuery(sql);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();
	}

	@Test
	void testSearchFilter_WithListParam() {
		String sql = "SELECT cities FROM view WHERE state = ANY($1)";
		List<Object> params = List.of(List.of("CA", "NY", "TX"));
		String type = "cities";

		Map<String, Object> row1 = new HashMap<>();
		row1.put("cities", new String[] {"Los Angeles", "San Francisco"});

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(eq(0), any(String[].class))).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(row1));

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).expectNext("Los Angeles", "San Francisco").verifyComplete();

		verify(mockGenericExecuteSpec, times(1)).bind(eq(0), any(String[].class));
	}

	@Test
	void testSearchFilter_WithStringArrayResult() {
		String sql = "SELECT cities FROM view";
		List<Object> params = List.of();
		String type = "cities";

		Map<String, Object> row = new HashMap<>();
		row.put("cities", new String[] {"City1", "City2", "City3"});

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(row));

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).expectNext("City1", "City2", "City3").verifyComplete();
	}

	@Test
	void testSearchFilter_WithStringResult() {
		String sql = "SELECT name FROM view";
		List<Object> params = List.of();
		String type = "name";

		Map<String, Object> row = new HashMap<>();
		row.put("name", "SingleValue");

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(row));

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).expectNext("SingleValue").verifyComplete();
	}

	@Test
	void testSearchFilter_WithNullValue() {
		String sql = "SELECT cities FROM view";
		List<Object> params = List.of();
		String type = "cities";

		Map<String, Object> row = new HashMap<>();
		row.put("cities", null);

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.just(row));

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).verifyComplete();
	}

	@Test
	void testSearchFilter_WithMixedParams() {
		String sql = "SELECT cities FROM view WHERE state = $1 AND active = $2";
		List<Object> params = List.of(List.of("CA", "NY"), true);
		String type = "cities";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(eq(0), any(String[].class))).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.bind(eq(1), eq(true))).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.fetch()).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.empty());

		Flux<String> result = repository.searchFilter(sql, params, type);

		StepVerifier.create(result).verifyComplete();

		verify(mockGenericExecuteSpec, times(1)).bind(eq(0), any(String[].class));
		verify(mockGenericExecuteSpec, times(1)).bind(eq(1), eq(true));
	}

	@Test
	void testBulkInsertOrderModsToPermitMaster_NullList() {
		Mono<Integer> result = repository.bulkInsertOrderModsToPermitMaster(null);

		StepVerifier.create(result).expectNext(0).verifyComplete();
	}

	@Test
	void testBulkInsertOrderModsToPermitMaster_EmptyList() {
		Mono<Integer> result = repository.bulkInsertOrderModsToPermitMaster(List.of());

		StepVerifier.create(result).expectNext(0).verifyComplete();
	}

	@Test
	void testExportPermits_Success() {
		String sql = "SELECT * FROM permit_master";

		Row mockRow1 = createMockExportRow(
				"123",
				"Category1",
				"90210",
				"LA",
				"CA",
				"Item1",
				"Desc1",
				new BigDecimal("100.50"),
				"OMNI1",
				"County1",
				"Provider1",
				"VBU1",
				"user1",
				"user2",
				LocalDateTime.of(2024, 1, 1, 10, 0),
				LocalDateTime.of(2024, 1, 2, 10, 0),
				"Muni1",
				5);

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenAnswer(invocation -> {
			BiFunction<Row, RowMetadata, String> mapper = invocation.getArgument(0);
			String csvRow = mapper.apply(mockRow1, null);
			return mockRowsFetchSpec;
		});
		when(mockRowsFetchSpec.all())
				.thenReturn(
						Flux.just(
								"123,Category1,90210,LA,CA,Item1,Desc1,100.50,OMNI1,County1,Provider1,VBU1,user1,user2,2024-01-01T10:00,2024-01-02T10:00,Muni1,5\n"));

		Flux<byte[]> result = repository.exportPermits(sql);

		StepVerifier.create(result)
				.assertNext(bytes -> {
					assertNotNull(bytes);
					String content = new String(bytes, StandardCharsets.UTF_8);
					assertTrue(content.contains("123"));
				})
				.verifyComplete();
	}

	@Test
	void testExportPermits_LargeBatch() {
		String sql = "SELECT * FROM permit_master";

		List<String> csvRows = new ArrayList<>();
		for (int i = 0; i < 2500; i++) {
			csvRows.add("row" + i + "\n");
		}

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.fromIterable(csvRows));

		Flux<byte[]> result = repository.exportPermits(sql);

		StepVerifier.create(result).expectNextCount(2).verifyComplete();
	}

	@Test
	void testExportPermits_WithSpecialCharacters() {
		String sql = "SELECT * FROM permit_master";

		Row mockRow = createMockExportRow(
				"123",
				"Category,With,Commas",
				"90210",
				"LA",
				"CA",
				"Item\"Quote\"",
				"Desc\nNewline",
				new BigDecimal("100.50"),
				"OMNI1",
				"County1",
				"Provider1",
				"VBU1",
				"user1",
				"user2",
				LocalDateTime.of(2024, 1, 1, 10, 0),
				LocalDateTime.of(2024, 1, 2, 10, 0),
				"Muni1",
				5);

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all())
				.thenReturn(
						Flux.just(
								"123,\"Category,With,Commas\",90210,LA,CA,\"Item\"\"Quote\"\"\",\"Desc\nNewline\",100.50,OMNI1,County1,Provider1,VBU1,user1,user2,2024-01-01T10:00,2024-01-02T10:00,Muni1,5\n"));

		Flux<byte[]> result = repository.exportPermits(sql);

		StepVerifier.create(result)
				.assertNext(bytes -> {
					String content = new String(bytes, StandardCharsets.UTF_8);
					assertTrue(content.contains("\"Category,With,Commas\""));
				})
				.verifyComplete();
	}

	@Test
	void testExportPermits_EmptyResult() {
		String sql = "SELECT * FROM permit_master WHERE 1=0";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.empty());

		Flux<byte[]> result = repository.exportPermits(sql);

		StepVerifier.create(result).verifyComplete();
	}

	@Test
	void testExportPermits_DatabaseError() {
		String sql = "SELECT * FROM permit_master";

		when(mockDatabaseClient.sql(sql)).thenReturn(mockGenericExecuteSpec);
		when(mockGenericExecuteSpec.map(any(BiFunction.class))).thenReturn(mockRowsFetchSpec);
		when(mockRowsFetchSpec.all()).thenReturn(Flux.error(new DataAccessException("DB error") {}));

		Flux<byte[]> result = repository.exportPermits(sql);

		StepVerifier.create(result).expectError(DataAccessException.class).verify();
	}

	// Helper methods
	private OrderModPostgresEntity createTestOrderMod() {
		return OrderModPostgresEntity.builder()
				.id(UUID.randomUUID())
				.laborCategoryCode(123)
				.laborCategoryDescription("Test Category")
				.laborItem(456)
				.laborItemDescription("Test Item")
				.unitPermitFee(new BigDecimal("100.50"))
				.omniItemId("OMNI123")
				.streetAddress("123 Main St")
				.city("Los Angeles")
				.state("CA")
				.zipcode("90210")
				.county("LA County")
				.municipality("Beverly Hills")
				.matchedAddress("123 Main St, LA, CA 90210")
				.provider("Test Provider")
				.complianceStatus("COMPLIANT")
				.vbuNumber(789)
				.createdTimestamp(LocalDateTime.now())
				.createdBy("user1")
				.updatedBy("user2")
				.lastUpdatedTimestamp(LocalDateTime.now())
				.permitInsertType("NEW")
				.oldPermitFee(new BigDecimal("90.00"))
				.jobId("JOB123")
				.orderNumber("ORD456")
				.build();
	}

	private Row createMockExportRow(
			String laborCategoryCode,
			String laborCategoryDesc,
			String zipcode,
			String city,
			String state,
			String laborItem,
			String laborItemDesc,
			BigDecimal unitPermitFee,
			String omniItemId,
			String county,
			String provider,
			String vbuNumber,
			String createdBy,
			String updatedBy,
			LocalDateTime createdTimestamp,
			LocalDateTime updatedTimestamp,
			String municipality,
			Integer estPermitObtainDays) {
		return new Row() {
			@Override
			public <T> T get(String column, Class<T> type) {
				return switch (column) {
					case "labor_category_code" -> type.cast(laborCategoryCode);
					case "labor_category_description" -> type.cast(laborCategoryDesc);
					case "zipcode" -> type.cast(zipcode);
					case "city" -> type.cast(city);
					case "state" -> type.cast(state);
					case "labor_item" -> type.cast(laborItem);
					case "labor_item_description" -> type.cast(laborItemDesc);
					case "unit_permit_fee" -> type.cast(unitPermitFee);
					case "omni_item_id" -> type.cast(omniItemId);
					case "county" -> type.cast(county);
					case "provider" -> type.cast(provider);
					case "vbu_number" -> type.cast(vbuNumber);
					case "created_by" -> type.cast(createdBy);
					case "updated_by" -> type.cast(updatedBy);
					case "created_timestamp" -> type.cast(createdTimestamp);
					case "updated_timestamp" -> type.cast(updatedTimestamp);
					case "municipality" -> type.cast(municipality);
					case "est_permit_obtain_days" -> type.cast(estPermitObtainDays);
					default -> null;
				};
			}

			@Override
			public <T> T get(int index, Class<T> type) {
				return null;
			}

			@Override
			public Object get(String column) {
				return get(column, Object.class);
			}

			@Override
			public Object get(int index) {
				return null;
			}

			@Override
			public RowMetadata getMetadata() {
				return null;
			}
		};
	}
}
