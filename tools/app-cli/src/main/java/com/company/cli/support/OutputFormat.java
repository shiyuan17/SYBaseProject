package com.company.cli.support;

import picocli.CommandLine.ParseResult;

public enum OutputFormat {
    TEXT,
    JSON;

    public static OutputFormat from(ParseResult parseResult) {
        if (parseResult == null) {
            return TEXT;
        }
        String matchedValue = parseResult.matchedOptionValue("output", "text");
        return "json".equalsIgnoreCase(matchedValue) ? JSON : TEXT;
    }
}

