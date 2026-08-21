package com.enderdash.agent.viaversionlimiter.policy;

import java.net.IDN;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class HostMatcher {
    private static final String WILDCARD = "*";

    private final Optional<String> expectedHost;

    private HostMatcher(Optional<String> expectedHost) {
        this.expectedHost = expectedHost;
    }

    public static HostMatcher exact(String host) {
        Objects.requireNonNull(host, "host");
        return new HostMatcher(Optional.of(normalize(host)));
    }

    public static HostMatcher disabled() {
        return new HostMatcher(Optional.empty());
    }

    public static HostMatcher any() {
        return new HostMatcher(Optional.of(WILDCARD));
    }

    public boolean matches(Optional<String> candidate) {
        if (expectedHost.isEmpty()) {
            return false;
        }
        if (expectedHost.get().equals(WILDCARD)) {
            return true;
        }
        if (candidate.isEmpty()) {
            return false;
        }

        try {
            return expectedHost.get().equals(normalize(candidate.get()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public Optional<String> expectedHost() {
        return expectedHost;
    }

    private static String normalize(String host) {
        String normalized = host.strip();
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("policy.bypass-domain cannot be blank");
        }
        return IDN.toASCII(normalized, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
    }
}
