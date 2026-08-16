package com.enderdash.agent.viaversionlimiter.config;

import com.enderdash.agent.viaversionlimiter.policy.VersionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiterConfigurationLoaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAndLoadsSafeDefaultConfiguration() throws IOException {
        LimiterConfigurationLoader loader = new LimiterConfigurationLoader(temporaryDirectory);

        LimiterConfiguration configuration = loader.load();

        assertTrue(Files.isRegularFile(loader.configPath()));
        assertFalse(configuration.enabled());
        assertEquals(VersionPolicy.Mode.ALLOWLIST, configuration.policy().versions().mode());
        assertEquals(1, configuration.policy().versions().protocols().size());
    }

    @Test
    void rejectsLegacyConfigurationInsteadOfGuessingItsMeaning() throws IOException {
        Files.writeString(temporaryDirectory.resolve("config.yml"), "enabled: true\n");
        LimiterConfigurationLoader loader = new LimiterConfigurationLoader(temporaryDirectory);

        assertThrows(IOException.class, loader::load);
    }

    @Test
    void rejectsAnEmptyVersionPolicy() throws IOException {
        Files.writeString(temporaryDirectory.resolve("config.yml"), """
                config-version: 2
                enabled: true
                policy:
                  mode: ALLOWLIST
                  versions: []
                """);
        LimiterConfigurationLoader loader = new LimiterConfigurationLoader(temporaryDirectory);

        assertThrows(IOException.class, loader::load);
    }
}
