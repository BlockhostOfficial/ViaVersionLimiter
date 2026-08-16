package com.enderdash.agent.viaversionlimiter.policy;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record VersionPolicy(Mode mode, Set<Integer> protocols) {
    public VersionPolicy {
        Objects.requireNonNull(mode, "mode");
        protocols = Set.copyOf(protocols);
        if (protocols.isEmpty()) {
            throw new IllegalArgumentException("policy.versions must contain at least one protocol");
        }
        if (protocols.stream().anyMatch(protocol -> protocol < 0)) {
            throw new IllegalArgumentException("policy.versions cannot contain negative protocols");
        }
    }

    public boolean supports(int protocol) {
        boolean listed = protocols.contains(protocol);
        return mode == Mode.ALLOWLIST ? listed : !listed;
    }

    public enum Mode {
        ALLOWLIST,
        BLOCKLIST;

        public static Mode parse(String value) {
            Objects.requireNonNull(value, "policy.mode");
            try {
                return valueOf(value.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "policy.mode must be ALLOWLIST or BLOCKLIST",
                        exception
                );
            }
        }
    }
}
