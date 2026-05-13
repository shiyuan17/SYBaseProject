package com.company.cli.support;

import picocli.CommandLine.Option;

public class OutputOptionMixin {

    @Option(
        names = "--output",
        defaultValue = "text",
        description = "Output format: ${COMPLETION-CANDIDATES}.")
    private OutputFormat outputFormat;

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }
}

