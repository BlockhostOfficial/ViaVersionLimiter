package com.enderdash.agent.viaversionlimiter;

import com.enderdash.agent.viaversionlimiter.config.LimiterConfiguration;
import com.enderdash.agent.viaversionlimiter.config.LimiterConfigurationHolder;
import com.enderdash.agent.viaversionlimiter.policy.ConnectionDecision;
import com.google.inject.Inject;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(
        id = "viaversionlimitervelocity",
        name = "ViaVersionLimiter",
        version = "2.0.0-SNAPSHOT",
        description = "Enforce Minecraft protocol version policy before backend connection.",
        url = "https://github.com/BlockhostOfficial/ViaVersionLimiter",
        authors = {"Blockhost", "AlexProgrammerDE"}
)
public final class ViaVersionLimiter {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path configPath;
    private final PlayerNotificationService notifications;
    private volatile LimiterConfigurationHolder configurationHolder;

    @Inject
    public ViaVersionLimiter(
            ProxyServer proxyServer,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        configPath = dataDirectory.resolve("config.yml");
        notifications = new PlayerNotificationService(proxyServer, this);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        registerCommand();
        ReloadResult result = reloadConfiguration(false);
        if (!result.successful()) {
            logger.error("ViaVersionLimiter started without an active configuration: {}", result.message());
        }
    }

    @Subscribe(priority = Short.MIN_VALUE, async = false)
    public void onLogin(LoginEvent event) {
        LimiterConfiguration current = currentConfiguration();
        if (current == null || !current.enabled() || !event.getResult().isAllowed()) {
            return;
        }

        Player player = event.getPlayer();
        ConnectionDecision decision = evaluate(current, player);
        switch (decision) {
            case SUPPORTED -> notifications.unmark(player);
            case BYPASSED -> notifications.markBypassed(player, false);
            case REJECTED -> {
                notifications.unmark(player);
                event.setResult(ResultedEvent.ComponentResult.denied(
                        LegacyTextRenderer.renderLines(current.kickMessage())
                ));
                logger.info(
                        "Rejected {} using protocol {} through host {}",
                        player.getUsername(),
                        player.getProtocolVersion().getProtocol(),
                        virtualHost(player).orElse("<unknown>")
                );
            }
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        notifications.onServerConnected(event.getPlayer(), event.getPreviousServer().isPresent());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        notifications.onDisconnect(event.getPlayer());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        notifications.close();
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
            logger.info(
                    "Loaded ViaVersionLimiter configuration: enabled={}, mode={}, protocols={}, bypassHost={}",
                    loaded.enabled(),
                    loaded.connectionPolicy().versions().mode(),
                    loaded.connectionPolicy().versions().protocols().size(),
                    loaded.connectionPolicy().bypassHost().expectedHost().orElse("disabled")
            );
            return new ReloadResult(true, "ViaVersionLimiter configuration reloaded.");
        } catch (RuntimeException exception) {
            logger.error("Could not load ViaVersionLimiter configuration", exception);
            return new ReloadResult(false, "Reload failed. Check the proxy log for details.");
        }
    }

    Component statusMessage() {
        LimiterConfiguration current = currentConfiguration();
        if (current == null) {
            return Component.text("ViaVersionLimiter has no valid configuration.", NamedTextColor.RED);
        }
        String state = current.enabled() ? "enabled" : "disabled";
        String summary = "ViaVersionLimiter is " + state
                + "; mode=" + current.connectionPolicy().versions().mode()
                + "; protocols=" + current.connectionPolicy().versions().protocols().size()
                + "; bypass=" + current.connectionPolicy().bypassHost().expectedHost().orElse("disabled");
        return Component.text(summary, current.enabled() ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    private void registerCommand() {
        LimiterCommandBrigadier.register(proxyServer, this, this);
    }

    private void enforceExistingPlayers(LimiterConfiguration current) {
        for (Player player : proxyServer.getAllPlayers()) {
            switch (evaluate(current, player)) {
                case SUPPORTED -> notifications.unmark(player);
                case BYPASSED -> notifications.markBypassed(player, true);
                case REJECTED -> {
                    notifications.unmark(player);
                    player.disconnect(LegacyTextRenderer.renderLines(current.kickMessage()));
                }
            }
        }
    }

    private static ConnectionDecision evaluate(LimiterConfiguration current, Player player) {
        return current.connectionPolicy().evaluate(
                player.getProtocolVersion().getProtocol(),
                virtualHost(player)
        );
    }

    private static Optional<String> virtualHost(Player player) {
        return player.getVirtualHost().map(InetSocketAddress::getHostString);
    }

    private LimiterConfiguration currentConfiguration() {
        LimiterConfigurationHolder holder = configurationHolder;
        return holder == null ? null : holder.get();
    }

    record ReloadResult(boolean successful, String message) {
    }
}
