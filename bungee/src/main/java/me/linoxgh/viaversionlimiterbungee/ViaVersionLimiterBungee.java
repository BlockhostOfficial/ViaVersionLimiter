package me.linoxgh.viaversionlimiterbungee;

import me.linoxgh.viaversionlimiter.shared.Config;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;

public final class ViaVersionLimiterBungee extends Plugin implements Listener {
    private Config config;

    @Override
    public void onEnable() {
        getLogger().info("Loading plugin configurations.");
        this.config = new ConfigBungee(getLogger(), getDataFolder().toPath());

        if (config.isEnabled()) getLogger().info("Enabling ViaVersionLimiter..");
        else {
            getLogger().info("ViaVersionLimiter is not enabled in the config!");
            return;
        }
        getProxy().getPluginManager().registerListener(this, this);

        if (config.isEnableMessage()) {
            getProxy().getScheduler().schedule(this, () -> {
                for (ProxiedPlayer p : getProxy().getPlayers()) {
                    if (isPlayerUnsupported(p, !config.isReverse()))
                        p.sendMessages(config.getMessage().toArray(String[]::new));
                }
            }, config.getDelay(), config.getDelay(), TimeUnit.MILLISECONDS);
        }
    }

    @EventHandler
    public void onConnect(ServerConnectedEvent event) {
        if (event.getServer().getAddress().getHostName().startsWith(config.getAllowedDomain())) return;

        ProxiedPlayer p = event.getPlayer();
        int ver = p.getPendingConnection().getVersion();

        if (isPlayerUnsupported(p, config.isWhitelist())) {
            getLogger().info("Player " + p.getName() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            StringBuilder kickMsg = new StringBuilder();
            config.getKickMessages().forEach(msg -> kickMsg.append(msg).append("\n"));
            p.disconnect(TextComponent.fromLegacyText(kickMsg.substring(0, kickMsg.length() - 1)));
            return;
        }

        if (config.isEnableMessage() && config.isOnJoin()) {
            if (isPlayerUnsupported(p, !config.isReverse())) {
                p.sendMessages(config.getMessage().toArray(String[]::new));
            }
        }
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer p = event.getPlayer();

        if (config.isEnableMessage() && config.isOnServerChange()) {
            if (isPlayerUnsupported(p, !config.isReverse())) {
                p.sendMessages(config.getMessage().toArray(String[]::new));
            }
        }
    }

    private boolean isPlayerUnsupported(ProxiedPlayer p, boolean whitelist) {
        int ver = p.getPendingConnection().getVersion();

        return (whitelist && !config.getVersions().contains(ver)) ||
                !whitelist && config.getVersions().contains(ver);
    }
}
