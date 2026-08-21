package com.enderdash.agent.viaversionlimiter.policy;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectionPolicyTest {
    private final ConnectionPolicy policy = new ConnectionPolicy(
            new VersionPolicy(VersionPolicy.Mode.ALLOWLIST, Set.of(769)),
            HostMatcher.exact("nosupport.example.org")
    );

    @Test
    void acceptsSupportedProtocolOnEveryHost() {
        assertEquals(
                ConnectionDecision.SUPPORTED,
                policy.evaluate(769, Optional.of("play.example.org"))
        );
        assertEquals(
                ConnectionDecision.SUPPORTED,
                policy.evaluate(769, Optional.empty())
        );
    }

    @Test
    void bypassRequiresAnExactNormalizedHostname() {
        assertEquals(
                ConnectionDecision.BYPASSED,
                policy.evaluate(578, Optional.of("NOSUPPORT.EXAMPLE.ORG."))
        );
        assertEquals(
                ConnectionDecision.REJECTED,
                policy.evaluate(578, Optional.of("fake-nosupport.example.org"))
        );
        assertEquals(
                ConnectionDecision.REJECTED,
                policy.evaluate(578, Optional.empty())
        );
    }

    @Test
    void disabledBypassRejectsUnsupportedProtocols() {
        ConnectionPolicy withoutBypass = new ConnectionPolicy(
                policy.versions(),
                HostMatcher.disabled()
        );

        assertEquals(
                ConnectionDecision.REJECTED,
                withoutBypass.evaluate(578, Optional.of("nosupport.example.org"))
        );
    }

    @Test
    void wildcardBypassAcceptsUnsupportedProtocolsOnEveryHost() {
        ConnectionPolicy withoutHostEnforcement = new ConnectionPolicy(
                policy.versions(),
                HostMatcher.any()
        );

        assertEquals(
                ConnectionDecision.BYPASSED,
                withoutHostEnforcement.evaluate(578, Optional.of("play.example.org"))
        );
        assertEquals(
                ConnectionDecision.BYPASSED,
                withoutHostEnforcement.evaluate(578, Optional.of("another.example.net"))
        );
        assertEquals(
                ConnectionDecision.BYPASSED,
                withoutHostEnforcement.evaluate(578, Optional.empty())
        );
    }

    @Test
    void malformedHandshakeHostsFailClosed() {
        assertEquals(ConnectionDecision.REJECTED, policy.evaluate(578, Optional.of("bad host")));
        assertEquals(ConnectionDecision.REJECTED, policy.evaluate(578, Optional.of(" ")));
    }
}
