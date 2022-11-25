package me.linoxgh.viaversionlimitervelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import me.linoxgh.viaversionlimiter.shared.Config;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "viaversionlimitervelocity",
        name = "ViaVersionLimiterVelocity",
        version = "1.0-SNAPSHOT",
        description = "Kick the players with unverified minecraft versions.",
        authors = {"LinoxGH"}
)
public class ViaVersionLimiterVelocity {

    private final Logger logger;
    private final Path dataFolder;

    private final Config config;

    @Inject public ViaVersionLimiterVelocity(Logger logger, @DataDirectory Path dataFolder) {
        this.logger = logger;
        this.dataFolder = dataFolder;

        this.config = new ConfigVelocity(logger, dataFolder);
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.debug("Loading plugin configurations.");
        config.loadConfig();
    }


    @Subscribe
    public void onProxyConnect(ServerConnectedEvent event) {
        if (event.getServer().getServerInfo().getAddress().getHostName().startsWith(config.getAllowedDomain())) return;

        Player p = event.getPlayer();
        int ver = p.getProtocolVersion().getProtocol();

        if (!config.getVersions().contains(ver)) {
            this.logger.debug("Player " + p.getUsername() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            StringBuilder kickMsg = new StringBuilder();
            config.getKickMessages().forEach(msg -> kickMsg.append(msg).append("\n"));
            p.disconnect(Component.text(kickMsg.substring(0, kickMsg.length() - 1)));
        }
    }
}
