package me.linoxgh.viaversionlimiter.shared;

import java.io.IOException;
import java.io.InputStream;
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
    protected Set<Integer> versions = new HashSet<>(Arrays.asList(759, 760));
    protected List<String> kickMessages = Arrays.asList("", "§cPlease join with 1.19.2,", "§calternatively use the domain nosupport.test.org but you won''t receive help!");
    protected String allowedDomain = "nosupport.test.org";

    protected boolean enableMessage = true;
    protected List<String> message = List.of("", "");
    protected boolean onJoin = true;
    protected boolean onServerChange = false;
    protected boolean enableBroadcast = true;
    protected long broadcastDelay = 600;

    protected boolean enableBossBar = true;
    protected String bossBarMessage = "&cUNSUPPORTED VERSION! USE 1.19.3";
    protected String bossBarColor = "RED";
    protected boolean enableActionBar = true;
    protected String actionBarMessage = "&cUNSUPPORTED VERSION! USE 1.19.3";

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
    public List<String> getMessage() {
        return message;
    }
    public boolean isOnJoin() {
        return onJoin;
    }
    public boolean isOnServerChange() {
        return onServerChange;
    }
    public boolean isEnableBroadcast() {
        return enableBroadcast;
    }
    public long getBroadcastDelay() {
        return broadcastDelay;
    }

    public boolean isEnableBossBar() {
        return enableBossBar;
    }
    public String getBossBarMessage() {
        return bossBarMessage;
    }
    public String getBossBarColor() {
        return bossBarColor;
    }
    public boolean isEnableActionBar() {
        return enableActionBar;
    }
    public String getActionBarMessage() {
        return actionBarMessage;
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
