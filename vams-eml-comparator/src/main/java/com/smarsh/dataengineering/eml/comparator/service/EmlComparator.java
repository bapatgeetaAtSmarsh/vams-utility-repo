package com.smarsh.dataengineering.eml.comparator.service;


import org.apache.james.mime4j.dom.*;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.stream.MimeConfig;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.smarsh.dataengineering.eml.comparator.model.EmlDiff;
import com.smarsh.dataengineering.eml.comparator.util.HeaderCanon;
import com.smarsh.dataengineering.eml.comparator.util.TextCanon;

@Service
public class EmlComparator {

	private final DefaultMessageBuilder builder;

	// If you want to ignore some headers entirely, list them here (lowercase).
	private static final Set<String> IGNORED_HEADERS = Set.of(
			// Uncomment if desired:
			// "message-id", "date", "received"
	);

	public EmlComparator() {
		MimeConfig config = MimeConfig.custom()
				.setMaxLineLen(1000000)   // tolerate long lines
				.setMaxHeaderLen(1000000)
				.setStrictParsing(false)
				.build();

		this.builder = new DefaultMessageBuilder();
		this.builder.setMimeEntityConfig(config);
	}

	public EmlDiff compare(String filename, Path encryptedEml, Path decryptedEml) {
		List<String> headerDiffs = new ArrayList<>();
		List<String> structDiffs = new ArrayList<>();
		List<String> notes = new ArrayList<>();

		try {
			Message enc = parse(encryptedEml);
			Message dec = parse(decryptedEml);

			compareHeaders(enc.getHeader(), dec.getHeader(), headerDiffs);
			compareStructure(enc, dec, structDiffs, "root");

			// Optional: raw EOF CRLF differences should not matter; we don’t flag them.
			// But if parsing failed to preserve some parts, we add notes.

		} catch (Exception e) {
			notes.add("Failed to compare due to exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		return new EmlDiff(filename, headerDiffs, structDiffs, notes);
	}

	private Message parse(Path path) throws Exception {
		try (InputStream in = Files.newInputStream(path)) {
			return builder.parseMessage(in);
		}
	}

	private void compareHeaders(Header h1, Header h2, List<String> out) {
		Map<String, List<String>> a = HeaderCanon.toCanonicalMap(h1, IGNORED_HEADERS);
		Map<String, List<String>> b = HeaderCanon.toCanonicalMap(h2, IGNORED_HEADERS);

		Set<String> all = new TreeSet<>();
		all.addAll(a.keySet());
		all.addAll(b.keySet());

		for (String name : all) {
			List<String> va = a.getOrDefault(name, List.of());
			List<String> vb = b.getOrDefault(name, List.of());

			if (!va.equals(vb)) {
				out.add("Header differs: " + name + "\n"
						+ "  encrypted: " + va + "\n"
						+ "  decrypted: " + vb);
			}
		}
	}

	private void compareStructure(Entity e1, Entity e2, List<String> out, String path) {
		// Compare MIME metadata that should be identical (or at least not “random”)
		compareEntityMeta(e1, e2, out, path);

		Body b1 = e1.getBody();
		Body b2 = e2.getBody();

		if (b1 == null && b2 == null) return;
		if (b1 == null || b2 == null) {
			out.add(path + ": One side has body, other does not.");
			return;
		}

		// Multipart: compare child count + recurse
		if (b1 instanceof Multipart mp1 && b2 instanceof Multipart mp2) {
			if (mp1.getCount() != mp2.getCount()) {
				out.add(path + ": multipart part count differs: encrypted=" + mp1.getCount() + ", decrypted=" + mp2.getCount());
			}
			int n = Math.min(mp1.getCount(), mp2.getCount());
			for (int i = 0; i < n; i++) {
				Entity p1 = mp1.getBodyParts().get(i);
				Entity p2 = mp2.getBodyParts().get(i);
				compareStructure(p1, p2, out, path + "/part[" + i + "]");
			}
			return;
		}

		// Message/rfc822 embedded
		if (b1 instanceof Message b1m && b2 instanceof Message b2m) {
			compareHeaders(b1m.getHeader(), b2m.getHeader(), out); // embedded header comparison
			compareStructure(b1m, b2m, out, path + "/message");
			return;
		}

		// Leaf bodies: expected to differ (text and attachments), so we don't compare decoded bytes.
		// But we DO want to detect unexpected whitespace-only differences in NON-ENCRYPTED parts:
		// we already compare metadata + headers. For leaf, optionally compare normalized "is empty vs not empty".
		long size1 = safeEstimateSize(b1);
		long size2 = safeEstimateSize(b2);

		// If you want: flag if one is empty and the other is not (sometimes indicates truncation)
		if ((size1 == 0 && size2 > 0) || (size2 == 0 && size1 > 0)) {
			out.add(path + ": leaf body empty/non-empty mismatch (encrypted=" + size1 + ", decrypted=" + size2 + ")");
		}
	}

	private void compareEntityMeta(Entity e1, Entity e2, List<String> out, String path) {
		String ct1 = TextCanon.nullToEmpty(e1.getMimeType()).toLowerCase(Locale.ROOT);
		String ct2 = TextCanon.nullToEmpty(e2.getMimeType()).toLowerCase(Locale.ROOT);
		if (!ct1.equals(ct2)) {
			out.add(path + ": mimeType differs: encrypted=" + ct1 + ", decrypted=" + ct2);
		}

		String disp1 = TextCanon.nullToEmpty(e1.getDispositionType()).toLowerCase(Locale.ROOT);
		String disp2 = TextCanon.nullToEmpty(e2.getDispositionType()).toLowerCase(Locale.ROOT);
		if (!disp1.equals(disp2)) {
			out.add(path + ": disposition differs: encrypted=" + disp1 + ", decrypted=" + disp2);
		}

		// filename (Content-Disposition filename or Content-Type name)
		String fn1 = TextCanon.canonParam(e1.getFilename());
		String fn2 = TextCanon.canonParam(e2.getFilename());
		if (!fn1.equals(fn2)) {
			out.add(path + ": filename param differs: encrypted=" + fn1 + ", decrypted=" + fn2);
		}

		String cte1 = TextCanon.canonParam(e1.getHeader().getField("Content-Transfer-Encoding") == null
				? null : e1.getHeader().getField("Content-Transfer-Encoding").getBody());
		String cte2 = TextCanon.canonParam(e2.getHeader().getField("Content-Transfer-Encoding") == null
				? null : e2.getHeader().getField("Content-Transfer-Encoding").getBody());
		if (!cte1.equals(cte2)) {
			out.add(path + ": Content-Transfer-Encoding differs: encrypted=" + cte1 + ", decrypted=" + cte2);
		}

		// Compare Content-Type params except boundary differences? (boundaries may legitimately differ in reserialization)
		// If boundaries should match in your case, remove the boundary ignore below.
		compareContentTypeParams(e1, e2, out, path);
	}

	private void compareContentTypeParams(Entity e1, Entity e2, List<String> out, String path) {
		ContentTypeField f1 = (ContentTypeField) e1.getHeader().getField("Content-Type");
		ContentTypeField f2 = (ContentTypeField) e2.getHeader().getField("Content-Type");

		Map<String, String> p1 = TextCanon.canonParams(f1);
		Map<String, String> p2 = TextCanon.canonParams(f2);

		// Ignore boundary by default (often regenerated and not semantically meaningful)
		p1.remove("boundary");
		p2.remove("boundary");

		if (!p1.equals(p2)) {
			out.add(path + ": Content-Type params differ (boundary ignored)\n"
					+ "  encrypted: " + p1 + "\n"
					+ "  decrypted: " + p2);
		}
	}

	private long safeEstimateSize(Body body) {
		try {
			// Mime4j bodies don't always expose a simple length. We do a conservative estimate:
			//  - if it's a TextBody, get the reader and count chars up to some cap (avoid huge memory).
			if (body instanceof TextBody tb) {
				try (var r = tb.getReader()) {
					char[] buf = new char[8192];
					long n = 0;
					int read;
					long cap = 5_000_000; // 5M chars cap for estimation
					while ((read = r.read(buf)) != -1) {
						n += read;
						if (n > cap) return cap;
					}
					return n;
				}
			}
			if (body instanceof BinaryBody bb) {
				// can stream count up to cap
				try (var in = bb.getInputStream()) {
					byte[] buf = new byte[8192];
					long n = 0;
					int read;
					long cap = 50_000_000; // 50MB cap
					while ((read = in.read(buf)) != -1) {
						n += read;
						if (n > cap) return cap;
					}
					return n;
				}
			}
		} catch (Exception ignored) {
		}
		return -1;
	}
}

