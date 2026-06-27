package com.company.cli.database;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "database",
    description = "Database tooling commands.",
    subcommands = {
        DatabaseDictionaryHtmlCommand.class,
        DatabaseLegacyDictionaryHtmlCommand.class
    }
)
public class DatabaseCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "dictionary-html", description = "Generate a Dameng database dictionary HTML report.")
class DatabaseDictionaryHtmlCommand implements Callable<Integer> {

    private static final DateTimeFormatter OUTPUT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final DatabaseDictionaryGenerator generator;

    @Option(
        names = "--targets",
        defaultValue = "auth-center,bl-center",
        description = "Comma-separated datasource targets. Default: ${DEFAULT-VALUE}.")
    private String targets;

    @Option(
        names = "--output",
        description = "HTML output path. Defaults to docs/reports/database-dictionary-<timestamp>.html")
    private Path outputPath;

    @Option(
        names = "--scope",
        defaultValue = "visible-all",
        description = "Metadata scope. Supported values: visible-all.")
    private String scope;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        Path resolvedOutput = outputPath == null ? defaultOutputPath() : outputPath.toAbsolutePath().normalize();
        DatabaseDictionaryRequest request = new DatabaseDictionaryRequest(
            parseTargets(targets),
            resolvedOutput,
            DatabaseDictionaryScope.fromCliValue(scope));
        DatabaseDictionaryGenerationResult result = generator.generate(request);
        writeHtml(resolvedOutput, result.html());

        DatabaseDictionarySummary summary = result.report().summary();
        spec.commandLine().getOut().printf(
            """
                generated: %s
                scope: %s
                sources: %d
                owners: %d
                tables: %d
                columns: %d
                indexes: %d
                foreignKeys: %d
                status: %s
                %n""",
            resolvedOutput,
            result.report().scope(),
            summary.sourceCount(),
            summary.ownerCount(),
            summary.tableCount(),
            summary.columnCount(),
            summary.indexCount(),
            summary.foreignKeyCount(),
            result.report().status());
        spec.commandLine().getOut().flush();
        return 0;
    }

    private Path defaultOutputPath() {
        String fileName = "database-dictionary-" + LocalDateTime.now().format(OUTPUT_TIMESTAMP) + ".html";
        return resolveRepositoryRoot().resolve("docs").resolve("reports").resolve(fileName).normalize();
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path cursor = current;
        while (cursor != null) {
            boolean hasGit = Files.exists(cursor.resolve(".git"));
            boolean hasRootPom = Files.exists(cursor.resolve("pom.xml"));
            boolean hasToolsModule = Files.exists(cursor.resolve("tools").resolve("app-cli").resolve("pom.xml"));
            if (hasGit || (hasRootPom && hasToolsModule)) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    private void writeHtml(Path output, String html) {
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, html, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw DatabaseDictionaryException.outputWriteFailed(output, ex);
        }
    }

    private List<String> parseTargets(String rawTargets) {
        return java.util.Arrays.stream(rawTargets.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }
}

@Component
@RequiredArgsConstructor
@Command(name = "dictionary-html-legacy", description = "Generate the legacy Dameng database dictionary HTML report.")
class DatabaseLegacyDictionaryHtmlCommand implements Callable<Integer> {

    private final DatabaseLegacyDictionaryGenerator generator;

    @Option(
        names = "--targets",
        defaultValue = "auth-center,bl-center",
        description = "Comma-separated datasource targets. Default: ${DEFAULT-VALUE}.")
    private String targets;

    @Option(
        names = "--output",
        description = "HTML output path. Defaults to docs/reports/DM数据表.html")
    private Path outputPath;

    @Option(
        names = "--scope",
        defaultValue = "visible-all",
        description = "Metadata scope. Supported values: visible-all.")
    private String scope;

    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        Path resolvedOutput = outputPath == null ? defaultOutputPath() : outputPath.toAbsolutePath().normalize();
        DatabaseDictionaryRequest request = new DatabaseDictionaryRequest(
            parseTargets(targets),
            resolvedOutput,
            DatabaseDictionaryScope.fromCliValue(scope));
        DatabaseDictionaryGenerationResult result = generator.generate(request);
        writeHtml(resolvedOutput, result.html());

        DatabaseDictionarySummary summary = result.report().summary();
        spec.commandLine().getOut().printf(
            """
                generated: %s
                scope: %s
                sources: %d
                owners: %d
                tables: %d
                columns: %d
                indexes: %d
                foreignKeys: %d
                status: %s
                %n""",
            resolvedOutput,
            result.report().scope(),
            summary.sourceCount(),
            summary.ownerCount(),
            summary.tableCount(),
            summary.columnCount(),
            summary.indexCount(),
            summary.foreignKeyCount(),
            result.report().status());
        spec.commandLine().getOut().flush();
        return 0;
    }

    private Path defaultOutputPath() {
        return resolveRepositoryRoot().resolve("docs").resolve("reports").resolve("DM数据表.html").normalize();
    }

    private Path resolveRepositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        Path cursor = current;
        while (cursor != null) {
            boolean hasGit = Files.exists(cursor.resolve(".git"));
            boolean hasRootPom = Files.exists(cursor.resolve("pom.xml"));
            boolean hasToolsModule = Files.exists(cursor.resolve("tools").resolve("app-cli").resolve("pom.xml"));
            if (hasGit || (hasRootPom && hasToolsModule)) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        return current;
    }

    private void writeHtml(Path output, String html) {
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(output, html, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw DatabaseDictionaryException.outputWriteFailed(output, ex);
        }
    }

    private List<String> parseTargets(String rawTargets) {
        return java.util.Arrays.stream(rawTargets.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }
}
