package com.enderdash.agent.viaversionlimiter.policy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionPolicyTest {
    @Test
    void allowlistSupportsOnlyListedProtocols() {
        VersionPolicy policy = new VersionPolicy(VersionPolicy.Mode.ALLOWLIST, Set.of(769, 770));

        assertTrue(policy.supports(769));
        assertFalse(policy.supports(578));
    }

    @Test
    void blocklistRejectsOnlyListedProtocols() {
        VersionPolicy policy = new VersionPolicy(VersionPolicy.Mode.BLOCKLIST, Set.of(578));

        assertFalse(policy.supports(578));
        assertTrue(policy.supports(769));
    }

    @Test
    void policyRequiresAtLeastOneProtocol() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VersionPolicy(VersionPolicy.Mode.ALLOWLIST, Set.of())
        );
    }
}
