package com.smarsh.dataengineering.vams.process.setup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SetupFiles {

	private static final String SNAPSHOT_SEARCH_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/SnapshotSearch";
	private static final String POLICY_ASSIGNMENT_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/PolicyAssignment";
	private static final String DOCUMENT_REINDEX_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/DocumentReindex";
	private static final String PARTICIPANT_SEARCH_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/ParticipantSearch";
	private static final String ELASTIC_QUERY_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/ElasticQuery";
	private static final String BACKLOGGER_REPLAY_FOLDER = "/Users/geetabapat/Public/projects/vams/configurations/AppConfig/BackloggerReplay";



	private static final String environment = "/sanity2/stagging";

	private static final String LOAD_FOR = SNAPSHOT_SEARCH_FOLDER;
	private static final String LOAD_INTO = "/Users/geetabapat/Public/projects/vams/repo/";
	private static final String project = "vams-jobs";

	public static void main(String[] args) {

		SetupFiles setup = new SetupFiles();
		setup.loadConfigFiles();

	}

	public void loadConfigFiles() {
		String sourceBase = LOAD_FOR + environment + "/" + project;
		String destBase = LOAD_INTO + project;

		File sourceDir = new File(sourceBase);
		if (!sourceDir.exists() || !sourceDir.isDirectory()) {
			System.out.println("Source directory does not exist: " + sourceDir);
			return;
		}

		File[] subDirs = sourceDir.listFiles(File::isDirectory);
		if (subDirs != null) {
			for (File subDir : subDirs) {
				String subFolder = subDir.getName();
				Path sourceFile = Paths.get(subDir.getAbsolutePath(), "application.yml");
				if (Files.exists(sourceFile)) {
					try {
						Path destDir = Paths.get(destBase, subFolder, "src/main/resources");
						Files.createDirectories(destDir);
						Path destFile = destDir.resolve("application.yml");
						Files.copy(sourceFile, destFile, StandardCopyOption.REPLACE_EXISTING);
						System.out.println("Copied application.yml from " + sourceFile + " into directory " + destDir);
					} catch (IOException e) {
						System.err.println("Error copying file for " + subFolder + ": " + e.getMessage());
					}
				} else {
					System.out.println("application.yml not found in " + subDir);
				}
			}
		}
	}
}
