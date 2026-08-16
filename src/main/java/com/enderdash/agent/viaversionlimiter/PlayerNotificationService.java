package com.enderdash.agent.viaversionlimiter;

import com.enderdash.agent.viaversionlimiter.config.LimiterConfiguration;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PlayerNotificationService implements AutoCloseable {
    private final ProxyServer proxyServer;
    private final Object plugin;
    private final Set<UUID> bypassedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();

    private volatile LimiterConfiguration configuration;

    PlayerNotificationService(ProxyServer proxyServer, Object plugin) {
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
            ScheduledTask task = proxyServer.getScheduler()
                    .buildTask(plugin, this::broadcastMessages)
                    .delay(notifications.broadcast().interval())
                    .repeat(notifications.broadcast().interval())
                    .schedule();
            tasks.add(task);
        }
        if (notifications.actionBar().enabled()) {
            ScheduledTask task = proxyServer.getScheduler()
                    .buildTask(plugin, this::broadcastActionBars)
                    .delay(notifications.actionBar().interval())
                    .repeat(notifications.actionBar().interval())
                    .schedule();
            tasks.add(task);
        }
    }

    void markBypassed(Player player, boolean connected) {
        bypassedPlayers.add(player.getUniqueId());
        if (connected) {
            showBossBar(player);
        }
    }

    void unmark(Player player) {
        bypassedPlayers.remove(player.getUniqueId());
        hideBossBar(player);
    }

    void onServerConnected(Player player, boolean serverChange) {
        if (!bypassedPlayers.contains(player.getUniqueId())) {
            return;
        }

        showBossBar(player);
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }

        LimiterConfiguration.Notifications notifications = current.notifications();
        LimiterConfiguration.Message message = notifications.message();
        boolean shouldSend = message.enabled()
                && (serverChange ? message.onServerChange() : message.onJoin());
        if (shouldSend) {
            sendMessages(player, message.lines());
        }
    }

    void onDisconnect(Player player) {
        unmark(player);
    }

    private void broadcastMessages() {
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }
        bypassedPlayers.stream()
                .map(proxyServer::getPlayer)
                .flatMap(java.util.Optional::stream)
                .forEach(player -> sendMessages(player, current.notifications().message().lines()));
    }

    private void broadcastActionBars() {
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled()) {
            return;
        }
        var actionBar = LegacyTextRenderer.render(current.notifications().actionBar().message());
        bypassedPlayers.stream()
                .map(proxyServer::getPlayer)
                .flatMap(java.util.Optional::stream)
                .forEach(player -> player.sendActionBar(actionBar));
    }

    private void showBossBar(Player player) {
        LimiterConfiguration current = configuration;
        if (current == null || !current.enabled() || !current.notifications().bossBar().enabled()) {
            return;
        }
        bossBars.computeIfAbsent(player.getUniqueId(), ignored -> {
            var settings = current.notifications().bossBar();
            BossBar bossBar = BossBar.bossBar(
                    LegacyTextRenderer.render(settings.message()),
                    1.0f,
                    BossBar.Color.valueOf(settings.color().name()),
                    BossBar.Overlay.PROGRESS
            );
            player.showBossBar(bossBar);
            return bossBar;
        });
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    private void hideAllBossBars() {
        bossBars.forEach((playerId, bossBar) -> proxyServer.getPlayer(playerId)
                .ifPresent(player -> player.hideBossBar(bossBar)));
        bossBars.clear();
    }

    private static void sendMessages(Player player, List<String> messages) {
        messages.stream()
                .map(LegacyTextRenderer::render)
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
