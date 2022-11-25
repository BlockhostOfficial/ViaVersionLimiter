package me.linoxgh.viaversionlimiterbungee;

import me.linoxgh.viaversionlimiter.shared.Config;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public final class ViaVersionLimiterBungee extends Plugin implements Listener {
    private Config config;

    @Override
    public void onEnable() {
        getLogger().info("Loading plugin configurations.");
        this.config = new ConfigBungee(getLogger(), getDataFolder().toPath());

        getProxy().getPluginManager().registerListener(this, this);
    }

    @EventHandler
    public void onConnect(ServerConnectedEvent event) {
        if (event.getServer().getAddress().getHostName().startsWith(config.getAllowedDomain())) return;

        ProxiedPlayer p = event.getPlayer();
        int ver = p.getPendingConnection().getVersion();

        if (!config.getVersions().contains(ver)) {
            getLogger().info("Player " + p.getName() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            StringBuilder kickMsg = new StringBuilder();
            config.getKickMessages().forEach(msg -> kickMsg.append(msg).append("\n"));
            p.disconnect(TextComponent.fromLegacyText(kickMsg.substring(0, kickMsg.length() - 1)));
        }
    }
}
