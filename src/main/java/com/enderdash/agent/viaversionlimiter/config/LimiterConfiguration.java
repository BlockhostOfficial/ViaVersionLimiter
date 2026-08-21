package com.enderdash.agent.viaversionlimiter.config;

import com.enderdash.agent.viaversionlimiter.policy.ConnectionPolicy;
import com.enderdash.agent.viaversionlimiter.policy.HostMatcher;
import com.enderdash.agent.viaversionlimiter.policy.VersionPolicy;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import net.blockhost.commons.config.VersionAwareConfiguration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Configuration
public final class LimiterConfiguration extends VersionAwareConfiguration {
    static final int CURRENT_VERSION = 2;

    @Comment("Keep enforcement disabled until the policy and bypass DNS are ready.")
    private boolean enabled;

    @Comment("Minecraft protocol policy and the optional bypass hostname.")
    private Policy policy = new Policy();

    @Comment("Message sent when an unsupported client is rejected during login.")
    private List<String> kickMessage = List.of(
            "",
            "&cUnsupported Minecraft version.",
            "&ePlease connect with a supported version.",
            ""
    );

    @Comment("Warnings sent to unsupported clients admitted by the bypass policy.")
    private Notifications notifications = new Notifications();

    public LimiterConfiguration() {
        super(CURRENT_VERSION);
    }

    public boolean enabled() {
        return enabled;
    }

    public ConnectionPolicy connectionPolicy() {
        Policy current = Objects.requireNonNull(policy, "policy is required");
        VersionPolicy versionPolicy = new VersionPolicy(
                Objects.requireNonNull(current.mode, "policy.mode is required"),
                Set.copyOf(requireValues(current.versions, "policy.versions"))
        );
        String bypassDomain = Objects.requireNonNull(current.bypassDomain, "policy.bypass-domain is required").strip();
        HostMatcher hostMatcher = switch (bypassDomain) {
            case "" -> HostMatcher.disabled();
            case "*" -> HostMatcher.any();
            default -> HostMatcher.exact(bypassDomain);
        };
        return new ConnectionPolicy(versionPolicy, hostMatcher);
    }

    public List<String> kickMessage() {
        return requireValues(kickMessage, "kick-message");
    }

    public Notifications notifications() {
        return Objects.requireNonNull(notifications, "notifications is required").validated();
    }

    public LimiterConfiguration validated() {
        connectionPolicy();
        kickMessage();
        notifications();
        return this;
    }

    private static <T> List<T> requireValues(List<T> values, String path) {
        List<T> copy = List.copyOf(Objects.requireNonNull(values, path + " is required"));
        if (copy.isEmpty()) {
            throw new IllegalArgumentException(path + " must contain at least one value");
        }
        return copy;
    }

    @Configuration
    public static final class Policy {
        @Comment("ALLOWLIST permits only listed protocols. BLOCKLIST rejects only listed protocols.")
        private VersionPolicy.Mode mode = VersionPolicy.Mode.ALLOWLIST;

        @Comment("Minecraft protocol IDs interpreted according to mode.")
        private List<Integer> versions = List.of(769);

        @Comment("Hostname for unsupported clients. Use '*' for all hosts or leave empty to disable bypass access.")
        private String bypassDomain = "nosupport.example.org";
    }

    @Configuration
    public static final class Notifications {
        private Message message = new Message();
        private Periodic broadcast = new Periodic();
        private BossBarSettings bossbar = new BossBarSettings();
        private ActionBarSettings actionbar = new ActionBarSettings();

        public Message message() {
            return Objects.requireNonNull(message, "notifications.message is required").validated();
        }

        public Periodic broadcast() {
            return Objects.requireNonNull(broadcast, "notifications.broadcast is required").validated();
        }

        public BossBarSettings bossBar() {
            return Objects.requireNonNull(bossbar, "notifications.bossbar is required").validated();
        }

        public ActionBarSettings actionBar() {
            return Objects.requireNonNull(actionbar, "notifications.actionbar is required").validated();
        }

        private Notifications validated() {
            message();
            broadcast();
            bossBar();
            actionBar();
            return this;
        }
    }

    @Configuration
    public static final class Message {
        private boolean enabled = true;
        private boolean onJoin = true;
        private boolean onServerChange;
        private List<String> lines = List.of(
                "&cYou are using an unsupported Minecraft version.",
                "&eSupport is not available for this client version."
        );

        public boolean enabled() {
            return enabled;
        }

        public boolean onJoin() {
            return onJoin;
        }

        public boolean onServerChange() {
            return onServerChange;
        }

        public List<String> lines() {
            return requireValues(lines, "notifications.message.lines");
        }

        private Message validated() {
            lines();
            return this;
        }
    }

    @Configuration
    public static final class Periodic {
        private boolean enabled = true;
        private long intervalSeconds = 600;

        public boolean enabled() {
            return enabled;
        }

        public Duration interval() {
            return Duration.ofSeconds(intervalSeconds);
        }

        private Periodic validated() {
            requirePositive(interval(), "notifications.broadcast.interval-seconds");
            return this;
        }
    }

    @Configuration
    public static final class BossBarSettings {
        private boolean enabled = true;
        private String message = "&cUNSUPPORTED MINECRAFT VERSION";
        private BossBarColor color = BossBarColor.RED;

        public boolean enabled() {
            return enabled;
        }

        public String message() {
            return requireText(message, "notifications.bossbar.message");
        }

        public BossBarColor color() {
            return Objects.requireNonNull(color, "notifications.bossbar.color is required");
        }

        private BossBarSettings validated() {
            message();
            color();
            return this;
        }
    }

    @Configuration
    public static final class ActionBarSettings {
        private boolean enabled = true;
        private String message = "&ePlease switch to a supported Minecraft version.";
        private long intervalSeconds = 2;

        public boolean enabled() {
            return enabled;
        }

        public String message() {
            return requireText(message, "notifications.actionbar.message");
        }

        public Duration interval() {
            return Duration.ofSeconds(intervalSeconds);
        }

        private ActionBarSettings validated() {
            message();
            requirePositive(interval(), "notifications.actionbar.interval-seconds");
            return this;
        }
    }

    public enum BossBarColor {
        BLUE,
        GREEN,
        PINK,
        PURPLE,
        RED,
        WHITE,
        YELLOW,
    }

    private static String requireText(String value, String path) {
        String text = Objects.requireNonNull(value, path + " is required");
        if (text.isBlank()) {
            throw new IllegalArgumentException(path + " cannot be blank");
        }
        return text;
    }

    private static void requirePositive(Duration duration, String path) {
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(path + " must be greater than zero");
        }
    }
}
