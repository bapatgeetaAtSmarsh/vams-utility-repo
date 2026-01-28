package com.smarsh.dataengineering.eml.comparator.model;

import java.util.List;

public record FolderDiff(
		List<String> onlyInEncrypted,
		List<String> onlyInDecrypted,
		int commonCount
) {}

