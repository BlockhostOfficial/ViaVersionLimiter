package com.enderdash.agent.viaversionlimiter.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.List;
import java.util.Locale;

final class BungeeLimiterCommand extends Command implements TabExecutor {
    private static final List<String> SUBCOMMANDS = List.of("reload", "status");

    private final BungeeViaVersionLimiter plugin;

    BungeeLimiterCommand(BungeeViaVersionLimiter plugin) {
        super("viaversionlimiter", "viaversionlimiter.admin", "vvl");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length == 0 || arguments[0].equalsIgnoreCase("status")) {
            sender.sendMessage(plugin.statusMessage());
            return;
        }
        if (arguments.length == 1 && arguments[0].equalsIgnoreCase("reload")) {
            BungeeViaVersionLimiter.ReloadResult result = plugin.reloadConfiguration(true);
            sender.sendMessage(TextComponent.fromLegacy(
                    (result.successful() ? ChatColor.GREEN : ChatColor.RED) + result.message()
            ));
            return;
        }
        sender.sendMessage(TextComponent.fromLegacy(ChatColor.RED + "Usage: /viaversionlimiter <reload|status>"));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] arguments) {
        if (arguments.length > 1) {
            return List.of();
        }
        String prefix = arguments.length == 0 ? "" : arguments[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
