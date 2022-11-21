package me.linoxgh.verifyversion;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

@Plugin(
        id = "verifyversion",
        name = "VerifyVersion",
        version = "1.0-SNAPSHOT",
        description = "Kick the players with unverified minecraft versions.",
        authors = {"LinoxGH"}
)
public class VerifyVersion {

    @Inject private Logger logger;

    @Subscribe
    public void onProxyConnect(ServerConnectedEvent event) {
        if (event.getServer().getServerInfo().getAddress().getHostName().startsWith("noverify.6b6t")) return;

        Player p = event.getPlayer();
        ProtocolVersion ver = p.getProtocolVersion();

        if (!ver.equals(ProtocolVersion.MINECRAFT_1_19) &&
                !ver.equals(ProtocolVersion.MINECRAFT_1_19_1) &&
                !ver.equals(ProtocolVersion.MAXIMUM_VERSION)) {

            this.logger.debug("Player " + p.getUsername() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            p.disconnect(Component.text("You are using an unsupported version. (" + ver + ") Please update or join with noverify.6b6t.org to play on this version however you won't receive any bug support."));
        }
    }
}
