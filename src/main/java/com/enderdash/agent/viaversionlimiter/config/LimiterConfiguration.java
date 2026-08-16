package com.enderdash.agent.viaversionlimiter.config;

import com.enderdash.agent.viaversionlimiter.policy.ConnectionPolicy;
import net.kyori.adventure.bossbar.BossBar;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record LimiterConfiguration(
        boolean enabled,
        ConnectionPolicy policy,
        List<String> kickMessage,
        Notifications notifications
) {
    public LimiterConfiguration {
        Objects.requireNonNull(policy, "policy");
        kickMessage = requireLines(kickMessage, "kick-message");
        Objects.requireNonNull(notifications, "notifications");
    }

    private static List<String> requireLines(List<String> lines, String path) {
        List<String> copy = List.copyOf(lines);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(path + " must contain at least one line");
        }
        return copy;
    }

    public record Notifications(
            boolean messageEnabled,
            boolean onJoin,
            boolean onServerChange,
            List<String> message,
            Periodic broadcast,
            BossBarSettings bossBar,
            ActionBarSettings actionBar
    ) {
        public Notifications {
            message = requireLines(message, "notifications.message.lines");
            Objects.requireNonNull(broadcast, "broadcast");
            Objects.requireNonNull(bossBar, "bossBar");
            Objects.requireNonNull(actionBar, "actionBar");
        }
    }

    public record Periodic(boolean enabled, Duration interval) {
        public Periodic {
            requirePositive(interval, "notifications.broadcast.interval-seconds");
        }
    }

    public record BossBarSettings(boolean enabled, String message, BossBar.Color color) {
        public BossBarSettings {
            Objects.requireNonNull(message, "message");
            Objects.requireNonNull(color, "color");
        }
    }

    public record ActionBarSettings(boolean enabled, String message, Duration interval) {
        public ActionBarSettings {
            Objects.requireNonNull(message, "message");
            requirePositive(interval, "notifications.actionbar.interval-seconds");
        }
    }

    private static void requirePositive(Duration duration, String path) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(path + " must be greater than zero");
        }
    }
}
