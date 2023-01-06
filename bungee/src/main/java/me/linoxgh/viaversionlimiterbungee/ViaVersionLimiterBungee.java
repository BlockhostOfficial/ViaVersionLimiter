package me.linoxgh.viaversionlimiterbungee;

import me.linoxgh.viaversionlimiter.shared.Config;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
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

        if (config.isEnableBroadcast()) {
            getProxy().getScheduler().schedule(this, () -> {
                for (ProxiedPlayer p : getProxy().getPlayers()) {
                    if (isPlayerUnsupported(p, !config.isWhitelist()))
                        p.sendMessages(config.getMessage().toArray(String[]::new));
                }
            }, config.getBroadcastDelay(), config.getBroadcastDelay(), TimeUnit.SECONDS);
        }
        if (config.isEnableActionBar()) {
            getProxy().getScheduler().schedule(this, () -> {
                for (ProxiedPlayer p : getProxy().getPlayers()) {
                    if (isPlayerUnsupported(p, !config.isWhitelist())) {
                        p.sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(config.getActionBarMessage()));
                    }
                }
            }, 2, TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public void onConnect(ServerConnectedEvent event) {
        ProxiedPlayer p = event.getPlayer();
        Optional<InetAddress> ip = getIPFromSocketAddress(p);
        if (ip.orElse(getIpFromOldMethod(p).get()).getHostName().equalsIgnoreCase(config.getAllowedDomain())) return;

        int ver = p.getPendingConnection().getVersion();

        if (isPlayerUnsupported(p, config.isWhitelist())) {
            getLogger().info("Player " + p.getName() + " (" + p.getUniqueId().toString() + ") tried to join with " + ver + ".");
            StringBuilder kickMsg = new StringBuilder();
            config.getKickMessages().forEach(msg -> kickMsg.append(msg).append("\n"));
            p.disconnect(TextComponent.fromLegacyText(kickMsg.substring(0, kickMsg.length() - 1)));
            return;
        }

        if (config.isEnableMessage() && config.isOnJoin()) {
            if (isPlayerUnsupported(p, !config.isWhitelist())) {
                p.sendMessages(config.getMessage().toArray(String[]::new));
            }
        }
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        ProxiedPlayer p = event.getPlayer();

        if (config.isEnableMessage() && config.isOnServerChange()) {
            if (isPlayerUnsupported(p, !config.isWhitelist())) {
                p.sendMessages(config.getMessage().toArray(String[]::new));
            }
        }
    }

    private boolean isPlayerUnsupported(ProxiedPlayer p, boolean whitelist) {
        int ver = p.getPendingConnection().getVersion();

        return (whitelist && !config.getVersions().contains(ver)) ||
                !whitelist && config.getVersions().contains(ver);
    }

    @SuppressWarnings("deprecation") // ProxiedPlayer#getAddress is deprecated
    private Optional<InetAddress> getIpFromOldMethod(ProxiedPlayer player) {
        try {
            return Optional.ofNullable(player.getAddress()).map(InetSocketAddress::getAddress);
        } catch (NoSuchMethodError e) {
            return Optional.empty();
        }
    }

    private Optional<InetAddress> getIPFromSocketAddress(ProxiedPlayer player) {
        try {
            SocketAddress socketAddress = player.getSocketAddress();
            if (socketAddress instanceof InetSocketAddress) {
                return Optional.of(((InetSocketAddress) socketAddress).getAddress());
            }

            // Unix domain socket address requires Java 16 compatibility.
            // These connections come from the same physical machine
            Class<?> jdk16SocketAddressType = Class.forName("java.net.UnixDomainSocketAddress");
            if (jdk16SocketAddressType.isAssignableFrom(socketAddress.getClass())) {
                return Optional.of(InetAddress.getLocalHost());
            }
        } catch (NoSuchMethodError | ClassNotFoundException | UnknownHostException e) {
            // Ignored
        }
        return Optional.empty();
    }
}
