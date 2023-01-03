package me.linoxgh.viaversionlimitervelocity;

import com.google.common.reflect.TypeToken;
import me.linoxgh.viaversionlimiter.shared.Config;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ConfigVelocity extends Config {
    private final Logger logger;

    protected ConfigVelocity(Logger logger, Path dataFolder) {
        super(dataFolder);

        this.logger = logger;
    }

    @Override public void loadConfig() {
        long start = System.currentTimeMillis();
        try {
            ensureConfigFile();

            YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                    .setPath(dataFolder.resolve("config.yml"))
                    .build();

            ConfigurationNode root = loader.load();

            if (!root.getNode("enable").isVirtual()) enable = root.getNode("enable").getBoolean();
            if (!root.getNode("whitelist").isVirtual()) whitelist = root.getNode("whitelist").getBoolean();
            if (!root.getNode("versions").isVirtual())
                versions = new HashSet<>(root.getNode("versions")
                        .getList(TypeToken.of(Integer.class)));
            if (!root.getNode("kick-message").isVirtual())
                kickMessages = root.getNode("kick-message")
                        .getList(TypeToken.of(String.class))
                        .stream().map(s -> s.replace('&', '§'))
                        .collect(Collectors.toList());
            if (!root.getNode("allowed-domain").isVirtual()) allowedDomain = root.getNode("allowed-domain").getString();

            if (!root.getNode("enable-message").isVirtual()) enableMessage = root.getNode("enable-message").getBoolean();
            if (!root.getNode("reverse").isVirtual()) reverse = root.getNode("reverse").getBoolean();
            if (!root.getNode("message").isVirtual())
                message = root.getNode("message")
                        .getList(TypeToken.of(String.class))
                        .stream().map(s -> s.replace('&', '§'))
                        .collect(Collectors.toList());
            if (!root.getNode("delay").isVirtual()) delay = root.getNode("delay").getLong();
            if (!root.getNode("on-join").isVirtual()) onJoin = root.getNode("on-join").getBoolean();
            if (!root.getNode("on-server-change").isVirtual()) onServerChange = root.getNode("on-server-change").getBoolean();

            debug("Loaded plugin configurations in " + (System.currentTimeMillis() - start) / 1000 + " seconds.");
        } catch (IOException | ObjectMappingException x) {
            error("Could not load plugin configurations.", x);
        }
    }

    @Override public void debug(String msg) {
        logger.debug(msg);
    }

    @Override public void error(String msg, Throwable x) {
        logger.error(msg, x);
    }
}
