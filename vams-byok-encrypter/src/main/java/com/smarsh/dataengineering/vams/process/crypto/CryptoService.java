package com.smarsh.dataengineering.vams.process.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {
	private final SecureRandom secureRandom = new SecureRandom();

	public SecretKey generateAes256Key() throws Exception {
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(256, secureRandom);
		return kg.generateKey();
	}

	public byte[] generateIv() throws Exception {
		byte[] iv = new byte[12];
		secureRandom.nextBytes(iv);
		return iv;
	}

	public byte[] encryptAesCbc(byte[] plaintext, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
		return cipher.doFinal(plaintext);
	}

	public byte[] decryptAesCbc(byte[] ciphertext, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
		return cipher.doFinal(ciphertext);
	}

	public static class AesKeyAndIv {
		public final SecretKey key;
		public final byte[] iv;

		public AesKeyAndIv(SecretKey key, byte[] iv) {
			this.key = key;
			this.iv = iv;
		}
	}

	public String encryptAesKeyWithRsaV2HeaderValue(byte[] iv, SecretKey aesKey, PublicKey rsaPublicKey) throws Exception {
		Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		rsa.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
		byte[] enc = rsa.doFinal(aesKey.getEncoded());
		byte[] data = new byte[iv.length + enc.length];
		System.arraycopy(iv, 0, data, 0, iv.length);
		System.arraycopy(enc, 0, data, iv.length, enc.length);
		return "V2.0#: " + Base64.getEncoder().encodeToString(data);
	}

	public AesKeyAndIv decryptAesKeyAndIvFromRsaV2HeaderValue(String headerValue, PrivateKey rsaPrivateKey) throws Exception {
		if (!headerValue.startsWith("V2.0#")) throw new IllegalArgumentException("Invalid header");
		String b64 = headerValue.substring(7);
		byte[] data = Base64.getDecoder().decode(b64);
		byte[] iv = new byte[12];
		System.arraycopy(data, 0, iv, 0, 12);
		byte[] enc = new byte[data.length - 12];
		System.arraycopy(data, 12, enc, 0, enc.length);
		Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		rsa.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
		byte[] keyBytes = rsa.doFinal(enc);
		SecretKey key = new SecretKeySpec(keyBytes, "AES");
		return new AesKeyAndIv(key, iv);
	}
}
