package com.company.common.test.filehealth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RepositoryFileHealthCheckerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRejectNonUtf8TextFile() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Files.createDirectories(repositoryRoot.resolve("docs"));
        Files.write(repositoryRoot.resolve("docs").resolve("bad.md"), new byte[]{(byte) 0xC3, 0x28});

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.UTF_8_DECODE);
    }

    @Test
    void shouldRejectUtf8Bom() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Files.createDirectories(repositoryRoot.resolve("docs"));
        Files.write(repositoryRoot.resolve("docs").resolve("bom.md"), new byte[]{
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'o', 'k', '\n'
        });

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.UTF_8_BOM);
    }

    @Test
    void shouldRejectCrLfForJavaFiles() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Path javaFile = repositoryRoot.resolve("common").resolve("Sample.java");
        Files.createDirectories(javaFile.getParent());
        Files.writeString(javaFile, "class Sample {\r\n}\r\n", StandardCharsets.UTF_8);

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.LINE_ENDING);
    }

    @Test
    void shouldRejectFilesOverDefaultLineLimit() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Path markdownFile = repositoryRoot.resolve("docs").resolve("too-long.md");
        Files.createDirectories(markdownFile.getParent());
        Files.writeString(markdownFile, repeatedLines(301), StandardCharsets.UTF_8);

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.MAX_LINES);
    }

    @Test
    void shouldAllowOversizedExemptSqlButStillRejectInvalidEncoding() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Path sqlDirectory = repositoryRoot.resolve("docs").resolve("database");
        Files.createDirectories(sqlDirectory);
        Files.writeString(sqlDirectory.resolve("large.sql"), repeatedLines(1_500), StandardCharsets.UTF_8);
        Files.write(sqlDirectory.resolve("invalid.sql"), new byte[]{(byte) 0xFF, (byte) 0xFE, 'B', 'A', 'D'});

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertFalse(containsViolationForPathAndRule(
            violations,
            "docs/database/large.sql",
            FileHealthRule.MAX_LINES));
        assertFalse(containsViolationForPathAndRule(
            violations,
            "docs/database/large.sql",
            FileHealthRule.MAX_SIZE));
        assertTrue(containsViolationForPathAndRule(
            violations,
            "docs/database/invalid.sql",
            FileHealthRule.UTF_8_DECODE));
    }

    @Test
    void shouldRejectExemptionCountGrowthBeyondBaseline() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Files.writeString(
            repositoryRoot.resolve("docs").resolve("file-health-exemptions.properties"),
            """
            version=1
            exemption.1.glob=docs/database/*.sql
            exemption.1.waive=MAX_LINES,MAX_SIZE
            exemption.1.reason=test sql exemption
            exemption.2.glob=docs/plans/*.md
            exemption.2.waive=MAX_LINES
            exemption.2.reason=test plan exemption
            """,
            StandardCharsets.UTF_8);

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.EXEMPTION_CONFIG);
    }

    @Test
    void shouldRejectExemptionWithoutReason() throws IOException {
        Path repositoryRoot = createRepositoryRoot();
        Files.writeString(
            repositoryRoot.resolve("docs").resolve("file-health-exemptions.properties"),
            """
            version=1
            exemption.1.glob=docs/database/*.sql
            exemption.1.waive=MAX_LINES,MAX_SIZE
            exemption.1.reason=
            """,
            StandardCharsets.UTF_8);

        List<FileHealthViolation> violations = createChecker(repositoryRoot).validateRepository();

        assertContainsRule(violations, FileHealthRule.EXEMPTION_CONFIG);
    }

    private RepositoryFileHealthChecker createChecker(Path repositoryRoot) throws IOException {
        return RepositoryFileHealthChecker.forRepositoryRoot(repositoryRoot);
    }

    private Path createRepositoryRoot() throws IOException {
        Files.createDirectories(tempDir.resolve("docs"));
        Files.createDirectories(tempDir.resolve("common"));
        Files.createDirectories(tempDir.resolve("user-center"));
        Files.writeString(tempDir.resolve("pom.xml"), "<project/>", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("README.md"), "# temp\n", StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve("docs").resolve("file-health-exemptions.properties"),
            """
            version=1
            exemption.1.glob=docs/database/*.sql
            exemption.1.waive=MAX_LINES,MAX_SIZE
            exemption.1.reason=test sql exemption
            """,
            StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve("docs").resolve("file-health-baseline.properties"),
            """
            version=1
            file.health.max-exemptions=1
            """,
            StandardCharsets.UTF_8);
        return tempDir;
    }

    private static boolean containsViolationForPathAndRule(
        List<FileHealthViolation> violations,
        String path,
        FileHealthRule rule
    ) {
        return violations.stream().anyMatch(violation -> violation.path().equals(path) && violation.rule() == rule);
    }

    private static String repeatedLines(int lineCount) {
        StringBuilder builder = new StringBuilder(lineCount * 8);
        for (int index = 0; index < lineCount; index++) {
            builder.append("line-").append(index).append('\n');
        }
        return builder.toString();
    }

    private static void assertContainsRule(
        List<FileHealthViolation> violations,
        FileHealthRule rule
    ) {
        assertTrue(violations.stream().anyMatch(violation -> violation.rule() == rule));
    }

}
