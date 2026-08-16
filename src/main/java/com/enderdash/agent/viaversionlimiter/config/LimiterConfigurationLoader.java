package com.enderdash.agent.viaversionlimiter.config;

import com.enderdash.agent.viaversionlimiter.policy.ConnectionPolicy;
import com.enderdash.agent.viaversionlimiter.policy.HostMatcher;
import com.enderdash.agent.viaversionlimiter.policy.VersionPolicy;
import net.kyori.adventure.bossbar.BossBar;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LimiterConfigurationLoader {
    private static final int CONFIG_VERSION = 2;
    private static final String DEFAULT_CONFIG_RESOURCE = "/config.yml";

    private final Path configPath;

    public LimiterConfigurationLoader(Path dataDirectory) {
        configPath = dataDirectory.resolve("config.yml");
    }

    public LimiterConfiguration load() throws IOException {
        createDefaultConfigIfMissing();
        try {
            ConfigurationNode root = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build()
                    .load();
            return map(root);
        } catch (ConfigurateException | IllegalArgumentException exception) {
            throw new IOException("Invalid configuration at " + configPath + ": " + exception.getMessage(), exception);
        }
    }

    public Path configPath() {
        return configPath;
    }

    private LimiterConfiguration map(ConfigurationNode root) throws ConfigurateException {
        int configVersion = root.node("config-version").getInt(-1);
        if (configVersion != CONFIG_VERSION) {
            throw new IllegalArgumentException(
                    "config-version must be " + CONFIG_VERSION + "; replace the legacy v1 configuration"
            );
        }

        ConfigurationNode policyNode = root.node("policy");
        VersionPolicy versionPolicy = new VersionPolicy(
                VersionPolicy.Mode.parse(requireString(policyNode.node("mode"), "policy.mode")),
                Set.copyOf(requireList(policyNode.node("versions"), Integer.class, "policy.versions"))
        );
        String bypassDomain = policyNode.node("bypass-domain").getString("").strip();
        HostMatcher hostMatcher = bypassDomain.isEmpty()
                ? HostMatcher.disabled()
                : HostMatcher.exact(bypassDomain);

        ConfigurationNode notificationsNode = root.node("notifications");
        ConfigurationNode messageNode = notificationsNode.node("message");
        ConfigurationNode broadcastNode = notificationsNode.node("broadcast");
        ConfigurationNode bossBarNode = notificationsNode.node("bossbar");
        ConfigurationNode actionBarNode = notificationsNode.node("actionbar");

        LimiterConfiguration.Notifications notifications = new LimiterConfiguration.Notifications(
                messageNode.node("enabled").getBoolean(true),
                messageNode.node("on-join").getBoolean(true),
                messageNode.node("on-server-change").getBoolean(false),
                requireList(messageNode.node("lines"), String.class, "notifications.message.lines"),
                new LimiterConfiguration.Periodic(
                        broadcastNode.node("enabled").getBoolean(false),
                        seconds(broadcastNode.node("interval-seconds"), 600)
                ),
                new LimiterConfiguration.BossBarSettings(
                        bossBarNode.node("enabled").getBoolean(false),
                        requireString(bossBarNode.node("message"), "notifications.bossbar.message"),
                        parseBossBarColor(bossBarNode.node("color").getString("RED"))
                ),
                new LimiterConfiguration.ActionBarSettings(
                        actionBarNode.node("enabled").getBoolean(false),
                        requireString(actionBarNode.node("message"), "notifications.actionbar.message"),
                        seconds(actionBarNode.node("interval-seconds"), 2)
                )
        );

        return new LimiterConfiguration(
                root.node("enabled").getBoolean(false),
                new ConnectionPolicy(versionPolicy, hostMatcher),
                requireList(root.node("kick-message"), String.class, "kick-message"),
                notifications
        );
    }

    private void createDefaultConfigIfMissing() throws IOException {
        if (Files.exists(configPath)) {
            return;
        }
        Files.createDirectories(configPath.getParent());
        try (InputStream input = LimiterConfigurationLoader.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing bundled config.yml");
            }
            Files.copy(input, configPath);
        }
    }

    private static Duration seconds(ConfigurationNode node, long fallback) {
        return Duration.ofSeconds(node.getLong(fallback));
    }

    private static BossBar.Color parseBossBarColor(String value) {
        try {
            return BossBar.Color.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "notifications.bossbar.color is not a supported boss bar color",
                    exception
            );
        }
    }

    private static String requireString(ConfigurationNode node, String path) {
        String value = node.getString();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(path + " is required");
        }
        return value;
    }

    private static <T> List<T> requireList(ConfigurationNode node, Class<T> type, String path)
            throws ConfigurateException {
        List<T> values = node.getList(type, List.of());
        if (values.isEmpty()) {
            throw new IllegalArgumentException(path + " must contain at least one value");
        }
        return values;
    }
}
