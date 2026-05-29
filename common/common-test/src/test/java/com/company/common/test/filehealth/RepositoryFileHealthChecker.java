package com.company.common.test.filehealth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class RepositoryFileHealthChecker {

    private static final long DEFAULT_MAX_SIZE_BYTES = 128L * 1024L;

    private static final Set<String> MANAGED_DIRECTORIES = Set.of(
        "docs",
        "common",
        "bl-center",
        "user-center",
        "tools",
        "scripts",
        "deploy",
        "infrastructure",
        "sql");

    private static final Set<String> ROOT_TEXT_FILES = Set.of(
        ".dockerignore",
        ".editorconfig",
        ".gitattributes",
        ".gitignore",
        ".gitlab-ci.yml",
        "README.md",
        "pom.xml",
        "mvnw",
        "mvnw.cmd");

    private static final Set<String> EXCLUDED_SEGMENTS = Set.of(".git", ".idea", ".m2", ".mvn", "target");

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        ".java",
        ".md",
        ".yml",
        ".yaml",
        ".xml",
        ".json",
        ".properties",
        ".sql",
        ".sh",
        ".ps1",
        ".cmd",
        ".bat",
        ".toml",
        ".alloy",
        ".txt");

    private static final Set<String> TEXT_FILENAMES = Set.of("Dockerfile", "mvnw");

    private static final Map<String, Integer> LINE_LIMITS = createLineLimits();

    private final Path repositoryRoot;
    private final FileHealthExemptionConfig exemptionConfig;

    private RepositoryFileHealthChecker(Path repositoryRoot, FileHealthExemptionConfig exemptionConfig) {
        this.repositoryRoot = repositoryRoot;
        this.exemptionConfig = exemptionConfig;
    }

    static RepositoryFileHealthChecker forRepositoryRoot(Path repositoryRoot) throws IOException {
        Path configPath = repositoryRoot.resolve("docs").resolve("file-health-exemptions.properties");
        return new RepositoryFileHealthChecker(repositoryRoot, FileHealthExemptionConfig.load(configPath));
    }

    static Path locateRepositoryRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            boolean hasRootMarkers = Files.exists(current.resolve("pom.xml"))
                && Files.isDirectory(current.resolve("docs"))
                && Files.isDirectory(current.resolve("common"))
                && Files.isDirectory(current.resolve("user-center"));
            if (hasRootMarkers) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate repository root from " + start);
    }

    List<FileHealthViolation> validateRepository() throws IOException {
        List<FileHealthViolation> violations = new ArrayList<>();
        for (Path file : collectManagedTextFiles()) {
            violations.addAll(validateFile(file));
        }
        return violations.stream()
            .sorted(Comparator.comparing(FileHealthViolation::path).thenComparing(v -> v.rule().name()))
            .toList();
    }

    List<FileHealthViolation> validateFile(Path file) throws IOException {
        Path relativePath = repositoryRoot.relativize(file);
        EnumSet<FileHealthRule> waivedRules = exemptionConfig.waivedRulesFor(relativePath);
        byte[] bytes = Files.readAllBytes(file);
        List<FileHealthViolation> violations = new ArrayList<>();

        if (hasUtf8Bom(bytes)) {
            violations.add(violation(relativePath, FileHealthRule.UTF_8_BOM, "UTF-8 BOM is not allowed"));
        }

        String content = decodeUtf8(bytes, relativePath, violations);
        validateLineEndings(relativePath, bytes, violations);
        validateLineCount(relativePath, content, waivedRules, violations);
        validateFileSize(relativePath, bytes.length, waivedRules, violations);
        return violations;
    }

    String formatViolations(List<FileHealthViolation> violations) {
        return violations.stream()
            .map(violation -> violation.path() + " [" + violation.rule().name() + "] " + violation.message())
            .collect(Collectors.joining(System.lineSeparator()));
    }

    private List<Path> collectManagedTextFiles() throws IOException {
        Set<Path> files = new LinkedHashSet<>();
        for (String rootFile : ROOT_TEXT_FILES) {
            Path candidate = repositoryRoot.resolve(rootFile);
            if (Files.isRegularFile(candidate)) {
                files.add(candidate);
            }
        }
        for (String managedDirectory : MANAGED_DIRECTORIES) {
            Path directory = repositoryRoot.resolve(managedDirectory);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(directory)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> !containsExcludedSegment(repositoryRoot.relativize(path)))
                    .filter(this::isManagedTextFile)
                    .forEach(files::add);
            }
        }
        return files.stream()
            .sorted(Comparator.comparing(path -> relativePathString(repositoryRoot.relativize(path))))
            .toList();
    }

    private boolean isManagedTextFile(Path path) {
        String fileName = path.getFileName().toString();
        return TEXT_FILENAMES.contains(fileName) || TEXT_EXTENSIONS.contains(extensionOf(fileName));
    }

    private boolean containsExcludedSegment(Path relativePath) {
        for (Path segment : relativePath) {
            if (EXCLUDED_SEGMENTS.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private String decodeUtf8(byte[] bytes, Path relativePath, List<FileHealthViolation> violations) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            violations.add(violation(relativePath, FileHealthRule.UTF_8_DECODE, "File cannot be decoded as strict UTF-8"));
            return null;
        }
    }

    private void validateLineEndings(Path relativePath, byte[] bytes, List<FileHealthViolation> violations) {
        boolean crAllowed = isCrLfAllowed(relativePath.getFileName().toString());
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] != '\r') {
                continue;
            }
            if (index + 1 >= bytes.length || bytes[index + 1] != '\n') {
                violations.add(violation(relativePath, FileHealthRule.LINE_ENDING, "Standalone CR line endings are not allowed"));
                return;
            }
            if (!crAllowed) {
                violations.add(violation(
                    relativePath,
                    FileHealthRule.LINE_ENDING,
                    "CRLF line endings are only allowed for .cmd and .bat files"));
                return;
            }
            index++;
        }
    }

    private void validateLineCount(
        Path relativePath,
        String content,
        EnumSet<FileHealthRule> waivedRules,
        List<FileHealthViolation> violations
    ) {
        if (content == null || waivedRules.contains(FileHealthRule.MAX_LINES)) {
            return;
        }
        Integer lineLimit = LINE_LIMITS.get(extensionOf(relativePath.getFileName().toString()));
        if (lineLimit == null) {
            return;
        }
        long actualLineCount = countLines(content);
        if (actualLineCount > lineLimit) {
            violations.add(violation(
                relativePath,
                FileHealthRule.MAX_LINES,
                "Line count " + actualLineCount + " exceeds limit " + lineLimit));
        }
    }

    private void validateFileSize(
        Path relativePath,
        long sizeBytes,
        EnumSet<FileHealthRule> waivedRules,
        List<FileHealthViolation> violations
    ) {
        if (!waivedRules.contains(FileHealthRule.MAX_SIZE) && sizeBytes > DEFAULT_MAX_SIZE_BYTES) {
            violations.add(violation(
                relativePath,
                FileHealthRule.MAX_SIZE,
                "File size " + sizeBytes + " exceeds limit " + DEFAULT_MAX_SIZE_BYTES + " bytes"));
        }
    }

    private static FileHealthViolation violation(Path relativePath, FileHealthRule rule, String message) {
        return new FileHealthViolation(relativePathString(relativePath), rule, message);
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xEF
            && (bytes[1] & 0xFF) == 0xBB
            && (bytes[2] & 0xFF) == 0xBF;
    }

    private static boolean isCrLfAllowed(String fileName) {
        return fileName.endsWith(".cmd") || fileName.endsWith(".bat");
    }

    private static long countLines(String content) {
        if (content.isEmpty()) {
            return 0L;
        }
        long count = content.chars().filter(ch -> ch == '\n').count();
        return content.charAt(content.length() - 1) == '\n' ? count : count + 1;
    }

    private static String extensionOf(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fileName.substring(lastDotIndex) : "";
    }

    private static String relativePathString(Path relativePath) {
        return relativePath.toString().replace('\\', '/');
    }

    private static Map<String, Integer> createLineLimits() {
        Map<String, Integer> lineLimits = new LinkedHashMap<>();
        lineLimits.put(".java", 1000);
        lineLimits.put(".md", 300);
        lineLimits.put(".yml", 200);
        lineLimits.put(".yaml", 200);
        lineLimits.put(".xml", 300);
        lineLimits.put(".json", 200);
        lineLimits.put(".properties", 200);
        lineLimits.put(".sh", 500);
        lineLimits.put(".ps1", 500);
        lineLimits.put(".cmd", 200);
        lineLimits.put(".bat", 200);
        return lineLimits;
    }
}
