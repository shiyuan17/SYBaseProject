package com.company.cli.support;

import picocli.CommandLine.ParseResult;

public enum OutputFormat {
    TEXT,
    JSON;

    public static OutputFormat from(ParseResult parseResult) {
        if (parseResult == null) {
            return TEXT;
        }
        Object matchedValue = parseResult.matchedOptionValue("output", "text");
        return matchedValue instanceof String value && "json".equalsIgnoreCase(value) ? JSON : TEXT;
    }
}
