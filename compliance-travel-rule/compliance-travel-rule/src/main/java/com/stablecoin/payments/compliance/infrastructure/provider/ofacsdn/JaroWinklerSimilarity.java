package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

final class JaroWinklerSimilarity {

    private static final double WINKLER_PREFIX_WEIGHT = 0.1;
    private static final int MAX_PREFIX_LENGTH = 4;

    private JaroWinklerSimilarity() {
    }

    static double similarity(String first, String second) {
        if (first == null || second == null) {
            return 0.0;
        }

        var s1 = normalize(first);
        var s2 = normalize(second);

        if (s1.equals(s2)) {
            return 1.0;
        }
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        double jaroScore = jaro(s1, s2);
        int prefixLength = commonPrefixLength(s1, s2);

        return jaroScore + (prefixLength * WINKLER_PREFIX_WEIGHT * (1.0 - jaroScore));
    }

    static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static double jaro(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();

        int matchWindow = Math.max(0, Math.max(len1, len2) / 2 - 1);

        boolean[] s1Matched = new boolean[len1];
        boolean[] s2Matched = new boolean[len2];

        int matches = 0;
        int transpositions = 0;

        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchWindow);
            int end = Math.min(i + matchWindow + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matched[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matched[i] = true;
                s2Matched[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matched[i]) {
                continue;
            }
            while (!s2Matched[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        double m = matches;
        return (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0;
    }

    private static int commonPrefixLength(String s1, String s2) {
        int maxLength = Math.min(Math.min(s1.length(), s2.length()), MAX_PREFIX_LENGTH);
        int prefixLength = 0;

        for (int i = 0; i < maxLength; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        return prefixLength;
    }
}
