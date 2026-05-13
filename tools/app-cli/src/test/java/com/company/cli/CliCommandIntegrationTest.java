package com.company.cli;

import com.company.cli.support.CliCommandLineFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CliApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CliCommandIntegrationTest {

    @Autowired
    private CliCommandLineFactory cliCommandLineFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldShowHelp() {
        CommandExecutionResult result = execute("--help");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("sybase");
        assertThat(result.stdout()).contains("user");
    }

    @Test
    void shouldPrintVersionInJson() throws Exception {
        CommandExecutionResult result = execute("version", "--output=json");

        assertThat(result.exitCode()).isZero();
        JsonNode jsonNode = objectMapper.readTree(result.stdout());
        assertThat(jsonNode.get("application").asText()).isEqualTo("app-cli");
        assertThat(jsonNode.get("javaVersion").asText()).isNotBlank();
    }

    @Test
    void shouldReportHealth() throws Exception {
        CommandExecutionResult result = execute("health", "--output=json");

        assertThat(result.exitCode()).isZero();
        JsonNode jsonNode = objectMapper.readTree(result.stdout());
        assertThat(jsonNode.get("status").asText()).isEqualTo("UP");
        assertThat(jsonNode.get("checks").isArray()).isTrue();
    }

    @Test
    void shouldCreateAndGetUser() throws Exception {
        CommandExecutionResult createResult = execute(
            "user", "create",
            "--name", "Alice",
            "--email", "alice@example.com",
            "--output=json");

        assertThat(createResult.exitCode()).isZero();
        String createdUserId = objectMapper.readTree(createResult.stdout()).get("userId").asText();

        CommandExecutionResult getResult = execute(
            "user", "get",
            "--id", createdUserId,
            "--output=json");

        assertThat(getResult.exitCode()).isZero();
        JsonNode jsonNode = objectMapper.readTree(getResult.stdout());
        assertThat(jsonNode.get("id").asText()).isEqualTo(createdUserId);
        assertThat(jsonNode.get("email").asText()).isEqualTo("alice@example.com");
    }

    @Test
    void shouldReturnBusinessErrorForInvalidEmail() {
        CommandExecutionResult result = execute(
            "user", "create",
            "--name", "Alice",
            "--email", "bad-email",
            "--output=json");

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stderr()).contains("INVALID_USER_EMAIL");
    }

    @Test
    void shouldReturnBusinessErrorWhenUserDoesNotExist() {
        CommandExecutionResult result = execute(
            "user", "get",
            "--id", "missing-user",
            "--output=json");

        assertThat(result.exitCode()).isEqualTo(3);
        assertThat(result.stderr()).contains("USER_NOT_FOUND");
    }

    @Test
    void shouldReturnParameterErrorWhenEmailIsMissing() {
        CommandExecutionResult result = execute(
            "user", "create",
            "--name", "Alice");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr()).contains("Missing required option");
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
