package com.lowes.permits;

import com.lowes.permits.service.CsvPermitPriceUpdaterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication(
		scanBasePackages = {"com.lowes"},
		exclude = {MongoAutoConfiguration.class, MongoReactiveAutoConfiguration.class})
@EnableScheduling
public class PermitDataManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(PermitDataManagementApplication.class, args);
	}


	@Bean
	public CommandLineRunner run(CsvPermitPriceUpdaterService csvPermitPriceUpdaterService) {
		return args -> {
			String inputFilePath = "src/main/resources/Permit_DB_file_w_Buffer_Day_4.30.2026 _formatted.csv";
			String outputFilePath =
					"src/main/resources/Permit_DB_file_w_Buffer_Day_4.30.2026 _formatted_updated_with_old_price.csv";

			if (args.length >= 1) {
				inputFilePath = args[0];
			}
			if (args.length >= 2) {
				outputFilePath = args[1];
			}

			log.info("Starting CSV price updater application");
			log.info("Input file: {}", inputFilePath);
			log.info("Output file: {}", outputFilePath);

			csvPermitPriceUpdaterService.processPermitCsvFile(inputFilePath, outputFilePath);

			log.info("CSV price updater application completed successfully");
			System.exit(0);
		};
	}

}
