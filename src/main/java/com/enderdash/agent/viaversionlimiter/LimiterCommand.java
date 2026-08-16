package com.enderdash.agent.viaversionlimiter;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import net.blockhost.commons.commands.help.CommandDescription;
import net.blockhost.commons.commands.help.HelpInjector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.strokkur.commands.Aliases;
import net.strokkur.commands.Command;
import net.strokkur.commands.CustomExecutorWrapper;
import net.strokkur.commands.Executes;
import net.strokkur.commands.permission.Permission;

import java.lang.reflect.Method;
import java.util.function.Function;

@CustomExecutorWrapper
@interface LimiterHelpWrapper {
}

@Aliases("vvl")
@Command("viaversionlimiter")
@LimiterHelpWrapper
@Permission("viaversionlimiter.admin")
public final class LimiterCommand {
    private final ViaVersionLimiter plugin;

    public LimiterCommand(ViaVersionLimiter plugin) {
        this.plugin = plugin;
    }

    @Executes
    public void help(CommandContext<CommandSource> context) {
        HelpInjector.sendDefaultHelp(context, Function.identity());
    }

    @LimiterHelpWrapper
    public static com.mojang.brigadier.Command<CommandSource> helpInjection(
            com.mojang.brigadier.Command<CommandSource> command,
            Method method
    ) {
        return HelpInjector.wrapCommand(command, method);
    }

    @CommandDescription("Shows the active version policy and bypass hostname.")
    @Executes("status")
    public void status(CommandSource source) {
        source.sendMessage(plugin.statusMessage());
    }

    @CommandDescription("Reloads and validates the ViaVersionLimiter configuration.")
    @Executes("reload")
    public void reload(CommandSource source) {
        ViaVersionLimiter.ReloadResult result = plugin.reloadConfiguration(true);
        source.sendMessage(Component.text(
                result.message(),
                result.successful() ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }
}
