package com.company.cli.support;

import lombok.Getter;
import picocli.CommandLine.Option;

@Getter
public class OutputOptionMixin {

    @Option(
        names = "--output",
        defaultValue = "text",
        description = "Output format: ${COMPLETION-CANDIDATES}.")
    private OutputFormat outputFormat;
}
