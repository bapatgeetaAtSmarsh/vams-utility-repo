package com.smarsh.dataengineering.eml.comparator.model;

import java.util.List;

public record EmlDiff(
		String filename,
		List<String> headerDifferences,
		List<String> structureDifferences,
		List<String> notes
) {
	public boolean hasDifferences() {
		return !(headerDifferences.isEmpty() && structureDifferences.isEmpty());
	}
}
