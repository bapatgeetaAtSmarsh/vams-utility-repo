package com.smarsh.dataengineering.eml.comparator.service;


import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.smarsh.dataengineering.eml.comparator.model.CompareReport;
import com.smarsh.dataengineering.eml.comparator.model.EmlDiff;
import com.smarsh.dataengineering.eml.comparator.model.FolderDiff;

@Service
public class FolderCompareService {

	private final EmlComparator emlComparator;

	public FolderCompareService(EmlComparator emlComparator) {
		this.emlComparator = emlComparator;
	}

	public CompareReport compare(Path encryptedDir, Path decryptedDir) throws IOException {
		Map<String, Path> enc = listEmlsByName(encryptedDir);
		Map<String, Path> dec = listEmlsByName(decryptedDir);

		Set<String> all = new TreeSet<>();
		all.addAll(enc.keySet());
		all.addAll(dec.keySet());

		List<String> onlyEnc = all.stream().filter(n -> enc.containsKey(n) && !dec.containsKey(n)).toList();
		List<String> onlyDec = all.stream().filter(n -> dec.containsKey(n) && !enc.containsKey(n)).toList();

		List<String> common = all.stream().filter(n -> enc.containsKey(n) && dec.containsKey(n)).toList();

		List<EmlDiff> diffs = new ArrayList<>();
		for (String name : common) {
			diffs.add(emlComparator.compare(name, enc.get(name), dec.get(name)));
		}

		// Keep only those with diffs, or keep all if you prefer:
		List<EmlDiff> withDiffs = diffs.stream()
				.filter(EmlDiff::hasDifferences)
				.collect(Collectors.toList());

		return new CompareReport(
				Instant.now(),
				new FolderDiff(onlyEnc, onlyDec, common.size()),
				withDiffs
		);
	}

	private Map<String, Path> listEmlsByName(Path dir) throws IOException {
		if (!Files.isDirectory(dir)) {
			throw new IllegalArgumentException("Not a directory: " + dir);
		}

		try (var stream = Files.walk(dir)) {
			return stream
					.filter(Files::isRegularFile)
					.filter(p -> "eml".equalsIgnoreCase(FilenameUtils.getExtension(p.getFileName().toString())))
					.collect(Collectors.toMap(
							p -> p.getFileName().toString(),
							p -> p,
							(a, b) -> {
								// If duplicates exist, keep first and note ambiguity at higher level if needed
								return a;
							},
							TreeMap::new
					));
		}
	}
}

