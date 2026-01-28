package com.smarsh.dataengineering.eml.comparator.cli;


import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import com.smarsh.dataengineering.eml.comparator.config.CompareProperties;
import com.smarsh.dataengineering.eml.comparator.model.CompareReport;
import com.smarsh.dataengineering.eml.comparator.service.FolderCompareService;
import com.smarsh.dataengineering.eml.comparator.service.ReportWriter;

// @Component - commented as not used at moment
public class CompareCommandLineRunner implements CommandLineRunner {

	private final CompareProperties props;
	private final FolderCompareService folderCompareService;
	private final ReportWriter reportWriter;

	public CompareCommandLineRunner(
			CompareProperties props,
			FolderCompareService folderCompareService,
			ReportWriter reportWriter
	) {
		this.props = props;
		this.folderCompareService = folderCompareService;
		this.reportWriter = reportWriter;
	}

	@Override
	public void run(String... args) throws Exception {
		if (props.encryptedDir() == null || props.decryptedDir() == null) {
			System.err.println("""
        Missing required properties.
        Provide:
          --compare.encryptedDir=/path/to/encrypted
          --compare.decryptedDir=/path/to/decrypted
          --compare.outputDir=/path/to/output (optional)
        """);
			return;
		}

		Path encrypted = Path.of(props.encryptedDir());
		Path decrypted = Path.of(props.decryptedDir());
		Path output = props.outputDir() == null ? Path.of("output") : Path.of(props.outputDir());

		CompareReport report = folderCompareService.compare(encrypted, decrypted);
		reportWriter.write(report, output);

		System.out.println("Report written to: " + output.toAbsolutePath());
	}
}

