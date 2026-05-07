package com.lowes.permits.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;

import com.lowes.permits.dto.PermitCsvRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvPermitPriceUpdaterService {

	private final DatabaseClient databaseClient;

	public void processPermitCsvFile(String inputFilePath, String outputFilePath) {
		log.info("Starting CSV processing. Input: {}, Output: {}", inputFilePath, outputFilePath);

		List<PermitCsvRow> rows = new ArrayList<>();
		String headerLine = null;

		try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
			headerLine = reader.readLine();
			String line;

			while ((line = reader.readLine()) != null) {
				PermitCsvRow row = parseCsvLine(line);
				rows.add(row);
			}

			log.info("Read {} rows from CSV file", rows.size());
		} catch (IOException e) {
			log.error("Error reading CSV file: {}", inputFilePath, e);
			throw new RuntimeException("Failed to read CSV file", e);
		}

		int processedCount = 0;
		int rowNumber = 0;
		for (PermitCsvRow row : rows) {
			rowNumber++;
			try {
		/*		if (rowNumber <= 3) {
					log.info("=== Processing Row {} ===", rowNumber);
					log.info("  laborCategoryCode: '{}'", row.getLaborCategoryCode());
					log.info("  zipcode: '{}'", row.getZipcode());
					log.info("  city: '{}'", row.getCity());
					log.info("  laborItem: '{}'", row.getLaborItem());
					log.info("  county: '{}'", row.getCounty());
					log.info("  municipality: '{}'", row.getMunicipality());
				}*/

				BigDecimal oldPrice = queryOldPrice(row, rowNumber);

				log.info("Old price: {} in row number{}", oldPrice,rowNumber);
				if (oldPrice != null) {
					row.setOldPrice(oldPrice.toString());
					processedCount++;
					/*if (processedCount <= 5) {
						log.info("Row {}: SUCCESS - Found old_price = {}", rowNumber, oldPrice);
					}*/
				} else {
					/*if (rowNumber <= 3) {
						log.info("Row {}: NO MATCH - old_price not found", rowNumber);
					}*/
				}
			} catch (Exception e) {
				log.error(
						"Row {}: ERROR - laborCategoryCode={}, zipcode={}, city={}, laborItem={}, county={}, municipality={}",
						rowNumber,
						row.getLaborCategoryCode(),
						row.getZipcode(),
						row.getCity(),
						row.getLaborItem(),
						row.getCounty(),
						row.getMunicipality(),
						e);
			}
		}

		log.info("Successfully queried old_price for {} out of {} rows", processedCount, rows.size());

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
			writer.write(headerLine);
			writer.newLine();

			for (PermitCsvRow row : rows) {
				writer.write(toCsvLine(row));
				writer.newLine();
			}

			log.info("Successfully wrote updated CSV to: {}", outputFilePath);
		} catch (IOException e) {
			log.error("Error writing CSV file: {}", outputFilePath, e);
			throw new RuntimeException("Failed to write CSV file", e);
		}
	}

	private PermitCsvRow parseCsvLine(String line) {
		String[] fields = parseCsvFields(line);

		return PermitCsvRow.builder()
				.laborCategoryCode(getField(fields, 0))
				.laborCategoryDescription(getField(fields, 1))
				.zipcode(getField(fields, 2))
				.city(getField(fields, 3))
				.state(getField(fields, 4))
				.laborItem(getField(fields, 5))
				.laborItemDescription(getField(fields, 6))
				.unitPermitFee(getField(fields, 7))
				.omniItemId(getField(fields, 8))
				.provider(getField(fields, 9))
				.vbuNumber(getField(fields, 10))
				.createdBy(getField(fields, 11))
				.createdTimestamp(getField(fields, 12))
				.updatedBy(getField(fields, 13))
				.updatedTimestamp(getField(fields, 14))
				.oldPrice(getField(fields, 15))
				.county(getField(fields, 16))
				.municipality(getField(fields, 17))
				.estPermitObtainDays(getField(fields, 18))
				.build();
	}

	private String[] parseCsvFields(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder currentField = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					currentField.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				fields.add(currentField.toString());
				currentField = new StringBuilder();
			} else {
				currentField.append(c);
			}
		}
		fields.add(currentField.toString());

		return fields.toArray(new String[0]);
	}

	private String getField(String[] fields, int index) {
		if (index < fields.length) {
			String value = fields[index].trim();
			return value.isEmpty() ? null : value;
		}
		return null;
	}

	private BigDecimal queryOldPrice(PermitCsvRow row, int rowNumber) {
		String sql = "SELECT old_price FROM permitmain.permit_master_prod_bkp "
				+ "WHERE labor_category_code = $1 "
				+ "AND zipcode = $2 "
				+ "AND city = $3 "
				+ "AND labor_item = $4 "
				+ "AND county = $5 "
				+ "AND municipality = $6 "
				+ "LIMIT 1";

		try {
			DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

			Integer laborCategoryCode = null;
			if (row.getLaborCategoryCode() != null && !row.getLaborCategoryCode().isEmpty()) {
				laborCategoryCode = Integer.parseInt(row.getLaborCategoryCode());
				spec = spec.bind(0, laborCategoryCode);
			} else {
				spec = spec.bindNull(0, Integer.class);
			}

			String zipcode = row.getZipcode() != null ? row.getZipcode() : "";
			String city = row.getCity() != null ? row.getCity() : "";
			spec = spec.bind(1, zipcode);
			spec = spec.bind(2, city);

			Integer laborItem = null;
			if (row.getLaborItem() != null && !row.getLaborItem().isEmpty()) {
				laborItem = Integer.parseInt(row.getLaborItem());
				spec = spec.bind(3, laborItem);
			} else {
				spec = spec.bindNull(3, Integer.class);
			}

			String county = row.getCounty() != null ? row.getCounty() : "";
			String municipality = row.getMunicipality() != null ? row.getMunicipality() : "";
			spec = spec.bind(4, county);
			spec = spec.bind(5, municipality);

		/*	if (rowNumber <= 3) {
				log.info("  Executing SQL query with params:");
				log.info("    $1 (labor_category_code) = {}", laborCategoryCode);
				log.info("    $2 (zipcode) = '{}'", zipcode);
				log.info("    $3 (city) = '{}'", city);
				log.info("    $4 (labor_item) = {}", laborItem);
				log.info("    $5 (county) = '{}'", county);
				log.info("    $6 (municipality) = '{}'", municipality);
			}*/

			BigDecimal result = spec.map((r, meta) -> {
						Object value = r.get("old_price");
						if (value == null) {
							return null;
						}
						return new BigDecimal(value.toString());
					})
					.one()
					.onErrorResume(e -> Mono.empty())
					.block();
/*
			if (rowNumber <= 3) {
				log.info("  Query result: {}", result != null ? result : "NULL");
			}*/

			return result;
		} catch (Exception e) {
			log.error("Row {}: Error executing query", rowNumber, e);
			return null;
		}
	}

	private String toCsvLine(PermitCsvRow row) {
		return escapeCsv(row.getLaborCategoryCode()) + ","
				+ escapeCsv(row.getLaborCategoryDescription()) + ","
				+ escapeCsv(row.getZipcode()) + ","
				+ escapeCsv(row.getCity()) + ","
				+ escapeCsv(row.getState()) + ","
				+ escapeCsv(row.getLaborItem()) + ","
				+ escapeCsv(row.getLaborItemDescription()) + ","
				+ escapeCsv(row.getUnitPermitFee()) + ","
				+ escapeCsv(row.getOmniItemId()) + ","
				+ escapeCsv(row.getProvider()) + ","
				+ escapeCsv(row.getVbuNumber()) + ","
				+ escapeCsv(row.getCreatedBy()) + ","
				+ escapeCsv(row.getCreatedTimestamp()) + ","
				+ escapeCsv(row.getUpdatedBy()) + ","
				+ escapeCsv(row.getUpdatedTimestamp()) + ","
				+ escapeCsv(row.getOldPrice()) + ","
				+ escapeCsv(row.getCounty()) + ","
				+ escapeCsv(row.getMunicipality()) + ","
				+ escapeCsv(row.getEstPermitObtainDays());
	}

	private String escapeCsv(String value) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
