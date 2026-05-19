package com.company.common.test.filehealth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryFileHealthGateTest {

    @Test
    void repositoryShouldFollowFileHealthRules() throws IOException {
        Path repositoryRoot = RepositoryFileHealthChecker.locateRepositoryRoot(Path.of("").toAbsolutePath());
        RepositoryFileHealthChecker checker = RepositoryFileHealthChecker.forRepositoryRoot(repositoryRoot);

        List<FileHealthViolation> violations = checker.validateRepository();

        assertTrue(violations.isEmpty(), () -> checker.formatViolations(violations));
    }
}
