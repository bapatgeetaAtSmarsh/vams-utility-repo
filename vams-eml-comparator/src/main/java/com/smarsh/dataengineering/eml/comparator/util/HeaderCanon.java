package com.smarsh.dataengineering.eml.comparator.util;


import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.dom.Header;

import java.util.*;

public final class HeaderCanon {
	private HeaderCanon() {}

	public static Map<String, List<String>> toCanonicalMap(Header header, Set<String> ignoredHeaderNamesLower) {
		Map<String, List<String>> map = new TreeMap<>();
		if (header == null) return map;

		for (Field f : header.getFields()) {
			String name = f.getName() == null ? "" : f.getName().trim().toLowerCase(Locale.ROOT);
			if (name.isEmpty()) continue;
			if (ignoredHeaderNamesLower != null && ignoredHeaderNamesLower.contains(name)) continue;

			// Canonicalize header body:
			// - normalize line endings and folding whitespace
			// - compress runs of whitespace to single space
			String body = f.getBody();
			String canon = TextCanon.canonHeaderValue(body);

			map.computeIfAbsent(name, k -> new ArrayList<>()).add(canon);
		}

		// If same header appears multiple times, order matters in RFC; we preserve order.
		// If you want order-insensitive compare for certain headers, handle them here.
		return map;
	}
}

