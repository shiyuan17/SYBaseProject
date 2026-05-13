package com.company.cli.support;

import com.company.common.core.enums.CommonErrorCode;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class CliParameterExceptionHandler implements CommandLine.IParameterExceptionHandler {

    private final CliOutputWriter outputWriter;

    public CliParameterExceptionHandler(CliOutputWriter outputWriter) {
        this.outputWriter = outputWriter;
    }

    @Override
    public int handleParseException(CommandLine.ParameterException ex, String[] args) throws Exception {
        CommandLine commandLine = ex.getCommandLine();
        OutputFormat outputFormat = OutputFormat.from(commandLine.getParseResult());
        if (outputFormat == OutputFormat.JSON) {
            outputWriter.writeError(
                commandLine,
                outputFormat,
                new CliErrorResponse(CommonErrorCode.VALIDATION_ERROR.code(), ex.getMessage()));
        } else {
            commandLine.getErr().println(ex.getMessage());
            commandLine.usage(commandLine.getErr());
            commandLine.getErr().flush();
        }
        return 2;
    }
}
