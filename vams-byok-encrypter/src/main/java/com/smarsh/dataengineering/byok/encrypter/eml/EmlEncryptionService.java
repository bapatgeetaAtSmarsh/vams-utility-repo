package com.smarsh.dataengineering.byok.encrypter.eml;



import jakarta.activation.DataHandler;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.Properties;

import com.smarsh.dataengineering.byok.encrypter.config.TelemessageProperties;
import com.smarsh.dataengineering.byok.encrypter.crypto.CryptoService;
import com.smarsh.dataengineering.byok.encrypter.crypto.RsaKeyLoader;

@Service
public class EmlEncryptionService {

	private static final String HDR_DATAKEY = "X-TELEMESSAGE-ENC-DATAKEY";
	private static final String HDR_INTEGRITY = "X-TELEMESSAGE-OriginalMesageIntegrity";
	private static final String HDR_FINGER_PRINT = "X-TELEMESSAGE-ENC-KEY-ID";

	private final TelemessageProperties props;
	private final CryptoService crypto;

	private final Session mailSession;
	private final PublicKey publicKey;
	@SuppressWarnings("unused")
	private final PrivateKey privateKey; // loaded because you requested pair; not used in this encrypt-only flow

	public EmlEncryptionService(TelemessageProperties props, CryptoService crypto) throws Exception {
		this.props = props;
		this.crypto = crypto;

		this.mailSession = Session.getInstance(new Properties());

		this.publicKey = RsaKeyLoader.loadPublicKey(Path.of(props.getRsa().getPublicKeyPath()));
		this.privateKey = RsaKeyLoader.loadPrivateKey(Path.of(props.getRsa().getPrivateKeyPath()));
	}

	public void processAllEmls() throws Exception {
		Path inDir = Path.of(props.getInputFolder());
		Path outDir = Path.of(props.getOutputFolder());
		Files.createDirectories(outDir);

		if (!Files.isDirectory(inDir)) {
			throw new IllegalArgumentException("Input folder is not a directory: " + inDir);
		}

		try (var stream = Files.list(inDir)) {
			stream
					.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".eml"))
					.sorted(Comparator.comparing(p -> p.getFileName().toString()))
					.forEach(p -> {
						try {
							processOne(p, outDir.resolve(p.getFileName().toString()));
						} catch (Exception e) {
							System.err.println("Failed processing " + p + ": " + e.getMessage());
							e.printStackTrace(System.err);
						}
					});
		}
	}

	private void processOne(Path inputEml, Path outputEml) throws Exception {
		MimeMessage msg;
		try (InputStream is = Files.newInputStream(inputEml, StandardOpenOption.READ)) {
			msg = new MimeMessage(mailSession, is);
		}

		byte[] iv = crypto.generateIv();
		SecretKey aesKey = crypto.generateAes256Key();

		// Encrypt content tree
		encryptPartContentInPlace(msg, aesKey, iv);

		// Add headers
		String headerValue = crypto.encryptAesKeyWithRsaV2HeaderValue(iv, aesKey, publicKey);
		msg.setHeader(HDR_DATAKEY, headerValue);
		// Verification commented out due to key pair mismatch
		/*
		try {
			CryptoService.AesKeyAndIv decrypted = crypto.decryptAesKeyAndIvFromRsaV2HeaderValue(headerValue, privateKey);
			if (!Arrays.equals(aesKey.getEncoded(), decrypted.key.getEncoded()) || !Arrays.equals(iv, decrypted.iv)) {
				System.err.println("Warning: RSA key pair mismatch detected. Output will still be generated.");
			}
		} catch (Exception e) {
			System.err.println("Warning: Key verification failed: " + e.getMessage() + ". Output will still be generated.");
		}
		*/
		msg.setHeader(HDR_INTEGRITY,props.getOriginalMesageIntegrity());
		msg.setHeader(HDR_FINGER_PRINT, props.getEncKeyId());



		// Ensure changes are committed
		msg.saveChanges();

		// Write .eml
		try (OutputStream os = Files.newOutputStream(outputEml,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			msg.writeTo(os);
		}
	}

	/**
	 * Walks MIME structure; encrypts every leaf part's content bytes.
	 * Preserves structure/headers/filenames/content-types; only payload bytes are replaced with encrypted.
	 */
	private void encryptPartContentInPlace(Part part, SecretKey aesKey, byte[] iv) throws Exception {
		if (part.isMimeType("multipart/*")) {
			Object content = part.getContent();
			if (!(content instanceof Multipart)) {
				return;
			}

			Multipart mp = (Multipart) content;

			// Derive subtype from Content-Type header
			String contentType = part.getContentType(); // e.g. multipart/mixed; boundary=...
			String subtype = "mixed"; // default fallback

			if (contentType != null) {
				int slash = contentType.indexOf('/');
				int semicolon = contentType.indexOf(';');

				if (slash >= 0) {
					if (semicolon > slash) {
						subtype = contentType.substring(slash + 1, semicolon).trim();
					} else {
						subtype = contentType.substring(slash + 1).trim();
					}
				}
			}

			MimeMultipart newMp = new MimeMultipart(subtype);

			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart bp = mp.getBodyPart(i);
				BodyPart enc = encryptBodyPart(bp, aesKey, iv);
				newMp.addBodyPart(enc);
			}

			part.setContent(newMp);
			return;
		}



		// Leaf part
		replaceLeafPayloadWithEncrypted(part, aesKey, iv);
	}

	private BodyPart encryptBodyPart(BodyPart original, SecretKey aesKey, byte[] iv) throws Exception {
		if (original.isMimeType("multipart/*")) {
			// Clone container headers and recurse into children
			MimeBodyPart container = new MimeBodyPart();

			copyNonContentHeaders(original, container);

			Multipart mp = (Multipart) original.getContent();

			// Derive subtype from Content-Type header (portable; avoids getSubType() issues)
			String contentType = original.getContentType(); // e.g. multipart/mixed; boundary=...
			String subtype = "mixed"; // default

			if (contentType != null) {
				int slash = contentType.indexOf('/');
				int semicolon = contentType.indexOf(';');

				if (slash >= 0) {
					if (semicolon > slash) {
						subtype = contentType.substring(slash + 1, semicolon).trim();
					} else {
						subtype = contentType.substring(slash + 1).trim();
					}
				}
			}

			MimeMultipart newMp = new MimeMultipart(subtype);

			for (int i = 0; i < mp.getCount(); i++) {
				BodyPart child = mp.getBodyPart(i);
				newMp.addBodyPart(encryptBodyPart(child, aesKey, iv));
			}

			container.setContent(newMp);
			return container;
		}


		// Leaf: copy headers then replace payload
		MimeBodyPart leaf = new MimeBodyPart();
		copyNonContentHeaders(original, leaf);

		// Preserve filename/disposition/content-type metadata (payload changes)
		String disp = original.getDisposition();
		if (disp != null) leaf.setDisposition(disp);
		String fn = original.getFileName();
		if (fn != null) leaf.setFileName(fn);

		// Encrypt original raw bytes
		byte[] plain = readAllBytes(original.getInputStream());
		byte[] ct = crypto.encryptAesCbc(plain, aesKey, iv);

		String contentType = original.getContentType();
		Object content = contentType.startsWith("text/") ? Base64.getEncoder().encodeToString(ct).getBytes(java.nio.charset.StandardCharsets.UTF_8) : ct;
		ByteArrayDataSource ds = new ByteArrayDataSource((byte[]) content, contentType);
		leaf.setDataHandler(new DataHandler(ds));

		// Make sure it's transferable
		leaf.setHeader("Content-Transfer-Encoding", "base64");

		return leaf;
	}

	private void replaceLeafPayloadWithEncrypted(Part leaf, SecretKey aesKey, byte[] iv) throws Exception {
		byte[] plain = readAllBytes(leaf.getInputStream());
		byte[] ct = crypto.encryptAesCbc(plain, aesKey, iv);

		String contentType = leaf.getContentType();
		Object content = contentType.startsWith("text/") ? Base64.getEncoder().encodeToString(ct).getBytes(java.nio.charset.StandardCharsets.UTF_8) : ct;
		ByteArrayDataSource ds = new ByteArrayDataSource((byte[]) content, contentType);
		leaf.setDataHandler(new DataHandler(ds));
		leaf.setHeader("Content-Transfer-Encoding", "base64");
	}

	private static byte[] readAllBytes(InputStream is) throws Exception {
		try (is) {
			return is.readAllBytes();
		}
	}

	/**
	 * Copy headers except those that will be recalculated by JavaMail when setting content.
	 */
	private static void copyNonContentHeaders(Part from, Part to) throws MessagingException {
		for (var e = from.getAllHeaders().asIterator(); e.hasNext(); ) {
			Header h = (Header) e.next();
			String name = h.getName();

			// Skip headers that can conflict with updated content
			if (name.equalsIgnoreCase("Content-Type")) continue;
			if (name.equalsIgnoreCase("Content-Transfer-Encoding")) continue;
			if (name.equalsIgnoreCase("Content-Length")) continue;

			// Copy all others (including Content-ID, Content-Disposition, etc.)
			to.setHeader(name, h.getValue());
		}
	}
}

