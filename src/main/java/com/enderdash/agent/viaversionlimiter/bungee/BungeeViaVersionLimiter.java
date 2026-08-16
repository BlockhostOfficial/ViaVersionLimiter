package com.enderdash.agent.viaversionlimiter.bungee;

import com.enderdash.agent.viaversionlimiter.config.LimiterConfiguration;
import com.enderdash.agent.viaversionlimiter.config.LimiterConfigurationHolder;
import com.enderdash.agent.viaversionlimiter.policy.ConnectionDecision;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

public final class BungeeViaVersionLimiter extends Plugin implements Listener {
    private Path configPath;
    private BungeeNotificationService notifications;
    private volatile LimiterConfigurationHolder configurationHolder;

    @Override
    public void onEnable() {
        configPath = getDataFolder().toPath().resolve("config.yml");
        notifications = new BungeeNotificationService(getProxy(), this);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new BungeeLimiterCommand(this));

        ReloadResult result = reloadConfiguration(false);
        if (!result.successful()) {
            getLogger().severe("ViaVersionLimiter started without an active configuration: " + result.message());
        }
    }

    @Override
    public void onDisable() {
        if (notifications != null) {
            notifications.close();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        LimiterConfiguration current = currentConfiguration();
        if (current == null || !current.enabled() || event.isCancelled()) {
            return;
        }

        PendingConnection connection = event.getConnection();
        if (evaluate(current, connection) != ConnectionDecision.REJECTED) {
            return;
        }

        event.setCancelled(true);
        event.setReason(BungeeTextRenderer.renderLines(current.kickMessage()));
        getLogger().info("Rejected " + connection.getName()
                + " using protocol " + connection.getVersion()
                + " through host " + virtualHost(connection).orElse("<unknown>"));
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        LimiterConfiguration current = currentConfiguration();
        ProxiedPlayer player = event.getPlayer();
        if (current == null || !current.enabled()) {
            notifications.unmark(player);
            return;
        }

        switch (evaluate(current, player.getPendingConnection())) {
            case SUPPORTED -> notifications.unmark(player);
            case BYPASSED -> notifications.handleBypass(player, event.getFrom() != null);
            case REJECTED -> {
                notifications.unmark(player);
                player.disconnect(BungeeTextRenderer.renderLines(current.kickMessage()));
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        notifications.onDisconnect(event.getPlayer());
    }

    synchronized ReloadResult reloadConfiguration(boolean enforceExistingPlayers) {
        try {
            LimiterConfigurationHolder holder = configurationHolder;
            LimiterConfiguration loaded;
            if (holder == null) {
                holder = new LimiterConfigurationHolder(configPath);
                configurationHolder = holder;
                loaded = holder.get();
            } else {
                loaded = holder.reload().validated();
            }
            notifications.reconfigure(loaded);
            if (enforceExistingPlayers && loaded.enabled()) {
                enforceExistingPlayers(loaded);
            }

            var policy = loaded.connectionPolicy();
            getLogger().info("Loaded ViaVersionLimiter configuration: enabled=" + loaded.enabled()
                    + ", mode=" + policy.versions().mode()
                    + ", protocols=" + policy.versions().protocols().size()
                    + ", bypassHost=" + policy.bypassHost().expectedHost().orElse("disabled"));
            return new ReloadResult(true, "ViaVersionLimiter configuration reloaded.");
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Could not load ViaVersionLimiter configuration", exception);
            return new ReloadResult(false, "Reload failed. Check the proxy log for details.");
        }
    }

    BaseComponent statusMessage() {
        LimiterConfiguration current = currentConfiguration();
        if (current == null) {
            return TextComponent.fromLegacy(ChatColor.RED + "ViaVersionLimiter has no valid configuration.");
        }
        var policy = current.connectionPolicy();
        String state = current.enabled() ? "enabled" : "disabled";
        String summary = "ViaVersionLimiter is " + state
                + "; mode=" + policy.versions().mode()
                + "; protocols=" + policy.versions().protocols().size()
                + "; bypass=" + policy.bypassHost().expectedHost().orElse("disabled");
        return TextComponent.fromLegacy((current.enabled() ? ChatColor.GREEN : ChatColor.YELLOW) + summary);
    }

    private void enforceExistingPlayers(LimiterConfiguration current) {
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            switch (evaluate(current, player.getPendingConnection())) {
                case SUPPORTED -> notifications.unmark(player);
                case BYPASSED -> notifications.markBypassed(player);
                case REJECTED -> {
                    notifications.unmark(player);
                    player.disconnect(BungeeTextRenderer.renderLines(current.kickMessage()));
                }
            }
        }
    }

    private LimiterConfiguration currentConfiguration() {
        LimiterConfigurationHolder holder = configurationHolder;
        return holder == null ? null : holder.get();
    }

    private static ConnectionDecision evaluate(LimiterConfiguration current, PendingConnection connection) {
        return current.connectionPolicy().evaluate(connection.getVersion(), virtualHost(connection));
    }

    private static Optional<String> virtualHost(PendingConnection connection) {
        return Optional.ofNullable(connection.getVirtualHost()).map(InetSocketAddress::getHostString);
    }

    record ReloadResult(boolean successful, String message) {
    }
}
