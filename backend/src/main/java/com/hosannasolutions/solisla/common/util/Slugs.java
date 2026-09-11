package com.hosannasolutions.solisla.common.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Shared slugify logic for categories and products (design doc §5/§6: "Blue Running Shoes" ->
 *  "blue-running-shoes"). Uniqueness (appending -2, -3, ...) is each module's own concern since it
 *  depends on that module's repository. */
public final class Slugs {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private Slugs() {
    }

    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS.matcher(normalized).replaceAll("");
        String lower = withoutDiacritics.toLowerCase(java.util.Locale.ROOT);
        String dashed = NON_ALPHANUMERIC.matcher(lower).replaceAll("-");
        return EDGE_DASHES.matcher(dashed).replaceAll("");
    }
}
