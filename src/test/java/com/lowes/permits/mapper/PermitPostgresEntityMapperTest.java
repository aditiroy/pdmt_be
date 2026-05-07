package com.lowes.permits.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.MockedStatic;

import com.lowes.permits.dto.request.CreatePermitRequest;
import com.lowes.permits.dto.request.DeletePermitRequest;
import com.lowes.permits.dto.request.UpdatePermitRequest;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.MappingContext;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitDbKey;
import com.lowes.permits.model.PermitStatus;
import com.lowes.permits.model.User;
import com.lowes.permits.model.UserToken;
import com.lowes.permits.util.PermitUtils;

class PermitPostgresEntityMapperTest {

	private PermitMapper mapper;
	private CreatePermitRequest createPermitRequest;
	private DeletePermitRequest deletePermitRequest;
	private UpdatePermitRequest updatePermitRequest;
	private MappingContext mappingContext;

	@BeforeEach
	void setUp() {
		mapper = Mappers.getMapper(PermitMapper.class);
		createPermitRequest = createTestCreatePermitRequest();
		deletePermitRequest = createTestDeletePermitRequest();
		updatePermitRequest = createTestUpdatePermitRequest();
		mappingContext = createTestMappingContext();
	}

	@Test
	void testGeneratePermitDbIdWithNullRequest() {
		// Test generatePermitDbId with null request
		assertThrows(NullPointerException.class, () -> {
			PermitMapper.generatePermitDbId(null);
		});
	}

	@Test
	void testBuildAuditFromContextWithNullTokenUser() {
		// Test buildAuditFromContext with null token user
		MappingContext nullUserContext = new MappingContext();
		nullUserContext.setXUserToken("invalid-token");
		nullUserContext.setCurrentTraceId("trace-123");
		nullUserContext.setCallerApp("test-app");

		// Mock PermitUtils to return null user
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken nullUserToken = new UserToken();
			nullUserToken.setUser(null);
			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("invalid-token"))
					.thenReturn(nullUserToken);

			Audit result = PermitMapper.buildAuditFromContext(nullUserContext);

			assertNotNull(result);
			assertNull(result.getCreatedByUserGroup());
			assertNull(result.getCreatedByUserRole());
			assertNull(result.getLastModifiedByUserGroup());
			assertNull(result.getLastModifiedByUserRole());
			assertNull(result.getCreatedById());
			assertNull(result.getCreatedByEmailId());
			assertNull(result.getCreatedByJobCode());
			assertNull(result.getLastModifiedByEmailId());
			assertNull(result.getLastModifiedByJobCode());
			assertNull(result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
		}
	}

	@Test
	void testBuildAddressFromPermitDbIdWithNullPermitDbId() {
		// Test buildAddressFromPermitDbId with null permitDbId
		assertThrows(NullPointerException.class, () -> {
			PermitMapper.buildAddressFromPermitDbId(null);
		});
	}

	@Test
	void testBuildLaborCategoryFromPermitDbIdWithNullPermitDbId() {
		// Test buildLaborCategoryFromPermitDbId with null permitDbId
		assertThrows(NullPointerException.class, () -> {
			PermitMapper.buildLaborCategoryFromPermitDbId(null);
		});
	}

	@Test
	void testExtractLaborItemFromPermitDbIdWithNullPermitDbId() {
		// Test extractLaborItemFromPermitDbId with null permitDbId
		assertThrows(NullPointerException.class, () -> {
			PermitMapper.extractLaborItemFromPermitDbId(null);
		});
	}

	@Test
	void testCreateRequestToEntityWithNullContext() {
		// Test createRequestToEntity with null context
		assertThrows(NullPointerException.class, () -> {
			mapper.createRequestToEntity(createPermitRequest, null);
		});
	}

	@Test
	void testCreateRequestToEntityWithValidData() {
		// Test createRequestToEntity with valid data
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			// Mock generatePermitDbId
			mockedPermitUtils
					.when(() -> PermitUtils.generatePermitDbId(
							eq("LOS ANGELES"), eq(123), eq(456), eq("90210"), eq("LOS ANGELES"), eq("BEVERLY HILLS")))
					.thenReturn("LOS ANGELES|123|456|90210|LOS ANGELES|BEVERLY HILLS");

			// Mock parseUserToken
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("John");
			testUserToken.setLastName("Doe");
			testUserToken.setUserGroup("ADMIN");
			User testUser = new User();
			testUser.setId("user123");
			testUser.setUserRole("ADMIN");
			testUser.setEmail("john.doe@example.com");
			testUser.setJobCode("JOB123");
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-user-token"))
					.thenReturn(testUserToken);

			PermitMongoEntity result = mapper.createRequestToEntity(createPermitRequest, mappingContext);

			assertNotNull(result);
			assertEquals("LOS ANGELES|123|456|90210|LOS ANGELES|BEVERLY HILLS", result.getPermitDbId());
			assertNotNull(result.getAudit());
			assertEquals("John Doe", result.getAudit().getCreatedByName());
			assertEquals("John Doe", result.getAudit().getLastModifiedByName());
			assertEquals("ADMIN", result.getAudit().getCreatedByUserGroup());
			assertEquals("ADMIN", result.getAudit().getCreatedByUserRole());
			assertEquals("ADMIN", result.getAudit().getLastModifiedByUserGroup());
			assertEquals("ADMIN", result.getAudit().getLastModifiedByUserRole());
			assertEquals("user123", result.getAudit().getCreatedById());
			assertEquals("john.doe@example.com", result.getAudit().getCreatedByEmailId());
			assertEquals("JOB123", result.getAudit().getCreatedByJobCode());
			assertEquals("john.doe@example.com", result.getAudit().getLastModifiedByEmailId());
			assertEquals("JOB123", result.getAudit().getLastModifiedByJobCode());
			assertEquals("user123", result.getAudit().getLastModifiedById());
			assertEquals("test-app", result.getAudit().getCreatedByApplicationName());
			assertEquals("test-app", result.getAudit().getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getAudit().getLastModifiedByTraceId());
			assertEquals(OperationType.CREATE, result.getOperationType());
			assertEquals(PermitStatus.NEW, result.getStatus());
			assertNull(result.getId());
			assertNull(result.getOldUnitPermitFee());
			assertNull(result.getRetryCount());
			assertNull(result.getErrorMessage());
		}
	}

	@Test
	void testDeleteRequestToEntityWithValidData() {
		// Test deleteRequestToEntity with valid data
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			// Mock decodePermitDbId
			PermitDbKey decodedKey = new PermitDbKey();
			decodedKey.setCity("Los Angeles");
			decodedKey.setLaborCategoryCode(123);
			decodedKey.setLaborItem(456);
			decodedKey.setZipCode("90210");
			decodedKey.setCounty("Los Angeles County");
			decodedKey.setMunicipality("Beverly Hills");

			mockedPermitUtils
					.when(() -> PermitUtils.decodePermitDbId("test-permit-id"))
					.thenReturn(decodedKey);

			// Mock parseUserToken
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("Jane");
			testUserToken.setLastName("Smith");
			testUserToken.setUserGroup("ADMIN");
			User testUser = new User();
			testUser.setId("user456");
			testUser.setUserRole("ADMIN");
			testUser.setEmail("jane.smith@example.com");
			testUser.setJobCode("JOB456");
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-user-token"))
					.thenReturn(testUserToken);

			PermitMongoEntity result = mapper.deleteRequestToEntity(deletePermitRequest, mappingContext);

			assertNotNull(result);
			assertEquals("test-permit-id", result.getPermitDbId());
			assertNotNull(result.getAudit());
			assertEquals("Jane Smith", result.getAudit().getCreatedByName());
			assertEquals("Jane Smith", result.getAudit().getLastModifiedByName());
			assertEquals("ADMIN", result.getAudit().getCreatedByUserGroup());
			assertEquals("ADMIN", result.getAudit().getCreatedByUserRole());
			assertEquals("ADMIN", result.getAudit().getLastModifiedByUserGroup());
			assertEquals("ADMIN", result.getAudit().getLastModifiedByUserRole());
			assertEquals("user456", result.getAudit().getCreatedById());
			assertEquals("jane.smith@example.com", result.getAudit().getCreatedByEmailId());
			assertEquals("JOB456", result.getAudit().getCreatedByJobCode());
			assertEquals("jane.smith@example.com", result.getAudit().getLastModifiedByEmailId());
			assertEquals("JOB456", result.getAudit().getLastModifiedByJobCode());
			assertEquals("user456", result.getAudit().getLastModifiedById());
			assertEquals("test-app", result.getAudit().getCreatedByApplicationName());
			assertEquals("test-app", result.getAudit().getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getAudit().getLastModifiedByTraceId());
			assertEquals(OperationType.CREATE, result.getOperationType());
			assertEquals(PermitStatus.NEW, result.getStatus());

			// Verify mapped fields from permitDbId
			assertNotNull(result.getAddress());
			assertEquals("Los Angeles", result.getAddress().getCity());
			assertEquals("90210", result.getAddress().getZipCode());
			assertEquals("Los Angeles County", result.getAddress().getCounty());
			assertEquals("Beverly Hills", result.getAddress().getMunicipality());

			assertEquals(Integer.valueOf(456), result.getLaborItem());

			assertEquals(new BigDecimal("50.25"), result.getUnitPermitFee());

			// Verify ignored fields are null
			assertNull(result.getId());
			assertNull(result.getLaborItemDescription());
			assertNull(result.getOldUnitPermitFee());
			assertNull(result.getOmniItemId());
			assertNull(result.getProvider());
			assertNull(result.getRetryCount());
			assertNull(result.getErrorMessage());
		}
	}

	@Test
	void testGeneratePermitDbIdWithValidData() {
		// Test generatePermitDbId with valid data
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			mockedPermitUtils
					.when(() -> PermitUtils.generatePermitDbId(
							eq("LOS ANGELES"), eq(123), eq(456), eq("90210"), eq("LOS ANGELES"), eq("BEVERLY HILLS")))
					.thenReturn("LOS ANGELES|123|456|90210|LOS ANGELES|BEVERLY HILLS");

			String result = PermitMapper.generatePermitDbId(createPermitRequest);

			assertEquals("LOS ANGELES|123|456|90210|LOS ANGELES|BEVERLY HILLS", result);
		}
	}

	@Test
	void testBuildAuditFromContextWithValidData() {
		// Test buildAuditFromContext with valid data
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("Alice");
			testUserToken.setLastName("Johnson");
			testUserToken.setUserGroup("ADMIN");
			User testUser = new User();
			testUser.setId("user999");
			testUser.setUserRole("ADMIN");
			testUser.setEmail("alice.johnson@example.com");
			testUser.setJobCode("JOB999");
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-user-token"))
					.thenReturn(testUserToken);

			Audit result = PermitMapper.buildAuditFromContext(mappingContext);

			assertNotNull(result);
			assertEquals("Alice Johnson", result.getCreatedByName());
			assertEquals("Alice Johnson", result.getLastModifiedByName());
			assertEquals("ADMIN", result.getCreatedByUserGroup());
			assertEquals("ADMIN", result.getCreatedByUserRole());
			assertEquals("ADMIN", result.getLastModifiedByUserGroup());
			assertEquals("ADMIN", result.getLastModifiedByUserRole());
			assertEquals("user999", result.getCreatedById());
			assertEquals("alice.johnson@example.com", result.getCreatedByEmailId());
			assertEquals("JOB999", result.getCreatedByJobCode());
			assertEquals("alice.johnson@example.com", result.getLastModifiedByEmailId());
			assertEquals("JOB999", result.getLastModifiedByJobCode());
			assertEquals("user999", result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
			assertTrue(result.getCreatedAt() > 0);
			assertTrue(result.getLastModifiedAt() > 0);
		}
	}

	@Test
	void testBuildAuditFromContextWithValidDataForDelete() {
		// Test buildAuditFromContext with valid data for delete
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("Charlie");
			testUserToken.setLastName("Brown");
			testUserToken.setUserGroup("ADMIN");
			User testUser = new User();
			testUser.setId("user888");
			testUser.setUserRole("ADMIN");
			testUser.setEmail("charlie.brown@example.com");
			testUser.setJobCode("JOB888");
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-user-token"))
					.thenReturn(testUserToken);

			Audit result = PermitMapper.buildAuditFromContext(mappingContext);

			assertNotNull(result);
			assertEquals("Charlie Brown", result.getCreatedByName());
			assertEquals("Charlie Brown", result.getLastModifiedByName());
			assertEquals("ADMIN", result.getCreatedByUserGroup());
			assertEquals("ADMIN", result.getCreatedByUserRole());
			assertEquals("ADMIN", result.getLastModifiedByUserGroup());
			assertEquals("ADMIN", result.getLastModifiedByUserRole());
			assertEquals("user888", result.getCreatedById());
			assertEquals("charlie.brown@example.com", result.getCreatedByEmailId());
			assertEquals("JOB888", result.getCreatedByJobCode());
			assertEquals("charlie.brown@example.com", result.getLastModifiedByEmailId());
			assertEquals("JOB888", result.getLastModifiedByJobCode());
			assertEquals("user888", result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
			assertTrue(result.getCreatedAt() > 0);
			assertTrue(result.getLastModifiedAt() > 0);
		}
	}

	@Test
	void testBuildAuditFromContextWithNullTokenUserForUpdate() {
		// Test buildAuditFromContext with null token user for update
		MappingContext nullUserContext = new MappingContext();
		nullUserContext.setXUserToken("invalid-token");
		nullUserContext.setCurrentTraceId("trace-123");
		nullUserContext.setCallerApp("test-app");

		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken nullUserToken = new UserToken();
			nullUserToken.setUser(null);
			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("invalid-token"))
					.thenReturn(nullUserToken);

			Audit result = PermitMapper.buildAuditFromContext(nullUserContext);

			assertNotNull(result);
			assertNull(result.getCreatedByUserRole());
			assertNull(result.getLastModifiedByUserGroup());
			assertNull(result.getLastModifiedByUserRole());
			assertNull(result.getCreatedById());
			assertNull(result.getCreatedByEmailId());
			assertNull(result.getCreatedByJobCode());
			assertNull(result.getLastModifiedByEmailId());
			assertNull(result.getLastModifiedByJobCode());
			assertNull(result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
		}
	}

	@Test
	void testBuildAuditFromContextWithNullTokenUserForDelete() {
		// Test buildAuditFromContext with null token user for delete
		MappingContext nullUserContext = new MappingContext();
		nullUserContext.setXUserToken("invalid-token");
		nullUserContext.setCurrentTraceId("trace-123");
		nullUserContext.setCallerApp("test-app");

		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken nullUserToken = new UserToken();
			nullUserToken.setUser(null);
			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("invalid-token"))
					.thenReturn(nullUserToken);

			Audit result = PermitMapper.buildAuditFromContext(nullUserContext);

			assertNotNull(result);
			assertNull(result.getLastModifiedByUserGroup());
			assertNull(result.getLastModifiedByUserRole());
			assertNull(result.getCreatedById());
			assertNull(result.getCreatedByEmailId());
			assertNull(result.getCreatedByJobCode());
			assertNull(result.getLastModifiedByEmailId());
			assertNull(result.getLastModifiedByJobCode());
			assertNull(result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
		}
	}

	@Test
	void testAuditWithPartialNullUserTokenFields() {
		// Test audit building when user token has partial null fields
		MappingContext testContext = new MappingContext();
		testContext.setXUserToken("test-token");
		testContext.setCurrentTraceId("trace-123");
		testContext.setCallerApp("test-app");

		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("John");
			testUserToken.setLastName(null);
			testUserToken.setUserGroup("ADMIN");
			User testUser = new User();
			testUser.setId("user123");
			testUser.setUserRole(null);
			testUser.setEmail(null);
			testUser.setJobCode(null);
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-token"))
					.thenReturn(testUserToken);

			Audit result = PermitMapper.buildAuditFromContext(testContext);

			assertNotNull(result);
			assertEquals("John", result.getCreatedByName());
			assertEquals("John", result.getLastModifiedByName());
			assertEquals("ADMIN", result.getCreatedByUserGroup());
			assertNull(result.getCreatedByUserRole());
			assertEquals("ADMIN", result.getLastModifiedByUserGroup());
			assertNull(result.getLastModifiedByUserRole());
			assertEquals("user123", result.getCreatedById());
			assertNull(result.getCreatedByEmailId());
			assertNull(result.getCreatedByJobCode());
			assertNull(result.getLastModifiedByEmailId());
			assertNull(result.getLastModifiedByJobCode());
			assertEquals("user123", result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
		}
	}

	@Test
	void testMapperInterface() {
		// Test that the class is properly annotated as a MapStruct mapper
		assertNotNull(mapper);
		assertInstanceOf(PermitMapper.class, mapper);
	}

	@Test
	void testMapperComponentModel() {
		// Test that the mapper uses spring component model
		// This is verified by the @Mapper(componentModel = "spring") annotation
		assertNotNull(mapper);
	}

	@Test
	void testPermitDbIdDecoding() {
		// Test that permitDbId is correctly decoded in all mapping methods
		String permitDbId = "TestCity|789|123|12345|TestCounty|TestMunicipality";

		// Mock PermitUtils to return the expected decoded key
		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			PermitDbKey decodedKey = new PermitDbKey();
			decodedKey.setCity("TestCity");
			decodedKey.setLaborCategoryCode(789);
			decodedKey.setLaborItem(123);
			decodedKey.setZipCode("12345");
			decodedKey.setCounty("TestCounty");
			decodedKey.setMunicipality("TestMunicipality");

			mockedPermitUtils
					.when(() -> PermitUtils.decodePermitDbId(permitDbId))
					.thenReturn(decodedKey);

			// Test address building
			Address address = PermitMapper.buildAddressFromPermitDbId(permitDbId);
			assertEquals("TestCity", address.getCity());
			assertEquals("12345", address.getZipCode());
			assertEquals("TestCounty", address.getCounty());
			assertEquals("TestMunicipality", address.getMunicipality());

			// Test labor category building
			LaborCategory laborCategory = PermitMapper.buildLaborCategoryFromPermitDbId(permitDbId);
			assertEquals(Integer.valueOf(789), laborCategory.getCode());

			// Test labor item extraction
			Integer laborItem = PermitMapper.extractLaborItemFromPermitDbId(permitDbId);
			assertEquals(Integer.valueOf(123), laborItem);
		}
	}

	@Test
	void testAuditWithNullUserTokenFields() {
		// Test audit building when user token has null fields
		MappingContext testContext = new MappingContext();
		testContext.setXUserToken("test-token");
		testContext.setCurrentTraceId("trace-123");
		testContext.setCallerApp("test-app");

		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName(null);
			testUserToken.setLastName(null);
			testUserToken.setUserGroup(null);
			testUserToken.setUser(null);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-token"))
					.thenReturn(testUserToken);

			Audit result = PermitMapper.buildAuditFromContext(testContext);

			assertNotNull(result);
			assertNull(result.getCreatedByUserGroup());
			assertNull(result.getCreatedByUserRole());
			assertNull(result.getLastModifiedByUserGroup());
			assertNull(result.getLastModifiedByUserRole());
			assertNull(result.getCreatedById());
			assertNull(result.getCreatedByEmailId());
			assertNull(result.getCreatedByJobCode());
			assertNull(result.getLastModifiedByEmailId());
			assertNull(result.getLastModifiedByJobCode());
			assertNull(result.getLastModifiedById());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
		}
	}

	@Test
	void testAuditWithEmptyUserTokenFields() {
		// Test audit building when user token has empty fields
		MappingContext testContext = new MappingContext();
		testContext.setXUserToken("test-token");
		testContext.setCurrentTraceId("trace-123");
		testContext.setCallerApp("test-app");

		try (MockedStatic<PermitUtils> mockedPermitUtils = mockStatic(PermitUtils.class)) {
			UserToken testUserToken = new UserToken();
			testUserToken.setFirstName("");
			testUserToken.setLastName("");
			testUserToken.setUserGroup("");
			User testUser = new User();
			testUser.setId("");
			testUser.setUserRole("");
			testUser.setEmail("");
			testUser.setJobCode("");
			testUserToken.setUser(testUser);

			mockedPermitUtils
					.when(() -> PermitUtils.parseUserToken("test-token"))
					.thenReturn(testUserToken);

			Audit result = PermitMapper.buildAuditFromContext(testContext);

			assertNotNull(result);
			assertEquals("", result.getCreatedByUserGroup());
			assertEquals("", result.getCreatedByUserRole());
			assertEquals("", result.getLastModifiedByUserGroup());
			assertEquals("", result.getLastModifiedByUserRole());
			assertEquals("", result.getCreatedById());
			assertEquals("", result.getCreatedByEmailId());
			assertEquals("", result.getCreatedByJobCode());
			assertEquals("", result.getLastModifiedByEmailId());
			assertEquals("", result.getLastModifiedByJobCode());
			assertEquals("", result.getLastModifiedById());
			assertEquals("test-app", result.getCreatedByApplicationName());
			assertEquals("test-app", result.getLastModifiedByApplicationName());
			assertEquals("trace-123", result.getLastModifiedByTraceId());
		}
	}

	// Helper methods to create test objects
	private CreatePermitRequest createTestCreatePermitRequest() {
		CreatePermitRequest request = new CreatePermitRequest();
		Address address = new Address();
		address.setCity("Los Angeles");
		address.setZipCode("90210");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		request.setAddress(address);

		LaborCategory laborCategory = new LaborCategory();
		laborCategory.setCode(123);
		request.setLaborCategory(laborCategory);
		request.setLaborItem(456);

		return request;
	}

	private DeletePermitRequest createTestDeletePermitRequest() {
		DeletePermitRequest request = new DeletePermitRequest();
		request.setPermitDbId("test-permit-id");
		request.setUnitPermitFee(new BigDecimal("50.25"));
		return request;
	}

	private UpdatePermitRequest createTestUpdatePermitRequest() {
		UpdatePermitRequest request = new UpdatePermitRequest();
		request.setPermitDbId("test-permit-id");
		request.setOldUnitPermitFee(new BigDecimal("75.50"));
		request.setUnitPermitFee(new BigDecimal("100.75"));
		return request;
	}

	private MappingContext createTestMappingContext() {
		MappingContext context = new MappingContext();
		context.setXUserToken("test-user-token");
		context.setCurrentTraceId("trace-123");
		context.setCallerApp("test-app");
		context.setOperationType(OperationType.CREATE);
		context.setStatus(PermitStatus.NEW);
		return context;
	}
}
