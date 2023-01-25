package me.linoxgh.viaversionlimitervelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.linoxgh.viaversionlimiter.shared.Config;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "viaversionlimitervelocity",
        name = "ViaVersionLimiterVelocity",
        version = "1.1",
        description = "Kick the players with unverified minecraft versions.",
        authors = {"LinoxGH"}
)
public class ViaVersionLimiterVelocity {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataFolder;

    private final Config config;

    @Inject public ViaVersionLimiterVelocity(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataFolder) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataFolder = dataFolder;

        this.config = new ConfigVelocity(logger, dataFolder);
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.debug("Loading plugin configurations.");
        config.loadConfig();
        if (config.isEnabled()) logger.debug("Enabling ViaVersionLimiter..");
        else logger.debug("ViaVersionLimiter is not enabled in the config!");

        if (config.isEnableBroadcast()) {
            proxyServer.getScheduler().buildTask(this, () -> {
                for (Player p : proxyServer.getAllPlayers()) {
                    if (isPlayerUnsupported(p, config.isWhitelist())) {
                        for (String msg : config.getMessage()) {
                            p.sendMessage(deserialize(msg));
                        }
                    }
                }
            }).repeat(config.getBroadcastDelay(), TimeUnit.SECONDS).schedule();
        }
        if (config.isEnableActionBar()) {
            proxyServer.getScheduler().buildTask(this, () -> {
                for (Player p : proxyServer.getAllPlayers()) {
                    if (isPlayerUnsupported(p, config.isWhitelist())) {
                        p.sendActionBar(deserialize(config.getActionBarMessage()));
                    }
                }
            }).repeat(2, TimeUnit.SECONDS).schedule();
        }
    }


    @Subscribe
    public void onProxyConnect(ServerConnectedEvent event) {
        if (!config.isEnabled()) return;

        Player p = event.getPlayer();

        if (config.isEnableMessage()) {
            if (event.getPreviousServer().isEmpty() && config.isOnJoin() ||
                    event.getPreviousServer().isPresent() && config.isOnServerChange()) {
                if (isPlayerUnsupported(p, config.isWhitelist())) {
                    for (String msg : config.getMessage()) {
                        p.sendMessage(deserialize(msg));
                    }
                }
            }
        }
        if (config.isEnableBossBar()) {
            if (isPlayerUnsupported(p, config.isWhitelist())) {
                p.showBossBar(BossBar.bossBar(deserialize(config.getBossBarMessage()), 1, BossBar.Color.RED, BossBar.Overlay.NOTCHED_6));
            }
        }

        if (event.getPlayer().getVirtualHost().get().getHostString().equalsIgnoreCase(config.getAllowedDomain())) return;

        int ver = p.getProtocolVersion().getProtocol();
        if (isPlayerUnsupported(p, config.isWhitelist())) {
            this.logger.debug("Player " + p.getUsername() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            StringBuilder kickMsg = new StringBuilder();
            config.getKickMessages().forEach(msg -> kickMsg.append(msg).append("\n"));
            p.disconnect(deserialize(kickMsg.substring(0, kickMsg.length() - 1)));
        }
    }

    private boolean isPlayerUnsupported(Player p, boolean whitelist) {
        int ver = p.getProtocolVersion().getProtocol();

        return (whitelist && !config.getVersions().contains(ver)) ||
                !whitelist && config.getVersions().contains(ver);
    }

    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacy('&');
    public TextComponent deserialize(String text) {
        return serializer.deserialize(text);
    }
}
