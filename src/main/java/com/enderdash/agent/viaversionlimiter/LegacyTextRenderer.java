package com.enderdash.agent.viaversionlimiter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

final class LegacyTextRenderer {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    private LegacyTextRenderer() {
    }

    static Component render(String value) {
        return SERIALIZER.deserialize(value);
    }

    static Component renderLines(List<String> lines) {
        return Component.join(
                JoinConfiguration.newlines(),
                lines.stream().map(LegacyTextRenderer::render).toList()
        );
    }
}
