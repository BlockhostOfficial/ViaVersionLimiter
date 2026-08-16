package com.enderdash.agent.viaversionlimiter.config;

import com.enderdash.agent.viaversionlimiter.policy.ConnectionDecision;
import com.enderdash.agent.viaversionlimiter.policy.VersionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiterConfigurationHolderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndLoadsSafeDefaultConfiguration() {
        Path configPath = temporaryDirectory.resolve("config.yml");

        LimiterConfiguration configuration = new LimiterConfigurationHolder(configPath).get();

        assertTrue(Files.isRegularFile(configPath));
        assertFalse(configuration.enabled());
        assertEquals(VersionPolicy.Mode.ALLOWLIST, configuration.connectionPolicy().versions().mode());
        assertEquals(1, configuration.connectionPolicy().versions().protocols().size());
    }

    @Test
    void migratesTheLegacyFlatConfiguration() throws IOException {
        Path configPath = temporaryDirectory.resolve("config.yml");
        Files.writeString(configPath, """
                enable: true
                whitelist: true
                versions: [340]
                allowed-domain: old.example.org
                kick-message: ['unsupported']
                enable-message: true
                message: ['warning']
                on-join: true
                on-server-change: false
                broadcast: false
                broadcast-delay: 60
                bossbar: false
                bossbar-message: warning
                bossbar-color: RED
                actionbar: false
                actionbar-message: warning
                """);

        LimiterConfiguration configuration = new LimiterConfigurationHolder(configPath).get();

        assertTrue(configuration.enabled());
        assertEquals(
                ConnectionDecision.BYPASSED,
                configuration.connectionPolicy().evaluate(578, Optional.of("old.example.org"))
        );
        assertEquals(2, configuration.version());
    }

    @Test
    void importsThePreviousV2ConfigurationVersionKey() throws IOException {
        Path configPath = temporaryDirectory.resolve("config.yml");
        Files.writeString(configPath, """
                config-version: 2
                enabled: true
                policy:
                  mode: BLOCKLIST
                  versions: [578]
                  bypass-domain: bypass.example.org
                """);

        LimiterConfiguration configuration = new LimiterConfigurationHolder(configPath).get();

        assertEquals(VersionPolicy.Mode.BLOCKLIST, configuration.connectionPolicy().versions().mode());
        assertEquals(2, configuration.version());
    }

    @Test
    void rejectsAnEmptyVersionPolicy() throws IOException {
        Path configPath = temporaryDirectory.resolve("config.yml");
        Files.writeString(configPath, """
                version: 2
                enabled: true
                policy:
                  mode: ALLOWLIST
                  versions: []
                  bypass-domain: ''
                """);

        assertThrows(RuntimeException.class, () -> new LimiterConfigurationHolder(configPath));
    }
}
