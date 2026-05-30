package com.company.common.test.filehealth;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class FileHealthBaselineConfig {

    private final int maxExemptions;

    private FileHealthBaselineConfig(int maxExemptions) {
        this.maxExemptions = maxExemptions;
    }

    static FileHealthBaselineConfig load(Path configPath) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        String maxExemptionsText = properties.getProperty("file.health.max-exemptions");
        if (maxExemptionsText == null || maxExemptionsText.isBlank()) {
            throw new IllegalArgumentException("Missing required property file.health.max-exemptions in " + configPath);
        }

        int maxExemptions = Integer.parseInt(maxExemptionsText.trim());
        if (maxExemptions < 0) {
            throw new IllegalArgumentException("file.health.max-exemptions must be >= 0 in " + configPath);
        }
        return new FileHealthBaselineConfig(maxExemptions);
    }

    int maxExemptions() {
        return maxExemptions;
    }
}
