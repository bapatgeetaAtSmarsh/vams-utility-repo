package com.smarsh.dataengineering.vams.process.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyLoader {

	private RsaKeyLoader() {}

	public static PublicKey loadPublicKey(Path pemPath) throws Exception {
		byte[] der = readPem(pemPath, "PUBLIC KEY");
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePublic(new X509EncodedKeySpec(der));
	}

	public static PrivateKey loadPrivateKey(Path pemPath) throws Exception {
		byte[] der = readPem(pemPath, "PRIVATE KEY");
		KeyFactory kf = KeyFactory.getInstance("RSA");
		return kf.generatePrivate(new PKCS8EncodedKeySpec(der));
	}

	private static byte[] readPem(Path path, String type) throws IOException {
		String pem = Files.readString(path, StandardCharsets.US_ASCII);
		pem = pem.replace("-----BEGIN " + type + "-----", "")
				.replace("-----END " + type + "-----", "")
				.replaceAll("\\s", "");
		return Base64.getDecoder().decode(pem);
	}
}

