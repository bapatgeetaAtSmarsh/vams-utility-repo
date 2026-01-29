package com.smarsh.dataengineering.byok.encrypter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telemessage")
public class TelemessageProperties {

	private Rsa rsa = new Rsa();
	private String inputFolder;
	private String outputFolder;
	private String originalMessageIntegrityFingerprint;
	private String encKeyId;
	private String originalMesageIntegrity;

	public static class Rsa {
		private String publicKeyPath;
		private String privateKeyPath;

		public String getPublicKeyPath() { return publicKeyPath; }
		public void setPublicKeyPath(String publicKeyPath) { this.publicKeyPath = publicKeyPath; }

		public String getPrivateKeyPath() { return privateKeyPath; }
		public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
	}

	public Rsa getRsa() { return rsa; }
	public void setRsa(Rsa rsa) { this.rsa = rsa; }

	public String getInputFolder() { return inputFolder; }
	public void setInputFolder(String inputFolder) { this.inputFolder = inputFolder; }

	public String getOutputFolder() { return outputFolder; }
	public void setOutputFolder(String outputFolder) { this.outputFolder = outputFolder; }

	public String getOriginalMessageIntegrityFingerprint() { return originalMessageIntegrityFingerprint; }
	public void setOriginalMessageIntegrityFingerprint(String v) { this.originalMessageIntegrityFingerprint = v; }

	public String getEncKeyId() { return encKeyId; }
	public void setEncKeyId(String encKeyId) { this.encKeyId = encKeyId; }

	public String getOriginalMesageIntegrity() { return originalMesageIntegrity; }
	public void setOriginalMesageIntegrity(String v) { this.originalMesageIntegrity = v; }
}

