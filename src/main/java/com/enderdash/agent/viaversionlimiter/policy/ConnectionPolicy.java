package com.enderdash.agent.viaversionlimiter.policy;

import java.util.Objects;
import java.util.Optional;

public record ConnectionPolicy(VersionPolicy versions, HostMatcher bypassHost) {
    public ConnectionPolicy {
        Objects.requireNonNull(versions, "versions");
        Objects.requireNonNull(bypassHost, "bypassHost");
    }

    public ConnectionDecision evaluate(int protocol, Optional<String> virtualHost) {
        if (versions.supports(protocol)) {
            return ConnectionDecision.SUPPORTED;
        }
        if (bypassHost.matches(virtualHost)) {
            return ConnectionDecision.BYPASSED;
        }
        return ConnectionDecision.REJECTED;
    }
}
