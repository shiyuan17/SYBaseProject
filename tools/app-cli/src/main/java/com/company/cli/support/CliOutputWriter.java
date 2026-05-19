package com.company.cli.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.io.PrintWriter;

@Component
@RequiredArgsConstructor
public class CliOutputWriter {

    private final ObjectMapper objectMapper;

    public void write(CommandSpec spec, OutputFormat outputFormat, Object jsonData, String textOutput) throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        if (outputFormat == OutputFormat.JSON) {
            out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));
        } else {
            out.println(textOutput);
        }
        out.flush();
    }

    public void writeError(CommandLine commandLine, OutputFormat outputFormat, CliErrorResponse errorResponse) throws IOException {
        PrintWriter err = commandLine.getErr();
        if (outputFormat == OutputFormat.JSON) {
            err.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse));
        } else {
            err.printf("code: %s%nmessage: %s%n", errorResponse.code(), errorResponse.message());
        }
        err.flush();
    }
}
