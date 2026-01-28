package com.smarsh.dataengineering.eml.comparator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "compare")
public record CompareProperties(
		String encryptedDir,
		String decryptedDir,
		String outputDir
) {}

