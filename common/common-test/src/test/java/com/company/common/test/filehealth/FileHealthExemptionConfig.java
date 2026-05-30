package com.company.common.test.filehealth;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

final class FileHealthExemptionConfig {

    private final List<Exemption> exemptions;

    private FileHealthExemptionConfig(List<Exemption> exemptions) {
        this.exemptions = List.copyOf(exemptions);
    }

    static FileHealthExemptionConfig load(Path configPath) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        List<Exemption> exemptions = new ArrayList<>();
        for (int index = 1; ; index++) {
            String prefix = "exemption." + index + ".";
            String glob = properties.getProperty(prefix + "glob");
            if (glob == null) {
                break;
            }
            String reason = properties.getProperty(prefix + "reason", "").trim();
            EnumSet<FileHealthRule> waivedRules = parseWaivedRules(properties.getProperty(prefix + "waive", ""));
            exemptions.add(new Exemption(index, glob, Pattern.compile(globToRegex(glob)), waivedRules, reason));
        }
        return new FileHealthExemptionConfig(exemptions);
    }

    int exemptionCount() {
        return exemptions.size();
    }

    List<Exemption> exemptions() {
        return exemptions;
    }

    EnumSet<FileHealthRule> waivedRulesFor(Path relativePath) {
        EnumSet<FileHealthRule> waivedRules = EnumSet.noneOf(FileHealthRule.class);
        String normalizedPath = relativePath.toString().replace('\\', '/');
        for (Exemption exemption : exemptions) {
            if (exemption.pattern().matcher(normalizedPath).matches()) {
                waivedRules.addAll(exemption.waivedRules());
            }
        }
        return waivedRules;
    }

    private static EnumSet<FileHealthRule> parseWaivedRules(String value) {
        EnumSet<FileHealthRule> waivedRules = EnumSet.noneOf(FileHealthRule.class);
        if (value == null || value.isBlank()) {
            return waivedRules;
        }
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            FileHealthRule rule = FileHealthRule.valueOf(trimmed);
            if (rule != FileHealthRule.MAX_LINES && rule != FileHealthRule.MAX_SIZE) {
                throw new IllegalArgumentException("Only MAX_LINES and MAX_SIZE can be waived, but got " + rule);
            }
            waivedRules.add(rule);
        }
        return waivedRules;
    }

    private static String globToRegex(String glob) {
        StringBuilder builder = new StringBuilder("^");
        for (int index = 0; index < glob.length(); index++) {
            char current = glob.charAt(index);
            if (current == '*') {
                boolean doubleStar = index + 1 < glob.length() && glob.charAt(index + 1) == '*';
                builder.append(doubleStar ? ".*" : "[^/]*");
                if (doubleStar) {
                    index++;
                }
                continue;
            }
            if (current == '?') {
                builder.append("[^/]");
                continue;
            }
            if (current == '/' || current == '\\') {
                builder.append('/');
                continue;
            }
            if ("\\.[]{}()+-^$|".indexOf(current) >= 0) {
                builder.append('\\');
            }
            builder.append(current);
        }
        return builder.append('$').toString();
    }

    record Exemption(int index, String glob, Pattern pattern, EnumSet<FileHealthRule> waivedRules, String reason) {

        Exemption {
            if (index <= 0) {
                throw new IllegalArgumentException("index must be positive");
            }
            Objects.requireNonNull(glob, "glob");
            Objects.requireNonNull(pattern, "pattern");
            Objects.requireNonNull(waivedRules, "waivedRules");
            Objects.requireNonNull(reason, "reason");
        }
    }
}
