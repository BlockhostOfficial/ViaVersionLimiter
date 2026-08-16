package com.enderdash.agent.viaversionlimiter.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

final class BungeeTextRenderer {
    private BungeeTextRenderer() {
    }

    static BaseComponent render(String value) {
        return TextComponent.fromLegacy(ChatColor.translateAlternateColorCodes('&', value));
    }

    static BaseComponent renderLines(List<String> lines) {
        return render(String.join("\n", lines));
    }
}
