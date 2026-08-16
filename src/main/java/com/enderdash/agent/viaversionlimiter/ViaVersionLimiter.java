package com.enderdash.agent.viaversionlimiter;

import com.enderdash.agent.viaversionlimiter.config.LimiterConfiguration;
import com.enderdash.agent.viaversionlimiter.config.LimiterConfigurationLoader;
import com.enderdash.agent.viaversionlimiter.policy.ConnectionDecision;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
    private final LimiterConfigurationLoader configurationLoader;
    private final PlayerNotificationService notifications;
    private final AtomicReference<LimiterConfiguration> configuration = new AtomicReference<>();

    @Inject
    public ViaVersionLimiter(
            ProxyServer proxyServer,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        configurationLoader = new LimiterConfigurationLoader(dataDirectory);
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
        LimiterConfiguration current = configuration.get();
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

    ReloadResult reloadConfiguration(boolean enforceExistingPlayers) {
        try {
            LimiterConfiguration loaded = configurationLoader.load();
            configuration.set(loaded);
            notifications.reconfigure(loaded);
            if (enforceExistingPlayers && loaded.enabled()) {
                enforceExistingPlayers(loaded);
            }
            logger.info(
                    "Loaded ViaVersionLimiter configuration: enabled={}, mode={}, protocols={}, bypassHost={}",
                    loaded.enabled(),
                    loaded.policy().versions().mode(),
                    loaded.policy().versions().protocols().size(),
                    loaded.policy().bypassHost().expectedHost().orElse("disabled")
            );
            return new ReloadResult(true, "ViaVersionLimiter configuration reloaded.");
        } catch (IOException exception) {
            logger.error("Could not load ViaVersionLimiter configuration", exception);
            return new ReloadResult(false, "Reload failed. Check the proxy log for details.");
        }
    }

    Component statusMessage() {
        LimiterConfiguration current = configuration.get();
        if (current == null) {
            return Component.text("ViaVersionLimiter has no valid configuration.", NamedTextColor.RED);
        }
        String state = current.enabled() ? "enabled" : "disabled";
        String summary = "ViaVersionLimiter is " + state
                + "; mode=" + current.policy().versions().mode()
                + "; protocols=" + current.policy().versions().protocols().size()
                + "; bypass=" + current.policy().bypassHost().expectedHost().orElse("disabled");
        return Component.text(summary, current.enabled() ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
    }

    private void registerCommand() {
        CommandManager commandManager = proxyServer.getCommandManager();
        var metadata = commandManager.metaBuilder("viaversionlimiter")
                .aliases("vvl")
                .plugin(this)
                .build();
        commandManager.register(metadata, new LimiterCommand(this));
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
        return current.policy().evaluate(
                player.getProtocolVersion().getProtocol(),
                virtualHost(player)
        );
    }

    private static Optional<String> virtualHost(Player player) {
        return player.getVirtualHost().map(InetSocketAddress::getHostString);
    }

    record ReloadResult(boolean successful, String message) {
    }
}
