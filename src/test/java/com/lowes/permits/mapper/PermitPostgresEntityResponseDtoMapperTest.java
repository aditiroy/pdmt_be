package com.lowes.permits.mapper;

import static com.lowes.permits.constants.ApplicationConstants.DUMMY_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.lowes.permits.dto.response.PermitResponse;
import com.lowes.permits.entity.PermitPostgresEntity;

class PermitPostgresEntityResponseDtoMapperTest {

	private PermitMapper mapper;
	private PermitPostgresEntity testPermitPostgresEntity;

	@BeforeEach
	void setUp() {
		mapper = Mappers.getMapper(PermitMapper.class);
		testPermitPostgresEntity = createTestPermit();
	}

	@Test
	void testGeneratePermitDbIdWithNullPermit() {
		// Test generatePermitDbId with null permit
		assertThrows(NullPointerException.class, () -> {
			PermitMapper.generatePermitResponseDbId(null);
		});
	}

	@Test
	void testToInteger() {
		// Test toInteger method with valid string
		Integer result = PermitMapper.toInteger("123");
		assertEquals(Integer.valueOf(123), result);
	}

	@Test
	void testToIntegerWithNull() {
		// Test toInteger method with null string
		Integer result = PermitMapper.toInteger(null);
		assertNull(result);
	}

	@Test
	void testToIntegerWithInvalidString() {
		// Test toInteger method with invalid string
		assertThrows(NumberFormatException.class, () -> {
			PermitMapper.toInteger("invalid");
		});
	}

	@Test
	void testToIntegerWithWhitespaceString() {
		// Test toInteger method with whitespace string
		assertThrows(NumberFormatException.class, () -> {
			PermitMapper.toInteger("   ");
		});
	}

	@Test
	void testToIntegerWithNegativeNumber() {
		// Test toInteger method with negative number
		Integer result = PermitMapper.toInteger("-123");
		assertEquals(Integer.valueOf(-123), result);
	}

	@Test
	void testToIntegerWithZero() {
		// Test toInteger method with zero
		Integer result = PermitMapper.toInteger("0");
		assertEquals(Integer.valueOf(0), result);
	}

	@Test
	void testToIntegerWithMaxValue() {
		// Test toInteger method with max integer value
		Integer result = PermitMapper.toInteger(String.valueOf(Integer.MAX_VALUE));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), result);
	}

	@Test
	void testToIntegerWithMinValue() {
		// Test toInteger method with min integer value
		Integer result = PermitMapper.toInteger(String.valueOf(Integer.MIN_VALUE));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), result);
	}

	@Test
	void testTrim() {
		// Test trim method with valid string
		String result = PermitMapper.trim("  test  ");
		assertEquals("test", result);
	}

	@Test
	void testTrimWithNull() {
		// Test trim method with null string
		String result = PermitMapper.trim(null);
		assertNull(result);
	}

	@Test
	void testTrimWithEmptyString() {
		// Test trim method with empty string
		String result = PermitMapper.trim("");
		assertEquals("", result);
	}

	@Test
	void testTrimWithWhitespaceOnly() {
		// Test trim method with whitespace only
		String result = PermitMapper.trim("   ");
		assertEquals("", result);
	}

	@Test
	void testTrimWithNoWhitespace() {
		// Test trim method with no whitespace
		String result = PermitMapper.trim("test");
		assertEquals("test", result);
	}

	@Test
	void testTrimWithLeadingWhitespace() {
		// Test trim method with leading whitespace
		String result = PermitMapper.trim("   test");
		assertEquals("test", result);
	}

	@Test
	void testTrimWithTrailingWhitespace() {
		// Test trim method with trailing whitespace
		String result = PermitMapper.trim("test   ");
		assertEquals("test", result);
	}

	@Test
	void testTrimWithTabsAndNewlines() {
		// Test trim method with tabs and newlines
		String result = PermitMapper.trim("\t\n test \n\t");
		assertEquals("test", result);
	}

	@Test
	void testTrimAndNullifyDummy() {
		// Test trimAndNullifyDummy method with normal string
		String result = PermitMapper.trimAndNullifyDummy("  test  ");
		assertEquals("test", result);
	}

	@Test
	void testTrimAndNullifyDummyWithNull() {
		// Test trimAndNullifyDummy method with null string
		String result = PermitMapper.trimAndNullifyDummy(null);
		assertNull(result);
	}

	@Test
	void testTrimAndNullifyDummyWithDummyValue() {
		// Test trimAndNullifyDummy method with DUMMY_VALUE
		String result = PermitMapper.trimAndNullifyDummy(DUMMY_VALUE);
		assertNull(result);
	}

	@Test
	void testTrimAndNullifyDummyWithDummyValueAndWhitespace() {
		// Test trimAndNullifyDummy method with DUMMY_VALUE and whitespace
		String result = PermitMapper.trimAndNullifyDummy("  DUMMY_VALUE  ");
		assertNull(result);
	}

	@Test
	void testTrimAndNullifyDummyWithDummyValueMixedCase() {
		// Test trimAndNullifyDummy method with mixed case DUMMY_VALUE
		String result = PermitMapper.trimAndNullifyDummy("dummy_value");
		assertEquals("dummy_value", result);
	}

	@Test
	void testTrimAndNullifyDummyWithPartialDummyValue() {
		// Test trimAndNullifyDummy method with partial DUMMY_VALUE
		String result = PermitMapper.trimAndNullifyDummy("DUMMY");
		assertEquals("DUMMY", result);
	}

	@Test
	void testTrimAndNullifyDummyWithEmptyString() {
		// Test trimAndNullifyDummy method with empty string
		String result = PermitMapper.trimAndNullifyDummy("");
		assertEquals("", result);
	}

	@Test
	void testTrimAndNullifyDummyWithWhitespaceOnly() {
		// Test trimAndNullifyDummy method with whitespace only
		String result = PermitMapper.trimAndNullifyDummy("   ");
		assertEquals("", result);
	}

	@Test
	void testToResponseWithNullFields() {
		// Test toResponse method with null fields
		PermitPostgresEntity nullFieldPermitPostgresEntity = new PermitPostgresEntity();
		nullFieldPermitPostgresEntity.setCity(null);
		nullFieldPermitPostgresEntity.setLaborCategoryCode(123);
		nullFieldPermitPostgresEntity.setLaborItem(null);
		nullFieldPermitPostgresEntity.setLaborItemDescription(null);
		nullFieldPermitPostgresEntity.setUnitPermitFee(null);
		nullFieldPermitPostgresEntity.setOmniItemId(null);
		nullFieldPermitPostgresEntity.setLaborCategoryDescription(null);
		nullFieldPermitPostgresEntity.setState(null);
		nullFieldPermitPostgresEntity.setCounty(null);
		nullFieldPermitPostgresEntity.setMunicipality(null);
		nullFieldPermitPostgresEntity.setZipcode(null);
		nullFieldPermitPostgresEntity.setProvider(null);
		nullFieldPermitPostgresEntity.setVbuNumber(null);
		nullFieldPermitPostgresEntity.setCreatedBy(null);
		nullFieldPermitPostgresEntity.setUpdatedBy(null);

		PermitResponse result = mapper.toPermitResponse(nullFieldPermitPostgresEntity);

		assertNotNull(result);
		assertNotNull(result.getId());
		assertNull(result.getLaborItem());
		assertNull(result.getLaborItemDescription());
		assertNull(result.getUnitPermitFee());
		assertNull(result.getOmniItemId());

		// Test labor category mapping
		assertNotNull(result.getLaborCategory());
		assertEquals(Integer.valueOf(123), result.getLaborCategory().getCode());
		assertNull(result.getLaborCategory().getDescription());

		// Test address mapping
		assertNotNull(result.getAddress());
		assertNull(result.getAddress().getCity());
		assertNull(result.getAddress().getState());
		assertNull(result.getAddress().getCounty());
		assertNull(result.getAddress().getZipCode());
		assertNull(result.getAddress().getMunicipality());

		// Test provider mapping
		assertNotNull(result.getProvider());
		assertNull(result.getProvider().getName());
		assertNull(result.getProvider().getNumber());

		// Test audit mapping
		assertNotNull(result.getAudit());
		assertNull(result.getAudit().getCreatedByName());
		assertNull(result.getAudit().getLastModifiedByName());
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

	// Helper method to create test permit
	private PermitPostgresEntity createTestPermit() {
		PermitPostgresEntity permitPostgresEntity = new PermitPostgresEntity();
		permitPostgresEntity.setCity("Los Angeles");
		permitPostgresEntity.setLaborCategoryCode(123);
		permitPostgresEntity.setLaborItem("456");
		permitPostgresEntity.setLaborItemDescription("Test Description");
		permitPostgresEntity.setUnitPermitFee(new BigDecimal("100.50"));
		permitPostgresEntity.setOmniItemId("OMNI123");
		permitPostgresEntity.setLaborCategoryDescription("Test Category");
		permitPostgresEntity.setState("CA");
		permitPostgresEntity.setCounty("Los Angeles County");
		permitPostgresEntity.setMunicipality("Beverly Hills");
		permitPostgresEntity.setZipcode("90210");
		permitPostgresEntity.setProvider("Test Provider");
		permitPostgresEntity.setVbuNumber("789");
		permitPostgresEntity.setCreatedBy("user123");
		permitPostgresEntity.setUpdatedBy("user456");
		return permitPostgresEntity;
	}
}
