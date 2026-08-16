package com.enderdash.agent.viaversionlimiter.bungee;

import com.enderdash.agent.viaversionlimiter.config.LimiterConfiguration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.protocol.packet.BossBar;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

final class BungeeNotificationService implements AutoCloseable {
    private static final int BOSS_BAR_PROTOCOL = 107;

    private final ProxyServer proxyServer;
    private final Plugin plugin;
    private final Set<UUID> bypassedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> bossBars = new ConcurrentHashMap<>();
    private final Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();

    private volatile LimiterConfiguration configuration;

    BungeeNotificationService(ProxyServer proxyServer, Plugin plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
    }

    void reconfigure(LimiterConfiguration newConfiguration) {
        cancelTasks();
        hideAllBossBars();
        configuration = newConfiguration;

        if (!newConfiguration.enabled()) {
            bypassedPlayers.clear();
            return;
        }

        LimiterConfiguration.Notifications notifications = newConfiguration.notifications();
        if (notifications.broadcast().enabled()) {
            ScheduledTask task = proxyServer.getScheduler().schedule(
                    plugin,
                    this::broadcastMessages,
                    notifications.broadcast().interval().toMillis(),
                    notifications.broadcast().interval().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            tasks.add(task);
        }
        if (notifications.actionBar().enabled()) {
            ScheduledTask task = proxyServer.getScheduler().schedule(
                    plugin,
                    this::broadcastActionBars,
                    notifications.actionBar().interval().toMillis(),
                    notifications.actionBar().interval().toMillis(),
                    TimeUnit.MILLISECONDS
            );
            tasks.add(task);
        }
    }

    void markBypassed(ProxiedPlayer player) {
        bypassedPlayers.add(player.getUniqueId());
        showBossBar(player);
    }

    void handleBypass(ProxiedPlayer player, boolean serverChange) {
        markBypassed(player);

        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }
        LimiterConfiguration.Message message = current.notifications().message();
        boolean shouldSend = message.enabled()
                && (serverChange ? message.onServerChange() : message.onJoin());
        if (shouldSend) {
            sendMessages(player, message.lines());
        }
    }

    void unmark(ProxiedPlayer player) {
        bypassedPlayers.remove(player.getUniqueId());
        hideBossBar(player);
    }

    void onDisconnect(ProxiedPlayer player) {
        bypassedPlayers.remove(player.getUniqueId());
        bossBars.remove(player.getUniqueId());
    }

    private void broadcastMessages() {
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }
        bypassedPlayers.stream()
                .map(proxyServer::getPlayer)
                .filter(java.util.Objects::nonNull)
                .forEach(player -> sendMessages(player, current.notifications().message().lines()));
    }

    private void broadcastActionBars() {
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }
        var actionBar = BungeeTextRenderer.render(current.notifications().actionBar().message());
        bypassedPlayers.stream()
                .map(proxyServer::getPlayer)
                .filter(java.util.Objects::nonNull)
                .forEach(player -> player.sendMessage(ChatMessageType.ACTION_BAR, actionBar));
    }

    private void showBossBar(ProxiedPlayer player) {
        LimiterConfiguration current = configuration;
        if (current == null
                || !current.enabled()
                || !current.notifications().bossBar().enabled()
                || player.getPendingConnection().getVersion() < BOSS_BAR_PROTOCOL) {
            return;
        }
        bossBars.computeIfAbsent(player.getUniqueId(), ignored -> {
            UUID bossBarId = UUID.randomUUID();
            LimiterConfiguration.BossBarSettings settings = current.notifications().bossBar();
            BossBar bossBar = new BossBar(bossBarId, 0);
            bossBar.setTitle(BungeeTextRenderer.render(settings.message()));
            bossBar.setHealth(1.0f);
            bossBar.setColor(colorId(settings.color()));
            bossBar.setDivision(0);
            player.unsafe().sendPacket(bossBar);
            return bossBarId;
        });
    }

    private void hideBossBar(ProxiedPlayer player) {
        UUID bossBarId = bossBars.remove(player.getUniqueId());
        if (bossBarId != null && player.isConnected()) {
            player.unsafe().sendPacket(new BossBar(bossBarId, 1));
        }
    }

    private void hideAllBossBars() {
        bossBars.forEach((playerId, bossBarId) -> {
            ProxiedPlayer player = proxyServer.getPlayer(playerId);
            if (player != null && player.isConnected()) {
                player.unsafe().sendPacket(new BossBar(bossBarId, 1));
            }
        });
        bossBars.clear();
    }

    private static int colorId(LimiterConfiguration.BossBarColor color) {
        return switch (color) {
            case PINK -> 0;
            case BLUE -> 1;
            case RED -> 2;
            case GREEN -> 3;
            case YELLOW -> 4;
            case PURPLE -> 5;
            case WHITE -> 6;
        };
    }

    private static void sendMessages(ProxiedPlayer player, List<String> messages) {
        messages.stream()
                .map(BungeeTextRenderer::render)
                .forEach(player::sendMessage);
    }

    private void cancelTasks() {
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    @Override
    public void close() {
        cancelTasks();
        hideAllBossBars();
        bypassedPlayers.clear();
        configuration = null;
    }
}
