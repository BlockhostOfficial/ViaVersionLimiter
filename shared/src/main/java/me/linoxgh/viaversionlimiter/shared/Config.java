package me.linoxgh.viaversionlimiter.shared;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Config {
    protected final Path dataFolder;

    protected Config(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    protected boolean whitelist = true;
    protected Set<Integer> versions = new HashSet<>(Arrays.asList(759, 750));
    protected List<String> kickMessages = Arrays.asList("", "§cPlease join with 1.19.2,", "§calternatively use the domain nosupport.test.org but you won''t receive help!");
    protected String allowedDomain = "nosupport.test.org";

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

    public void ensureConfigFile() throws IOException {
        if (!dataFolder.resolve("config.yml").toFile().exists()) {
            dataFolder.toFile().mkdirs();

            try {
                Files.copy(
                        Path.of(getClass().getResource("config.yml").toURI()),
                        dataFolder.resolve("config.yml"),
                        StandardCopyOption.REPLACE_EXISTING);

            } catch (URISyntaxException x) {
                error("Could not save default plugin configurations.", x);
            }
        }
    }

    public abstract void loadConfig();

    public abstract void debug(String msg);
    public abstract void error(String msg, Throwable x);
}
