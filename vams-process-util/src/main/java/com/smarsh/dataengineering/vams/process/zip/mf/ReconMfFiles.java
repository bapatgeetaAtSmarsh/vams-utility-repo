package com.smarsh.dataengineering.vams.process.zip.mf;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ReconMfFiles {

	// Source folder
	private static final String zipFolder = "/Users/geetabapat/Downloads/100Batch";

	public static void main(String[] args) {

		ReconMfFiles reconMfFiles = new ReconMfFiles();
		reconMfFiles.checkForDuplicateFiles(zipFolder);
	}

	public void checkForDuplicateFiles(String zipFolder) {
		File folder = new File(zipFolder);
		File[] zipFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
		if (zipFiles == null) {
			System.out.println("No zip files found.");
			return;
		}
		Map<String, List<String>> fileToZips = new HashMap<>();
		for (File zipFile : zipFiles) {
			try (ZipFile zf = new ZipFile(zipFile)) {
				Enumeration<? extends ZipEntry> entries = zf.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					String name = entry.getName();
					if (name.toLowerCase().endsWith(".eml") || name.toLowerCase().endsWith(".mf")) {
						fileToZips.computeIfAbsent(name, k -> new ArrayList<>()).add(zipFile.getName());
					}
				}
			} catch (IOException e) {
				System.err.println("Error reading " + zipFile.getName() + ": " + e.getMessage());
			}
		}
		// Now, find duplicates for .eml
		for (Map.Entry<String, List<String>> entry : fileToZips.entrySet()) {
			if (entry.getValue().size() > 1 && entry.getKey().toLowerCase().endsWith(".eml")) {
				System.out.println("Duplicate .eml file: " + entry.getKey());
				System.out.println("Found in zips: " + String.join(", ", entry.getValue()));
			}
		}
	}
}
