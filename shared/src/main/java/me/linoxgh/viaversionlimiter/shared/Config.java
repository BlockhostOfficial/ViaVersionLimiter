package me.linoxgh.viaversionlimiter.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public abstract class Config {
    protected final Path dataFolder;

    protected Config(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    protected boolean enable = false;
    protected boolean whitelist = true;
    protected Set<Integer> versions = new HashSet<>(Arrays.asList(759, 750));
    protected List<String> kickMessages = Arrays.asList("", "§cPlease join with 1.19.2,", "§calternatively use the domain nosupport.test.org but you won''t receive help!");
    protected String allowedDomain = "nosupport.test.org";

    protected boolean enableMessage = true;
    protected boolean reverse = false;
    protected List<String> message = List.of("", "");
    protected long delay = 2000;
    protected boolean onJoin = true;
    protected boolean onServerChange = false;

    public boolean isEnabled() {
        return enable;
    }
    public boolean isWhitelist() {
        return whitelist;
    }
    public Set<Integer> getVersions() {
        return versions;
    }
    public List<String> getKickMessages() {
        return kickMessages;
    }
    public String getAllowedDomain() {
        return allowedDomain;
    }

    public boolean isEnableMessage() {
        return enableMessage;
    }
    public boolean isReverse() {
        return reverse;
    }
    public List<String> getMessage() {
        return message;
    }
    public long getDelay() {
        return delay;
    }
    public boolean isOnJoin() {
        return onJoin;
    }
    public boolean isOnServerChange() {
        return onServerChange;
    }

    public void ensureConfigFile() throws IOException {
        if (!dataFolder.resolve("config.yml").toFile().exists()) {
            dataFolder.toFile().mkdirs();

            InputStream is = getClass().getResourceAsStream("/config.yml");
            Files.copy(is, dataFolder.resolve("config.yml"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public abstract void loadConfig();

    public abstract void debug(String msg);
    public abstract void error(String msg, Throwable x);
}
