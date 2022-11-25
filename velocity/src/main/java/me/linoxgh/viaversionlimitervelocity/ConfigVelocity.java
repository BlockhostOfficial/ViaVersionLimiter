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
