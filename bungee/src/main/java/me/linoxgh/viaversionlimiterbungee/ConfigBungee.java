package me.linoxgh.viaversionlimiterbungee;

import me.linoxgh.viaversionlimiter.shared.Config;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigBungee extends Config {
    private final Logger logger;

    protected ConfigBungee(Logger logger, Path dataFolder) {
        super(dataFolder);

        this.logger = logger;
    }

    @Override public void loadConfig() {
        long start = System.currentTimeMillis();
        try {
            ensureConfigFile();

            Configuration root = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(dataFolder.resolve("config.yml").toFile());

            if (root.contains("enable")) enable = root.getBoolean("enable");
            if (root.contains("whitelist")) whitelist = root.getBoolean("whitelist");
            if (root.contains("versions"))
                versions = new HashSet<>(root.getIntList("versions"));
            if (root.contains("kick-message"))
                kickMessages = root.getStringList("kick-message")
                        .stream().map(s -> s.replace('&', '§'))
                        .collect(Collectors.toList());
            if (root.contains("allowed-domain")) allowedDomain = root.getString("allowed-domain");

            if (root.contains("enable-message")) enableMessage = root.getBoolean("enable-message");
            if (root.contains("message"))
                message = root.getStringList("message")
                        .stream().map(s -> s.replace('&', '§'))
                        .collect(Collectors.toList());
            if (root.contains("on-join")) onJoin = root.getBoolean("on-join");
            if (root.contains("on-server-change")) onServerChange = root.getBoolean("on-server-change");

            if (root.contains("broadcast")) enableBroadcast = root.getBoolean("broadcast");
            if (root.contains("broadcast-delay")) broadcastDelay = root.getLong("broadcast-delay");

            if (root.contains("bossbar")) enableBossBar = root.getBoolean("bossbar");
            if (root.contains("bossbar-message")) bossBarMessage = root.getString("bossbar-message");

            if (root.contains("actionbar")) enableActionBar = root.getBoolean("actionbar");
            if (root.contains("actionbar-message")) actionBarMessage = root.getString("actionbar-message");

            debug("Loaded plugin configurations in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
        } catch (IOException x) {
            error("Could not load plugin configurations.", x);
        }
    }

    @Override public void debug(String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override public void error(String msg, Throwable x) {
        logger.log(Level.SEVERE, msg, x);
    }
}
