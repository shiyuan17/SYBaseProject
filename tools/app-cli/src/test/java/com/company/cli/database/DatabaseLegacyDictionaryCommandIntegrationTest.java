package com.company.cli.database;

import com.company.cli.CliApplication;
import com.company.cli.support.CliCommandLineFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = CliApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class DatabaseLegacyDictionaryCommandIntegrationTest {

    @Autowired
    private CliCommandLineFactory cliCommandLineFactory;

    @MockBean
    private DatabaseLegacyDictionaryGenerator generator;

    @Test
    void shouldWriteLegacyHtmlToRequestedOutputPath() throws Exception {
        Path output = Files.createTempFile("database-dictionary-legacy-", ".html");
        Files.deleteIfExists(output);
        given(generator.generate(any())).willReturn(new DatabaseDictionaryGenerationResult(
            "<!DOCTYPE html><html><body>legacy</body></html>",
            new DatabaseDictionaryReport(
                Instant.parse("2026-06-26T08:00:00Z"),
                DatabaseDictionaryReportStatus.OK,
                DatabaseDictionaryScope.VISIBLE_ALL,
                List.of(new DatabaseSourceReport(List.of("bl-center"), "jdbc:dm://127.0.0.1:5236", "SYSDBA", List.of())),
                new DatabaseDictionarySummary(1, 0, 0, 0, 0, 0))));

        CommandExecutionResult result = execute(
            "database", "dictionary-html-legacy",
            "--output", output.toString(),
            "--targets", "bl-center");

        assertThat(result.exitCode()).withFailMessage(result.stderr()).isZero();
        assertThat(Files.readString(output)).contains("legacy");
        assertThat(result.stdout()).contains(output.toString());
    }

    private CommandExecutionResult execute(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = cliCommandLineFactory
            .create(
                new PrintWriter(stdout, true, StandardCharsets.UTF_8),
                new PrintWriter(stderr, true, StandardCharsets.UTF_8))
            .execute(args);
        return new CommandExecutionResult(
            exitCode,
            stdout.toString(StandardCharsets.UTF_8),
            stderr.toString(StandardCharsets.UTF_8));
    }

    private record CommandExecutionResult(int exitCode, String stdout, String stderr) {
    }
}
