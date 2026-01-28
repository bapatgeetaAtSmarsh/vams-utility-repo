package com.smarsh.dataengineering.eml.comparator;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

import com.smarsh.dataengineering.eml.comparator.config.CompareProperties;
import com.smarsh.dataengineering.eml.comparator.model.CompareReport;
import com.smarsh.dataengineering.eml.comparator.service.FolderCompareService;
import com.smarsh.dataengineering.eml.comparator.service.ReportWriter;

@SpringBootApplication
@EnableConfigurationProperties(CompareProperties.class)
public class VamsEmlComparatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(VamsEmlComparatorApplication.class, args);
	}

	@Bean
	ApplicationRunner runCompare(CompareProperties props,
			FolderCompareService folderCompareService,
			ReportWriter reportWriter) {
		return args -> {
			if (props.encryptedDir() == null || props.decryptedDir() == null) {
				System.err.println("""
          Missing required configuration in application.yml

          Required:
            compare.encryptedDir
            compare.decryptedDir

          Optional:
            compare.outputDir
          """);
				return;
			}

			Path encrypted = Path.of(props.encryptedDir());
			Path decrypted = Path.of(props.decryptedDir());
			Path output = (props.outputDir() == null || props.outputDir().isBlank())
					? Path.of("output")
					: Path.of(props.outputDir());

			CompareReport report = folderCompareService.compare(encrypted, decrypted);
			reportWriter.write(report, output);

			System.out.println("Report written to: " + output.toAbsolutePath());
		};
	}
}

