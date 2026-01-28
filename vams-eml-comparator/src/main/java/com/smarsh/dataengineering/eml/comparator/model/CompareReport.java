package com.smarsh.dataengineering.eml.comparator.model;

import java.time.Instant;
import java.util.List;

public record CompareReport(
		Instant generatedAt,
		FolderDiff folderDiff,
		List<EmlDiff> emlDiffs
) {}

