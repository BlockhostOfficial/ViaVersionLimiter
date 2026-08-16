package com.enderdash.agent.viaversionlimiter;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Locale;

final class LimiterCommand implements SimpleCommand {
    private static final String PERMISSION = "viaversionlimiter.admin";
    private static final List<String> SUBCOMMANDS = List.of("reload", "status");

    private final ViaVersionLimiter plugin;

    LimiterCommand(ViaVersionLimiter plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 0 || arguments[0].equalsIgnoreCase("status")) {
            invocation.source().sendMessage(plugin.statusMessage());
            return;
        }
        if (arguments.length == 1 && arguments[0].equalsIgnoreCase("reload")) {
            ViaVersionLimiter.ReloadResult result = plugin.reloadConfiguration(true);
            invocation.source().sendMessage(Component.text(
                    result.message(),
                    result.successful() ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            return;
        }
        invocation.source().sendMessage(Component.text(
                "Usage: /viaversionlimiter <reload|status>",
                NamedTextColor.RED
        ));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length > 1) {
            return List.of();
        }
        String prefix = invocation.arguments().length == 0
                ? ""
                : invocation.arguments()[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }
}
