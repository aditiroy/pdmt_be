package com.lowes.permits.mapper;

import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.lowes.permits.dto.response.ActivityResponse;
import com.lowes.permits.entity.PermitMongoEntity;
import com.lowes.permits.model.Address;
import com.lowes.permits.model.Audit;
import com.lowes.permits.model.LaborCategory;
import com.lowes.permits.model.OperationType;
import com.lowes.permits.model.PermitStatus;

class ActivityMapperTest {

	private PermitMapper activityMapper;
	private PermitMongoEntity testEntity;

	@BeforeEach
	void setUp() {
		activityMapper = Mappers.getMapper(PermitMapper.class);
		testEntity = createTestPermitEntity();
	}

	@Test
	void testToActivityResponseWithValidEntity() {
		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertEquals(testEntity.getId(), result.getId());
		assertEquals(testEntity.getPermitDbId(), result.getPermitDbId());
		assertEquals(testEntity.getStatus(), result.getStatus());
		assertEquals(testEntity.getOperationType(), result.getOperationType());
		assertEquals(testEntity.getUnitPermitFee(), result.getUnitPermitFee());
		assertEquals(testEntity.getOldUnitPermitFee(), result.getOldUnitPermitFee());
		assertEquals(testEntity.getLaborItem(), result.getLaborItem());
		assertEquals(testEntity.getLaborItemDescription(), result.getLaborItemDescription());
		assertEquals(testEntity.getOmniItemId(), result.getOmniItemId());
		assertEquals(testEntity.getLaborCategory(), result.getLaborCategory());
		assertEquals(testEntity.getAddress(), result.getAddress());
		assertEquals(testEntity.getAudit(), result.getAudit());
	}

	@Test
	void testToActivityResponseWithNullAddress() {
		testEntity.setAddress(null);

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNull(result.getAddress());
	}

	@Test
	void testToActivityResponseWithDummyCountyValue() {
		Address address = testEntity.getAddress();
		address.setCounty(DUMMY_VALUE);

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNotNull(result.getAddress());
		assertNull(result.getAddress().getCounty());
		assertEquals("CA", result.getAddress().getState());
	}

	@Test
	void testToActivityResponseWithDummyMunicipalityValue() {
		Address address = testEntity.getAddress();
		address.setMunicipality(DUMMY_VALUE);

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNotNull(result.getAddress());
		assertNull(result.getAddress().getMunicipality());
		assertEquals("Beverly Hills", result.getAddress().getCity());
		assertEquals("CA", result.getAddress().getState());
	}

	@Test
	void testToActivityResponseWithBothDummyValues() {
		Address address = testEntity.getAddress();
		address.setCounty(DUMMY_VALUE);
		address.setMunicipality(DUMMY_VALUE);

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNotNull(result.getAddress());
		assertNull(result.getAddress().getCounty());
		assertNull(result.getAddress().getMunicipality());
		assertEquals("Beverly Hills", result.getAddress().getCity());
		assertEquals("CA", result.getAddress().getState());
	}

	@Test
	void testToActivityResponseWithCaseInsensitiveDummyValues() {
		Address address = testEntity.getAddress();
		address.setCounty("dummy_value");
		address.setMunicipality("DUMMY_value");

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNotNull(result.getAddress());
	}

	@Test
	void testToActivityResponseWithNullFields() {
		PermitMongoEntity nullFieldEntity = new PermitMongoEntity();
		nullFieldEntity.setId("test-id");
		nullFieldEntity.setPermitDbId("test-permit-db-id");
		nullFieldEntity.setStatus(PermitStatus.NEW);
		nullFieldEntity.setOperationType(OperationType.CREATE);
		nullFieldEntity.setUnitPermitFee(null);
		nullFieldEntity.setOldUnitPermitFee(null);
		nullFieldEntity.setLaborItem(null);
		nullFieldEntity.setLaborItemDescription(null);
		nullFieldEntity.setOmniItemId(null);
		nullFieldEntity.setLaborCategory(null);
		nullFieldEntity.setAddress(null);
		nullFieldEntity.setAudit(null);

		ActivityResponse result = activityMapper.toActivityResponse(nullFieldEntity);

		assertNotNull(result);
		assertEquals("test-id", result.getId());
		assertEquals("test-permit-db-id", result.getPermitDbId());
		assertEquals(PermitStatus.NEW, result.getStatus());
		assertEquals(OperationType.CREATE, result.getOperationType());
		assertNull(result.getUnitPermitFee());
		assertNull(result.getOldUnitPermitFee());
		assertNull(result.getLaborItem());
		assertNull(result.getLaborItemDescription());
		assertNull(result.getOmniItemId());
		assertNull(result.getLaborCategory());
		assertNull(result.getAddress());
		assertNull(result.getAudit());
	}

	@Test
	void testToActivityResponseWithEmptyAddress() {
		Address emptyAddress = new Address();
		testEntity.setAddress(emptyAddress);

		ActivityResponse result = activityMapper.toActivityResponse(testEntity);

		assertNotNull(result);
		assertNotNull(result.getAddress());
		assertNull(result.getAddress().getCity());
		assertNull(result.getAddress().getState());
		assertNull(result.getAddress().getCounty());
		assertNull(result.getAddress().getMunicipality());
		assertNull(result.getAddress().getZipCode());
	}

	@Test
	void testComponentAnnotation() {
		assertNotNull(activityMapper);
	}

	private PermitMongoEntity createTestPermitEntity() {
		Address address = new Address();
		address.setCity("Beverly Hills");
		address.setState("CA");
		address.setCounty("Los Angeles County");
		address.setMunicipality("Beverly Hills");
		address.setZipCode("90210");

		Audit audit = new Audit();
		audit.setCreatedByName("user123");
		audit.setLastModifiedByName("user456");
		audit.setCreatedAt(System.currentTimeMillis());
		audit.setLastModifiedAt(System.currentTimeMillis());

		LaborCategory laborCategory = new LaborCategory(123, "Test Labor Category");

		return PermitMongoEntity.builder()
				.id("test-id")
				.permitDbId("test-permit-db-id")
				.status(PermitStatus.NEW)
				.operationType(OperationType.CREATE)
				.unitPermitFee(new BigDecimal("100.50"))
				.oldUnitPermitFee(new BigDecimal("50.25"))
				.laborItem(123)
				.laborItemDescription("Test Description")
				.omniItemId("OMNI123")
				.laborCategory(laborCategory)
				.address(address)
				.audit(audit)
				.build();
	}
}
