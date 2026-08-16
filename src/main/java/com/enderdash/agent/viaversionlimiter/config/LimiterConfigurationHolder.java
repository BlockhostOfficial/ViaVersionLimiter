package com.enderdash.agent.viaversionlimiter.config;

import net.blockhost.commons.config.ConfigurationHolder;
import net.blockhost.commons.config.migration.ConfigMigrator;
import net.blockhost.commons.config.migration.Migration;
import net.blockhost.commons.config.migration.MigrationContext;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LimiterConfigurationHolder extends ConfigurationHolder<LimiterConfiguration> {
    private static final ConfigMigrator MIGRATOR = ConfigMigrator.builder()
            .envPrefix("CONFIG_VIAVERSIONLIMITER")
            .createBackups(true)
            .useTimestampedBackups(true)
            .register(Migration.of(1, "Import the legacy ViaVersionLimiter configuration", LimiterConfigurationHolder::migrateLegacy))
            .register(Migration.of(2, "Adopt the 6b6t Commons configuration version field", context -> {
                context.data().remove("config-version");
            }))
            .build();

    public LimiterConfigurationHolder(Path configPath) {
        super(
                () -> load(configPath),
                configPath,
                LimiterConfiguration.class
        );
    }

    private static LimiterConfiguration load(Path configPath) {
        return MIGRATOR.migrateAndLoad(
                configPath,
                LimiterConfiguration.class,
                LimiterConfiguration.CURRENT_VERSION
        ).validated();
    }

    private static void migrateLegacy(MigrationContext context) {
        Map<String, Object> data = context.data();
        Object configVersion = data.remove("config-version");
        if (configVersion instanceof Number number && number.intValue() == 2) {
            return;
        }

        Map<String, Object> policy = new LinkedHashMap<>();
        boolean allowlist = booleanValue(data.remove("whitelist"), true);
        policy.put("mode", allowlist ? "ALLOWLIST" : "BLOCKLIST");
        move(data, "versions", policy, "versions");
        move(data, "allowed-domain", policy, "bypass-domain");
        data.put("policy", policy);

        Object legacyEnable = data.remove("enable");
        if (legacyEnable != null && !data.containsKey("enabled")) {
            data.put("enabled", legacyEnable);
        }

        Map<String, Object> message = new LinkedHashMap<>();
        move(data, "enable-message", message, "enabled");
        move(data, "on-join", message, "on-join");
        move(data, "on-server-change", message, "on-server-change");
        move(data, "message", message, "lines");

        Map<String, Object> broadcast = new LinkedHashMap<>();
        move(data, "broadcast", broadcast, "enabled");
        move(data, "broadcast-delay", broadcast, "interval-seconds");

        Map<String, Object> bossbar = new LinkedHashMap<>();
        move(data, "bossbar", bossbar, "enabled");
        move(data, "bossbar-message", bossbar, "message");
        move(data, "bossbar-color", bossbar, "color");

        Map<String, Object> actionbar = new LinkedHashMap<>();
        move(data, "actionbar", actionbar, "enabled");
        move(data, "actionbar-message", actionbar, "message");

        Map<String, Object> notifications = new LinkedHashMap<>();
        notifications.put("message", message);
        notifications.put("broadcast", broadcast);
        notifications.put("bossbar", bossbar);
        notifications.put("actionbar", actionbar);
        data.put("notifications", notifications);
    }

    private static void move(Map<String, Object> source, String sourceKey, Map<String, Object> target, String targetKey) {
        Object value = source.remove(sourceKey);
        if (value != null) {
            target.put(targetKey, value);
        }
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean booleanValue ? booleanValue : fallback;
    }
}
