package com.hosannasolutions.solisla.common.util;

/** Digits-only comparison so "+1 555-123-4567" and "15551234567" are recognized as the same
 *  number regardless of how either side formatted it. */
public final class Phones {

    private Phones() {
    }

    public static boolean matches(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String digitsA = digitsOnly(a);
        return !digitsA.isEmpty() && digitsA.equals(digitsOnly(b));
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("[^0-9]", "");
    }
}
