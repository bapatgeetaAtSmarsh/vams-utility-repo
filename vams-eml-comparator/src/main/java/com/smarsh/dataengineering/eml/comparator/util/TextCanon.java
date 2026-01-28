package com.smarsh.dataengineering.eml.comparator.util;

import org.apache.james.mime4j.dom.field.ContentTypeField;

import java.util.*;

public final class TextCanon {
	private TextCanon() {}

	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	public static String canonHeaderValue(String v) {
		if (v == null) return "";
		// Normalize CRLF/LF and unfold:
		String s = v.replace("\r\n", "\n").replace("\r", "\n");
		// Unfold: replace newline + WSP with single space
		s = s.replaceAll("\n[ \t]+", " ");
		// Compress whitespace
		s = s.replaceAll("[ \t]+", " ").trim();
		// Normalize any trailing whitespace differences
		return s;
	}

	public static String canonParam(String v) {
		if (v == null) return "";
		String s = v.replace("\r\n", "\n").replace("\r", "\n").trim();
		s = s.replaceAll("[ \t]+", " ");
		return s;
	}

	public static Map<String, String> canonParams(ContentTypeField f) {
		Map<String, String> out = new TreeMap<>();
		if (f == null || f.getParameters() == null) return out;

		for (var entry : f.getParameters().entrySet()) {
			if (entry.getKey() == null) continue;
			String k = entry.getKey().trim().toLowerCase(Locale.ROOT);
			String v = entry.getValue();
			out.put(k, canonParam(v));
		}
		return out;
	}
}

