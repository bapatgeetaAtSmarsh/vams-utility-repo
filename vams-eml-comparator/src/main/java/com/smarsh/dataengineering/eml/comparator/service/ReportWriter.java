package com.smarsh.dataengineering.eml.comparator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.smarsh.dataengineering.eml.comparator.model.CompareReport;

@Service
public class ReportWriter {

	private final ObjectMapper om;

	public ReportWriter() {
		this.om = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
				.enable(SerializationFeature.INDENT_OUTPUT);
	}

	public void write(CompareReport report, Path outputDir) throws Exception {
		Files.createDirectories(outputDir);

		// JSON
		Path json = outputDir.resolve("report.json");
		Files.writeString(json, om.writeValueAsString(report), StandardCharsets.UTF_8);

		// Markdown
		Path md = outputDir.resolve("report.md");
		Files.writeString(md, toMarkdown(report), StandardCharsets.UTF_8);
	}

	private String toMarkdown(CompareReport r) {
		var sb = new StringBuilder();
		sb.append("# EML Comparison Report\n\n");
		sb.append("- Generated at: ").append(r.generatedAt()).append("\n\n");

		sb.append("## Folder differences\n\n");
		sb.append("- Common files: ").append(r.folderDiff().commonCount()).append("\n");
		sb.append("- Only in encrypted: ").append(r.folderDiff().onlyInEncrypted().size()).append("\n");
		sb.append("- Only in decrypted: ").append(r.folderDiff().onlyInDecrypted().size()).append("\n\n");

		if (!r.folderDiff().onlyInEncrypted().isEmpty()) {
			sb.append("### Only in encrypted\n\n");
			r.folderDiff().onlyInEncrypted().forEach(f -> sb.append("- ").append(f).append("\n"));
			sb.append("\n");
		}

		if (!r.folderDiff().onlyInDecrypted().isEmpty()) {
			sb.append("### Only in decrypted\n\n");
			r.folderDiff().onlyInDecrypted().forEach(f -> sb.append("- ").append(f).append("\n"));
			sb.append("\n");
		}

		sb.append("## Content differences (unexpected)\n\n");
		if (r.emlDiffs().isEmpty()) {
			sb.append("No unexpected differences found among common files.\n");
			return sb.toString();
		}

		for (var d : r.emlDiffs()) {
			sb.append("### ").append(d.filename()).append("\n\n");

			if (!d.headerDifferences().isEmpty()) {
				sb.append("**Header differences**\n\n");
				for (String x : d.headerDifferences()) {
					sb.append("- ").append(codeBlockInline(x)).append("\n");
				}
				sb.append("\n");
			}

			if (!d.structureDifferences().isEmpty()) {
				sb.append("**MIME structure / metadata differences**\n\n");
				for (String x : d.structureDifferences()) {
					sb.append("- ").append(codeBlockInline(x)).append("\n");
				}
				sb.append("\n");
			}

			if (!d.notes().isEmpty()) {
				sb.append("**Notes**\n\n");
				for (String x : d.notes()) {
					sb.append("- ").append(x).append("\n");
				}
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	private String codeBlockInline(String s) {
		// Keep markdown readable; this is “inline-ish”
		return s.replace("\n", "  \n  ");
	}
}

